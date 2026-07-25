package chext.amba.axi4.full.components

import chext.amba.axi4

import chext.elastic
import elastic.ConnectOp._

import chext.bundles

import chisel3._
import chisel3.util._
import chisel3.experimental.{prefix, SourceInfo}

import axi4.Casts._
import axi4.BufferConfig
import axi4.full.{SlaveBuffered, MasterBuffered, ReadDataChannel, WriteDataChannel}

import bundles._

case class DemuxConfig(
    val axiSlaveCfg: chext.amba.axi4.Config,
    val numMasters: Int = 4,
    val decodeFn: (UInt) => (UInt),
    val numIdsTrackedRead: Int = 4,
    val numIdsTrackedWrite: Int = 4,
    val numOutstandingRead: Int = 16,
    val numOutstandingWrite: Int = 16,
    val slaveBuffers: BufferConfig = BufferConfig.all(2),
    val masterBuffers: BufferConfig = BufferConfig.all(0),
    val arbiterPolicy: elastic.Chooser = elastic.Chooser.rr
) {
  private val require_ = chext.util.Require.inferred()

  require_(!axiSlaveCfg.lite)
  require_(axiSlaveCfg.read || axiSlaveCfg.write)
  require_(numMasters > 0)

  require_(numIdsTrackedRead > 0)
  require_(numIdsTrackedWrite > 0)
  require_(numOutstandingRead > 0)
  require_(numOutstandingWrite > 0)

  val wIdTrackedRead: Int = log2Ceil(numIdsTrackedRead + 1)
  val wIdTrackedWrite: Int = log2Ceil(numIdsTrackedWrite + 1)
  val wOutstandingRead: Int = log2Ceil(numOutstandingRead + 1)
  val wOutstandingWrite: Int = log2Ceil(numOutstandingWrite + 1)

  val wPort = log2Ceil(numMasters)

  val axiMasterCfg = axiSlaveCfg
}

class Demux(val cfg: DemuxConfig) extends Module with chext.AnnotatedModule {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.full.Master.many(numMasters, axiMasterCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)
  declareAxi4Interface(m_axi)

  private val resolver = new Demux_Resolver(this)

  private val s_axi_ = SlaveBuffered(s_axi, slaveBuffers)
  private val m_axi_ = MasterBuffered(m_axi, masterBuffers)

  private val genPort = UInt(wPort.W)

  private def implRead(): Unit = prefix("read") {
    val transactionTracker = Module(
      new helpers.TransactionTracker(
        wIdTrackedRead,
        wPort,
        wOutstandingRead
      )
    )

    transactionTracker.noQuery()
    transactionTracker.noComplete()
    transactionTracker.noInitiate()

    def arLogic: Unit = {
      val genArPort = new Bundle2(s_axi_.ar.$bits.cloneType, genPort)
      val arPort = elastic.EWire(genArPort)

      val stall0 = new elastic.Stall(s_axi_.ar, arPort) {
        val id = in.id
        val addr = in.addr
        val port = decodeFn(addr)

        out._1 := in
        out._2 := port

        cond { !transactionTracker.canInitiate(id, port) }
        fire { transactionTracker.initiate(id, port) }
      }

      val demuxInput = elastic.EWire(s_axi_.ar.$bits.cloneType)
      val demuxSelect = elastic.EWire(genPort)

      val fork0 = new elastic.Fork(arPort) {
        fork(in._1) :=> demuxInput
        fork(in._2) :=> demuxSelect
      }

      val demux0 = new elastic.Demux(
        demuxInput,
        m_axi_.map { _.ar },
        demuxSelect
      )
    }

    def rLogic: Unit = {
      // R channel supports burst interleaving, so no isLastFn
      val arbiter0 = new elastic.ArbiterNs(
        m_axi_.map { _.r },
        s_axi_.r,
        arbiterPolicy
      )

      when(s_axi_.r.fire && s_axi_.r.$bits.last) {
        transactionTracker.complete(s_axi_.r.$bits.id)
      }
    }

    arLogic
    rLogic
  }

  private def implWrite(): Unit = prefix("write") {
    val transactionTracker = Module(
      new helpers.TransactionTracker(
        wIdTrackedWrite,
        wPort,
        wOutstandingWrite
      )
    )

    transactionTracker.noQuery()
    transactionTracker.noComplete()
    transactionTracker.noInitiate()

    val queuePort = elastic.Queue(
      genPort,
      numOutstandingWrite,
      flow = true,
      pipe = true
    )

    def awLogic: Unit = {
      val genAwPort = new Bundle2(s_axi_.aw.$bits.cloneType, genPort)
      val awPort = elastic.EWire(genAwPort)

      val stall0 = new elastic.Stall(s_axi_.aw, awPort) {
        val id = in.id
        val addr = in.addr
        val port = decodeFn(addr)

        out._1 := in
        out._2 := port

        cond { !transactionTracker.canInitiate(id, port) }
        fire { transactionTracker.initiate(id, port) }
      }

      val demuxInput = elastic.EWire(s_axi_.aw.$bits.cloneType)
      val demuxSelect = elastic.EWire(genPort)

      val fork0 = new elastic.Fork(awPort) {
        fork(in._1) :=> demuxInput
        fork(in._2) :=> demuxSelect
        fork(in._2) :=> queuePort.source
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
      val arbiter0 = new elastic.ArbiterNs(
        m_axi_.map { _.b },
        s_axi_.b,
        arbiterPolicy
      )

      when(s_axi_.b.fire) {
        transactionTracker.complete(s_axi_.b.$bits.id)
      }
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
  import axi4.tracking.properties.Slave

  private val SlaveRequests = bindSlave(owner.s_axi)
  private val MasterRequests = bindMaster(owner.m_axi.toSeq)

  override def kind: String = "demux"
  override def resolver: String = "DemuxResolver"

  private val noSlaveAggregate =
    "Demux keeps each downstream slave capability separate instead of aggregating them"

  private val read = owner.cfg.axiSlaveCfg.read
  private val write = owner.cfg.axiSlaveCfg.write

  if (read) {
    owner.s_axi.slaveProps(Slave.ReadOutstandingTransactions) =
      owner.cfg.numOutstandingRead
    owner.s_axi.slaveProps(Slave.ReadThreads) =
      owner.cfg.numIdsTrackedRead
  }
  if (write) {
    owner.s_axi.slaveProps(Slave.WriteOutstandingTransactions) =
      owner.cfg.numOutstandingWrite
    owner.s_axi.slaveProps(Slave.WriteThreads) =
      owner.cfg.numIdsTrackedWrite
  }

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case SlaveRequests(Slave.MemoryMap) =>
        request.incomplete()
      case SlaveRequests(_) =>
        request.dontCare(noSlaveAggregate)
      case MasterRequests(_, _) =>
        forwardTo(request, owner.s_axi)
      case _ =>
        request.failure(
          s"Demux cannot resolve '${request.qualifiedName}' from this endpoint"
        )
    }
}
