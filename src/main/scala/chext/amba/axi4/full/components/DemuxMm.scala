package chext.amba.axi4.full.components

import chisel3._
import chisel3.experimental.{prefix, SourceInfo}
import chisel3.util._

import chext.amba.axi4
import chext.amba.axi4.full.WriteDataChannel
import chext.amba.axi4.util.{Decoder, MemoryMap}
import chext.bundles._
import chext.elastic
import elastic.ConnectOp._

import io.circe.generic.auto._

/** Configuration for [[DemuxMm]]. Address decoding is derived from the memory maps resolved at the
  * master interfaces.
  */
case class DemuxMmConfig(
    val axiSlaveCfg: axi4.Config,
    val numMasters: Int = 4,
    val numIdsTrackedRead: Int = 4,
    val numIdsTrackedWrite: Int = 4,
    val numOutstandingRead: Int = 16,
    val numOutstandingWrite: Int = 16,
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
  val wPort: Int = log2Ceil(numMasters)
  val axiMasterCfg: axi4.Config = axiSlaveCfg
}

/** Memory-map-driven AXI4 full demultiplexer.
  *
  * Read and write decoding remain independent, allowing AR and AW transactions to be decoded
  * concurrently. Call [[genDecoder]] after the memory maps of the connected slave devices can be
  * resolved at the master interfaces to attach combinational map-based decoders.
  */
