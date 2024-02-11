package chext.axi4.full.components

import chext.{axi4, elastic}

import chisel3._
import chisel3.util._
import chisel3.experimental._

import elastic._
import elastic.ConnectOp._

import axi4.Casts._
import axi4.full.{SlaveBuffer, MasterBuffer, WriteDataChannel}

case class MuxConfig(
    val slaveBuffers: axi4.BufferConfig = axi4.BufferConfig.all(0),
    val masterBuffers: axi4.BufferConfig = axi4.BufferConfig.all(1),
    val arbiterPolicy: Chooser.ChooserFn = Chooser.rr
)

private[components] object IdExtend {
  def apply(
      slaveInterfaces: Seq[axi4.full.Interface],
      axiCfgSlave: axi4.Config,
      axiCfgMaster: axi4.Config
  ): Seq[axi4.full.Interface] = {
    slaveInterfaces.zipWithIndex.map {
      case (interface, port) => {
        val result = Wire(Flipped(axi4.full.Interface(axiCfgMaster)))

        if (interface.cfg.read) {
          interface.ar :=> result.ar
          result.r :=> interface.r

          result.ar.bits.id := port.U ## interface.ar.bits.id
        }

        if (interface.cfg.write) {
          interface.aw :=> result.aw
          interface.w :=> result.w
          result.b :=> interface.b

          result.aw.bits.id := port.U ## interface.aw.bits.id
          result.w.bits.id := port.U ## interface.w.bits.id
        }

        result
      }
    }
  }
}

class Mux(
    val axiCfgSlave: axi4.Config,
    val numSlaves: Int = 4,
    val muxCfg: MuxConfig = MuxConfig()
) extends Module {
  require(!axiCfgSlave.lite)
  require(axiCfgSlave.read || axiCfgSlave.write)
  require(numSlaves > 0)

  override def desiredName: String = "axi4FullMux"

  private val wPort = log2Up(numSlaves)
  private val genPort = UInt(wPort.W)
  val axiCfgMaster = axiCfgSlave.copy(wId = axiCfgSlave.wId + wPort)

  val S_AXI = IO(Vec(numSlaves, axi4.Slave(axiCfgSlave)))
  val M_AXI = IO(axi4.Master(axiCfgMaster))

  val s_axi = IdExtend(
    S_AXI.map { (x) =>
      SlaveBuffer(x.asFull, muxCfg.slaveBuffers)
    },
    axiCfgSlave,
    axiCfgMaster
  )

  val m_axi = MasterBuffer(M_AXI.asFull, muxCfg.masterBuffers)

  private def implRead(): Unit = prefix("read") {
    def arLogic: Unit = {
      elastic.Arbiter(s_axi.map { _.ar }, m_axi.ar, muxCfg.arbiterPolicy)
    }

    def rLogic: Unit = {
      val demuxInput = Wire(Irrevocable(m_axi.r.bits.cloneType))
      val demuxSelect = Wire(Irrevocable(UInt(wPort.W)))

      new Fork(m_axi.r) {
        protected def onFork: Unit = {
          demuxInput <> fork { in }
          demuxSelect <> fork { in.id >> axiCfgSlave.wId }
        }
      }

      // NOTE we SHOULD NOT need to preserve bursts on R-arbiter
      // that might cause deadlocks
      elastic.Demux(demuxInput, s_axi.map { _.r }, demuxSelect)
    }

    arLogic
    rLogic
  }

  private def implWrite(): Unit = prefix("write") {
    val portQueue = Module(new Queue(genPort, 32, flow = true, pipe = true))

    def awLogic: Unit = {
      elastic.Arbiter(
        s_axi.map { _.aw },
        m_axi.aw,
        muxCfg.arbiterPolicy,
        Some(portQueue.io.enq)
      )
    }

    def wLogic: Unit = {
      elastic.Mux(
        s_axi.map { _.w },
        m_axi.w,
        portQueue.io.deq,
        isLastFn = (x: WriteDataChannel) => x.last
      )
    }

    def bLogic: Unit = {
      val demuxInput = Wire(Irrevocable(m_axi.b.bits.cloneType))
      val demuxSelect = Wire(Irrevocable(UInt(wPort.W)))

      new Fork(m_axi.b) {
        protected def onFork: Unit = {
          demuxInput <> fork { in }
          demuxSelect <> fork { in.id >> axiCfgSlave.wId }
        }
      }

      elastic.Demux(demuxInput, s_axi.map { _.b }, demuxSelect)
    }

    awLogic
    wLogic
    bLogic
  }

  if (axiCfgSlave.read) implRead()
  if (axiCfgSlave.write) implWrite()
}

object MuxEmitter extends App {
  def muxModule = new Mux(
    axi4.Config(
      wId = 4,
      wAddr = 32,
      wData = 256,
      read = true,
      write = true,
      lite = false
    ),
    numSlaves = 8
  )

  emitVerilog(muxModule, Array("--target-dir", "output/"))
}
