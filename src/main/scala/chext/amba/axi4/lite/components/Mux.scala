package chext.amba.axi4.lite.components

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

import chext.elastic
import elastic.ConnectOp._

import chext.amba.axi4
import axi4.Casts._
import axi4.lite.{SlaveBuffered, MasterBuffered}

case class MuxConfig(
    val axiSlaveCfg: axi4.Config,
    val numSlaves: Int = 4,
    val capacityPortQueueR: Int = 8,
    val capacityPortQueueW: Int = 8,
    val capacityPortQueueB: Int = 8,
    val slaveBuffers: axi4.BufferConfig = axi4.BufferConfig.all(0),
    val masterBuffers: axi4.BufferConfig = axi4.BufferConfig.all(2),
    val arbiterPolicy: elastic.Chooser = elastic.Chooser.rr
) {
  require(axiSlaveCfg.lite, "should use AXI4 lite")
  require(axiSlaveCfg.read || axiSlaveCfg.write, "must be at least read or write")
  require(numSlaves > 0, "number of slaves must be positive")

  require(capacityPortQueueR > 0)
  require(capacityPortQueueW > 0)
  require(capacityPortQueueB > 0)

  val wPort = log2Up(numSlaves)

  val axiMasterCfg = axiSlaveCfg
}

class Mux(val cfg: MuxConfig) extends Module {
  import cfg._

  override def desiredName: String = "axi4LiteMux"

  val s_axil = IO(axi4.lite.Slave.many(numSlaves, axiSlaveCfg))
  val m_axil = IO(axi4.lite.Master(axiMasterCfg))

  private val genPort = UInt(wPort.W)

  private val s_axil_ = SlaveBuffered(s_axil, slaveBuffers)
  private val m_axil_ = MasterBuffered(m_axil, masterBuffers)

  private def implRead(): Unit = prefix("read") {
    val queuePort = elastic.Queue(
      genPort,
      capacityPortQueueR,
      flow = true,
      pipe = true
    )

    def arLogic: Unit = {
      val arbiter0 = new elastic.Arbiter(
        s_axil_.map { _.ar },
        m_axil_.ar,
        queuePort.source,
        arbiterPolicy
      )
    }

    def rLogic: Unit = {
      val demux0 = new elastic.Demux(m_axil_.r, s_axil_.map { _.r }, queuePort.sink)
    }

    arLogic
    rLogic
  }

  private def implWrite(): Unit = prefix("write") {
    val queuePortW = elastic.Queue(
      genPort,
      capacityPortQueueW,
      flow = true,
      pipe = true
    )

    val queuePortB = elastic.Queue(
      genPort,
      capacityPortQueueB,
      flow = true,
      pipe = true
    )

    def awLogic: Unit = {
      val arbiterSelect = elastic.EWire(genPort)

      val arbiter0 = new elastic.Arbiter(
        s_axil_.map { _.aw },
        m_axil_.aw,
        arbiterSelect,
        arbiterPolicy
      )

      new elastic.Fork(arbiterSelect) {
        fork() :=> queuePortW.source
        fork() :=> queuePortB.source
      }
    }

    def wLogic: Unit = {
      val mux0 = new elastic.Mux(s_axil_.map { _.w }, m_axil_.w, queuePortW.sink)
    }

    def bLogic: Unit = {
      val demux0 = new elastic.Demux(m_axil_.b, s_axil_.map { _.b }, queuePortB.sink)
    }

    awLogic
    wLogic
    bLogic
  }

  if (axiSlaveCfg.read) implRead()
  if (axiSlaveCfg.write) implWrite()
}
