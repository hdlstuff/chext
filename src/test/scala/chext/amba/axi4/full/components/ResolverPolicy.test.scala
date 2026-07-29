package chext.amba.axi4.full.components

import chisel3._

import chext.amba.axi4
import chext.amba.axi4.BurstType.Encoding.{FIXED, INCR, WRAP}
import chext.amba.axi4.tracking.{ResolveRequest, ResolveResult, Resolver}
import chext.amba.axi4.tracking.{properties => p}
import chext.amba.axi4.tracking.values.{BurstShape, MemoryMap, ThreadMode, TrafficProfile}
import chext.util.ElaborationTest

import java.nio.file.Path

class DemuxResolverPolicyTestTop
    extends Demux(
      DemuxConfig(
        axiSlaveCfg = axi4.Config(wId = 4, wAddr = 32, wData = 64),
        numMasters = 2,
        decodeFn = _ >> 12,
        numIdsTrackedRead = 3,
        numOutstandingRead = 7
      )
    ) {
  private val burstShape = BurstShape(
    maxBeats = 16,
    types = Seq(INCR, WRAP),
    sizes = Seq(0, 1, 2, 3),
    aligned = true
  )
  private val masterReadThreadMode = ThreadMode.SingleThread
  s_axi.properties(p.MasterReadBurstShape) = burstShape
  s_axi.properties(p.MasterReadThreadMode) = masterReadThreadMode

  m_axi.foreach { output =>
    val burstRequest = ResolveRequest(output, p.MasterReadBurstShape)
    assert(Resolver.resolve(burstRequest).result == ResolveResult.Success())
    assert(burstRequest.cell.valueOption.contains(burstShape))

    val threadsRequest = ResolveRequest(output, p.MasterReadThreadMode)
    assert(Resolver.resolve(threadsRequest).result == ResolveResult.Success())
    assert(threadsRequest.cell.valueOption.contains(masterReadThreadMode))
  }

  val downstreamBurstAggregateRequest = ResolveRequest(s_axi, p.SlaveReadBurstShape)
  assert(Resolver.resolve(downstreamBurstAggregateRequest).result == ResolveResult.Success())
  assert(
    downstreamBurstAggregateRequest.cell.state match {
      case p.State.DontCare(message) => message.contains("separate")
      case _                         => false
    }
  )
}

class DemuxWriteOnlyResolverPolicyTestTop
    extends Demux(
      DemuxConfig(
        axiSlaveCfg = axi4.Config(wId = 4, wAddr = 32, wData = 64, read = false, write = true),
        numMasters = 2,
        decodeFn = _ >> 12
      )
    ) {
  private val masterWriteThreadMode = ThreadMode.SingleThread
  s_axi.properties(p.MasterWriteThreadMode) = masterWriteThreadMode

  assert(s_axi.properties(p.SlaveReadThreadMode).state == p.State.Undefined)
  val slaveReadRequest = ResolveRequest(s_axi, p.SlaveReadThreadMode)
  assert(Resolver.resolve(slaveReadRequest).result == ResolveResult.Success())
  assert(slaveReadRequest.cell.state == p.State.Undefined)

  m_axi.foreach { output =>
    assert(output.properties(p.MasterReadThreadMode).state == p.State.Undefined)
    val masterReadRequest = ResolveRequest(output, p.MasterReadThreadMode)
    assert(Resolver.resolve(masterReadRequest).result == ResolveResult.Success())
    assert(masterReadRequest.cell.state == p.State.Undefined)

    val masterWriteRequest = ResolveRequest(output, p.MasterWriteThreadMode)
    assert(Resolver.resolve(masterWriteRequest).result == ResolveResult.Success())
    assert(masterWriteRequest.cell.valueOption.contains(masterWriteThreadMode))
  }
}

