package chext.amba.axi4.full.components

import chisel3._
import chisel3.experimental.{prefix, SourceInfo}

import chext.amba.axi4
import chext.elastic

import elastic.ConnectOp._
import chext.util.BitOps._

import axi4.full.{WriteDataChannel}

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

class IdDemux(val cfg: IdDemuxConfig) extends Module with chext.AnnotatedModule {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.full.Master.many(numMasters, axiMasterCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)
  declareAxi4Interface(m_axi)

  private val resolver = new IdDemux_Resolver(this)

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

private final class IdDemux_Resolver(owner: IdDemux)(implicit sourceInfo: SourceInfo)
    extends axi4.tracking.Resolver(owner) {
  import axi4.tracking._
  import axi4.tracking.{properties => p, values => v}

  bindSlave(owner.s_axi)
  bindMaster(owner.m_axi.toSeq)

  private val noSlaveBurstAggregate =
    "IdDemux keeps the slave burst properties of its m_axi interfaces separate"

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case ResolveRequest(_, p.Key(_, _, p.MemoryMap | p.TrafficProfile)) =>
        request.incomplete()
      case ResolveRequest(_, p.Key(p.Slave, _, p.BurstShape)) =>
        request.dontCare(noSlaveBurstAggregate)
      case ResolveRequest(_, p.Key(p.Slave, _, p.ThreadMode)) =>
        request.aggregateFrom(owner.m_axi.toSeq, p.ThreadMode) { downstream =>
          val aggregate = v.ThreadMode.intersect(downstream)
          if (owner.m_axi.head.cfg.wId == 0)
            v.ThreadMode.idlessBackward(aggregate)
          else
            aggregate
        }
      case ResolveRequest(_, p.Key(p.Master, _, p.BurstShape)) =>
        request.forwardTo(owner.s_axi)
      case ResolveRequest(output, p.Key(p.Master, _, p.ThreadMode)) =>
        if (output.cfg.wId == 0)
          request.mapFrom(owner.s_axi, p.ThreadMode)(v.ThreadMode.idlessForward)
        else
          request.forwardTo(owner.s_axi)
      case _ =>
        request.missingCase()
    }
}
