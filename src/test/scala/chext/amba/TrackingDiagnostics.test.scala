package chext.amba

import chisel3._
import chisel3.experimental.{prefix, SourceInfo}
import java.nio.file.Path

import chext.{elastic => e}
import chext.elastic.ConnectOp._
import chext.amba.{axi4, axi4s}
import chext.amba.axi4.Casts._
import chext.amba.axi4.ConnectOp._
import chext.amba.axi4.tracking.{properties => p, values => v}
import chext.amba.axi4.full.{ConnectOp => Axi4FullConnectOp}
import chext.amba.axi4.lite.{ConnectOp => Axi4LiteConnectOp}
import chext.amba.axi4s.Casts._
import chext.tracking.{Component, withComponent}
import chext.util.ElaborationTest

object TrackingDiagnosticsUtil {
  def satisfyAxi4Tracking(interface: axi4.tracking.Tracked): Unit = {
    val cfg = interface.cfg

    if (cfg.read) {
      if (!cfg.lite) {
        val shape = v.BurstShape(
          maxBeats = v.BurstShape.maxBeatsFor(cfg),
          types = v.BurstShape.supportedTypesFor(cfg),
          sizes = v.BurstShape.supportedSizesFor(cfg),
          aligned = false
        )
        interface.properties(p.MasterReadBurstShape) = shape
        interface.properties(p.SlaveReadBurstShape) = shape
      }
      interface.properties(p.MasterReadThreadMode) = v.ThreadMode.SingleTransaction
      interface.properties(p.SlaveReadThreadMode) = v.ThreadMode.SingleThread
    }

    if (cfg.write) {
      if (!cfg.lite) {
        val shape = v.BurstShape(
          maxBeats = v.BurstShape.maxBeatsFor(cfg),
          types = v.BurstShape.supportedTypesFor(cfg),
          sizes = v.BurstShape.supportedSizesFor(cfg),
          aligned = false
        )
        interface.properties(p.MasterWriteBurstShape) = shape
        interface.properties(p.SlaveWriteBurstShape) = shape
      }
      interface.properties(p.MasterWriteThreadMode) = v.ThreadMode.SingleTransaction
      interface.properties(p.SlaveWriteThreadMode) = v.ThreadMode.SingleThread
    }

    interface.properties(p.SlaveMemoryMap) =
      v.MemoryMap(size = BigInt(1) << math.min(cfg.wAddr, 8))
  }

  def driveAxi4Slave(axi: axi4.RawInterface): Unit = {
    axi.ARREADY.foreach(_ := false.B)
    axi.RVALID.foreach(_ := false.B)
    axi.RID.foreach(_ := 0.U)
    axi.RDATA.foreach(_ := 0.U)
    axi.RRESP.foreach(_ := 0.U)
    axi.RLAST.foreach(_ := false.B)
    axi.RUSER.foreach(_ := 0.U)
    axi.AWREADY.foreach(_ := false.B)
    axi.WREADY.foreach(_ := false.B)
    axi.BVALID.foreach(_ := false.B)
    axi.BID.foreach(_ := 0.U)
    axi.BRESP.foreach(_ := 0.U)
    axi.BUSER.foreach(_ := 0.U)
  }

  def driveAxi4SlaveInputs(axi: axi4.RawInterface): Unit = {
    axi.ARVALID.foreach(_ := false.B)
    axi.ARID.foreach(_ := 0.U)
    axi.ARADDR.foreach(_ := 0.U)
    axi.ARLEN.foreach(_ := 0.U)
    axi.ARSIZE.foreach(_ := 0.U)
    axi.ARBURST.foreach(_ := 0.U)
    axi.ARLOCK.foreach(_ := 0.U)
    axi.ARCACHE.foreach(_ := 0.U)
    axi.ARPROT.foreach(_ := 0.U)
    axi.ARQOS.foreach(_ := 0.U)
    axi.ARREGION.foreach(_ := 0.U)
    axi.ARUSER.foreach(_ := 0.U)
    axi.RREADY.foreach(_ := false.B)
    axi.AWVALID.foreach(_ := false.B)
    axi.AWID.foreach(_ := 0.U)
    axi.AWADDR.foreach(_ := 0.U)
    axi.AWLEN.foreach(_ := 0.U)
    axi.AWSIZE.foreach(_ := 0.U)
    axi.AWBURST.foreach(_ := 0.U)
    axi.AWLOCK.foreach(_ := 0.U)
    axi.AWCACHE.foreach(_ := 0.U)
    axi.AWPROT.foreach(_ := 0.U)
    axi.AWQOS.foreach(_ := 0.U)
    axi.AWREGION.foreach(_ := 0.U)
    axi.AWUSER.foreach(_ := 0.U)
    axi.WVALID.foreach(_ := false.B)
    axi.WDATA.foreach(_ := 0.U)
    axi.WSTRB.foreach(_ := 0.U)
    axi.WLAST.foreach(_ := false.B)
    axi.WUSER.foreach(_ := 0.U)
    axi.BREADY.foreach(_ := false.B)
  }

  def driveAxi4sSlave(axis: axi4s.Interface): Unit =
    axis.TREADY := false.B

  def driveAxi4sSlaveInputs(axis: axi4s.Interface): Unit = {
    axis.TVALID := false.B
    axis.TDATA := 0.U
    axis.TSTRB := 0.U
    axis.TKEEP := 0.U
    axis.TLAST := 0.U
    axis.TID := 0.U
    axis.TDEST := 0.U
    axis.TUSER := 0.U
  }
}

private class ElasticConnectTop extends Module with chext.AnnotatedModule {
  val source = IO(e.Source(UInt(8.W)))
  val sink = IO(e.Sink(UInt(8.W)))

  source :=> sink
}

private class ElasticConnectManyTop extends Module {
  val sources = IO(e.Source.many(2, UInt(8.W)))
  val sinks = IO(e.Sink.many(2, UInt(8.W)))

  sources :=> sinks
}

private class ElasticNullTop extends Module {
  val source = IO(e.Source(UInt(8.W)))
  val sink = IO(e.Sink(UInt(8.W)))

  val nullSink0 = new e.NullSink(source)
  val nullSource0 = new e.NullSource(sink)
}

private class ElasticUnusedTop extends Module {
  val source = IO(e.Source(UInt(8.W)))
  val sink = IO(e.Sink(UInt(8.W)))

  dontTouch(source.$valid)
  val nullSource0 = new e.NullSource(sink)
}

private class ElasticDuplicateSourceTop extends Module {
  val source = IO(e.Source(UInt(8.W)))
  val sink0 = IO(e.Sink(UInt(8.W)))
  val sink1 = IO(e.Sink(UInt(8.W)))

  source :=> sink0
  source :=> sink1
}

private class ElasticDuplicateSinkTop extends Module {
  val source0 = IO(e.Source(UInt(8.W)))
  val source1 = IO(e.Source(UInt(8.W)))
  val sink = IO(e.Sink(UInt(8.W)))