class DemuxMm(val cfg: DemuxMmConfig) extends Module with chext.AnnotatedModule {
  import cfg._
  private val require_ = chext.util.Require.inferred()

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.full.Master.many(numMasters, axiMasterCfg))

  private val readDec_req = IO(elastic.Sink(UInt(axiSlaveCfg.wAddr.W)))
  private val readDec_resp = IO(elastic.Source(new Decoder.Response(axiSlaveCfg.wAddr, wPort)))
  private val writeDec_req = IO(elastic.Sink(UInt(axiSlaveCfg.wAddr.W)))
  private val writeDec_resp = IO(elastic.Source(new Decoder.Response(axiSlaveCfg.wAddr, wPort)))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)
  declareAxi4Interface(m_axi)
  declareElasticInterface(readDec_req, "Address")
  declareElasticInterface(readDec_resp, "Port")
  declareElasticInterface(writeDec_req, "Address")
  declareElasticInterface(writeDec_resp, "Port")

  private val resolver = new DemuxMm_Resolver(this)

  private val genPort = UInt(wPort.W)

  private def decodedAddress[T <: Data](
      source: elastic.Interface[T],
      address: T => UInt,
      request: elastic.Interface[UInt],
      response: elastic.Interface[Decoder.Response],
      sink: elastic.Interface[Bundle2[T, UInt]]
  ): Unit = {
    // TODO Add a buffer on the fork branch that retains pending address-channel
    // payloads, allowing multiple decoder requests to be in flight.
    val pending = elastic.EWire.like(source)

    val forkDecode = new elastic.Fork(source) {
      fork() :=> pending
      fork(address(in)) :=> request
    }

    val joinDecode = new elastic.Join(sink) {
      val pendingAddress = join(pending)
      val decoded = join(response)

      out._1 := pendingAddress
      address(out._1) := decoded.address
      out._2 := decoded.port
    }
  }

  private def implRead(): Unit = prefix("read") {
    val transactionTracker = Module(
      new helpers.TransactionTracker(wIdTrackedRead, wPort, wOutstandingRead)
    )

    transactionTracker.noQuery()
    transactionTracker.noComplete()
    transactionTracker.noInitiate()

    val genArPort = new Bundle2(s_axi.ar.$bits.cloneType, genPort)
    val decodedAr = elastic.EWire(genArPort)
    decodedAddress(
      s_axi.ar,
      (channel: axi4.full.AddressChannel) => channel.addr,
      readDec_req,
      readDec_resp,
      decodedAr
    )

    val arPort = elastic.EWire.like(decodedAr)
    val stall0 = new elastic.Stall(decodedAr, arPort) {
      val id = in._1.id
      val port = in._2

      out := in
      cond { !transactionTracker.canInitiate(id, port) }
      fire { transactionTracker.initiate(id, port) }
    }

    val demuxInput = elastic.EWire.like(s_axi.ar)
    val demuxSelect = elastic.EWire(genPort)
    val forkAr = new elastic.Fork(arPort) {
      fork(in._1) :=> demuxInput
      fork(in._2) :=> demuxSelect
    }
    val demux0 = new elastic.Demux(demuxInput, m_axi.map(_.ar), demuxSelect)

    val arbiter0 = new elastic.ArbiterNs(m_axi.map(_.r), s_axi.r, arbiterPolicy) {
      fire {
        when(s_axi.r.$bits.last) {
          transactionTracker.complete(s_axi.r.$bits.id)
        }
      }
    }
  }

  private def implWrite(): Unit = prefix("write") {
    val transactionTracker = Module(
      new helpers.TransactionTracker(wIdTrackedWrite, wPort, wOutstandingWrite)
    )

    transactionTracker.noQuery()
    transactionTracker.noComplete()
    transactionTracker.noInitiate()

    val queuePort = elastic.Queue(genPort, numOutstandingWrite, flow = true, pipe = true)
    val genAwPort = new Bundle2(s_axi.aw.$bits.cloneType, genPort)
    val decodedAw = elastic.EWire(genAwPort)
    decodedAddress(
      s_axi.aw,
      (channel: axi4.full.AddressChannel) => channel.addr,
      writeDec_req,
      writeDec_resp,
      decodedAw
    )

    val awPort = elastic.EWire.like(decodedAw)
    val stall0 = new elastic.Stall(decodedAw, awPort) {
      val id = in._1.id
      val port = in._2

      out := in
      cond { !transactionTracker.canInitiate(id, port) }
      fire { transactionTracker.initiate(id, port) }
    }

    val demuxAwInput = elastic.EWire.like(s_axi.aw)
    val demuxAwSelect = elastic.EWire.like(queuePort.sink)
    val forkAw = new elastic.Fork(awPort) {
      fork(in._1) :=> demuxAwInput
      fork(in._2) :=> demuxAwSelect
      fork(in._2) :=> queuePort.source
    }
    val demux0 = new elastic.Demux(demuxAwInput, m_axi.map(_.aw), demuxAwSelect)

    val demux1 = new elastic.Demux(s_axi.w, m_axi.map(_.w), queuePort.sink) {
      last { (x: WriteDataChannel) => x.last }
    }

    val arbiter0 = new elastic.ArbiterNs(m_axi.map(_.b), s_axi.b, arbiterPolicy) {
      fire {
        transactionTracker.complete(s_axi.b.$bits.id)
      }
    }
  }

  if (axiSlaveCfg.read) implRead()
  else
    prefix("read") {
      val nullSourceReq = new elastic.NullSource(readDec_req)
      val stallSinkResp = new elastic.StallSink(readDec_resp)
    }

  if (axiSlaveCfg.write) implWrite()
  else
    prefix("write") {
      val nullSourceReq = new elastic.NullSource(writeDec_req)
      val stallSinkResp = new elastic.StallSink(writeDec_resp)
    }

  private var decoderGenerated = false

  /** Aggregates the resolved `Slave.MemoryMap` property of each master interface, creates the
    * applicable combinational decoders, and connects them to this demultiplexer. The optional
    * permutation selects the order in which mapped master interfaces appear in the address map. An
    * optional error-slave interface is excluded from aggregation and selected for addresses outside
    * all mapped segments.
    *
    * This method is called from the parent module after constructing `DemuxMm`.
    */
  def genDecoder(
      permutation: Option[Seq[Int]] = None,
      errorSlave: Option[Int] = None,
      allocationScheme: MemoryMap.AllocationScheme =
        MemoryMap.AllocationScheme.AlignedPacked,
      memoryMapPath: Seq[String] = Seq.empty,
      captureResolutionTrace: Boolean = false
  ): MemoryMap = {
    require_(!decoderGenerated, "DemuxMm.genDecoder() must be called at most once")
    errorSlave.foreach { index =>
      require_(
        index >= 0 && index < numMasters,
        "Error-slave index must be a valid m_axi index"
      )
    }

    val mappedIndices = m_axi.indices.filterNot(errorSlave.contains)
    val order = permutation.getOrElse(mappedIndices)
    require_(
      order.distinct.sorted == mappedIndices,
      "Permutation must contain every non-error m_axi index exactly once"
    )

    val memoryMaps = order.map { index =>
      resolver.resolveMemoryMap(m_axi(index), s"m_axi($index)", captureResolutionTrace)
    }
    val aggregatedMemoryMap =
      if (memoryMaps.nonEmpty)
        MemoryMap.aggregate(memoryMaps, allocationScheme).copy(path = memoryMapPath)
      else MemoryMap(path = memoryMapPath, size = 0)
    val memoryMap = aggregatedMemoryMap.copy(
      origin = resolver.interfacePath(s_axi)
    )
    val defaultPort = errorSlave.getOrElse(order.head)
    genDecoder(memoryMap, memoryMap.children.zip(order), defaultPort)
  }

  private def genDecoder(
      memoryMap: MemoryMap,
      routes: Seq[(MemoryMap, Int)],
      defaultPort: Int
  ): MemoryMap = {
    memoryMap.validate match {
      case MemoryMap.ValidationResult.Success => ()
      case failure: MemoryMap.ValidationResult.Failure =>
        require_(false, s"Invalid aggregated memory map: ${failure.render}")
    }

    val addressSpaceSize = BigInt(1) << axiSlaveCfg.wAddr
    require_(
      memoryMap.allocatedSize <= addressSpaceSize,
      s"Aggregated memory map size 0x${memoryMap.allocatedSize.toString(16)} exceeds " +
        s"DemuxMm address space 0x${addressSpaceSize.toString(16)}"
    )

    resolver.setMemoryMap(memoryMap)

    prefix("read") {
      if (axiSlaveCfg.read) {
        val decoder = Module(new Decoder(axiSlaveCfg.wAddr, wPort, routes, defaultPort))
        readDec_req :=> decoder.dec_req
        decoder.dec_resp :=> readDec_resp
      } else {
        val stallSinkReq = new elastic.StallSink(readDec_req)
        val nullSourceResp = new elastic.NullSource(readDec_resp)
      }
    }

    prefix("write") {
      if (axiSlaveCfg.write) {
        val decoder = Module(new Decoder(axiSlaveCfg.wAddr, wPort, routes, defaultPort))
        writeDec_req :=> decoder.dec_req
        decoder.dec_resp :=> writeDec_resp
      } else {
        val stallSinkReq = new elastic.StallSink(writeDec_req)
        val nullSourceResp = new elastic.NullSource(writeDec_resp)
      }
    }

    decoderGenerated = true
    memoryMap
  }
}

