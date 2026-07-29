package chext.amba.axi4.tracking

import chisel3._
import chisel3.experimental.{prefix, SourceInfo}

import java.nio.file.Path

import chext.amba.axi4
import chext.amba.axi4.full.ConnectOp._
import chext.amba.axi4.full.components.{
  Downscale,
  DownscaleConfig,
  IdSerialize,
  IdSerializeConfig,
  Mux => AxiMux,
  MuxConfig
}
import chext.amba.axi4.tracking.{properties => p, values => v}
import chext.{elastic, stream}
import chext.tracking.Component
import chext.util.ElaborationTest

private object CompositionDiagnosticsUtil {
  def readConfig: stream.ReadConfig[UInt] =
    stream.ReadConfig(
      axiCfg = axi4.Config(
        wId = 0,
        wAddr = 32,
        wData = 128,
        read = true,
        write = false
      ),
      resultMode = stream.ReadResultMode.DropEmpty,
      maxBurstLength = 16
    )

  def terminateStreamRead(read: stream.Read[UInt]): Unit = {
    val nullSourceTask = new elastic.NullSource(read.sourceTask)
    val nullSinkResult = new elastic.NullSink(read.sinkResult)
  }

  def terminateNarrowAxiRead(interface: axi4.full.Interface): Unit = {
    require(interface.cfg.read && !interface.cfg.write)
    require(interface.cfg.wData == 32)

    val nullSinkAr = new elastic.NullSink(interface.ar)
    val nullSourceR = new elastic.NullSource(interface.r)

    interface.properties(p.SlaveReadBurstShape) = v.BurstShape(
      maxBeats = v.BurstShape.maxBeatsFor(interface.cfg),
      types = v.BurstShape.supportedTypesFor(interface.cfg),
      sizes = v.BurstShape.supportedSizesFor(interface.cfg),
      aligned = false
    )
    interface.properties(p.SlaveReadThreadMode) = v.ThreadMode.Unconstrained
    interface.properties(p.SlaveMemoryMap) = v.MemoryMap(size = 0x10000)
  }
}

private class ReadDownscaleWithoutUnburstTop extends Module {
  import CompositionDiagnosticsUtil._

  private val readCfg = readConfig
  private val read = Module(new stream.Read(readCfg))
  private val downscale =
    Module(new Downscale(DownscaleConfig(readCfg.axiCfg, wDataMaster = 32)))

  prefix("read") {
    terminateStreamRead(read)
  }

  read.m_axi :=> downscale.s_axi

  prefix("downstream") {
    terminateNarrowAxiRead(downscale.m_axi)
  }
}

private class TwoReadsMuxIdSerializeDownscaleWithoutUnburstTop extends Module {
  import CompositionDiagnosticsUtil._

  private val readCfg = readConfig
  private val read0 = Module(new stream.Read(readCfg))
  private val read1 = Module(new stream.Read(readCfg))
  private val mux =
    Module(new AxiMux(MuxConfig(readCfg.axiCfg, numSlaves = 2)))
  private val idSerialize =
    Module(new IdSerialize(IdSerializeConfig(mux.cfg.axiMasterCfg)))
  private val downscale =
    Module(new Downscale(DownscaleConfig(idSerialize.cfg.axiMasterCfg, wDataMaster = 32)))

  prefix("read0") {
    terminateStreamRead(read0)
  }
  prefix("read1") {
    terminateStreamRead(read1)
  }

  read0.m_axi :=> mux.s_axi(0)
  read1.m_axi :=> mux.s_axi(1)
  mux.m_axi :=> idSerialize.s_axi
  idSerialize.m_axi :=> downscale.s_axi

  prefix("downstream") {
    terminateNarrowAxiRead(downscale.m_axi)
  }
}

private final class ComponentBurstLimit(
    interface: axi4.full.Interface
)(implicit val sourceInfo: SourceInfo)
    extends Component {
  def tpe: String = "ComponentBurstLimit"
  def namePrefix: String = "componentBurstLimit"

  private val resolver = new ComponentBurstLimitResolver(this, interface)
}