  source0 :=> sink
  source1 :=> sink
}

private class ElasticWrongRoleTop extends Module {
  val source = IO(e.Source(UInt(8.W)))
  val sink = IO(e.Sink(UInt(8.W)))

  val nullSink0 = new e.NullSink(sink)
  val nullSource0 = new e.NullSource(source)
}

private class ElasticWireOnceTop extends Module {
  val source = IO(e.Source(UInt(8.W)))
  val sink = IO(e.Sink(UInt(8.W)))
  private val wire = e.EWire(UInt(8.W))

  source :=> wire
  wire :=> sink
}

private class ElasticWireUnusedTop extends Module {
  private val wire = e.EWire(UInt(8.W))

  dontTouch(wire.$valid)
}

private class ElasticWireDuplicateSourceTop extends Module {
  val sink0 = IO(e.Sink(UInt(8.W)))
  val sink1 = IO(e.Sink(UInt(8.W)))
  private val wire = e.EWire(UInt(8.W))

  val nullSource0 = new e.NullSource(wire)
  wire :=> sink0
  wire :=> sink1
}

private class ElasticWireDuplicateSinkTop extends Module {
  val source0 = IO(e.Source(UInt(8.W)))
  val source1 = IO(e.Source(UInt(8.W)))
  private val wire = e.EWire(UInt(8.W))

  source0 :=> wire
  source1 :=> wire
  val nullSink0 = new e.NullSink(wire)
}

private class ElasticForkJoinTop extends Module {
  val source = IO(e.Source(UInt(8.W)))
  val sink = IO(e.Sink(UInt(8.W)))

  val fork0 = new e.Fork(source) {
    private val left = fork()
    private val right = fork()

    val join0 = new e.Join(sink) {
      val a = join(left)
      val b = join(right)

      out := a + b
    }
  }
}

private class ElasticLeaf extends Module {
  val source = IO(e.Source(UInt(8.W)))
  val sink = IO(e.Sink(UInt(8.W)))

  val transform0 = new e.Transform(source, sink) {
    out := in + 1.U
  }
}

private class ElasticPathWarningTop extends Module {
  val source = IO(e.Source(UInt(8.W)))
  val sink = IO(e.Sink(UInt(8.W)))

  prefix("same_path") {
    val transform0 = new e.Transform(source, sink) {
      out := in
    }
  }
  prefix("same_path") {
    val transform0 = new e.Transform(source, sink) {
      out := in
    }
  }
}

private final class NamingTestComponent(expectedPrefix: String)(implicit
    val sourceInfo: SourceInfo
) extends Component {
  def tpe: String = "NamingTestComponent"
  def namePrefix: String = expectedPrefix
}

private final class NamingTestParent(expectedPrefix: String)(implicit
    val sourceInfo: SourceInfo
) extends Component {
  def tpe: String = "NamingTestParent"
  def namePrefix: String = expectedPrefix
}

private class NamePrefixAcceptedTop extends Module {
  Seq(
    "queue",
    "queue0",
    "queue0_",
    "queue0_1",
    "queue_0_1",
    "queueRd",
    "queueABC",
    "queue_x0",
    "queue_abc",
    "queue_a",
    "queue_b",
    "queue_c"
  ).foreach { name =>
    prefix(name) {
      new NamingTestComponent("queue")
    }
  }

  prefix("scope0") {
    new NamingTestParent("scope")
  }

  prefix("sourceBuffer0") {
    prefix("queueNested") {
      new NamingTestComponent("queue")
    }
  }
}

private final class HierarchyTestComponent(
    source: e.Interface[UInt],
    sink: e.Interface[UInt]
)(implicit val sourceInfo: SourceInfo)
    extends Component {
  def tpe: String = "HierarchyTest"
  def namePrefix: String = "hierarchyTest"

  private val elasticState = trackingState(e.tracking.Tag)
  elasticState.addSource("source", source, boundary = true)
  elasticState.addSink("sink", sink, boundary = true)

  withComponent(this) {
    val connect0 = new e.Connect(source, sink)
  }
}

private class ComponentHierarchyTop extends Module {
  val source = IO(e.Source(UInt(8.W)))
  val sink = IO(e.Sink(UInt(8.W)))

  val hierarchyTest0 = new HierarchyTestComponent(source, sink)
}

private final class InvalidCompositeComponent(
    source: e.Interface[UInt],
    sink: e.Interface[UInt]
)(implicit val sourceInfo: SourceInfo)
    extends Component {
  def tpe: String = "InvalidComposite"
  def namePrefix: String = "invalidComposite"

  private val elasticState = trackingState(e.tracking.Tag)
  elasticState.addSource("source", source)
  elasticState.addSink("sink", sink)

  withComponent(this) {
    val connect0 = new e.Connect(source, sink)
  }
}

private class InvalidCompositeTop extends Module {
  val source = IO(e.Source(UInt(8.W)))
  val sink = IO(e.Sink(UInt(8.W)))

  val invalidComposite0 = new InvalidCompositeComponent(source, sink)
}

private final class InvalidBoundaryMonitorComponent(
    source: e.Interface[UInt],
    sink: e.Interface[UInt]
)(implicit val sourceInfo: SourceInfo)
    extends Component {
  def tpe: String = "InvalidBoundaryMonitor"
  def namePrefix: String = "invalidBoundaryMonitor"

  private val elasticState = trackingState(e.tracking.Tag)
  elasticState.addSource("source", source, boundary = true)
  elasticState.addSink("sink", sink, boundary = true)

  source.$ready := sink.$ready
  sink.$valid := source.$valid
  sink.$bits := source.$bits

  val monitor0 = new chext.deadlock.Monitor(this)
}

private class InvalidBoundaryMonitorTop extends Module {
  val source = IO(e.Source(UInt(8.W)))
  val sink = IO(e.Sink(UInt(8.W)))

  val invalidBoundaryMonitor0 = new InvalidBoundaryMonitorComponent(source, sink)
}

private class ElasticCompositeBoundaryTop extends Module {
  val sourceRepeat = IO(e.Source(UInt(8.W)))
  val sinkRepeat = IO(e.Sink(UInt(8.W)))
  val repeat0 = new e.Repeat(sourceRepeat, sinkRepeat, 8) {
    len { _ => 1.U }
    out { (in, _, _, _) => in }
  }

  val sourceRandomStall = IO(e.Source(UInt(8.W)))
  val sinkRandomStall = IO(e.Sink(UInt(8.W)))
  val randomStall0 = new e.RandomStall(sourceRandomStall, sinkRandomStall)

  val sourceLoop = IO(e.Source(UInt(8.W)))
  val sinkLoop = IO(e.Sink(UInt(8.W)))
  val loop0 = new e.Loop(sourceLoop, sinkLoop) {
    end { _ => true.B }
    val connectBody0 = new e.Connect(sinkCurrent, sourceNext)
  }