class MuxResolverPolicyTestTop
    extends Mux(
      MuxConfig(
        axiSlaveCfg = axi4.Config(wId = 4, wAddr = 32, wData = 64),
        numSlaves = 2
      )
    ) {
  private val acceptedBurstShape = BurstShape(
    maxBeats = 16,
    types = Seq(INCR),
    sizes = Seq(0, 1, 2, 3),
    aligned = false
  )
  m_axi.properties(p.SlaveReadBurstShape) = acceptedBurstShape
  s_axi(0).properties(p.MasterReadBurstShape) = acceptedBurstShape
  s_axi(1).properties(p.MasterReadBurstShape) =
    BurstShape(16, Seq(INCR, WRAP), Seq(0, 1, 2, 3), false)

  s_axi.foreach { input =>
    val slaveRequest = ResolveRequest(input, p.SlaveReadBurstShape)
    assert(Resolver.resolve(slaveRequest).result == ResolveResult.Success())
    assert(slaveRequest.cell.valueOption.contains(acceptedBurstShape))
  }

  val masterAggregateRequest = ResolveRequest(m_axi, p.MasterReadBurstShape)
  assert(Resolver.resolve(masterAggregateRequest).result == ResolveResult.Success())
  assert(
    masterAggregateRequest.cell.state match {
      case p.State.DontCare(message) => message.contains("separate")
      case _                         => false
    }
  )

  val threadAggregateRequest = ResolveRequest(m_axi, p.MasterReadThreadMode)
  assert(Resolver.resolve(threadAggregateRequest).result == ResolveResult.Success())
  assert(threadAggregateRequest.cell.valueOption.contains(ThreadMode.Unconstrained))
}

class SingleMuxResolverPolicyTestTop
    extends Mux(
      MuxConfig(
        axiSlaveCfg = axi4.Config(wId = 4, wAddr = 32, wData = 64),
        numSlaves = 1
      )
    ) {
  s_axi.head.properties(p.MasterReadThreadMode) = ThreadMode.UniqueThreads

  private val request = ResolveRequest(m_axi, p.MasterReadThreadMode)
  assert(Resolver.resolve(request).result == ResolveResult.Success())
  assert(request.cell.valueOption.contains(ThreadMode.UniqueThreads))
}

class IdDemuxResolverPolicyTestTop
    extends IdDemux(
      IdDemuxConfig(
        axiSlaveCfg = axi4.Config(wId = 4, wAddr = 32, wData = 64),
        wIdSel = 1
      )
    ) {
  private val shape =
    BurstShape(8, Seq(INCR, WRAP), Seq(0, 1, 2, 3), true)
  s_axi.properties(p.MasterReadBurstShape) = shape
  s_axi.properties(p.MasterReadThreadMode) = ThreadMode.UniqueThreads

  m_axi.foreach { output =>
    val burstRequest = ResolveRequest(output, p.MasterReadBurstShape)
    assert(Resolver.resolve(burstRequest).result == ResolveResult.Success())
    assert(burstRequest.cell.valueOption.contains(shape))

    val threadRequest = ResolveRequest(output, p.MasterReadThreadMode)
    assert(Resolver.resolve(threadRequest).result == ResolveResult.Success())
    assert(threadRequest.cell.valueOption.contains(ThreadMode.UniqueThreads))
  }

  private def assertSlaveDontCare[T](key: p.Key[T]): Unit = {
    val request = ResolveRequest(s_axi, key)
    assert(Resolver.resolve(request).result == ResolveResult.Success())
    assert(request.cell.state.isInstanceOf[p.State.DontCare])
  }
  assertSlaveDontCare(p.SlaveReadBurstShape)
  assertSlaveDontCare(p.SlaveReadThreadMode)

  private val memoryMapRequest = ResolveRequest(s_axi, p.SlaveMemoryMap)
  assert(Resolver.resolve(memoryMapRequest).result == ResolveResult.Success())
  assert(memoryMapRequest.cell.state == p.State.Incomplete)
}

class ZeroIdDemuxResolverPolicyTestTop
    extends IdDemux(
      IdDemuxConfig(
        axiSlaveCfg = axi4.Config(wId = 2, wAddr = 32, wData = 64, write = false),
        wIdSel = 2
      )
    ) {
  s_axi.properties(p.MasterReadThreadMode) = ThreadMode.UniqueThreads

  m_axi.foreach { output =>
    val request = ResolveRequest(output, p.MasterReadThreadMode)
    assert(Resolver.resolve(request).result == ResolveResult.Success())
    assert(request.cell.valueOption.contains(ThreadMode.SingleThread))
  }
}

