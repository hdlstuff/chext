package chext.amba.axi4.full.components

import chisel3._

import chext.amba.axi4
import chext.amba.axi4.BurstType.Encoding.{INCR, WRAP}
import chext.amba.axi4.tracking.{PropertyState, ResolveRequest, ResolveResult, Resolver}
import chext.amba.axi4.tracking.properties.{Master, Slave}
import chext.amba.axi4.tracking.values.{BurstShape, MemoryMap, ThreadMode}
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
    len = 16,
    tpe = Seq(INCR, WRAP),
    size = Seq(0, 1, 2, 3),
    align = 0
  )
  private val masterReadThreadMode = ThreadMode.SingleThread
  s_axi.masterProps(Master.ReadBurstShape) = burstShape
  s_axi.masterProps(Master.ReadThreadMode) = masterReadThreadMode

  m_axi.foreach { output =>
    val burstRequest = ResolveRequest(output, Master.ReadBurstShape)
    assert(Resolver.resolve(burstRequest).result == ResolveResult.Success())
    assert(burstRequest.valueOption.contains(burstShape))

    val threadsRequest = ResolveRequest(output, Master.ReadThreadMode)
    assert(Resolver.resolve(threadsRequest).result == ResolveResult.Success())
    assert(threadsRequest.valueOption.contains(masterReadThreadMode))
  }

  val downstreamBurstAggregateRequest = ResolveRequest(s_axi, Slave.ReadBurstShape)
  assert(Resolver.resolve(downstreamBurstAggregateRequest).result == ResolveResult.Success())
  assert(
    downstreamBurstAggregateRequest.state match {
      case PropertyState.DontCare(message) => message.contains("separate")
      case _                               => false
    }
  )
}

class DemuxWriteOnlyResolverPolicyTestTop
    extends Demux(
      DemuxConfig(
        axiSlaveCfg =
          axi4.Config(wId = 4, wAddr = 32, wData = 64, read = false, write = true),
        numMasters = 2,
        decodeFn = _ >> 12
      )
    ) {
  private val masterWriteThreadMode = ThreadMode.SingleThread
  s_axi.masterProps(Master.WriteThreadMode) = masterWriteThreadMode

  assert(s_axi.slaveProps(Slave.ReadThreadMode).state == PropertyState.Undefined)
  val slaveReadRequest = ResolveRequest(s_axi, Slave.ReadThreadMode)
  assert(Resolver.resolve(slaveReadRequest).result == ResolveResult.Success())
  assert(slaveReadRequest.state == PropertyState.Undefined)

  m_axi.foreach { output =>
    assert(output.masterProps(Master.ReadThreadMode).state == PropertyState.Undefined)
    val masterReadRequest = ResolveRequest(output, Master.ReadThreadMode)
    assert(Resolver.resolve(masterReadRequest).result == ResolveResult.Success())
    assert(masterReadRequest.state == PropertyState.Undefined)

    val masterWriteRequest = ResolveRequest(output, Master.WriteThreadMode)
    assert(Resolver.resolve(masterWriteRequest).result == ResolveResult.Success())
    assert(masterWriteRequest.valueOption.contains(masterWriteThreadMode))
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
    len = 16,
    tpe = Seq(INCR),
    size = Seq(0, 1, 2, 3),
    align = 0
  )
  m_axi.slaveProps(Slave.ReadBurstShape) = acceptedBurstShape
  s_axi(0).masterProps(Master.ReadBurstShape) = acceptedBurstShape
  s_axi(1).masterProps(Master.ReadBurstShape) =
    BurstShape(16, Seq(INCR, WRAP), Seq(0, 1, 2, 3), 0)

  s_axi.foreach { input =>
    val slaveRequest = ResolveRequest(input, Slave.ReadBurstShape)
    assert(Resolver.resolve(slaveRequest).result == ResolveResult.Success())
    assert(slaveRequest.valueOption.contains(acceptedBurstShape))
  }

  val masterAggregateRequest = ResolveRequest(m_axi, Master.ReadBurstShape)
  assert(Resolver.resolve(masterAggregateRequest).result == ResolveResult.Success())
  assert(
    masterAggregateRequest.state match {
      case PropertyState.DontCare(message) => message.contains("separate")
      case _                               => false
    }
  )

  val threadAggregateRequest = ResolveRequest(m_axi, Master.ReadThreadMode)
  assert(Resolver.resolve(threadAggregateRequest).result == ResolveResult.Success())
  assert(threadAggregateRequest.valueOption.contains(ThreadMode.Unconstrained))
}