  val sourceScope = IO(e.Source(UInt(8.W)))
  val sinkScope = IO(e.Sink(UInt(8.W)))
  val scope0 = new e.Scope(sourceScope, sinkScope) {
    val connectBody0 = new e.Connect(sinkBegin, sourceEnd)
  }

  val sourceSwitch = IO(e.Source(UInt(8.W)))
  val sinkSwitch = IO(e.Sink(UInt(8.W)))
  val switch0 = new e.Switch(sourceSwitch, sinkSwitch) {
    branch { _ => true.B } { (source, sink) =>
      val connect0 = new e.Connect(source, sink)
    }
  }

  val sourceFold = IO(e.Source(UInt(8.W)))
  val sourceFoldInit = IO(e.Source(UInt(8.W)))
  val sinkFold = IO(e.Sink(UInt(8.W)))
  val fold0 = new e.Fold(sourceFold, sourceFoldInit, sinkFold) {
    operand { in => in }
    last { _ => true.B }
    val join0 = new e.Join(sourceResult) {
      out := join(sinkA) + join(sinkB)
    }
  }
}

private class NamePrefixWarningTop extends Module {
  Seq("queueing0", "myQueue0", "rightBuffer0", "readConnectMany0").foreach { name =>
    prefix(name) {
      new NamingTestComponent("queue")
    }
  }

  prefix("queue0") {
    prefix("rightBufferNested") {
      new NamingTestComponent("queue")
    }
  }
}

private class ElasticNestedTop extends Module {
  val source = IO(e.Source(UInt(8.W)))
  val sink = IO(e.Sink(UInt(8.W)))

  private val leaf0 = Module(new ElasticLeaf)
  private val leaf1 = Module(new ElasticLeaf)

  source :=> leaf0.source
  leaf0.sink :=> leaf1.source
  leaf1.sink :=> sink
}

private class ElasticNestedDuplicateTop extends Module {
  val source = IO(e.Source(UInt(8.W)))
  val sink0 = IO(e.Sink(UInt(8.W)))
  val sink1 = IO(e.Sink(UInt(8.W)))

  private val leaf = Module(new ElasticLeaf)

  source :=> leaf.source
  leaf.sink :=> sink0
  leaf.sink :=> sink1
}

private class Axi4FullNativeTop extends Module {
  import Axi4FullConnectOp._

  private val cfg = axi4.Config(wId = 1, wAddr = 16, wData = 32)
  val s_axi = IO(axi4.full.Slave(cfg))
  val m_axi = IO(axi4.full.Master(cfg))

  s_axi :=> m_axi
}

private class Axi4LiteNativeTop extends Module {
  import Axi4LiteConnectOp._

  private val cfg = axi4.Config(lite = true, wAddr = 16, wData = 32)
  val s_axi = IO(axi4.lite.Slave(cfg))
  val m_axi = IO(axi4.lite.Master(cfg))

  s_axi :=> m_axi
}

private class Axi4FullNativeDuplicateTop extends Module {
  private val cfg = axi4.Config(wId = 1, wAddr = 16, wData = 32)
  val s_axi = IO(axi4.full.Slave(cfg))

  val nullSinkAr0 = new e.NullSink(s_axi.ar)
  val nullSinkAr1 = new e.NullSink(s_axi.ar)
  val nullSourceR0 = new e.NullSource(s_axi.r)
  val nullSinkAw0 = new e.NullSink(s_axi.aw)
  val nullSinkW0 = new e.NullSink(s_axi.w)
  val nullSourceB0 = new e.NullSource(s_axi.b)
}

private class Axi4TrackingFailureTop extends Module {
  private val cfg =
    axi4.Config(wId = 1, wAddr = 16, wData = 32, write = false)

  val s_axi = IO(axi4.Slave(cfg))
  private val view = s_axi.asFull

  val nullSinkAr0 = new e.NullSink(view.ar)
  val nullSourceR0 = new e.NullSource(view.r)

  private val transferTypes = Seq(axi4.BurstType.Encoding.INCR)
  private val transferSizes = Seq(v.BurstShape.fullSize(cfg.wData))

  view.properties(p.MasterReadBurstShape) =
    v.BurstShape(
      maxBeats = 16,
      types = transferTypes,
      sizes = transferSizes,
      aligned = true
    )
  view.properties(p.SlaveReadBurstShape) =
    v.BurstShape(
      maxBeats = 1,
      types = transferTypes,
      sizes = transferSizes,
      aligned = true
    )
  view.properties(p.MasterReadThreadMode) = v.ThreadMode.SingleTransaction
  view.properties(p.SlaveReadThreadMode) = v.ThreadMode.Unconstrained
  view.properties(p.SlaveMemoryMap) = v.MemoryMap(size = 0x100)
}

private class Axi4ViewRootOnceTop extends Module {
  import TrackingDiagnosticsUtil._

  val axi = IO(axi4.Slave(axi4.Config(lite = true)))

  private val view = axi.asLite
  satisfyAxi4Tracking(view)

  val nullSinkAr0 = new e.NullSink(view.ar)
  val nullSourceR0 = new e.NullSource(view.r)
  val nullSinkAw0 = new e.NullSink(view.aw)
  val nullSinkW0 = new e.NullSink(view.w)
  val nullSourceB0 = new e.NullSource(view.b)
}

private class Axi4ViewRepeatedRootTop extends Module {
  import TrackingDiagnosticsUtil._

  val axi = IO(axi4.Slave(axi4.Config(lite = true)))

  private val view0 = axi.asLite
  private val view1 = axi.asLite
  satisfyAxi4Tracking(view0)
  satisfyAxi4Tracking(view1)
  driveAxi4Slave(axi)
}

private class Axi4ViewChild extends Module {
  import TrackingDiagnosticsUtil._

  val axi = IO(axi4.Slave(axi4.Config(lite = true)))

  private val view = axi.asLite
  satisfyAxi4Tracking(view)
  driveAxi4Slave(axi)
}

private class Axi4ViewChildTop extends Module {
  import TrackingDiagnosticsUtil._

  private val child = Module(new Axi4ViewChild)

  driveAxi4SlaveInputs(child.axi)
}

private class Axi4ViewRootWireTop extends Module {
  import TrackingDiagnosticsUtil._

  private val axi = Wire(axi4.Slave(axi4.Config(lite = true)))

  axi := DontCare
  private val view = axi.asLite
  satisfyAxi4Tracking(view)
}

private class Axi4ViewLeaf extends Module {
  import TrackingDiagnosticsUtil._

  private val cfg = axi4.Config(wId = 2, wAddr = 16, wData = 32, wUserAR = 1, wUserR = 1)

  val s_axi = IO(axi4.Slave(cfg))
  val m_axi = IO(axi4.Master(cfg))

  private val s_view = s_axi.asFull
  private val m_view = m_axi.asFull
  satisfyAxi4Tracking(s_view)
  satisfyAxi4Tracking(m_view)