class IdMuxResolverPolicyTestTop(wIdSel: Int)
    extends IdMux(
      IdMuxConfig(
        axiSlaveCfg = axi4.Config(wId = 3, wAddr = 32, wData = 64),
        wIdSel = wIdSel
      )
    ) {
  private val accepted =
    BurstShape(16, Seq(INCR), Seq(0, 1, 2, 3), false)
  m_axi.properties(p.SlaveReadBurstShape) = accepted
  s_axi.foreach(_.properties(p.MasterReadThreadMode) = ThreadMode.SingleThread)

  s_axi.foreach { input =>
    val request = ResolveRequest(input, p.SlaveReadBurstShape)
    assert(Resolver.resolve(request).result == ResolveResult.Success())
    assert(request.cell.valueOption.contains(accepted))
  }

  private val burstRequest = ResolveRequest(m_axi, p.MasterReadBurstShape)
  assert(Resolver.resolve(burstRequest).result == ResolveResult.Success())
  assert(burstRequest.cell.state.isInstanceOf[p.State.DontCare])

  private val threadRequest = ResolveRequest(m_axi, p.MasterReadThreadMode)
  assert(Resolver.resolve(threadRequest).result == ResolveResult.Success())
  assert(
    threadRequest.cell.valueOption.contains(
      if (wIdSel == 0) ThreadMode.SingleThread else ThreadMode.Unconstrained
    )
  )
}

class IdSerializeResolverPolicyTestTop
    extends IdSerialize(
      IdSerializeConfig(
        axiSlaveCfg = axi4.Config(wId = 3, wAddr = 32, wData = 64, write = false)
      )
    ) {
  private val shape =
    BurstShape(8, Seq(INCR, WRAP), Seq(0, 1, 2, 3), true)
  private val acceptedShape =
    BurstShape(4, Seq(INCR), Seq(0, 1, 2, 3), false)
  s_axi.properties(p.MasterReadBurstShape) = shape
  s_axi.properties(p.MasterReadThreadMode) = ThreadMode.Unconstrained
  m_axi.properties(p.SlaveReadBurstShape) = acceptedShape

  private val slaveBurstRequest = ResolveRequest(s_axi, p.SlaveReadBurstShape)
  assert(Resolver.resolve(slaveBurstRequest).result == ResolveResult.Success())
  assert(slaveBurstRequest.cell.valueOption.contains(acceptedShape))

  private val slaveThreadRequest = ResolveRequest(s_axi, p.SlaveReadThreadMode)
  assert(Resolver.resolve(slaveThreadRequest).result == ResolveResult.Success())
  assert(slaveThreadRequest.cell.state.isInstanceOf[p.State.DontCare])

  private val masterBurstRequest = ResolveRequest(m_axi, p.MasterReadBurstShape)
  assert(Resolver.resolve(masterBurstRequest).result == ResolveResult.Success())
  assert(masterBurstRequest.cell.valueOption.contains(shape))

  private val masterThreadRequest = ResolveRequest(m_axi, p.MasterReadThreadMode)
  assert(Resolver.resolve(masterThreadRequest).result == ResolveResult.Success())
  assert(masterThreadRequest.cell.valueOption.contains(ThreadMode.SingleThread))
}

