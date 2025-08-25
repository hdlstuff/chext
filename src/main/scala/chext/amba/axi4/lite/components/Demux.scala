package chext.amba.axi4.lite.components

import chisel3._
import chisel3.util._

import chext.elastic
import elastic.ConnectOp._

import chext.bundles
import chext.bundles._

import chext.amba.axi4
import axi4.Casts._
import axi4.lite.{SlaveBuffer, MasterBuffer}

import chext.Prefix.prefix

case class DemuxConfig(
    val axiSlaveCfg: axi4.Config,
    val numMasters: Int = 4,
    val decodeFn: (UInt) => (UInt),
    val capacityPortQueueR: Int = 8,
    val capacityPortQueueW: Int = 8,
    val capacityPortQueueB: Int = 8,
    val slaveBuffers: axi4.BufferConfig = axi4.BufferConfig.all(2),
    val masterBuffers: axi4.BufferConfig = axi4.BufferConfig.all(0)
) {
  require(axiSlaveCfg.lite, "should use AXI4 lite")
  require(axiSlaveCfg.read || axiSlaveCfg.write, "must be at least read or write")
  require(numMasters > 0, "number of masters must be positive")

  require(capacityPortQueueR > 0)
  require(capacityPortQueueW > 0)
  require(capacityPortQueueB > 0)

  val wPort = log2Up(numMasters)

  val axiMasterCfg = axiSlaveCfg
}

class Demux(val cfg: DemuxConfig) extends Module {
  import cfg._

  override def desiredName: String = "axi4LiteDemux"

  val s_axil = IO(axi4.lite.Slave(axiSlaveCfg))
  val m_axil = IO(Vec(numMasters, axi4.lite.Master(axiMasterCfg)))

  private val genPort = UInt(wPort.W)

  private val s_axil_ = SlaveBuffer(s_axil, slaveBuffers)
  private val m_axil_ = m_axil.map { (x) =>
    MasterBuffer(x, masterBuffers)
  }

  private def implRead(): Unit = prefix("read") {
    val portQueue = elastic.Queue(
      genPort,
      capacityPortQueueR,
      flow = true,
      pipe = true
    )

    def arLogic: Unit = {
      val genArPort = new Bundle2(s_axil_.ar.$bits.cloneType, genPort)
      val arPort = Wire(elastic.Interface(genArPort))

      new elastic.Transform(s_axil_.ar, arPort) {
        out._1 := in
        out._2 := decodeFn(in.addr)
      }

      val demuxInput = Wire(elastic.Interface(s_axil_.ar.$bits.cloneType))
      val demuxSelect = Wire(elastic.Interface(genPort))

      new elastic.Fork(arPort) {
        fork(in._1) :=> demuxInput
        fork(in._2) :=> demuxSelect
        fork(in._2) :=> portQueue.source
      }

      elastic.Demux(demuxInput, m_axil_.map(_.ar), demuxSelect)
    }

    def rLogic: Unit = {
      elastic.Mux(m_axil_.map { _.r }, s_axil_.r, portQueue.sink)
    }

    arLogic
    rLogic
  }

  private def implWrite(): Unit = prefix("write") {
    val portQueueW = elastic.Queue(
      genPort,
      capacityPortQueueW,
      flow = true,
      pipe = true
    )

    val portQueueB = elastic.Queue(
      genPort,
      capacityPortQueueB,
      flow = true,
      pipe = true
    )

    def awLogic: Unit = {
      val genAwPort = new Bundle2(s_axil_.aw.$bits.cloneType, genPort)
      val awPort = Wire(elastic.Interface(genAwPort))

      new elastic.Transform(s_axil_.aw, awPort) {
        out._1 := in
        out._2 := decodeFn(in.addr)
      }

      val demuxAwInput = Wire(elastic.Interface(s_axil_.aw.$bits.cloneType))
      val demuxAwSelect = Wire(elastic.Interface(genPort))

      new elastic.Fork(awPort) {
        fork(in._1) :=> demuxAwInput

        fork(in._2) :=> demuxAwSelect
        fork(in._2) :=> portQueueW.source
        fork(in._2) :=> portQueueB.source
      }

      elastic.Demux(demuxAwInput, m_axil_.map { _.aw }, demuxAwSelect)
    }

    def wLogic: Unit = {
      elastic.Demux(s_axil_.w, m_axil_.map { _.w }, portQueueW.sink)
    }

    def bLogic: Unit = {
      elastic.Mux(m_axil_.map { _.b }, s_axil_.b, portQueueB.sink)
    }

    awLogic
    wLogic
    bLogic
  }

  if (axiSlaveCfg.read) implRead()
  if (axiSlaveCfg.write) implWrite()
}
