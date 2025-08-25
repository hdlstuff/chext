package chext.amba.axi4.lite.components

import chisel3._
import chisel3.util._

import chext.elastic
import elastic.ConnectOp._

import chext.amba.axi4
import axi4.Casts._
import axi4.lite.{SlaveBuffer, MasterBuffer}

import chext.Prefix.prefix

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

  val s_axil = IO(Vec(numSlaves, axi4.lite.Slave(axiSlaveCfg)))
  val m_axil = IO(axi4.lite.Master(axiMasterCfg))

  private val genPort = UInt(wPort.W)

  private val s_axil_ = s_axil.map { (x) =>
    SlaveBuffer(x, slaveBuffers)
  }
  private val m_axil_ = MasterBuffer(m_axil, masterBuffers)

  private def implRead(): Unit = prefix("read") {
    val portQueue = elastic.Queue(
      genPort,
      capacityPortQueueR,
      flow = true,
      pipe = true
    )

    def arLogic: Unit = {
      elastic.Arbiter(
        s_axil_.map { _.ar },
        m_axil_.ar,
        arbiterPolicy,
        Some(portQueue.source)
      )
    }

    def rLogic: Unit = {
      elastic.Demux(m_axil_.r, s_axil_.map { _.r }, portQueue.sink)
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
      val arbiterSelect = Wire(elastic.Interface(genPort))

      elastic.Arbiter(
        s_axil_.map { _.aw },
        m_axil_.aw,
        arbiterPolicy,
        Some(arbiterSelect)
      )

      new elastic.Fork(arbiterSelect) {
        fork() :=> portQueueW.source
        fork() :=> portQueueB.source
      }
    }

    def wLogic: Unit = {
      elastic.Mux(s_axil_.map { _.w }, m_axil_.w, portQueueW.sink)
    }

    def bLogic: Unit = {
      elastic.Demux(m_axil_.b, s_axil_.map { _.b }, portQueueB.sink)
    }

    awLogic
    wLogic
    bLogic
  }

  if (axiSlaveCfg.read) implRead()
  if (axiSlaveCfg.write) implWrite()
}
