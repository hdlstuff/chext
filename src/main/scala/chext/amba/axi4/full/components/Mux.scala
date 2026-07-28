package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.{prefix, SourceInfo}

import chext.elastic
import elastic.ConnectOp._

import chext.amba.axi4
import axi4.full.{SlaveBuffered, MasterBuffered, WriteDataChannel}

case class MuxConfig(
    val axiSlaveCfg: axi4.Config,
    val numSlaves: Int = 4,
    val slaveBuffers: axi4.BufferConfig = axi4.BufferConfig.all(0),
    val masterBuffers: axi4.BufferConfig = axi4.BufferConfig.all(2),
    val arbiterPolicy: elastic.Chooser = elastic.Chooser.rr
) {
  private val require_ = chext.util.Require.inferred()

  require_(!axiSlaveCfg.lite)
  require_(axiSlaveCfg.read || axiSlaveCfg.write)
  require_(numSlaves > 0)

  val wPort = log2Ceil(numSlaves)
  val axiMasterCfg = axiSlaveCfg.copy(wId = axiSlaveCfg.wId + wPort)
}

class Mux(val cfg: MuxConfig) extends Module with chext.AnnotatedModule {
  import cfg._

  private val genPort = UInt(wPort.W)

  val s_axi = IO(axi4.full.Slave.many(numSlaves, axiSlaveCfg))
  val m_axi = IO(axi4.full.Master(axiMasterCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)
  declareAxi4Interface(m_axi)

  private val resolver = new Mux_Resolver(this)

  private val s_axi_ = {
    val result = Wire(Vec(numSlaves, axi4.full.Interface(axiMasterCfg)))

    val buffered = SlaveBuffered(s_axi, slaveBuffers)

    if (axiSlaveCfg.read)
      helpers.IdExtend.read(buffered, result)

    if (axiSlaveCfg.write)
      helpers.IdExtend.write(buffered, result)

    result
  }
  private val m_axi_ = MasterBuffered(m_axi, masterBuffers)

  private def implRead(): Unit = prefix("read") {
    def arLogic: Unit = {
      val arbiterAR = new elastic.ArbiterNs(
        s_axi_.map { _.ar },
        m_axi_.ar,
        arbiterPolicy
      )
    }

    def rLogic: Unit = {
      val demuxInput = elastic.EWire(m_axi_.r.$bits.cloneType)
      val demuxSelect = elastic.EWire(UInt(wPort.W))

      val fork0 = new elastic.Fork(m_axi_.r) {
        fork { in } :=> demuxInput
        fork { in.id >> axiSlaveCfg.wId } :=> demuxSelect
      }

      // R channel supports burst interleaving, so no isLastFn
      val demuxR = new elastic.Demux(
        demuxInput,
        s_axi_.map { _.r },
        demuxSelect
      )
    }

    arLogic
    rLogic
  }

  private def implWrite(): Unit = prefix("write") {
    val queuePort = elastic.Queue(genPort, 32, flow = true, pipe = true)

    def awLogic: Unit = {
      val arbiterAW = new elastic.Arbiter(
        s_axi_.map { _.aw },
        m_axi_.aw,
        queuePort.source,
        arbiterPolicy
      )
    }

    def wLogic: Unit = {
      // W channel does not support burst interleaving due to the selection logic
      // so isLastFn
      val muxW = new elastic.Mux(s_axi_.map { _.w }, m_axi_.w, queuePort.sink) {
        last { (x: WriteDataChannel) => x.last }
      }
    }

    def bLogic: Unit = {
      val demuxInput = elastic.EWire(m_axi_.b.$bits.cloneType)
      val demuxSelect = elastic.EWire(UInt(wPort.W))

      val fork1 = new elastic.Fork(m_axi_.b) {
        fork { in } :=> demuxInput
        fork { in.id >> axiSlaveCfg.wId } :=> demuxSelect
      }

      val demuxB = new elastic.Demux(
        demuxInput,
        s_axi_.map { _.b },
        demuxSelect
      )
    }

    awLogic
    wLogic
    bLogic
  }

  if (axiSlaveCfg.read) implRead()
  if (axiSlaveCfg.write) implWrite()
}

private final class Mux_Resolver(owner: Mux)(implicit sourceInfo: SourceInfo)
    extends axi4.tracking.Resolver(owner) {
  import axi4.tracking._
  import axi4.tracking.{properties => p, values => v}

  bindSlave(owner.s_axi.toSeq)
  bindMaster(owner.m_axi)

  private val noMasterAggregate =
    "Mux keeps each upstream master's traffic properties separate instead of aggregating them"

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case ResolveRequest(_, p.Key(_, _, p.TrafficProfile)) =>
        request.incomplete()
      case ResolveRequest(_, p.Key(p.Slave, _, p.MemoryMap | p.BurstShape | p.ThreadMode)) =>
        request.forwardTo(owner.m_axi)
      case ResolveRequest(_, p.Key(p.Master, _, p.ThreadMode)) =>
        if (owner.s_axi.length == 1) request.forwardTo(owner.s_axi.head)
        else request.calculate(v.ThreadMode.Unconstrained)
      case ResolveRequest(_, p.Key(p.Master, _, p.BurstShape)) =>
        request.dontCare(noMasterAggregate)
      case _ =>
        request.missingCase()
    }
}
