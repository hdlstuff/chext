package chext.amba.axi4.full.components

import chisel3._

import chext.amba.axi4
import chext.amba.axi4.tracking.{PropertyState, ResolveRequest, ResolveResult, Resolver}
import chext.amba.axi4.tracking.properties.{Master, Slave}
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
  private val burstTypes = Set(1, 2)
  private val masterReadOutstanding = 2
  private val masterReadThreads = 2
  s_axi.masterProps(Master.ReadBurstTypes) = burstTypes
  s_axi.masterProps(Master.ReadOutstandingTransactions) = masterReadOutstanding
  s_axi.masterProps(Master.ReadThreads) = masterReadThreads

  m_axi.foreach { output =>
    val burstRequest = ResolveRequest(output, Master.ReadBurstTypes)
    assert(Resolver.resolve(burstRequest).result == ResolveResult.Success())
    assert(burstRequest.valueOption.contains(burstTypes))

    val outstandingRequest = ResolveRequest(output, Master.ReadOutstandingTransactions)
    assert(Resolver.resolve(outstandingRequest).result == ResolveResult.Success())
    assert(outstandingRequest.valueOption.contains(masterReadOutstanding))

    val threadsRequest = ResolveRequest(output, Master.ReadThreads)
    assert(Resolver.resolve(threadsRequest).result == ResolveResult.Success())
    assert(threadsRequest.valueOption.contains(masterReadThreads))
  }

  assert(
    s_axi.slaveProps(Slave.ReadOutstandingTransactions).state ==
      PropertyState.Enforced(cfg.numOutstandingRead)
  )
  assert(
    s_axi.slaveProps(Slave.ReadThreads).state ==
      PropertyState.Enforced(cfg.numIdsTrackedRead)
  )

  val localOutstandingRequest = ResolveRequest(s_axi, Slave.ReadOutstandingTransactions)
  assert(Resolver.resolve(localOutstandingRequest).result == ResolveResult.Success())
  assert(localOutstandingRequest.valueOption.contains(cfg.numOutstandingRead))

  val localThreadsRequest = ResolveRequest(s_axi, Slave.ReadThreads)
  assert(Resolver.resolve(localThreadsRequest).result == ResolveResult.Success())
  assert(localThreadsRequest.valueOption.contains(cfg.numIdsTrackedRead))

  val downstreamBurstAggregateRequest = ResolveRequest(s_axi, Slave.ReadBurstTypes)
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
  private val masterWriteThreads = 2
  s_axi.masterProps(Master.WriteThreads) = masterWriteThreads

  assert(s_axi.slaveProps(Slave.ReadThreads).state == PropertyState.Undefined)
  val slaveReadRequest = ResolveRequest(s_axi, Slave.ReadThreads)
  assert(Resolver.resolve(slaveReadRequest).result == ResolveResult.Success())
  assert(slaveReadRequest.state == PropertyState.Undefined)

  m_axi.foreach { output =>
    assert(output.masterProps(Master.ReadThreads).state == PropertyState.Undefined)
    val masterReadRequest = ResolveRequest(output, Master.ReadThreads)
    assert(Resolver.resolve(masterReadRequest).result == ResolveResult.Success())
    assert(masterReadRequest.state == PropertyState.Undefined)

    val masterWriteRequest = ResolveRequest(output, Master.WriteThreads)
    assert(Resolver.resolve(masterWriteRequest).result == ResolveResult.Success())
    assert(masterWriteRequest.valueOption.contains(masterWriteThreads))
  }
}

