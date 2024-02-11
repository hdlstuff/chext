package chext.axi4.full.components

import chext.{axi4, elastic, bundles}

import chisel3._
import chisel3.util._
import chisel3.experimental._

import elastic._
import elastic.ConnectOp._

import axi4.Casts._
import axi4.BufferConfig
import axi4.full.{SlaveBuffer, MasterBuffer, ReadDataChannel, WriteDataChannel}

import bundles._

import elastic.{Chooser, OnPacket, Fork}
import chext.axi4.Config

/** Manages (1) the number of outstanding requests, (2) the port that a thread
  * is associated with.
  *
  * @param wIdTracked
  * @param wOutstanding
  */
private[components] class TransactionTracker(
    val wIdTracked: Int,
    val wPort: Int,
    val wOutstanding: Int
) extends Module {
  require(wIdTracked > 0)
  require(wPort > 0)
  require(wOutstanding > 0)

  class IdCountPort extends Bundle {
    val id = Input(UInt(wIdTracked.W))
    val count = Output(UInt(wOutstanding.W))
    val port = Output(UInt(wPort.W))
  }

  class EnId extends Bundle {
    val en = Input(Bool())
    val id = Input(UInt(wIdTracked.W))
  }

  class EnIdPort extends Bundle {
    val en = Input(Bool())
    val id = Input(UInt(wIdTracked.W))
    val port = Input(UInt(wPort.W))
  }

  val io = IO(new Bundle {
    val initiate = new EnIdPort
    val complete = new EnId
    val query = new IdCountPort
  })

  def noQuery() = {
    io.query.id := DontCare
  }

  def noComplete() = {
    io.complete.en := false.B
    io.complete.id := DontCare
  }

  def complete(id: UInt) = {
    io.complete.en := true.B
    io.complete.id := id
  }

  def noInitiate() = {
    io.initiate.en := false.B
    io.initiate.id := DontCare
    io.initiate.port := DontCare
  }

  def initiate(id: UInt, port: UInt) = {
    io.initiate.en := true.B
    io.initiate.id := id
    io.initiate.port := port
  }

  def getCountPort(id: UInt): (UInt, UInt) = {
    io.query.id := id
    (io.query.count, io.query.port)
  }

  def canInitiate(id: UInt, port: UInt): Bool = {
    val (count_, port_) = getCountPort(id)
    count_ === 0.U || port_ === port
  }

  private val outstandingMax = (-1).S(wOutstanding.W).asUInt
  private val numIds = 1 << wIdTracked
  private val tableNumOutstanding = RegInit(
    VecInit( //
      Seq.fill(numIds) {
        0.U(wOutstanding.W)
      }
    )
  )
  private val tablePort = RegInit(
    VecInit(Seq.fill(numIds) { 0.U(wPort.W) })
  )

  when(io.initiate.en) {
    tablePort(io.initiate.id) := io.initiate.port
  }

  when(io.initiate.id =/= io.complete.id) {
    when(io.initiate.en) {
      tableNumOutstanding(io.initiate.id) :=
        tableNumOutstanding(io.initiate.id) + 1.U
    }

    when(io.complete.en) {
      tableNumOutstanding(io.complete.id) :=
        tableNumOutstanding(io.complete.id) - 1.U
    }
  }.otherwise {
    when(io.initiate.en && !io.complete.en) {
      tableNumOutstanding(io.initiate.id) :=
        tableNumOutstanding(io.initiate.id) + 1.U
    }.elsewhen(!io.initiate.en && io.complete.en) {
      tableNumOutstanding(io.initiate.id) :=
        tableNumOutstanding(io.initiate.id) - 1.U
    }
  }

  io.query.count := tableNumOutstanding(io.query.id)
  io.query.port := tablePort(io.query.id)
}

case class DemuxConfig(
    val numIdsTrackedRead: Int = 4,
    val numIdsTrackedWrite: Int = 4,
    val numOutstandingRead: Int = 16,
    val numOutstandingWrite: Int = 16,
    val capacityPortQueueW: Int = 8,
    val slaveBuffers: BufferConfig = BufferConfig.all(1),
    val masterBuffers: BufferConfig = BufferConfig.all(0),
    val arbiterPolicy: Chooser.ChooserFn = Chooser.rr
) {
  require(numIdsTrackedRead > 0)
  require(numIdsTrackedWrite > 0)
  require(numOutstandingRead > 0)
  require(numOutstandingWrite > 0)

  val wIdTrackedRead: Int = log2Ceil(numIdsTrackedRead + 1)
  val wIdTrackedWrite: Int = log2Ceil(numIdsTrackedWrite + 1)
  val wOutstandingRead: Int = log2Ceil(numOutstandingRead + 1)
  val wOutstandingWrite: Int = log2Ceil(numOutstandingWrite + 1)
}