class IdParallelizeResolverPolicyTestTop
    extends IdParallelize(
      IdParallelizeConfig(
        axiSlaveCfg = axi4.Config(wId = 0, wAddr = 32, wData = 64, write = false),
        wIdMaster = 3,
        wBufferIndex = 3
      )
    ) {
  private val shape =
    BurstShape(8, Seq(INCR, WRAP), Seq(0, 1, 2, 3), true)
  s_axi.properties(p.MasterReadBurstShape) = shape
  s_axi.properties(p.MasterReadThreadMode) = ThreadMode.SingleThread

  private val slaveBurstRequest = ResolveRequest(s_axi, p.SlaveReadBurstShape)
  assert(Resolver.resolve(slaveBurstRequest).result == ResolveResult.Success())
  assert(
    slaveBurstRequest.cell.valueOption.contains(
      BurstShape(
        maxBeats = 8,
        types = BurstShape.supportedTypesFor(s_axi.cfg),
        sizes = BurstShape.supportedSizesFor(s_axi.cfg),
        aligned = false
      )
    )
  )

  private val masterBurstRequest = ResolveRequest(m_axi, p.MasterReadBurstShape)
  assert(Resolver.resolve(masterBurstRequest).result == ResolveResult.Success())
  assert(masterBurstRequest.cell.valueOption.contains(shape))

  private val masterThreadRequest = ResolveRequest(m_axi, p.MasterReadThreadMode)
  assert(Resolver.resolve(masterThreadRequest).result == ResolveResult.Success())
  assert(masterThreadRequest.cell.valueOption.contains(ThreadMode.UniqueThreads))
}

class CreditBufferResolverPolicyTestTop
    extends CreditBuffer(
      CreditBufferConfig(
        axiCfg = axi4.Config(wId = 0, wAddr = 32, wData = 64),
        rBuffer = 7,
        wBuffer = 5
      )
    ) {
  private val readInputShape =
    BurstShape(7, Seq(INCR, WRAP), Seq(0, 1, 2, 3), true)
  s_axi.properties(p.MasterReadBurstShape) = readInputShape
  s_axi.properties(p.MasterReadThreadMode) = ThreadMode.SingleTransaction
  private val writeInputShape =
    BurstShape(5, Seq(FIXED, INCR), Seq(0, 1, 2), false)
  s_axi.properties(p.MasterWriteBurstShape) = writeInputShape
  s_axi.properties(p.MasterWriteThreadMode) = ThreadMode.SingleThread

  private val slaveBurstRequest = ResolveRequest(s_axi, p.SlaveReadBurstShape)
  assert(Resolver.resolve(slaveBurstRequest).result == ResolveResult.Success())
  assert(
    slaveBurstRequest.cell.valueOption.contains(
      BurstShape(
        maxBeats = 7,
        types = BurstShape.supportedTypesFor(s_axi.cfg),
        sizes = BurstShape.supportedSizesFor(s_axi.cfg),
        aligned = false
      )
    )
  )

  private val slaveThreadRequest = ResolveRequest(s_axi, p.SlaveReadThreadMode)
  assert(Resolver.resolve(slaveThreadRequest).result == ResolveResult.Success())
  assert(slaveThreadRequest.cell.state.isInstanceOf[p.State.DontCare])

  private val masterBurstRequest = ResolveRequest(m_axi, p.MasterReadBurstShape)
  assert(Resolver.resolve(masterBurstRequest).result == ResolveResult.Success())
  assert(masterBurstRequest.cell.valueOption.contains(readInputShape))

  private val masterThreadRequest = ResolveRequest(m_axi, p.MasterReadThreadMode)
  assert(Resolver.resolve(masterThreadRequest).result == ResolveResult.Success())
  assert(masterThreadRequest.cell.valueOption.contains(ThreadMode.SingleTransaction))

  private val slaveWriteBurstRequest =
    ResolveRequest(s_axi, p.SlaveWriteBurstShape)
  assert(Resolver.resolve(slaveWriteBurstRequest).result == ResolveResult.Success())
  assert(
    slaveWriteBurstRequest.cell.valueOption.contains(
      BurstShape(
        maxBeats = 5,
        types = BurstShape.supportedTypesFor(s_axi.cfg),
        sizes = BurstShape.supportedSizesFor(s_axi.cfg),
        aligned = false
      )
    )
  )

  private val masterWriteBurstRequest =
    ResolveRequest(m_axi, p.MasterWriteBurstShape)
  assert(Resolver.resolve(masterWriteBurstRequest).result == ResolveResult.Success())
  assert(masterWriteBurstRequest.cell.valueOption.contains(writeInputShape))

  private val masterWriteThreadRequest =
    ResolveRequest(m_axi, p.MasterWriteThreadMode)
  assert(Resolver.resolve(masterWriteThreadRequest).result == ResolveResult.Success())
  assert(masterWriteThreadRequest.cell.valueOption.contains(ThreadMode.SingleThread))
}