class SingleMuxResolverPolicyTestTop
    extends Mux(
      MuxConfig(
        axiSlaveCfg = axi4.Config(wId = 4, wAddr = 32, wData = 64),
        numSlaves = 1
      )
    ) {
  s_axi.head.masterProps(Master.ReadThreadMode) = ThreadMode.UniqueThreads

  private val request = ResolveRequest(m_axi, Master.ReadThreadMode)
  assert(Resolver.resolve(request).result == ResolveResult.Success())
  assert(request.valueOption.contains(ThreadMode.UniqueThreads))
}

class IdDemuxResolverPolicyTestTop
    extends IdDemux(
      IdDemuxConfig(
        axiSlaveCfg = axi4.Config(wId = 4, wAddr = 32, wData = 64),
        wIdSel = 1
      )
    ) {
  private val shape =
    BurstShape(8, Seq(INCR, WRAP), Seq(0, 1, 2, 3), 0)
  s_axi.masterProps(Master.ReadBurstShape) = shape
  s_axi.masterProps(Master.ReadThreadMode) = ThreadMode.UniqueThreads

  m_axi.foreach { output =>
    val burstRequest = ResolveRequest(output, Master.ReadBurstShape)
    assert(Resolver.resolve(burstRequest).result == ResolveResult.Success())
    assert(burstRequest.valueOption.contains(shape))

    val threadRequest = ResolveRequest(output, Master.ReadThreadMode)
    assert(Resolver.resolve(threadRequest).result == ResolveResult.Success())
    assert(threadRequest.valueOption.contains(ThreadMode.UniqueThreads))
  }

  private def assertSlaveDontCare[T](key: axi4.tracking.PropertyKey[T]): Unit = {
    val request = ResolveRequest(s_axi, key)
    assert(Resolver.resolve(request).result == ResolveResult.Success())
    assert(request.state.isInstanceOf[PropertyState.DontCare])
  }
  assertSlaveDontCare(Slave.ReadBurstShape)
  assertSlaveDontCare(Slave.ReadThreadMode)
  assertSlaveDontCare(Slave.MemoryMap)
}

class IdMuxResolverPolicyTestTop(wIdSel: Int)
    extends IdMux(
      IdMuxConfig(
        axiSlaveCfg = axi4.Config(wId = 3, wAddr = 32, wData = 64),
        wIdSel = wIdSel
      )
    ) {
  private val accepted =
    BurstShape(16, Seq(INCR), Seq(0, 1, 2, 3), 0)
  m_axi.slaveProps(Slave.ReadBurstShape) = accepted
  s_axi.foreach(_.masterProps(Master.ReadThreadMode) = ThreadMode.SingleThread)

  s_axi.foreach { input =>
    val request = ResolveRequest(input, Slave.ReadBurstShape)
    assert(Resolver.resolve(request).result == ResolveResult.Success())
    assert(request.valueOption.contains(accepted))
  }

  private val burstRequest = ResolveRequest(m_axi, Master.ReadBurstShape)
  assert(Resolver.resolve(burstRequest).result == ResolveResult.Success())
  assert(burstRequest.state.isInstanceOf[PropertyState.DontCare])

  private val threadRequest = ResolveRequest(m_axi, Master.ReadThreadMode)
  assert(Resolver.resolve(threadRequest).result == ResolveResult.Success())
  assert(
    threadRequest.valueOption.contains(
      if (wIdSel == 0) ThreadMode.SingleThread else ThreadMode.Unconstrained
    )
  )
}

