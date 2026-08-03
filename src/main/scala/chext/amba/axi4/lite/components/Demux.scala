package chext.amba.axi4.lite.components

import chisel3._
import chisel3.util._
import chisel3.experimental.{prefix, SourceInfo}

import chext.elastic
import elastic.ConnectOp._

import chext.bundles._

import chext.amba.axi4
import axi4.lite.{SlaveBuffered, MasterBuffered}

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
  private val require_ = chext.util.Require.inferred()

  require_(axiSlaveCfg.lite, "should use AXI4 lite")
  require_(axiSlaveCfg.read || axiSlaveCfg.write, "must be at least read or write")
  require_(numMasters > 0, "number of masters must be positive")

  require_(capacityPortQueueR > 0)
  require_(capacityPortQueueW > 0)
  require_(capacityPortQueueB > 0)

  val wPort = log2Up(numMasters)

  val axiMasterCfg = axiSlaveCfg
}

class Demux(val cfg: DemuxConfig) extends Module with chext.AnnotatedModule {
  import cfg._

  override def desiredName: String = "axi4LiteDemux"

  val s_axil = IO(axi4.lite.Slave(axiSlaveCfg))
  val m_axil = IO(axi4.lite.Master.many(numMasters, axiMasterCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axil)
  declareAxi4Interface(m_axil)

  private val resolver = new Demux_Resolver(this)

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
      val genArPort = new Bundle2(s_axil_.ar.$bits.cloneType, genPort)
      val arPort = elastic.EWire(genArPort)

      new elastic.Transform(s_axil_.ar, arPort) {
        out._1 := in
        out._2 := decodeFn(in.addr)
      }

      val demuxInput = elastic.EWire(s_axil_.ar.$bits.cloneType)
      val demuxSelect = elastic.EWire(genPort)

      new elastic.Fork(arPort) {
        fork(in._1) :=> demuxInput
        fork(in._2) :=> demuxSelect
        fork(in._2) :=> queuePort.source
      }

      val demux0 = new elastic.Demux(demuxInput, m_axil_.map(_.ar), demuxSelect)
    }

    def rLogic: Unit = {
      val mux0 = new elastic.Mux(m_axil_.map { _.r }, s_axil_.r, queuePort.sink)
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
      val genAwPort = new Bundle2(s_axil_.aw.$bits.cloneType, genPort)
      val awPort = elastic.EWire(genAwPort)

      new elastic.Transform(s_axil_.aw, awPort) {
        out._1 := in
        out._2 := decodeFn(in.addr)
      }

      val demuxAwInput = elastic.EWire(s_axil_.aw.$bits.cloneType)
      val demuxAwSelect = elastic.EWire(genPort)

      new elastic.Fork(awPort) {
        fork(in._1) :=> demuxAwInput

        fork(in._2) :=> demuxAwSelect
        fork(in._2) :=> queuePortW.source
        fork(in._2) :=> queuePortB.source
      }

      val demux0 = new elastic.Demux(demuxAwInput, m_axil_.map { _.aw }, demuxAwSelect)
    }

    def wLogic: Unit = {
      val demux1 = new elastic.Demux(s_axil_.w, m_axil_.map { _.w }, queuePortW.sink)
    }

    def bLogic: Unit = {
      val mux0 = new elastic.Mux(m_axil_.map { _.b }, s_axil_.b, queuePortB.sink)
    }

    awLogic
    wLogic
    bLogic
  }

  if (axiSlaveCfg.read) implRead()
  if (axiSlaveCfg.write) implWrite()
}

private final class Demux_Resolver(owner: Demux)(implicit sourceInfo: SourceInfo)
    extends axi4.tracking.Resolver(owner) {
  import axi4.tracking._
  import axi4.tracking.{properties => p}

  bindSlave(owner.s_axil)
  bindMaster(owner.m_axil.toSeq)

  private val noSlaveAggregate =
    "Demux keeps the slave properties of its m_axil interfaces separate instead of aggregating them"

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case ResolveRequest(_, p.Key(p.Slave, _, p.MemoryMap)) =>
        request.incomplete()
      case ResolveRequest(_, p.Key(_, _, p.TrafficProfile)) =>
        request.incomplete()
      case ResolveRequest(_, p.Key(p.Slave, _, p.ThreadMode)) =>
        request.dontCare(noSlaveAggregate)
      case ResolveRequest(_, p.Key(p.Master, _, p.ThreadMode)) =>
        request.forwardTo(owner.s_axil)
      case _ =>
        request.missingCase()
    }
}