class ProtocolConverterResolverPolicyTestTop
    extends ProtocolConverter(
      ProtocolConverterConfig(
        axiSlaveCfg = axi4.Config(wId = 0, wAddr = 16, wData = 64),
        axiMasterCfg = axi4.Config(wId = 0, wAddr = 16, wData = 32)
      )
    ) {
  private val outputShape = BurstShape(
    maxBeats = 1,
    types = Seq(INCR),
    sizes = Seq(BurstShape.fullSize(m_axi.cfg.wData)),
    aligned = false
  )
  private val memoryMap = MemoryMap(size = 0x100)

  s_axi.properties(p.MasterReadBurstShape) = BurstShape()
  s_axi.properties(p.MasterWriteBurstShape) = BurstShape()
  s_axi.properties(p.MasterReadThreadMode) = ThreadMode.SingleTransaction
  s_axi.properties(p.MasterWriteThreadMode) = ThreadMode.SingleTransaction
  m_axi.properties(p.SlaveReadBurstShape) = outputShape
  m_axi.properties(p.SlaveWriteBurstShape) = outputShape
  m_axi.properties(p.SlaveReadThreadMode) = ThreadMode.Unconstrained
  m_axi.properties(p.SlaveWriteThreadMode) = ThreadMode.Unconstrained
  m_axi.properties(p.SlaveMemoryMap) = memoryMap

  private val mapRequest = ResolveRequest(s_axi, p.SlaveMemoryMap)
  assert(Resolver.resolve(mapRequest).result == ResolveResult.Success())
  assert(mapRequest.cell.valueOption.contains(memoryMap))

  private val outputRequest = ResolveRequest(m_axi, p.MasterReadBurstShape)
  assert(Resolver.resolve(outputRequest).result == ResolveResult.Success())
  assert(outputRequest.cell.valueOption.contains(BurstShape()))
}

class DownscaleAlignmentResolverPolicyTestTop
    extends Downscale(
      DownscaleConfig(
        axiSlaveCfg = axi4.Config(wId = 0, wAddr = 16, wData = 64, write = false),
        wDataMaster = 32
      )
    ) {
  private val inputShape = BurstShape(
    maxBeats = 1,
    types = Seq(INCR),
    sizes = Seq(3),
    aligned = true
  )
  private val downstreamShape = BurstShape(
    maxBeats = BurstShape.maxBeatsFor(m_axi.cfg),
    types = Seq(INCR),
    sizes = Seq(2),
    aligned = true
  )
  s_axi.properties(p.MasterReadBurstShape) = inputShape
  s_axi.properties(p.MasterReadThreadMode) = ThreadMode.SingleTransaction
  m_axi.properties(p.SlaveReadBurstShape) = downstreamShape
  m_axi.properties(p.SlaveReadThreadMode) = ThreadMode.Unconstrained
  m_axi.properties(p.SlaveMemoryMap) = MemoryMap(size = 0x100)

  private val masterRequest = ResolveRequest(m_axi, p.MasterReadBurstShape)
  assert(Resolver.resolve(masterRequest).result == ResolveResult.Success())
  assert(masterRequest.cell.valueOption.exists(_.maxBeats == BurstShape.maxBeatsFor(m_axi.cfg)))
  assert(masterRequest.cell.valueOption.exists(_.types == Seq(INCR)))
  assert(masterRequest.cell.valueOption.exists(_.aligned))

  private val slaveRequest = ResolveRequest(s_axi, p.SlaveReadBurstShape)
  assert(Resolver.resolve(slaveRequest).result == ResolveResult.Success())
  assert(
    slaveRequest.cell.valueOption.contains(
      BurstShape(
        maxBeats = 1,
        types = BurstShape.supportedTypesFor(s_axi.cfg),
        sizes = BurstShape.supportedSizesFor(s_axi.cfg),
        aligned = false
      )
    )
  )

  private val threadRequest = ResolveRequest(m_axi, p.MasterReadThreadMode)
  assert(Resolver.resolve(threadRequest).result == ResolveResult.Success())
  assert(threadRequest.cell.valueOption.contains(ThreadMode.SingleTransaction))

  private val slaveTrafficRequest =
    ResolveRequest(s_axi, p.SlaveReadTrafficProfile)
  assert(Resolver.resolve(slaveTrafficRequest).result == ResolveResult.Success())
  assert(slaveTrafficRequest.cell.state.isInstanceOf[p.State.DontCare])
}