  val transformAr0 = new e.Transform(s_view.ar, m_view.ar) {
    out := in
  }
  val transformR0 = new e.Transform(m_view.r, s_view.r) {
    out := in
  }
  val transformAw0 = new e.Transform(s_view.aw, m_view.aw) {
    out := in
  }
  val transformW0 = new e.Transform(s_view.w, m_view.w) {
    out := in
  }
  val transformB0 = new e.Transform(m_view.b, s_view.b) {
    out := in
  }
}

private class Axi4ViewNestedTop extends Module {
  import Axi4FullConnectOp._
  import TrackingDiagnosticsUtil._

  private val cfg = axi4.Config(wId = 2, wAddr = 16, wData = 32, wUserAR = 1, wUserR = 1)

  val s_axi = IO(axi4.Slave(cfg))
  val m_axi = IO(axi4.Master(cfg))

  private val leaf = Module(new Axi4ViewLeaf)

  private val s_view = s_axi.asFull
  private val leaf_s_view = leaf.s_axi.asFull
  private val leaf_m_view = leaf.m_axi.asFull
  private val m_view = m_axi.asFull
  satisfyAxi4Tracking(s_view)
  satisfyAxi4Tracking(leaf_s_view)
  satisfyAxi4Tracking(leaf_m_view)
  satisfyAxi4Tracking(m_view)

  s_view :=> leaf_s_view
  leaf_m_view :=> m_view
}

private class Axi4ViewNullTop extends Module {
  import TrackingDiagnosticsUtil._

  private val cfg = axi4.Config(wId = 1, wAddr = 16, wData = 32)

  val s_axi = IO(axi4.Slave(cfg))
  val m_axi = IO(axi4.Master(cfg))

  private val s_view = s_axi.asFull
  private val m_view = m_axi.asFull
  satisfyAxi4Tracking(s_view)
  satisfyAxi4Tracking(m_view)

  val nullSinkAr0 = new e.NullSink(s_view.ar)
  val nullSourceR0 = new e.NullSource(s_view.r)
  val nullSinkAw0 = new e.NullSink(s_view.aw)
  val nullSinkW0 = new e.NullSink(s_view.w)
  val nullSourceB0 = new e.NullSource(s_view.b)

  val nullSourceAr1 = new e.NullSource(m_view.ar)
  val nullSinkR1 = new e.NullSink(m_view.r)
  val nullSourceAw1 = new e.NullSource(m_view.aw)
  val nullSourceW1 = new e.NullSource(m_view.w)
  val nullSinkB1 = new e.NullSink(m_view.b)
}

private class Axi4ViewZeroTop extends Module {
  import TrackingDiagnosticsUtil._

  private val cfg = axi4.Config(wId = 1, wAddr = 16, wData = 32)

  val s_axi = IO(axi4.Slave(cfg))
  val m_axi = IO(axi4.Master(cfg))

  private val s_view = s_axi.asFull
  private val m_view = m_axi.asFull
  satisfyAxi4Tracking(s_view)
  satisfyAxi4Tracking(m_view)

  dontTouch(s_view.ar.$valid)
  dontTouch(m_view.r.$valid)
}

private class Axi4ViewMissingChannelTop extends Module {
  import TrackingDiagnosticsUtil._

  private val cfg = axi4.Config(wId = 1, wAddr = 16, wData = 32)

  val s_axi = IO(axi4.Slave(cfg))

  private val s_view = s_axi.asFull
  satisfyAxi4Tracking(s_view)

  val nullSinkAr0 = new e.NullSink(s_view.ar)
  val nullSinkAw0 = new e.NullSink(s_view.aw)
  val nullSinkW0 = new e.NullSink(s_view.w)
  val nullSourceB0 = new e.NullSource(s_view.b)

  dontTouch(s_view.r.$valid)
}

private class Axi4ViewDuplicatedChannelTop extends Module {
  import TrackingDiagnosticsUtil._

  private val cfg = axi4.Config(wId = 1, wAddr = 16, wData = 32)

  val s_axi = IO(axi4.Slave(cfg))

  private val s_view = s_axi.asFull
  satisfyAxi4Tracking(s_view)

  val nullSinkAr0 = new e.NullSink(s_view.ar)
  val nullSinkAr1 = new e.NullSink(s_view.ar)
  val nullSourceR0 = new e.NullSource(s_view.r)
  val nullSinkAw0 = new e.NullSink(s_view.aw)
  val nullSinkW0 = new e.NullSink(s_view.w)
  val nullSourceB0 = new e.NullSource(s_view.b)
}

private class Axi4ViewMultiConnectTop extends Module {
  import TrackingDiagnosticsUtil._

  private val cfg = axi4.Config(wId = 1, wAddr = 16, wData = 32)

  val s_axi = IO(axi4.Slave(cfg))
  val m_axi = IO(axi4.Master(cfg))

  private val s_view = s_axi.asFull
  private val m_view = m_axi.asFull
  satisfyAxi4Tracking(s_view)
  satisfyAxi4Tracking(m_view)

  val transformAr0 = new e.Transform(s_view.ar, m_view.ar) {
    out := in
  }
  val transformR0 = new e.Transform(m_view.r, s_view.r) {
    out := in
  }
  val transformAw0 = new e.Transform(s_view.aw, m_view.aw) {
    out := in
  }
  val transformW0 = new e.Transform(s_view.w, m_view.w) {
    out := in
  }
  val transformB0 = new e.Transform(m_view.b, s_view.b) {
    out := in
  }

  val transformAr1 = new e.Transform(s_view.ar, m_view.ar) {
    out := in
  }
  val transformR1 = new e.Transform(m_view.r, s_view.r) {
    out := in
  }
  val transformAw1 = new e.Transform(s_view.aw, m_view.aw) {
    out := in
  }
  val transformW1 = new e.Transform(s_view.w, m_view.w) {
    out := in
  }
  val transformB1 = new e.Transform(m_view.b, s_view.b) {
    out := in
  }
}

private class Axi4ViewWrongRoleTop extends Module {
  private val cfg = axi4.Config(wId = 1, wAddr = 16, wData = 32)

  val s_axi = IO(axi4.Slave(cfg))
  val m_axi = IO(axi4.Master(cfg))

  private val s_view = s_axi.asFull
  private val m_view = m_axi.asFull

  val transformAr0 = new e.Transform(s_view.ar, m_view.ar) {
    out := in
  }
  val transformR0 = new e.Transform(m_view.r, s_view.r) {
    out := in
  }
  val transformAw0 = new e.Transform(s_view.aw, m_view.aw) {
    out := in
  }
  val transformW0 = new e.Transform(s_view.w, m_view.w) {
    out := in
  }
  val transformB0 = new e.Transform(m_view.b, s_view.b) {
    out := in
  }
  val nullSinkAr0 = new e.NullSink(m_view.ar)
}

private class Axi4sViewRootOnceTop extends Module {
  val axis = IO(axi4s.Slave(axi4s.Config(wData = 32)))