class Demux(
    val axiCfg: chext.axi4.Config,
    val numMasters: Int = 4,
    val decodeFn: (UInt) => (UInt),
    val demuxCfg: DemuxConfig = DemuxConfig()
) extends Module {
  require(!axiCfg.lite)
  require(axiCfg.read || axiCfg.write)
  require(numMasters > 0)

  override def desiredName: String = "axi4FullDemux"

  val S_AXI = IO(axi4.Slave(axiCfg))
  val M_AXI = IO(Vec(numMasters, axi4.Master(axiCfg)))

  private val wPort = log2Up(numMasters)
  private val genPort = UInt(wPort.W)

  private def implRead(): Unit = prefix("read") {
    val transactionTracker = Module(
      new TransactionTracker(
        demuxCfg.wIdTrackedRead,
        wPort,
        demuxCfg.wOutstandingRead
      )
    )

    transactionTracker.noQuery()
    transactionTracker.noComplete()
    transactionTracker.noInitiate()

    val s_axi = SlaveBuffer(S_AXI.asFull, demuxCfg.slaveBuffers)
    val m_axi = M_AXI.map { (x) =>
      MasterBuffer(x.asFull, demuxCfg.masterBuffers)
    }

    def arLogic: Unit = {
      val genArPort = new Bundle2(s_axi.ar.bits.cloneType, genPort)
      val arPort = Wire(Irrevocable(genArPort))

      new OnPacket(s_axi.ar, arPort) {
        override protected def onPacket: Unit = {
          val id = bits.id
          val addr = bits.addr
          val port = decodeFn(addr)

          when(transactionTracker.canInitiate(id, port)) {
            transactionTracker.initiate(id, port)
            accept(WireBundleN(bits, port))
          }.otherwise {
            noAccept()
          }
        }
      }

      val demuxInput = Wire(Irrevocable(s_axi.ar.bits.cloneType))
      val demuxSelect = Wire(Irrevocable(genPort))

      new Fork(arPort) {
        override protected def onFork = {
          fork(in._1) :=> demuxInput
          fork(in._2) :=> demuxSelect
        }
      }

      chext.elastic.Demux(
        demuxInput,
        m_axi.map { _.ar },
        demuxSelect
      )
    }

    def rLogic: Unit = {
      chext.elastic.Arbiter(
        m_axi.map { _.r },
        s_axi.r,
        demuxCfg.arbiterPolicy,
        isLastFn = (x: ReadDataChannel) => x.last
      )

      when(s_axi.r.fire && s_axi.r.bits.last) {
        transactionTracker.complete(s_axi.r.bits.id)
      }
    }

    arLogic
    rLogic
  }

  private def implWrite(): Unit = prefix("write") {
    val transactionTracker = Module(
      new TransactionTracker(
        demuxCfg.wIdTrackedRead,
        wPort,
        demuxCfg.wOutstandingRead
      )
    )

    transactionTracker.noQuery()
    transactionTracker.noComplete()
    transactionTracker.noInitiate()

    val s_axi = SlaveBuffer(S_AXI.asFull, demuxCfg.slaveBuffers)
    val m_axi = M_AXI.map { (x) =>
      MasterBuffer(x.asFull, demuxCfg.masterBuffers)
    }

    val portQueue = Module(
      new Queue(
        genPort,
        demuxCfg.capacityPortQueueW,
        flow = true,
        pipe = true
      )
    )

    def awLogic: Unit = {
      val genAwPort = new Bundle2(s_axi.aw.bits.cloneType, genPort)
      val awPort = Wire(Irrevocable(genAwPort))

      new OnPacket(s_axi.aw, awPort) {
        protected def onPacket: Unit = {
          val id = bits.id
          val addr = bits.addr
          val port = decodeFn(addr)

          when(transactionTracker.canInitiate(id, port)) {
            transactionTracker.initiate(id, port)
            accept(WireBundleN(bits, port))
          }.otherwise {
            noAccept()
          }
        }
      }

      val demuxInput = Wire(Irrevocable(s_axi.aw.bits.cloneType))
      val demuxSelect = Wire(Irrevocable(genPort))

      new Fork(awPort) {
        override protected def onFork = {
          fork(in._1) :=> demuxInput
          fork(in._2) :=> demuxSelect
          fork(in._2) :=> portQueue.io.enq
        }
      }

      chext.elastic.Demux(demuxInput, m_axi.map { _.aw }, demuxSelect)
    }

    def wLogic: Unit = {
      chext.elastic.Demux(
        s_axi.w,
        m_axi.map { _.w },
        portQueue.io.deq,
        isLastFn = (x: WriteDataChannel) => x.last
      )
    }

    def bLogic: Unit = {
      chext.elastic.Arbiter(m_axi.map { _.b }, s_axi.b, demuxCfg.arbiterPolicy)

      when(s_axi.b.fire) {
        transactionTracker.complete(s_axi.b.bits.id)
      }
    }

    awLogic
    wLogic
    bLogic
  }

  if (axiCfg.read) implRead()
  if (axiCfg.write) implWrite()
}

object DemuxEmitter extends App {
  def demuxModule = new Demux(
    Config(
      wId = 4,
      wAddr = 32,
      wData = 256,
      read = true,
      write = true,
      lite = false
    ),
    numMasters = 8,
    decodeFn = (_ >> 8)
  )

  emitVerilog(demuxModule, Array("--target-dir", "output/"))
}
