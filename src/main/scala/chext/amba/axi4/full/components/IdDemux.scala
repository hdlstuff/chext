package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

import chext.amba.axi4
import chext.elastic

import elastic.ConnectOp._
import chext.util.BitOps._

import axi4.full.{
  AddressChannel,
  WriteDataChannel,
  ReadDataChannel,
  WriteResponseChannel,
  SlaveBuffer,
  MasterBuffer
}

case class IdDemuxConfig(
    val axiSlaveCfg: chext.amba.axi4.Config,
    val wIdSel: Int,
    val capacityPortQueueW: Int = 8,
    val arbiterPolicy: elastic.Chooser = elastic.Chooser.rr
) {
  private val require_ = chext.util.Require.inferred()

  require_(!axiSlaveCfg.lite)
  require_(axiSlaveCfg.read || axiSlaveCfg.write)
  require_(wIdSel >= 0)
  require_(axiSlaveCfg.wId >= wIdSel)
  require_(capacityPortQueueW > 0)

  val numMasters = 1 << wIdSel
  val axiMasterCfg = axiSlaveCfg.copy(wId = axiSlaveCfg.wId - wIdSel)
}

class IdDemux(val cfg: IdDemuxConfig) extends Module {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.full.Master.many(numMasters, axiMasterCfg))

  private val s_axi_ = s_axi
  private val m_axi_ = m_axi

  private val genSelect = UInt(wIdSel.W)

  private def implRead(): Unit = prefix("read") {
    def arLogic: Unit = {
      val demuxInput = elastic.EWire(axi4.full.ReadAddressChannel(axiMasterCfg))
      val demuxSelect = elastic.EWire(genSelect)

      new elastic.Fork(s_axi_.ar) {
        val sel = in.id.lsbN(wIdSel)
        val ar = Wire(axi4.full.ReadAddressChannel(axiMasterCfg))

        ar := in
        ar.id := in.id.dropLsbN(wIdSel)

        fork(ar) :=> demuxInput
        fork(sel) :=> demuxSelect
      }

      val demux0 = new elastic.Demux(
        demuxInput,
        m_axi_.map { _.ar },
        demuxSelect
      )
    }

    def rLogic: Unit = {
      val r = Wire(Vec(numMasters, elastic.Interface(axi4.full.ReadDataChannel(axiSlaveCfg))))

      m_axi_.map { _.r }.zip(r).zipWithIndex.foreach {
        case ((source, sink), index) => {
          new elastic.Transform(source, sink) {
            out := in
            out.id := in.id ## index.U(wIdSel.W)
          }
        }
      }

      // R channel supports burst interleaving, so no isLastFn
      val arbiter0 = new elastic.ArbiterNs(
        r,
        s_axi_.r,
        arbiterPolicy
      )
    }

    arLogic
    rLogic
  }

  private def implWrite(): Unit = prefix("write") {
    val queuePort = elastic.Queue(
      genSelect,
      capacityPortQueueW,
      flow = true,
      pipe = true
    )

    def awLogic: Unit = {
      val demuxInput = elastic.EWire(axi4.full.WriteAddressChannel(axiMasterCfg))
      val demuxSelect = elastic.EWire(genSelect)

      new elastic.Fork(s_axi_.aw) {
        val sel = in.id.lsbN(wIdSel)
        val aw = Wire(axi4.full.WriteAddressChannel(axiMasterCfg))

        aw := in
        aw.id := in.id.dropLsbN(wIdSel)

        fork(aw) :=> demuxInput
        fork(sel) :=> demuxSelect
        fork(sel) :=> queuePort.source
      }

      val demux0 = new elastic.Demux(demuxInput, m_axi_.map { _.aw }, demuxSelect)
    }

    def wLogic: Unit = {
      // W channel does not support burst interleaving due to the selection logic
      // so isLastFn
      val demux1 = new elastic.Demux(s_axi_.w, m_axi_.map { _.w }, queuePort.sink) {
        last { (x: WriteDataChannel) => x.last }
      }
    }

    def bLogic: Unit = {
      val b = Wire(Vec(numMasters, elastic.Interface(axi4.full.WriteResponseChannel(axiSlaveCfg))))

      m_axi_.map { _.b }.zip(b).zipWithIndex.foreach {
        case ((source, sink), index) => {
          new elastic.Transform(source, sink) {
            out := in
            out.id := in.id ## index.U(wIdSel.W)
          }
        }
      }

      val arbiter0 = new elastic.ArbiterNs(
        b,
        s_axi_.b,
        arbiterPolicy
      )
    }

    awLogic
    wLogic
    bLogic
  }

  if (axiSlaveCfg.read) implRead()
  if (axiSlaveCfg.write) implWrite()
}