/** Resolves and initializes interface properties for one [[DemuxMm]]. */
private final class DemuxMm_Resolver(owner: DemuxMm)(implicit sourceInfo: SourceInfo)
    extends axi4.tracking.Resolver(owner) {
  import axi4.tracking._

  bindSlave(owner.s_axi)
  bindMaster(owner.m_axi.toSeq)

  private var memoryMapOption = Option.empty[MemoryMap]
  private val noSlaveAggregate =
    "DemuxMm keeps each downstream slave capability separate instead of aggregating them"

  def interfacePath(interface: axi4.tracking.Tracked): String =
    TrackingPath.interface(interface)

  def resolveMemoryMap(
      interface: axi4.tracking.Tracked,
      name: String,
      captureResolutionTrace: Boolean
  ): MemoryMap = {
    val request = ResolveRequest(interface, properties.Slave.MemoryMap)
    val resolution = Resolver.resolve(request)
    resolution.result match {
      case ResolveResult.Success() => ()
      case ResolveResult.Failure(message, _) =>
        throw new IllegalArgumentException(s"Could not resolve $name slave memory map: $message")
      case ResolveResult.Retry(_) =>
        throw new AssertionError("recursive memory-map resolution unexpectedly returned Retry")
    }

    val memoryMap = request.valueOption.getOrElse {
      throw new IllegalArgumentException(s"Resolved $name slave memory map has no value")
    }
    val origin = resolution.steps.lastOption
      .map(_.interfaceTo)
      .getOrElse(TrackingPath.interface(interface))
    val traceArgs =
      if (captureResolutionTrace && resolution.steps.nonEmpty)
        Map("resolutionTrace" -> hdlinfo.TypedObject(ResolutionTrace(resolution.steps)))
      else Map.empty[String, hdlinfo.TypedObject]

    memoryMap
      .withDefaultOrigin(origin)
      .copy(args = memoryMap.args ++ traceArgs)
  }

  def setMemoryMap(memoryMap: MemoryMap): Unit = {
    memoryMapOption.foreach { existing =>
      require(
        existing == memoryMap,
        "DemuxMm slave memory map is already set to a different value"
      )
    }
    memoryMapOption = Some(memoryMap)
    owner.s_axi.slaveProps(properties.Slave.MemoryMap).enforce(memoryMap)
  }

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case SlaveRequests(MemoryMap()) =>
        request.failure(
          "DemuxMm.genDecoder() was not called before resolving slave.memoryMap"
        )
      case Request(TrafficProfile()) =>
        request.incomplete()
      case SlaveRequests(BurstShape() | ThreadMode()) =>
        request.dontCare(noSlaveAggregate)
      case MasterRequests(BurstShape() | ThreadMode()) =>
        forwardTo(request, owner.s_axi)
      case _ =>
        missingCase(request)
    }
}