class UpscaleAlignmentResolverPolicyTestTop
    extends Upscale(
      UpscaleConfig(
        axiSlaveCfg = axi4.Config(wId = 0, wAddr = 16, wData = 32, write = false),
        wDataMaster = 64
      )
    ) {
  private val inputShape = BurstShape(
    maxBeats = 4,
    types = Seq(INCR),
    sizes = Seq(2),
    aligned = true
  )
  private val downstreamShape = BurstShape(
    maxBeats = 4,
    types = Seq(INCR),
    sizes = Seq(2),
    aligned = false
  )
  s_axi.properties(p.MasterReadBurstShape) = inputShape
  s_axi.properties(p.MasterReadThreadMode) = ThreadMode.SingleTransaction
  private val inputTrafficProfile = TrafficProfile(4, 1, Some(2.0))
  s_axi.properties(p.MasterReadTrafficProfile) = inputTrafficProfile
  m_axi.properties(p.SlaveReadBurstShape) = downstreamShape
  m_axi.properties(p.SlaveReadThreadMode) = ThreadMode.Unconstrained
  m_axi.properties(p.SlaveMemoryMap) = MemoryMap(size = 0x100)

  private val masterRequest = ResolveRequest(m_axi, p.MasterReadBurstShape)
  assert(Resolver.resolve(masterRequest).result == ResolveResult.Success())
  assert(masterRequest.cell.valueOption.contains(inputShape))
  assert(masterRequest.cell.valueOption.exists(_.aligned))

  private val slaveRequest = ResolveRequest(s_axi, p.SlaveReadBurstShape)
  assert(Resolver.resolve(slaveRequest).result == ResolveResult.Success())
  assert(slaveRequest.cell.state.isInstanceOf[p.State.DontCare])

  private val threadRequest = ResolveRequest(m_axi, p.MasterReadThreadMode)
  assert(Resolver.resolve(threadRequest).result == ResolveResult.Success())
  assert(threadRequest.cell.valueOption.contains(ThreadMode.SingleTransaction))

  private val trafficRequest = ResolveRequest(m_axi, p.MasterReadTrafficProfile)
  assert(Resolver.resolve(trafficRequest).result == ResolveResult.Success())
  assert(trafficRequest.cell.valueOption.contains(inputTrafficProfile))

  private val slaveTrafficRequest =
    ResolveRequest(s_axi, p.SlaveReadTrafficProfile)
  assert(Resolver.resolve(slaveTrafficRequest).result == ResolveResult.Success())
  assert(slaveTrafficRequest.cell.state.isInstanceOf[p.State.DontCare])
}

class UnburstAlignmentResolverPolicyTestTop
    extends Unburst(
      UnburstConfig(
        axiCfg = axi4.Config(wId = 0, wAddr = 16, wData = 64, write = false)
      )
    ) {
  private val inputShape = BurstShape(
    maxBeats = 4,
    types = Seq(INCR),
    sizes = Seq(2, 3),
    aligned = true
  )
  private val downstreamShape = BurstShape(
    maxBeats = 1,
    types = Seq(INCR),
    sizes = Seq(2, 3),
    aligned = true
  )
  s_axi.properties(p.MasterReadBurstShape) = inputShape
  s_axi.properties(p.MasterReadThreadMode) = ThreadMode.SingleTransaction
  m_axi.properties(p.SlaveReadBurstShape) = downstreamShape
  m_axi.properties(p.SlaveReadThreadMode) = ThreadMode.Unconstrained
  m_axi.properties(p.SlaveMemoryMap) = MemoryMap(size = 0x100)

  private val masterRequest = ResolveRequest(m_axi, p.MasterReadBurstShape)
  assert(Resolver.resolve(masterRequest).result == ResolveResult.Success())
  assert(masterRequest.cell.valueOption.exists(_.aligned))

  private val slaveRequest = ResolveRequest(s_axi, p.SlaveReadBurstShape)
  assert(Resolver.resolve(slaveRequest).result == ResolveResult.Success())
  assert(slaveRequest.cell.state.isInstanceOf[p.State.DontCare])

  private val threadRequest = ResolveRequest(m_axi, p.MasterReadThreadMode)
  assert(Resolver.resolve(threadRequest).result == ResolveResult.Success())
  assert(threadRequest.cell.valueOption.contains(ThreadMode.SingleThread))
}