class ProtocolConverterResolverPolicyTestTop
    extends ProtocolConverter(
      ProtocolConverterConfig(
        axiSlaveCfg = axi4.Config(wId = 0, wAddr = 16, wData = 64),
        axiMasterCfg = axi4.Config(wId = 0, wAddr = 16, wData = 32)
      )
    ) {
  private val outputShape = BurstShape(
    len = 1,
    tpe = Seq(INCR),
    size = Seq(BurstShape.fullSize(m_axi.cfg.wData)),
    align = 0
  )
  private val memoryMap = MemoryMap(size = 0x100)

  s_axi.masterProps(Master.ReadBurstShape) = BurstShape()
  s_axi.masterProps(Master.WriteBurstShape) = BurstShape()
  s_axi.masterProps(Master.ReadThreadMode) = ThreadMode.SingleTransaction
  s_axi.masterProps(Master.WriteThreadMode) = ThreadMode.SingleTransaction
  m_axi.slaveProps(Slave.ReadBurstShape) = outputShape
  m_axi.slaveProps(Slave.WriteBurstShape) = outputShape
  m_axi.slaveProps(Slave.ReadThreadMode) = ThreadMode.Unconstrained
  m_axi.slaveProps(Slave.WriteThreadMode) = ThreadMode.Unconstrained
  m_axi.slaveProps(Slave.MemoryMap) = memoryMap

  private val mapRequest = ResolveRequest(s_axi, Slave.MemoryMap)
  assert(Resolver.resolve(mapRequest).result == ResolveResult.Success())
  assert(mapRequest.valueOption.contains(memoryMap))

  private val outputRequest = ResolveRequest(m_axi, Master.ReadBurstShape)
  assert(Resolver.resolve(outputRequest).result == ResolveResult.Success())
  assert(outputRequest.valueOption.contains(BurstShape()))
}

class DownscaleAlignmentResolverPolicyTestTop
    extends Downscale(
      DownscaleConfig(
        axiSlaveCfg =
          axi4.Config(wId = 0, wAddr = 16, wData = 64, write = false),
        wDataMaster = 32
      )
    ) {
  private val inputShape = BurstShape(
    len = 1,
    tpe = Seq(INCR),
    size = Seq(3),
    align = 3
  )
  private val downstreamShape = BurstShape(
    len = 2,
    tpe = Seq(INCR),
    size = Seq(2),
    align = 2
  )
  s_axi.masterProps(Master.ReadBurstShape) = inputShape
  s_axi.masterProps(Master.ReadThreadMode) = ThreadMode.SingleTransaction
  m_axi.slaveProps(Slave.ReadBurstShape) = downstreamShape
  m_axi.slaveProps(Slave.ReadThreadMode) = ThreadMode.Unconstrained
  m_axi.slaveProps(Slave.MemoryMap) = MemoryMap(size = 0x100)

  private val masterRequest = ResolveRequest(m_axi, Master.ReadBurstShape)
  assert(Resolver.resolve(masterRequest).result == ResolveResult.Success())
  assert(masterRequest.valueOption.exists(_.align == 3))

  private val slaveRequest = ResolveRequest(s_axi, Slave.ReadBurstShape)
  assert(Resolver.resolve(slaveRequest).result == ResolveResult.Success())
  assert(slaveRequest.valueOption.exists(_.align == 2))
}

class UpscaleAlignmentResolverPolicyTestTop
    extends Upscale(
      UpscaleConfig(
        axiSlaveCfg =
          axi4.Config(wId = 0, wAddr = 16, wData = 32, write = false),
        wDataMaster = 64
      )
    ) {
  private val inputShape = BurstShape(
    len = 4,
    tpe = Seq(INCR),
    size = Seq(2),
    align = 2
  )
  private val downstreamShape = BurstShape(
    len = 4,
    tpe = Seq(INCR),
    size = Seq(2),
    align = 1
  )
  s_axi.masterProps(Master.ReadBurstShape) = inputShape
  s_axi.masterProps(Master.ReadThreadMode) = ThreadMode.SingleTransaction
  m_axi.slaveProps(Slave.ReadBurstShape) = downstreamShape
  m_axi.slaveProps(Slave.ReadThreadMode) = ThreadMode.Unconstrained
  m_axi.slaveProps(Slave.MemoryMap) = MemoryMap(size = 0x100)

  private val masterRequest = ResolveRequest(m_axi, Master.ReadBurstShape)
  assert(Resolver.resolve(masterRequest).result == ResolveResult.Success())
  assert(masterRequest.valueOption.exists(_.align == 2))

  private val slaveRequest = ResolveRequest(s_axi, Slave.ReadBurstShape)
  assert(Resolver.resolve(slaveRequest).result == ResolveResult.Success())
  assert(slaveRequest.valueOption.exists(_.align == 1))
}

