package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

import chext.elastic
import elastic.ConnectOp._

import chext.amba.axi4
import axi4.Casts._
import axi4.full.{SlaveBuffer, MasterBuffer, WriteDataChannel}

case class IdMuxConfig(
    val axiSlaveCfg: axi4.Config,
    val wIdSel: Int,
    val arbiterPolicy: elastic.Chooser = elastic.Chooser.rr
) {
  private val require_ = chext.util.Require.inferred()

  require_(!axiSlaveCfg.lite)
  require_(axiSlaveCfg.read || axiSlaveCfg.write)
  require_(wIdSel >= 0)

  val numSlaves = 1 << wIdSel
  val axiMasterCfg = axiSlaveCfg.copy(wId = axiSlaveCfg.wId + wIdSel)
}

class IdMux(val cfg: IdMuxConfig) extends Module {
  import cfg._

  val s_axi = IO(axi4.full.Slave.many(numSlaves, axiSlaveCfg))
  val m_axi = IO(axi4.full.Master(axiMasterCfg))

  private val s_axi_ = {
    val result = Wire(Vec(numSlaves, axi4.full.Interface(axiMasterCfg)))

    if (axiSlaveCfg.read)
      helpers.IdExtend.read(s_axi, result)

    if (axiSlaveCfg.write)
      helpers.IdExtend.write(s_axi, result)

    result
  }
  private val m_axi_ = m_axi

  private val genSelect = UInt(wIdSel.W)

  private def implRead(): Unit = prefix("read") {
    def arLogic: Unit = {
      val arbiter0 =
        new elastic.ArbiterNs(s_axi_.map { _.ar }, m_axi_.ar, arbiterPolicy)
    }

    def rLogic: Unit = {
      val demuxInput = elastic.EWire(m_axi_.r.$bits.cloneType)
      val demuxSelect = elastic.EWire(genSelect)

      new elastic.Fork(m_axi_.r) {
        fork { in } :=> demuxInput
        fork { in.id >> axiSlaveCfg.wId } :=> demuxSelect
      }

      // R channel supports burst interleaving, so no isLastFn
      val demux0 = new elastic.Demux(demuxInput, s_axi_.map { _.r }, demuxSelect)
    }

    arLogic
    rLogic
  }

  private def implWrite(): Unit = prefix("write") {
    val queuePort = elastic.Queue(genSelect, 32, flow = true, pipe = true)

    def awLogic: Unit = {
      val arbiter0 = new elastic.Arbiter(
        s_axi_.map { _.aw },
        m_axi_.aw,
        queuePort.source,
        arbiterPolicy
      )
    }

    def wLogic: Unit = {
      // W channel does not support burst interleaving due to the selection logic
      // so isLastFn
      val mux0 = new elastic.Mux(s_axi_.map { _.w }, m_axi_.w, queuePort.sink) {
        last { (x: WriteDataChannel) => x.last }
      }
    }

    def bLogic: Unit = {
      val demuxInput = elastic.EWire(m_axi_.b.$bits.cloneType)
      val demuxSelect = elastic.EWire(genSelect)

      new elastic.Fork(m_axi_.b) {
        fork { in } :=> demuxInput
        fork { in.id >> axiSlaveCfg.wId } :=> demuxSelect
      }

      val demux0 = new elastic.Demux(demuxInput, s_axi_.map { _.b }, demuxSelect)
    }

    awLogic
    wLogic
    bLogic
  }

  if (axiSlaveCfg.read) implRead()
  if (axiSlaveCfg.write) implWrite()
}