class MuxResolverPolicyTestTop
    extends Mux(
      MuxConfig(
        axiSlaveCfg = axi4.Config(wId = 4, wAddr = 32, wData = 64),
        numSlaves = 2
      )
    ) {
  private val acceptedBurstTypes = Set(1)
  m_axi.slaveProps(Slave.ReadBurstTypes) = acceptedBurstTypes
  s_axi(0).masterProps(Master.ReadBurstTypes) = Set(1)
  s_axi(1).masterProps(Master.ReadBurstTypes) = Set(1, 2)

  s_axi.foreach { input =>
    val slaveRequest = ResolveRequest(input, Slave.ReadBurstTypes)
    assert(Resolver.resolve(slaveRequest).result == ResolveResult.Success())
    assert(slaveRequest.valueOption.contains(acceptedBurstTypes))
  }

  val masterAggregateRequest = ResolveRequest(m_axi, Master.ReadBurstTypes)
  assert(Resolver.resolve(masterAggregateRequest).result == ResolveResult.Success())
  assert(
    masterAggregateRequest.state match {
      case PropertyState.DontCare(message) => message.contains("separate")
      case _                               => false
    }
  )
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
  private val burstTypes = Set(1)
  private val masterReadOutstanding = 2
  private val masterWriteOutstanding = 3
  s_axil.masterProps(Master.ReadBurstTypes) = burstTypes
  s_axil.masterProps(Master.ReadOutstandingTransactions) = masterReadOutstanding
  s_axil.masterProps(Master.WriteOutstandingTransactions) = masterWriteOutstanding

  m_axil.foreach { output =>
    val burstRequest = ResolveRequest(output, Master.ReadBurstTypes)
    assert(Resolver.resolve(burstRequest).result == ResolveResult.Success())
    assert(burstRequest.valueOption.contains(burstTypes))

    val readOutstandingRequest =
      ResolveRequest(output, Master.ReadOutstandingTransactions)
    assert(Resolver.resolve(readOutstandingRequest).result == ResolveResult.Success())
    assert(readOutstandingRequest.valueOption.contains(masterReadOutstanding))

    val writeOutstandingRequest =
      ResolveRequest(output, Master.WriteOutstandingTransactions)
    assert(Resolver.resolve(writeOutstandingRequest).result == ResolveResult.Success())
    assert(writeOutstandingRequest.valueOption.contains(masterWriteOutstanding))
  }

  assert(
    s_axil.slaveProps(Slave.ReadOutstandingTransactions).state ==
      PropertyState.Enforced(cfg.capacityPortQueueR)
  )
  assert(
    s_axil.slaveProps(Slave.WriteOutstandingTransactions).state ==
      PropertyState.Enforced(cfg.capacityPortQueueW min cfg.capacityPortQueueB)
  )

  val localReadOutstanding =
    ResolveRequest(s_axil, Slave.ReadOutstandingTransactions)
  assert(Resolver.resolve(localReadOutstanding).result == ResolveResult.Success())
  assert(localReadOutstanding.valueOption.contains(cfg.capacityPortQueueR))

  val localWriteOutstanding =
    ResolveRequest(s_axil, Slave.WriteOutstandingTransactions)
  assert(Resolver.resolve(localWriteOutstanding).result == ResolveResult.Success())
  assert(
    localWriteOutstanding.valueOption.contains(
      cfg.capacityPortQueueW min cfg.capacityPortQueueB
    )
  )
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
  private val masterWriteThreads = 1
  s_axil.masterProps(Master.WriteThreads) = masterWriteThreads

  assert(s_axil.slaveProps(Slave.ReadThreads).state == PropertyState.Undefined)
  val slaveReadRequest = ResolveRequest(s_axil, Slave.ReadThreads)
  assert(Resolver.resolve(slaveReadRequest).result == ResolveResult.Success())
  assert(slaveReadRequest.state == PropertyState.Undefined)

  m_axil.foreach { output =>
    assert(output.masterProps(Master.ReadThreads).state == PropertyState.Undefined)
    val masterReadRequest = ResolveRequest(output, Master.ReadThreads)
    assert(Resolver.resolve(masterReadRequest).result == ResolveResult.Success())
    assert(masterReadRequest.state == PropertyState.Undefined)

    val masterWriteRequest = ResolveRequest(output, Master.WriteThreads)
    assert(Resolver.resolve(masterWriteRequest).result == ResolveResult.Success())
    assert(masterWriteRequest.valueOption.contains(masterWriteThreads))
  }
}

object ResolverPolicy_Test extends App with ElaborationTest {
  suite(
    name = "axi4-resolver-policy",
    outputDir = Path.of("output", "axi4_resolver_policy")
  )
  test(name = "demux", gen = () => new DemuxResolverPolicyTestTop)
  test(name = "demux_write_only", gen = () => new DemuxWriteOnlyResolverPolicyTestTop)
  test(name = "mux", gen = () => new MuxResolverPolicyTestTop)
  test(name = "lite_demux", gen = () => new LiteDemuxResolverPolicyTestTop)
  test(name = "lite_demux_write_only", gen = () => new LiteDemuxWriteOnlyResolverPolicyTestTop)
  runTests()
}