class UnburstAlignmentResolverPolicyTestTop
    extends Unburst(
      UnburstConfig(
        axiCfg = axi4.Config(wId = 0, wAddr = 16, wData = 64, write = false)
      )
    ) {
  private val inputShape = BurstShape(
    len = 4,
    tpe = Seq(INCR),
    size = Seq(2, 3),
    align = 3
  )
  private val downstreamShape = BurstShape(
    len = 1,
    tpe = Seq(INCR),
    size = Seq(2, 3),
    align = 2
  )
  s_axi.masterProps(Master.ReadBurstShape) = inputShape
  s_axi.masterProps(Master.ReadThreadMode) = ThreadMode.SingleTransaction
  m_axi.slaveProps(Slave.ReadBurstShape) = downstreamShape
  m_axi.slaveProps(Slave.ReadThreadMode) = ThreadMode.Unconstrained
  m_axi.slaveProps(Slave.MemoryMap) = MemoryMap(size = 0x100)

  private val masterRequest = ResolveRequest(m_axi, Master.ReadBurstShape)
  assert(Resolver.resolve(masterRequest).result == ResolveResult.Success())
  assert(masterRequest.valueOption.exists(_.align == 2))

  private val slaveRequest = ResolveRequest(s_axi, Slave.ReadBurstShape)
  assert(Resolver.resolve(slaveRequest).result == ResolveResult.Success())
  assert(slaveRequest.valueOption.exists(_.align == 2))
  assert(slaveRequest.valueOption.exists(_.size == Seq(2, 3)))
}

class WidenAlignmentResolverPolicyTestTop
    extends Widen(
      WidenConfig(
        axiCfg = axi4.Config(wId = 0, wAddr = 16, wData = 64, write = false)
      )
    ) {
  private val inputShape = BurstShape(
    len = 4,
    tpe = Seq(INCR),
    size = Seq(2),
    align = 2
  )
  private val downstreamShape = BurstShape(
    len = 16,
    tpe = Seq(INCR),
    size = Seq(3),
    align = 1
  )
  s_axi.masterProps(Master.ReadBurstShape) = inputShape
  s_axi.masterProps(Master.ReadThreadMode) = ThreadMode.SingleTransaction
  m_axi.slaveProps(Slave.ReadBurstShape) = downstreamShape
  m_axi.slaveProps(Slave.ReadThreadMode) = ThreadMode.Unconstrained
  m_axi.slaveProps(Slave.MemoryMap) = MemoryMap(size = 0x100)

  private val masterRequest = ResolveRequest(m_axi, Master.ReadBurstShape)
  assert(Resolver.resolve(masterRequest).result == ResolveResult.Success())
  assert(masterRequest.valueOption.exists(_.align == 2))

  private val slaveRequest = ResolveRequest(s_axi, Slave.ReadBurstShape)
  assert(Resolver.resolve(slaveRequest).result == ResolveResult.Success())
  assert(slaveRequest.valueOption.exists(_.align == 1))
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
  assert(s_axil.masterProps(Master.ReadBurstShape).state == PropertyState.Undefined)
  assert(s_axil.masterProps(Master.WriteBurstShape).state == PropertyState.Undefined)

  m_axil.foreach { output =>
    val burstRequest = ResolveRequest(output, Master.ReadBurstShape)
    assert(Resolver.resolve(burstRequest).result == ResolveResult.Success())
    assert(burstRequest.state == PropertyState.Undefined)
  }

  val trafficRequest = ResolveRequest(s_axil, Slave.ReadTrafficProfile)
  assert(Resolver.resolve(trafficRequest).result == ResolveResult.Success())
  assert(trafficRequest.state == PropertyState.Incomplete)
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
  s_axil.masterProps(Master.WriteThreadMode) = masterWriteThreadMode

  assert(s_axil.slaveProps(Slave.ReadThreadMode).state == PropertyState.Undefined)
  val slaveReadRequest = ResolveRequest(s_axil, Slave.ReadThreadMode)
  assert(Resolver.resolve(slaveReadRequest).result == ResolveResult.Success())
  assert(slaveReadRequest.state == PropertyState.Undefined)

  m_axil.foreach { output =>
    assert(output.masterProps(Master.ReadThreadMode).state == PropertyState.Undefined)
    val masterReadRequest = ResolveRequest(output, Master.ReadThreadMode)
    assert(Resolver.resolve(masterReadRequest).result == ResolveResult.Success())
    assert(masterReadRequest.state == PropertyState.Undefined)

    val masterWriteRequest = ResolveRequest(output, Master.WriteThreadMode)
    assert(Resolver.resolve(masterWriteRequest).result == ResolveResult.Success())
    assert(masterWriteRequest.valueOption.contains(masterWriteThreadMode))
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
  test(name = "id_mux", expected = Failure, gen = () => new IdMuxResolverPolicyTestTop(1))
  test(name = "id_mux_single", expected = Failure, gen = () => new IdMuxResolverPolicyTestTop(0))
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
