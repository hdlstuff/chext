package chext.amba.axi4.full.components

import chext.amba.axi4
import chext.elastic

import chisel3._
import chisel3.util._
import chisel3.experimental._

import elastic._
import elastic.ConnectOp._

import axi4.Casts._
import axi4.full.{SlaveBuffer, MasterBuffer, WriteDataChannel}

case class IdMuxConfig(
    val axiCfgSlave: axi4.Config,
    val wIdSel: Int,
    val arbiterPolicy: Chooser.ChooserFn = Chooser.rr
) {
  require(!axiCfgSlave.lite)
  require(axiCfgSlave.read || axiCfgSlave.write)
  require(wIdSel >= 0)

  val numSlaves = 1 << wIdSel
  val axiCfgMaster = axiCfgSlave.copy(wId = axiCfgSlave.wId + wIdSel)
}

class IdMux(val cfg: IdMuxConfig) extends Module {
  import cfg._

  val s_axi = IO(Vec(numSlaves, axi4.full.Slave(axiCfgSlave)))
  val m_axi = IO(axi4.full.Master(axiCfgMaster))

  private val s_axi_ = {
    val result = Wire(Vec(numSlaves, axi4.full.Interface(axiCfgMaster)))

    if (axiCfgSlave.read)
      helpers.IdExtend.read(s_axi, result)

    if (axiCfgSlave.write)
      helpers.IdExtend.write(s_axi, result)

    result
  }
  private val m_axi_ = m_axi

  private val genSelect = UInt(wIdSel.W)

  private def implRead(): Unit = prefix("read") {
    def arLogic: Unit = {
      elastic.Arbiter(s_axi_.map { _.ar }, m_axi_.ar, arbiterPolicy)
    }

    def rLogic: Unit = {
      val demuxInput = Wire(Irrevocable(m_axi_.r.bits.cloneType))
      val demuxSelect = Wire(Irrevocable(genSelect))

      new Fork(m_axi_.r) {
        protected def onFork: Unit = {
          fork { in } :=> demuxInput
          fork { in.id >> axiCfgSlave.wId } :=> demuxSelect
        }
      }

      // R channel supports burst interleaving, so no isLastFn
      elastic.Demux(demuxInput, s_axi_.map { _.r }, demuxSelect)
    }

    arLogic
    rLogic
  }

  private def implWrite(): Unit = prefix("write") {
    val portQueue = Module(new Queue(genSelect, 32, flow = true, pipe = true))

    def awLogic: Unit = {
      elastic.Arbiter(
        s_axi_.map { _.aw },
        m_axi_.aw,
        arbiterPolicy,
        Some(portQueue.io.enq)
      )
    }

    def wLogic: Unit = {
      // W channel does not support burst interleaving due to the selection logic
      // so isLastFn
      elastic.Mux(
        s_axi_.map { _.w },
        m_axi_.w,
        portQueue.io.deq,
        isLastFn = (x: WriteDataChannel) => x.last
      )
    }

    def bLogic: Unit = {
      val demuxInput = Wire(Irrevocable(m_axi_.b.bits.cloneType))
      val demuxSelect = Wire(Irrevocable(genSelect))

      new Fork(m_axi_.b) {
        protected def onFork: Unit = {
          fork { in } :=> demuxInput
          fork { in.id >> axiCfgSlave.wId } :=> demuxSelect
        }
      }

      elastic.Demux(demuxInput, s_axi_.map { _.b }, demuxSelect)
    }

    awLogic
    wLogic
    bLogic
  }

  if (axiCfgSlave.read) implRead()
  if (axiCfgSlave.write) implWrite()
}