private final class ComponentBurstLimitResolver(
    owner: ComponentBurstLimit,
    interface: axi4.full.Interface
)(implicit sourceInfo: SourceInfo)
    extends Resolver(owner) {
  bindSlave(interface)

  interface.properties(p.SlaveReadBurstShape) = v.BurstShape(
    maxBeats = 1,
    types = v.BurstShape.supportedTypesFor(interface.cfg),
    sizes = v.BurstShape.supportedSizesFor(interface.cfg),
    aligned = false
  )
  interface.properties(p.SlaveReadThreadMode) = v.ThreadMode.Unconstrained
  interface.properties(p.SlaveMemoryMap) = v.MemoryMap(size = 0x1000)

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case ResolveRequest(_, p.Key(_, _, p.TrafficProfile)) => request.incomplete()
      case _                                                 => request.missingCase()
    }
}

private class ComponentOwnerDiagnosticTop extends Module {
  private val cfg = CompositionDiagnosticsUtil.readConfig.axiCfg
  private val interface = Wire(axi4.full.Master(cfg))

  axi4.tracking.registerView(interface)
  private val componentBurstLimit = new ComponentBurstLimit(interface)

  interface.ar.$bits := 0.U.asTypeOf(interface.ar.$bits)
  interface.ar.$valid := false.B
  interface.ar.$ready := false.B
  interface.r.$bits := 0.U.asTypeOf(interface.r.$bits)
  interface.r.$valid := false.B
  interface.r.$ready := false.B

  interface.properties(p.MasterReadBurstShape) = v.BurstShape(
    maxBeats = 16,
    types = v.BurstShape.supportedTypesFor(cfg),
    sizes = v.BurstShape.supportedSizesFor(cfg),
    aligned = false
  )
  interface.properties(p.MasterReadThreadMode) = v.ThreadMode.SingleTransaction
}

private class OwnerSourceInfoGrandchild extends Module {
  val trackingOwner: Owner = Owner(this)
  val earlyLookupFailed: Boolean =
    try {
      trackingOwner.moduleInstantiationSourceInfo
      false
    } catch {
      case _: IllegalStateException => true
    }
}

private class OwnerSourceInfoChild extends Module {
  val trackingOwner: Owner = Owner(this)
  val grandchild = Module(new OwnerSourceInfoGrandchild)
  val grandchildLookupBeforeAssignmentFailed: Boolean =
    try {
      grandchild.trackingOwner.moduleInstantiationSourceInfo
      false
    } catch {
      case _: IllegalStateException => true
    }
}

private class OwnerSourceInfoTop extends Module {
  Tag.initialize()
  private val child = Module(new OwnerSourceInfoChild)
  require(child.grandchild.earlyLookupFailed)
  require(child.grandchildLookupBeforeAssignmentFailed)
  private val childLookupBeforeAssignmentFailed =
    try {
      child.trackingOwner.moduleInstantiationSourceInfo
      false
    } catch {
      case _: IllegalStateException => true
    }
  private val grandchildLookupBeforeAssignmentFailed =
    try {
      child.grandchild.trackingOwner.moduleInstantiationSourceInfo
      false
    } catch {
      case _: IllegalStateException => true
    }
  require(childLookupBeforeAssignmentFailed)
  require(grandchildLookupBeforeAssignmentFailed)
  chext.tracking.onComplete(this) {
    require(child.trackingOwner.moduleInstantiationSourceInfo.nonEmpty)
    require(child.grandchild.trackingOwner.moduleInstantiationSourceInfo.nonEmpty)
  }
}

object CompositionDiagnostics_Test extends App with ElaborationTest {
  suite(
    name = "axi4-tracking-compositions",
    outputDir = Path.of("output", "axi4_tracking_compositions")
  )