  private val view = axis.asFull

  val nullSink0 = new e.NullSink(view)
}

private class Axi4sViewRepeatedRootTop extends Module {
  import TrackingDiagnosticsUtil._

  val axis = IO(axi4s.Slave(axi4s.Config(wData = 32)))

  axis.asFull
  axis.asFull
  driveAxi4sSlave(axis)
}

private class Axi4sViewChild extends Module {
  import TrackingDiagnosticsUtil._

  val axis = IO(axi4s.Slave(axi4s.Config(wData = 32)))

  axis.asFull
  driveAxi4sSlave(axis)
}

private class Axi4sViewChildTop extends Module {
  import TrackingDiagnosticsUtil._

  private val child = Module(new Axi4sViewChild)

  driveAxi4sSlaveInputs(child.axis)
}

private class Axi4sViewRootWireTop extends Module {
  private val axis = Wire(axi4s.Slave(axi4s.Config(wData = 32)))

  axis := DontCare
  axis.asFull
}

private class Axi4sViewPassTop extends Module {
  private val cfg = axi4s.Config(wData = 32, wId = 2, wDest = 1, wUser = 4)

  val s_axis = IO(axi4s.Slave(cfg))
  val m_axis = IO(axi4s.Master(cfg))

  private val s_view = s_axis.asFull
  private val m_view = m_axis.asFull

  val transform0 = new e.Transform(s_view, m_view) {
    out := in
  }
}

private class Axi4sViewNullTop extends Module {
  private val cfg = axi4s.Config(wData = 32, wId = 1, wDest = 1, wUser = 2)

  val s_axis = IO(axi4s.Slave(cfg))
  val m_axis = IO(axi4s.Master(cfg))

  private val s_view = s_axis.asFull
  private val m_view = m_axis.asFull

  val nullSink0 = new e.NullSink(s_view)
  val nullSource0 = new e.NullSource(m_view)
}

private class Axi4sViewUnusedTop extends Module {
  private val cfg = axi4s.Config(wData = 32)

  val axis = IO(axi4s.Slave(cfg))

  private val view = axis.asFull

  dontTouch(view.$valid)
}

private class Axi4sViewDuplicateSourceTop extends Module {
  private val cfg = axi4s.Config(wData = 32)

  val axis = IO(axi4s.Slave(cfg))

  private val view = axis.asFull

  val nullSink0 = new e.NullSink(view)
  val nullSink1 = new e.NullSink(view)
}

private class Axi4sViewDuplicateSinkTop extends Module {
  private val cfg = axi4s.Config(wData = 32)

  val axis = IO(axi4s.Master(cfg))

  private val view = axis.asFull

  val nullSource0 = new e.NullSource(view)
  val nullSource1 = new e.NullSource(view)
}

object TrackingDiagnostics_Test extends App with ElaborationTest {
  private val NoPathWarnings = "no-path-warnings"
  private val NoNamePrefixWarnings = "no-name-prefix-warnings"
  private val NoUnknownInterfaces = "no-unknown-interfaces"

  suite(
    name = "tracking-diagnostics",
    outputDir = Path.of("output", "tracking_diagnostics"),
    commonChecks = Seq(
      (Log excludes "[ WARN ] tracking/pathChecks").named(NoPathWarnings),
      (Log excludes "[ WARN ] tracking/namePrefixChecks").named(NoNamePrefixWarnings),
      (ModuleGraphJson excludes "Encountered an interface which is neither an IO or Wire.")
        .named(NoUnknownInterfaces),
      (Log excludes "Encountered an interface which is neither an IO or Wire.")
        .named(NoUnknownInterfaces)
    ),
    reportArtifacts = Seq(
      SystemVerilog,
      Log,
      Errors,
      ModuleGraphJson,
      HdlInfoJson
    )
  )

  test(
    name = "elastic_connect",
    description = "Plain elastic source-to-sink connection without any DataView usage.",
    gen = () => new ElasticConnectTop,
    checks = Seq(
      SystemVerilog contains "module ElasticConnectTop",
      ModuleGraph.check("elastic source and sink paths") { graph =>
        val sourcePaths = graph.sources.map(_.path)
        val sinkPaths = graph.sinks.map(_.path)
        Option.unless(sourcePaths.contains("/source") && sinkPaths.contains("/sink"))(
          s"sources: $sourcePaths; sinks: $sinkPaths"
        )
      },
      Log excludes "Interface never marked!",
      Log excludes "more than 1 times",
      HdlInfo.check("top module name") { info =>
        Option.unless(info.name == "ElasticConnectTop")(s"actual name: ${info.name}")
      }
    )
  )

  test(
    name = "elastic_connect_many",
    description = "Sequence elastic connections use a semantic connectMany prefix in the module graph.",
    gen = () => new ElasticConnectManyTop,
    checks = Seq(
      SystemVerilog contains "module ElasticConnectManyTop",
      ModuleGraphJson contains """"path" : "/connectMany0_0_connect0"""",
      ModuleGraphJson contains """"path" : "/connectMany0_1_connect0"""",
      ModuleGraphJson contains """"/connectMany0_0_connect0"""",
      ModuleGraphJson contains """"/connectMany0_1_connect0"""",
      Log excludes "Interface never marked!",
      Log excludes "more than 1 times"
    )
  )

  test(
    name = "elastic_null",
    description = "Plain elastic IO endpoints are consumed and produced by null components.",
    gen = () => new ElasticNullTop,
    checks = Seq(
      SystemVerilog contains "module ElasticNullTop",
      ModuleGraphJson contains "/source",
      ModuleGraphJson contains "/sink",
      Log excludes "Interface never marked!",
      Log excludes "more than 1 times"
    )
  )

  test(
    name = "tracking_path_warning",
    description = "Two deliberately same-prefixed components verify that tracking path warnings are still reported separately.",
    gen = () => new ElasticPathWarningTop,
    checks = Seq(
      SystemVerilog contains "module ElasticPathWarningTop",
      Log contains "[ WARN ] tracking/pathChecks : Multiple components use the same path, which should be avoided"
    ),
    disabledCommonChecks = Set(NoPathWarnings)
  )

  test(
    name = "tracking_name_prefix_accepted",
    description = "Components accept numeric, camel-case, and underscore suffixes after their expected namePrefix.",
    gen = () => new NamePrefixAcceptedTop,
    checks = Seq(
      SystemVerilog contains "module NamePrefixAcceptedTop",
      Log excludes "[ WARN ] tracking/namePrefixChecks"
    )
  )

