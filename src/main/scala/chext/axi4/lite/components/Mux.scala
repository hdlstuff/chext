package chext.axi4.lite.components

import chext.{axi4, elastic}

import chisel3._
import chisel3.util._
import chisel3.experimental._

import elastic._
import elastic.ConnectOp._

import axi4.Casts._
import axi4.lite.{SlaveBuffer, MasterBuffer}

case class MuxConfig(
    val capacityPortQueueR: Int = 8,
    val capacityPortQueueW: Int = 8,
    val capacityPortQueueB: Int = 8,
    val slaveBuffers: axi4.BufferConfig = axi4.BufferConfig.all(0),
    val masterBuffers: axi4.BufferConfig = axi4.BufferConfig.all(1),
    val arbiterPolicy: Chooser.ChooserFn = Chooser.rr
)

class Mux(
    val axiCfg: axi4.Config,
    val numSlaves: Int = 4,
    val muxCfg: MuxConfig = MuxConfig()
) extends Module {
  require(axiCfg.lite, "should use AXI4 lite")
  require(axiCfg.read || axiCfg.write, "must be at least read or write")
  require(numSlaves > 0, "number of slaves must be positive")

  override def desiredName: String = "axi4LiteMux"

  val s_axil = IO(Vec(numSlaves, axi4.lite.Slave(axiCfg)))
  val m_axil = IO(axi4.lite.Master(axiCfg))

  private val wPort = log2Up(numSlaves)
  private val genPort = UInt(wPort.W)

  val s_axil_ = s_axil.map { (x) =>
    SlaveBuffer(x, muxCfg.slaveBuffers)
  }
  val m_axil_ = MasterBuffer(m_axil, muxCfg.masterBuffers)

  private def implRead(): Unit = prefix("read") {
    val portQueue = Module(
      new Queue(
        genPort,
        muxCfg.capacityPortQueueR,
        flow = true,
        pipe = true
      )
    )

    def arLogic: Unit = {
      chext.elastic.Arbiter(
        s_axil_.map { _.ar },
        m_axil_.ar,
        muxCfg.arbiterPolicy,
        Some(portQueue.io.enq)
      )
    }

    def rLogic: Unit = {
      chext.elastic.Demux(m_axil_.r, s_axil_.map { _.r }, portQueue.io.deq)
    }

    arLogic
    rLogic
  }

  private def implWrite(): Unit = prefix("write") {
    val portQueueW = Module(
      new Queue(
        genPort,
        muxCfg.capacityPortQueueW,
        flow = true,
        pipe = true
      )
    )

    val portQueueB = Module(
      new Queue(
        genPort,
        muxCfg.capacityPortQueueB,
        flow = true,
        pipe = true
      )
    )

    def awLogic: Unit = {
      val arbiterSelect = Wire(Irrevocable(genPort))

      chext.elastic.Arbiter(
        s_axil_.map { _.aw },
        m_axil_.aw,
        muxCfg.arbiterPolicy,
        Some(arbiterSelect)
      )

      new Fork(arbiterSelect) {
        protected def onFork: Unit = {
          fork() :=> portQueueW.io.enq
          fork() :=> portQueueB.io.enq
        }
      }
    }

    def wLogic: Unit = {
      chext.elastic.Mux(s_axil_.map { _.w }, m_axil_.w, portQueueW.io.deq)
    }

    def bLogic: Unit = {
      chext.elastic.Demux(m_axil_.b, s_axil_.map { _.b }, portQueueB.io.deq)
    }

    awLogic
    wLogic
    bLogic
  }

  if (axiCfg.read) implRead()
  if (axiCfg.write) implWrite()
}