  private val missingUnburstChecks = Seq(
    Log contains "axi4.tracking : error: burst shapes are incompatible",
    Log contains "Property: master.read_burstShape -> slave.read_burstShape",
    Log contains "Detail: master maxBeats 16 exceeds slave maxBeats 1",
    Log contains "Master trace:",
    Log contains "enforced locally at src/main/scala/chext/stream/Read.scala",
    Log contains "Slave trace:",
    Log contains "enforced at src/main/scala/chext/amba/axi4/full/components/Downscale.scala",
    Log contains "Master property enforced by:",
    Log contains "Module: chext.stream.Read",
    Log contains "Module path: /read",
    Log contains "Module defined at: src/main/scala/chext/stream/Read.scala",
    Log contains "Module instantiated at: src/test/scala/chext/amba/axi4/tracking/CompositionDiagnostics.test.scala",
    Log contains "Slave property enforced by:",
    Log contains "Module: chext.amba.axi4.full.components.Downscale",
    Log contains "Module path: /downscale",
    Log contains "Interface declared at: src/main/scala/chext/stream/Read.scala",
    Log contains "Interface declared at: src/main/scala/chext/amba/axi4/full/components/Downscale.scala",
    Log excludes "Owner:",
    Log excludes "Owner path:",
    Log excludes "Owner defined at:",
    Log excludes "elastic.tracking",
    Errors excludes "axi4.tracking"
  )

  test(
    name = "read_downscale_without_unburst",
    description =
      "A 128-bit stream Read can issue 16-beat bursts, but a direct Downscale to 32 bits accepts only single-beat input.",
    gen = () => new ReadDownscaleWithoutUnburstTop,
    checks =
      Seq(
        SystemVerilog contains "module ReadDownscaleWithoutUnburstTop",
        Log contains "/read/m_axi -> /downscale/s_axi"
      ) ++
        missingUnburstChecks
  )

  test(
    name = "two_reads_mux_id_serialize_downscale_without_unburst",
    description =
      "Two 128-bit stream Reads pass through Mux and IdSerialize before a 32-bit Downscale; the missing Unburst remains visible.",
    gen = () => new TwoReadsMuxIdSerializeDownscaleWithoutUnburstTop,
    checks =
      Seq(
        SystemVerilog contains "module TwoReadsMuxIdSerializeDownscaleWithoutUnburstTop",
        Log contains "Interface: /read0/m_axi",
        Log contains "Interface: /read1/m_axi",
        Log excludes "Interface: /mux/s_axi_0",
        Log excludes "Interface: /mux/s_axi_1",
        Log contains "/mux/s_axi_0 -> /mux/m_axi",
        Log contains "/idSerialize/s_axi -> /idSerialize/m_axi"
      ) ++ missingUnburstChecks
  )

  test(
    name = "component_owner_diagnostic",
    description =
      "Resolver-owned enforcement preserves a component owner instead of falling back to its containing module.",
    gen = () => new ComponentOwnerDiagnosticTop,
    checks = Seq(
      SystemVerilog contains "module ComponentOwnerDiagnosticTop",
      Log contains "axi4.tracking : error: burst shapes are incompatible",
      Log contains "Slave property enforced by:",
      Log contains "Component: chext.amba.axi4.tracking.ComponentBurstLimit",
      Log contains "Component path: /componentBurstLimit",
      Log contains "Component instantiated at: src/test/scala/chext/amba/axi4/tracking/CompositionDiagnostics.test.scala",
      Log contains "Module: chext.amba.axi4.tracking.ComponentOwnerDiagnosticTop",
      Log contains "Module instantiated at: (root elaboration)",
      Log contains "Interface declared at: src/test/scala/chext/amba/axi4/tracking/CompositionDiagnostics.test.scala",
      Log excludes "Owner:",
      Log excludes "Owner path:",
      Log excludes "Owner defined at:",
      Errors excludes "axi4.tracking"
    )
  )

  test(
    name = "owner_instantiation_source_info",
    description =
      "Direct and nested owner instantiation SourceInfo is unavailable before the root AXI hierarchy assignment and available afterward.",
    gen = () => new OwnerSourceInfoTop,
    checks = Seq(SystemVerilog contains "module OwnerSourceInfoTop")
  )

  runTests()
}