  test(
    name = "tracking_name_prefix_warning",
    description = "A component warns when its latest prefix does not recognizably start with its expected namePrefix.",
    gen = () => new NamePrefixWarningTop,
    checks = Seq(
      SystemVerilog contains "module NamePrefixWarningTop",
      Log contains "[ WARN ] tracking/namePrefixChecks : Component path does not start with its expected namePrefix",
      Log contains "Expected namePrefix: queue",
      Log contains "Latest prefix: queueing0",
      Log contains "Latest prefix: myQueue0",
      Log contains "Latest prefix: rightBuffer0",
      Log contains "Latest prefix: readConnectMany0",
      Log contains "Latest prefix: rightBufferNested",
      Log contains "Path: queue0_rightBufferNested"
    ),
    disabledCommonChecks = Set(NoNamePrefixWarnings)
  )

  test(
    name = "component_hierarchy",
    description = "A unified component records hierarchy-only boundary ports without marking them as operational endpoints.",
    gen = () => new ComponentHierarchyTop,
    checks = Seq(
      ModuleGraphJson contains "\"tpe\" : \"HierarchyTest\"",
      ModuleGraphJson contains "\"boundary\" : true",
      ModuleGraphJson contains "\"children\" : [",
      ModuleGraphJson contains "\"/hierarchyTest0_connect0\"",
      ModuleGraphJson contains "\"parent\" : \"/hierarchyTest0\"",
      ModuleGraphJson excludes "\"containers\"",
      Log excludes "\"containers\"",
      ModuleGraphJson excludes "Encountered an interface which is neither an IO or Wire.",
      Log excludes "Encountered an interface which is neither an IO or Wire.",
      ModuleGraphJson.occurs("\"boundary\" : true", 2),
      ModuleGraphJson.occurs("\"boundary\" : false", 2),
      Log excludes "marked as source more than 1 times",
      Log excludes "marked as sink more than 1 times"
    )
  )

  test(
    name = "component_children_with_elastic_ports",
    description = "Elastic tracking rejects a component that owns both child components and Elastic ports.",
    expected = Failure,
    gen = () => new InvalidCompositeTop,
    checks = Seq(
      FailureMessage contains "A component with children must not define operational Elastic source or sink interfaces.",
      FailureMessage contains "InvalidComposite"
    )
  )

  test(
    name = "component_boundary_ports_with_deadlock_monitor",
    description = "Deadlock monitors reject components whose registered interfaces include boundary ports.",
    expected = Failure,
    gen = () => new InvalidBoundaryMonitorTop,
    checks = Seq(
      FailureMessage contains "A deadlock monitor must not be attached to a component with boundary interfaces."
    )
  )

  test(
    name = "elastic_composite_boundary_interfaces",
    description = "Every built-in Elastic composite exposes its external interfaces as boundary references.",
    gen = () => new ElasticCompositeBoundaryTop,
    checks = Seq(
      ModuleGraphJson contains "\"tpe\" : \"Repeat\"",
      ModuleGraphJson contains "\"tpe\" : \"RandomStall\"",
      ModuleGraphJson contains "\"tpe\" : \"Loop\"",
      ModuleGraphJson contains "\"tpe\" : \"Scope\"",
      ModuleGraphJson contains "\"tpe\" : \"Switch\"",
      ModuleGraphJson contains "\"tpe\" : \"Fold\"",
      ModuleGraphJson contains "\"sourceInit\"",
      ModuleGraphJson contains "\"sinkExit\"",
      ModuleGraphJson contains "\"sinkBegin\"",
      ModuleGraphJson contains "\"sourceEnd\"",
      ModuleGraphJson contains "\"sinkCurrent\"",
      ModuleGraphJson contains "\"sourceNext\"",
      ModuleGraphJson contains "\"sinkA\"",
      ModuleGraphJson contains "\"sinkB\"",
      ModuleGraphJson contains "\"sourceResult\"",
      ModuleGraphJson contains "\"sink_b0\"",
      ModuleGraphJson contains "\"source_b0\"",
      ModuleGraphJson.occurs("\"boundary\" : true", 22),
      Log excludes "more than 1 times"
    )
  )

  test(
    name = "elastic_unused",
    description = "A root elastic source is not used, so tracking should warn before FIRRTL reports incomplete initialization.",
    expected = Failure,
    gen = () => new ElasticUnusedTop,
    checks = Seq(
      Log contains "[ WARN ] elastic.tracking/sanityChecks",
      Log excludes "axi4.tracking",
      Log contains "Interface never marked!",
      Log contains "Interface '/source' is defined by",
      FailureMessage contains "not fully initialized"
    )
  )

  test(
    name = "elastic_duplicate_source",
    description = "One source IO feeds two sinks; hardware can emit, but tracking must report repeated source use.",
    gen = () => new ElasticDuplicateSourceTop,
    checks = Seq(
      ModuleGraphJson contains "/source",
      ModuleGraphJson contains "/sink0",
      ModuleGraphJson contains "/sink1",
      Log contains "Interface marked as source more than 1 times!",
      Log contains "Interface '/source' is defined by"
    )
  )

  test(
    name = "elastic_duplicate_sink",
    description = "Two source IOs feed one sink; hardware can emit, but tracking must report repeated sink use.",
    gen = () => new ElasticDuplicateSinkTop,
    checks = Seq(
      ModuleGraphJson contains "/source0",
      ModuleGraphJson contains "/source1",
      ModuleGraphJson contains "/sink",
      Log contains "Interface marked as sink more than 1 times!",
      Log contains "Interface '/sink' is defined by"
    )
  )

  test(
    name = "elastic_wrong_role",
    description = "A source endpoint is used as a sink and a sink endpoint is used as a source.",
    expected = Failure,
    gen = () => new ElasticWrongRoleTop,
    checks = Seq(
      FailureMessage contains "elastic.tracking:",
      FailureMessage excludes "axi4.tracking",
      FailureMessage contains "declared as a Sink, but marked as Source"
    )
  )

  test(
    name = "elastic_wire_once",
    description = "An internal elastic wire is used once as a sink and once as a source.",
    gen = () => new ElasticWireOnceTop,
    checks = Seq(
      ModuleGraphJson contains "/wire",
      Log excludes "Interface never marked!",
      Log excludes "more than 1 times"
    )
  )

  test(
    name = "elastic_wire_unused",
    description = "An internal elastic wire is never marked by a component.",
    expected = Failure,
    gen = () => new ElasticWireUnusedTop,
    checks = Seq(
      Log contains "Interface never marked!",
      Log contains "Interface '/wire' is defined by",
      FailureMessage contains "not fully initialized"
    )
  )

  test(
    name = "elastic_wire_duplicate_source",
    description = "An internal elastic wire is driven once but consumed by two sinks.",
    gen = () => new ElasticWireDuplicateSourceTop,
    checks = Seq(
      Log contains "Interface marked as source more than 1 times!",
      Log contains "Interface '/wire' is defined by"
    )
  )

  test(
    name = "elastic_wire_duplicate_sink",
    description = "Two sources drive the same internal elastic wire.",
    gen = () => new ElasticWireDuplicateSinkTop,
    checks = Seq(
      Log contains "Interface marked as sink more than 1 times!",
      Log contains "Interface '/wire' is defined by"
    )
  )