class WidenAlignmentResolverPolicyTestTop
    extends Widen(
      WidenConfig(
        axiCfg = axi4.Config(wId = 0, wAddr = 16, wData = 64, write = false)
      )
    ) {
  private val inputShape = BurstShape(
    maxBeats = 4,
    types = Seq(INCR),
    sizes = Seq(2),
    aligned = true
  )
  private val downstreamShape = BurstShape(
    maxBeats = 16,
    types = Seq(INCR),
    sizes = Seq(3),
    aligned = false
  )
  s_axi.properties(p.MasterReadBurstShape) = inputShape
  s_axi.properties(p.MasterReadThreadMode) = ThreadMode.SingleTransaction
  m_axi.properties(p.SlaveReadBurstShape) = downstreamShape
  m_axi.properties(p.SlaveReadThreadMode) = ThreadMode.Unconstrained
  m_axi.properties(p.SlaveMemoryMap) = MemoryMap(size = 0x100)

  private val masterRequest = ResolveRequest(m_axi, p.MasterReadBurstShape)
  assert(Resolver.resolve(masterRequest).result == ResolveResult.Success())
  assert(masterRequest.cell.valueOption.exists(shape => !shape.aligned))

  private val slaveRequest = ResolveRequest(s_axi, p.SlaveReadBurstShape)
  assert(Resolver.resolve(slaveRequest).result == ResolveResult.Success())
  assert(
    slaveRequest.cell.valueOption.contains(
      BurstShape(
        maxBeats = BurstShape.maxBeatsFor(s_axi.cfg),
        types = BurstShape
          .supportedTypesFor(s_axi.cfg)
          .filterNot(
            _ == axi4.BurstType.Encoding.FIXED
          ),
        sizes = BurstShape.supportedSizesFor(s_axi.cfg),
        aligned = false
      )
    )
  )

  private val slaveThreadRequest = ResolveRequest(s_axi, p.SlaveReadThreadMode)
  assert(Resolver.resolve(slaveThreadRequest).result == ResolveResult.Success())
  assert(slaveThreadRequest.cell.state.isInstanceOf[p.State.DontCare])

  private val masterThreadRequest = ResolveRequest(m_axi, p.MasterReadThreadMode)
  assert(Resolver.resolve(masterThreadRequest).result == ResolveResult.Success())
  assert(masterThreadRequest.cell.valueOption.contains(ThreadMode.SingleTransaction))
}

class LiteDemuxResolverPolicyTestTop
    extends axi4.lite.components.Demux(
      axi4.lite.components.DemuxConfig(
        axiSlaveCfg = axi4.Config(wId = 0, wAddr = 32, wData = 64, lite = true),
        numMasters = 2,
        decodeFn = _ >> 12,
        capacityPortQueueR = 3,
        capacityPortQueueW = 5,
        capacityPortQueueB = 4
      )
    ) {
  assert(s_axil.properties(p.MasterReadBurstShape).state == p.State.Undefined)
  assert(s_axil.properties(p.MasterWriteBurstShape).state == p.State.Undefined)

  m_axil.foreach { output =>
    val burstRequest = ResolveRequest(output, p.MasterReadBurstShape)
    assert(Resolver.resolve(burstRequest).result == ResolveResult.Success())
    assert(burstRequest.cell.state == p.State.Undefined)
  }

  val trafficRequest = ResolveRequest(s_axil, p.SlaveReadTrafficProfile)
  assert(Resolver.resolve(trafficRequest).result == ResolveResult.Success())
  assert(trafficRequest.cell.state == p.State.Incomplete)
}