  test(
    name = "elastic_fork_join",
    description = "Fork and join components exercise internal wires and multi-port component graph naming.",
    gen = () => new ElasticForkJoinTop,
    checks = Seq(
      SystemVerilog contains "module ElasticForkJoinTop",
      ModuleGraphJson contains "Fork",
      ModuleGraphJson contains "Join",
      ModuleGraphJson contains "/source",
      ModuleGraphJson contains "/sink",
      Log excludes "Interface never marked!",
      Log excludes "more than 1 times"
    )
  )

  test(
    name = "elastic_nested",
    description = "Two child modules are chained through elastic IO to check child IO graph paths.",
    gen = () => new ElasticNestedTop,
    checks = Seq(
      SystemVerilog contains "module ElasticNestedTop",
      SystemVerilog contains "module ElasticLeaf",
      ModuleGraphJson contains "/leaf0/source",
      ModuleGraphJson contains "/leaf1/sink",
      ModuleGraphJson contains "/source",
      ModuleGraphJson contains "/sink",
      Log excludes "Interface never marked!",
      Log excludes "more than 1 times"
    )
  )

  test(
    name = "elastic_nested_duplicate",
    description = "A child module output is consumed twice by the parent, so tracking should name the child IO cleanly.",
    gen = () => new ElasticNestedDuplicateTop,
    checks = Seq(
      ModuleGraphJson contains "/leaf/source",
      ModuleGraphJson contains "/leaf/sink",
      ModuleGraphJson contains "/sink0",
      ModuleGraphJson contains "/sink1",
      Log contains "Interface marked as source more than 1 times!",
      Log contains "Interface '/leaf/sink' is defined by"
    )
  )

  test(
    name = "axi_full_native",
    description = "Native AXI4 full elastic interfaces connect without raw DataView conversion.",
    gen = () => new Axi4FullNativeTop,
    checks = Seq(
      SystemVerilog contains "module Axi4FullNativeTop",
      ModuleGraphJson contains "/s_axi_ar",
      ModuleGraphJson contains "/m_axi_r",
      ModuleGraphJson contains "\"master_ar\"",
      ModuleGraphJson contains "\"slave_b\"",
      ModuleGraphJson.occurs("\"boundary\" : true", 10),
      Log excludes "axi4View",
      Log excludes "Interface never marked!",
      Log excludes "more than 1 times"
    )
  )

  test(
    name = "axi_lite_native",
    description = "Native AXI4-Lite connect exposes both aggregate sides as channel-level boundary references.",
    gen = () => new Axi4LiteNativeTop,
    checks = Seq(
      SystemVerilog contains "module Axi4LiteNativeTop",
      ModuleGraphJson contains "/s_axi_ar",
      ModuleGraphJson contains "/m_axi_r",
      ModuleGraphJson contains "\"master_ar\"",
      ModuleGraphJson contains "\"slave_b\"",
      ModuleGraphJson.occurs("\"boundary\" : true", 10),
      Log excludes "axi4View",
      Log excludes "Interface never marked!",
      Log excludes "more than 1 times"
    )
  )

  test(
    name = "axi_full_native_duplicate",
    description = "Native AXI4 full interface channel is consumed twice without any raw DataView conversion.",
    gen = () => new Axi4FullNativeDuplicateTop,
    checks = Seq(
      ModuleGraphJson contains "/s_axi_ar",
      ModuleGraphJson contains "/s_axi_r",
      ModuleGraphJson contains "/s_axi_b",
      Log contains "Interface marked as source more than 1 times!",
      Log contains "Interface '/s_axi_ar' is defined by",
      Log excludes "axi4View"
    )
  )

  test(
    name = "axi4_tracking_incompatible_burst",
    description =
      "AXI4 property incompatibility is reported as axi4.tracking without aborting elaboration.",
    gen = () => new Axi4TrackingFailureTop,
    checks = Seq(
      SystemVerilog contains "module Axi4TrackingFailureTop",
      Log contains "axi4.tracking : error:",
      Log contains "master.read_burstShape",
      Log excludes "elastic.tracking",
      Errors excludes "axi4.tracking"
    )
  )

  test(
    name = "axi4_view_root_once",
    description = "Raw AXI4 root IO is viewed once and the elastic view is reused for all channels.",
    gen = () => new Axi4ViewRootOnceTop,
    checks = Seq(
      SystemVerilog contains "module Axi4ViewRootOnceTop",
      Log excludes "[ WARN ] axi4/axi4View",
      Log excludes "Interface never marked!",
      Log excludes "more than 1 times"
    )
  )

  test(
    name = "axi4_view_repeated",
    description = "The same raw AXI4 root IO is viewed twice, which should show current and previous call sites.",
    gen = () => new Axi4ViewRepeatedRootTop,
    checks = Seq(
      Log contains "[ WARN ] axi4/axi4View : bad use of AXI4 view: raw interface viewed multiple times; current call is .asLite",
      Log contains "Called .asLite by",
      Log contains "Previous calls:",
      Log contains "Chisel .viewAs does not create hardware",
      Log excludes "called outside the root module",
      Log excludes "was not called on root IO"
    )
  )

  test(
    name = "axi4_view_child",
    description = "Raw AXI4 IO is viewed in a child module, which is unsafe even if the child hardware emits.",
    gen = () => new Axi4ViewChildTop,
    checks = Seq(
      Log contains "[ WARN ] axi4/axi4View : bad use of AXI4 view: .asLite called outside the root module",
      Log contains "Safe pattern: call .asLite/.asFull exactly once at the root module on root IO",
      Log excludes "bad use of AXI4 view: .asLite was not called on root IO"
    )
  )

  test(
    name = "axi4_view_root_wire",
    description = "Raw AXI4 root wire is viewed; it is root-module use but not root IO.",
    gen = () => new Axi4ViewRootWireTop,
    checks = Seq(
      Log contains "[ WARN ] axi4/axi4View : bad use of AXI4 view: .asLite was not called on root IO",
      Log excludes "called outside the root module"
    )
  )

  test(
    name = "axi4_view_nested",
    description = "A child module views each raw AXI4 IO once and the parent connects raw IO around it.",
    gen = () => new Axi4ViewNestedTop,
    checks = Seq(
      SystemVerilog contains "module Axi4ViewNestedTop",
      SystemVerilog contains "module Axi4ViewLeaf",
      ModuleGraphJson contains "/leaf/s_axi$view_ar",
      ModuleGraphJson contains "/leaf/m_axi$view_r",
      Log contains "called outside the root module",
      Log excludes "Encountered an interface which is neither an IO or Wire."
    )
  )

  test(
    name = "axi4_view_null",
    description = "Raw AXI4 root IOs are viewed once and tied off through elastic null endpoints.",
    gen = () => new Axi4ViewNullTop,
    checks = Seq(
      SystemVerilog contains "module Axi4ViewNullTop",
      ModuleGraphJson contains "/s_axi$view_ar",
      ModuleGraphJson contains "/m_axi$view_r",
      Log excludes "raw interface viewed multiple times"
    )
  )

  test(
    name = "axi4_view_zero",
    description = "Raw AXI4 root IOs are viewed once but not connected to tracking components.",
    expected = Failure,
    gen = () => new Axi4ViewZeroTop,
    checks = Seq(
      Log contains "Interface never marked!",
      Log contains "Interface '/s_axi$view_ar' is defined by",
      FailureMessage contains "not fully initialized"
    )
  )

  test(
    name = "axi4_view_missing_channel",
    description = "A raw AXI4 root IO is viewed once but one channel is left unused.",
    expected = Failure,
    gen = () => new Axi4ViewMissingChannelTop,
    checks = Seq(
      ModuleGraphJson contains "/s_axi$view_ar",
      ModuleGraphJson contains "/s_axi$view_b",
      Log contains "Interface never marked!",
      Log contains "Interface '/s_axi$view_r' is defined by",
      Log excludes "raw interface viewed multiple times",
      FailureMessage contains "not fully initialized"
    )
  )

  test(
    name = "axi4_view_duplicate_channel",
    description = "A raw AXI4 root IO is viewed once but one viewed channel endpoint is used twice.",
    gen = () => new Axi4ViewDuplicatedChannelTop,
    checks = Seq(
      ModuleGraphJson.occurs("/s_axi$view_ar", 2),
      Log contains "Interface marked as source more than 1 times!",
      Log contains "Interface '/s_axi$view_ar' is defined by",
      Log excludes "raw interface viewed multiple times"
    )
  )

  test(
    name = "axi4_view_multi_connect",
    description = "Two raw AXI4 root IOs are viewed once, then the viewed channels are connected twice.",
    gen = () => new Axi4ViewMultiConnectTop,
    checks = Seq(
      ModuleGraphJson contains "/s_axi$view_ar",
      ModuleGraphJson contains "/m_axi$view_r",
      Log contains "Interface marked as source more than 1 times!",
      Log contains "Interface marked as sink more than 1 times!",
      Log contains "Interface '/s_axi$view_ar' is defined by",
      Log contains "Interface '/m_axi$view_b' is defined by",
      Log excludes "raw interface viewed multiple times"
    )
  )

  test(
    name = "axi4_view_wrong_role",
    description = "A viewed AXI4 endpoint is connected with the wrong channel role; tracking should catch the view role mismatch early.",
    expected = Failure,
    gen = () => new Axi4ViewWrongRoleTop,
    checks = Seq(
      FailureMessage contains "is declared as a Sink, but marked as Source"
    )
  )

  test(
    name = "axi4s_view_root_once",
    description = "Raw AXI4 Stream root IO is viewed once and the elastic view is reused.",
    gen = () => new Axi4sViewRootOnceTop,
    checks = Seq(
      SystemVerilog contains "module Axi4sViewRootOnceTop",
      Log excludes "[ WARN ] axi4s/axi4sView",
      Log excludes "Interface never marked!",
      Log excludes "more than 1 times"
    )
  )

  test(
    name = "axi4s_view_repeated",
    description = "The same raw AXI4 Stream root IO is viewed twice.",
    gen = () => new Axi4sViewRepeatedRootTop,
    checks = Seq(
      Log contains "[ WARN ] axi4s/axi4sView : bad use of AXI4 Stream view: raw interface viewed multiple times; current call is .asFull",
      Log contains "Called .asFull by",
      Log contains "Previous calls:",
      Log contains "Chisel .viewAs does not create hardware",
      Log excludes "called outside the root module",
      Log excludes "was not called on root IO"
    )
  )

  test(
    name = "axi4s_view_child",
    description = "Raw AXI4 Stream IO is viewed in a child module.",
    gen = () => new Axi4sViewChildTop,
    checks = Seq(
      Log contains "[ WARN ] axi4s/axi4sView : bad use of AXI4 Stream view: .asFull called outside the root module",
      Log contains "Safe pattern: call .asLite/.asFull exactly once at the root module on root IO",
      Log excludes "bad use of AXI4 Stream view: .asFull was not called on root IO"
    )
  )

  test(
    name = "axi4s_view_root_wire",
    description = "Raw AXI4 Stream root wire is viewed, so the root-IO warning should fire.",
    gen = () => new Axi4sViewRootWireTop,
    checks = Seq(
      Log contains "[ WARN ] axi4s/axi4sView : bad use of AXI4 Stream view: .asFull was not called on root IO",
      Log excludes "called outside the root module"
    )
  )

  test(
    name = "axi4s_view_pass",
    description = "Raw AXI4 Stream slave and master root IOs are viewed once and connected through an elastic transform.",
    gen = () => new Axi4sViewPassTop,
    checks = Seq(
      SystemVerilog contains "module Axi4sViewPassTop",
      ModuleGraphJson contains "/s_axis$view",
      ModuleGraphJson contains "/m_axis$view",
      Log excludes "raw interface viewed multiple times",
      Log excludes "Interface never marked!",
      Log excludes "more than 1 times"
    )
  )

  test(
    name = "axi4s_view_null",
    description = "Raw AXI4 Stream root IOs are viewed once and tied off with elastic null endpoints.",
    gen = () => new Axi4sViewNullTop,
    checks = Seq(
      SystemVerilog contains "module Axi4sViewNullTop",
      ModuleGraphJson contains "/s_axis$view",
      ModuleGraphJson contains "/m_axis$view",
      Log excludes "raw interface viewed multiple times",
      Log excludes "Interface never marked!",
      Log excludes "more than 1 times"
    )
  )

  test(
    name = "axi4s_view_unused",
    description = "A raw AXI4 Stream root IO is viewed once but not connected to a tracking component.",
    expected = Failure,
    gen = () => new Axi4sViewUnusedTop,
    checks = Seq(
      Log contains "Interface never marked!",
      Log contains "Interface '/axis$view'",
      FailureMessage contains "not fully initialized"
    )
  )

  test(
    name = "axi4s_view_duplicate_source",
    description = "A raw AXI4 Stream slave view is consumed twice, so tracking should report repeated source use.",
    gen = () => new Axi4sViewDuplicateSourceTop,
    checks = Seq(
      ModuleGraphJson contains "/axis$view",
      Log contains "Interface marked as source more than 1 times!",
      Log contains "Interface '/axis$view'",
      Log excludes "raw interface viewed multiple times"
    )
  )

  test(
    name = "axi4s_view_duplicate_sink",
    description = "A raw AXI4 Stream master view is driven twice, so tracking should report repeated sink use.",
    gen = () => new Axi4sViewDuplicateSinkTop,
    checks = Seq(
      ModuleGraphJson contains "/axis$view",
      Log contains "Interface marked as sink more than 1 times!",
      Log contains "Interface '/axis$view'",
      Log excludes "raw interface viewed multiple times"
    )
  )
  runTests()
}