class LiteDemuxWriteOnlyResolverPolicyTestTop
    extends axi4.lite.components.Demux(
      axi4.lite.components.DemuxConfig(
        axiSlaveCfg =
          axi4.Config(wId = 0, wAddr = 32, wData = 64, read = false, write = true, lite = true),
        numMasters = 2,
        decodeFn = _ >> 12
      )
    ) {
  private val masterWriteThreadMode = ThreadMode.SingleThread
  s_axil.properties(p.MasterWriteThreadMode) = masterWriteThreadMode

  assert(s_axil.properties(p.SlaveReadThreadMode).state == p.State.Undefined)
  val slaveReadRequest = ResolveRequest(s_axil, p.SlaveReadThreadMode)
  assert(Resolver.resolve(slaveReadRequest).result == ResolveResult.Success())
  assert(slaveReadRequest.cell.state == p.State.Undefined)

  m_axil.foreach { output =>
    assert(output.properties(p.MasterReadThreadMode).state == p.State.Undefined)
    val masterReadRequest = ResolveRequest(output, p.MasterReadThreadMode)
    assert(Resolver.resolve(masterReadRequest).result == ResolveResult.Success())
    assert(masterReadRequest.cell.state == p.State.Undefined)

    val masterWriteRequest = ResolveRequest(output, p.MasterWriteThreadMode)
    assert(Resolver.resolve(masterWriteRequest).result == ResolveResult.Success())
    assert(masterWriteRequest.cell.valueOption.contains(masterWriteThreadMode))
  }
}

object ResolverPolicy_Test extends App with ElaborationTest {
  suite(
    name = "axi4-resolver-policy",
    outputDir = Path.of("output", "axi4_resolver_policy")
  )
  test(name = "demux", expected = Failure, gen = () => new DemuxResolverPolicyTestTop)
  test(
    name = "demux_write_only",
    expected = Failure,
    gen = () => new DemuxWriteOnlyResolverPolicyTestTop
  )
  test(name = "mux", expected = Failure, gen = () => new MuxResolverPolicyTestTop)
  test(name = "mux_single", expected = Failure, gen = () => new SingleMuxResolverPolicyTestTop)
  test(name = "id_demux", expected = Failure, gen = () => new IdDemuxResolverPolicyTestTop)
  test(
    name = "id_demux_zero_id",
    expected = Failure,
    gen = () => new ZeroIdDemuxResolverPolicyTestTop
  )
  test(name = "id_mux", expected = Failure, gen = () => new IdMuxResolverPolicyTestTop(1))
  test(name = "id_mux_single", expected = Failure, gen = () => new IdMuxResolverPolicyTestTop(0))
  test(
    name = "id_serialize",
    expected = Failure,
    gen = () => new IdSerializeResolverPolicyTestTop
  )
  test(
    name = "id_parallelize",
    expected = Failure,
    gen = () => new IdParallelizeResolverPolicyTestTop
  )
  test(
    name = "credit_buffer",
    expected = Failure,
    gen = () => new CreditBufferResolverPolicyTestTop
  )
  test(
    name = "protocol_converter",
    gen = () => new ProtocolConverterResolverPolicyTestTop
  )
  test(
    name = "downscale_alignment",
    gen = () => new DownscaleAlignmentResolverPolicyTestTop
  )
  test(
    name = "upscale_alignment",
    gen = () => new UpscaleAlignmentResolverPolicyTestTop
  )
  test(
    name = "unburst_alignment",
    gen = () => new UnburstAlignmentResolverPolicyTestTop
  )
  test(
    name = "widen_alignment",
    gen = () => new WidenAlignmentResolverPolicyTestTop
  )
  test(name = "lite_demux", expected = Failure, gen = () => new LiteDemuxResolverPolicyTestTop)
  test(
    name = "lite_demux_write_only",
    expected = Failure,
    gen = () => new LiteDemuxWriteOnlyResolverPolicyTestTop
  )
  runTests()
}
