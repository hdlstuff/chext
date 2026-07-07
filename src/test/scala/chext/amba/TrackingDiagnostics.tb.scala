package chext.amba

import chisel3._
import chisel3.experimental.prefix
import circt.stage.ChiselStage

import java.io.{ByteArrayOutputStream, PrintStream, PrintWriter, StringWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import scala.util.control.NonFatal

import chext.{elastic => e}
import chext.elastic.ConnectOp._
import chext.amba.{axi4, axi4s}
import chext.amba.axi4.Casts._
import chext.amba.axi4.ConnectOp._
import chext.amba.axi4.full.{ConnectOp => Axi4FullConnectOp}
import chext.amba.axi4s.Casts._
import io.circe.generic.auto._
import io.circe.syntax._

object TrackingDiagnosticsEmit {
  case class Result(
      sv: Option[String],
      graph: Option[String],
      log: String,
      failure: Option[Throwable]
  )

  def apply(gen: => RawModule): Result = {
    val logBytes = new ByteArrayOutputStream()
    val logStream = new PrintStream(logBytes)
    var graph = Option.empty[String]

    val result =
      try {
        val sv = Console.withOut(logStream) {
          Console.withErr(logStream) {
            ChiselStage.emitSystemVerilog(
              {
                val module = gen

                chext.tracking.onComplete(module) {
                  graph = chext.elastic.tracking.moduleGraphOption(module).map(_.flatten.asJson.toString())
                }

                module
              },
              args = Array("--no-source-info"),
              firtoolOpts = Array(
                "-disable-all-randomization",
                "-strip-debug-info",
                "-default-layer-specialization=enable"
              )
            )
          }
        }

        Result(Some(sv), graph, "", None)
      } catch {
        case NonFatal(e) => Result(None, graph, "", Some(e))
      }

    logStream.flush()
    result.copy(log = logBytes.toString(StandardCharsets.UTF_8))
  }
}

object TrackingDiagnosticsOutput {
  val dir: Path = Path.of("output", "tracking_diagnostics")

  def init(): Unit = {
    Files.createDirectories(dir)
    Files.deleteIfExists(dir.resolve("report.txt"))
  }

  def nameFor(name: String): String =
    name.toLowerCase.replaceAll("[^a-z0-9]+", "_").stripPrefix("_").stripSuffix("_")

  def pathFor(name: String): Path =
    dir.resolve(s"${nameFor(name)}.txt")

  def write(name: String, contents: String): Path = {
    val path = pathFor(name)
    Files.writeString(path, contents, StandardCharsets.UTF_8)
    path
  }
}

object TrackingDiagnosticsUtil {
  val sectionLine: String = "=" * 80

  def stackTrace(t: Throwable): String = {
    val writer = new StringWriter()
    t.printStackTrace(new PrintWriter(writer))
    writer.toString
  }

  def section(name: String, body: String): String =
    s"> $name\n$body\n$sectionLine\n"

  def count(haystack: String, needle: String): Int =
    java.util.regex.Pattern.quote(needle).r.findAllMatchIn(haystack).length

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

private class ElasticConnectTop extends Module {
  val source = IO(e.Source(UInt(8.W)))
  val sink = IO(e.Sink(UInt(8.W)))

  source :=> sink
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

private class Axi4ViewRootOnceTop extends Module {
  val axi = IO(axi4.Slave(axi4.Config(lite = true)))

  private val view = axi.asLite

  val nullSinkAr0 = new e.NullSink(view.ar)
  val nullSourceR0 = new e.NullSource(view.r)
  val nullSinkAw0 = new e.NullSink(view.aw)
  val nullSinkW0 = new e.NullSink(view.w)
  val nullSourceB0 = new e.NullSource(view.b)
}

private class Axi4ViewRepeatedRootTop extends Module {
  import TrackingDiagnosticsUtil._

  val axi = IO(axi4.Slave(axi4.Config(lite = true)))

  axi.asLite
  axi.asLite
  driveAxi4Slave(axi)
}

private class Axi4ViewChild extends Module {
  import TrackingDiagnosticsUtil._

  val axi = IO(axi4.Slave(axi4.Config(lite = true)))

  axi.asLite
  driveAxi4Slave(axi)
}

private class Axi4ViewChildTop extends Module {
  import TrackingDiagnosticsUtil._

  private val child = Module(new Axi4ViewChild)

  driveAxi4SlaveInputs(child.axi)
}

private class Axi4ViewRootWireTop extends Module {
  private val axi = Wire(axi4.Slave(axi4.Config(lite = true)))

  axi := DontCare
  axi.asLite
}

private class Axi4ViewLeaf extends Module {
  private val cfg = axi4.Config(wId = 2, wAddr = 16, wData = 32, wUserAR = 1, wUserR = 1)

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
}

private class Axi4ViewNestedTop extends Module {
  private val cfg = axi4.Config(wId = 2, wAddr = 16, wData = 32, wUserAR = 1, wUserR = 1)

  val s_axi = IO(axi4.Slave(cfg))
  val m_axi = IO(axi4.Master(cfg))

  private val leaf = Module(new Axi4ViewLeaf)

  s_axi :=> leaf.s_axi
  leaf.m_axi :=> m_axi
}

private class Axi4ViewNullTop extends Module {
  private val cfg = axi4.Config(wId = 1, wAddr = 16, wData = 32)

  val s_axi = IO(axi4.Slave(cfg))
  val m_axi = IO(axi4.Master(cfg))

  private val s_view = s_axi.asFull
  private val m_view = m_axi.asFull

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
  private val cfg = axi4.Config(wId = 1, wAddr = 16, wData = 32)

  val s_axi = IO(axi4.Slave(cfg))
  val m_axi = IO(axi4.Master(cfg))

  private val s_view = s_axi.asFull
  private val m_view = m_axi.asFull

  dontTouch(s_view.ar.$valid)
  dontTouch(m_view.r.$valid)
}

private class Axi4ViewMissingChannelTop extends Module {
  private val cfg = axi4.Config(wId = 1, wAddr = 16, wData = 32)

  val s_axi = IO(axi4.Slave(cfg))

  private val s_view = s_axi.asFull

  val nullSinkAr0 = new e.NullSink(s_view.ar)
  val nullSinkAw0 = new e.NullSink(s_view.aw)
  val nullSinkW0 = new e.NullSink(s_view.w)
  val nullSourceB0 = new e.NullSource(s_view.b)

  dontTouch(s_view.r.$valid)
}

private class Axi4ViewDuplicatedChannelTop extends Module {
  private val cfg = axi4.Config(wId = 1, wAddr = 16, wData = 32)

  val s_axi = IO(axi4.Slave(cfg))

  private val s_view = s_axi.asFull

  val nullSinkAr0 = new e.NullSink(s_view.ar)
  val nullSinkAr1 = new e.NullSink(s_view.ar)
  val nullSourceR0 = new e.NullSource(s_view.r)
  val nullSinkAw0 = new e.NullSink(s_view.aw)
  val nullSinkW0 = new e.NullSink(s_view.w)
  val nullSourceB0 = new e.NullSource(s_view.b)
}

private class Axi4ViewMultiConnectTop extends Module {
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

object TrackingDiagnostics_Tb extends App {
  case class TestCase(
      name: String,
      description: String,
      shouldPass: Boolean,
      gen: () => RawModule,
      svContains: Seq[String] = Seq.empty,
      graphContains: Seq[String] = Seq.empty,
      graphExcludes: Seq[String] = Seq("Encountered an interface which is neither an IO or Wire."),
      graphOccurrences: Seq[(String, Int)] = Seq.empty,
      logContains: Seq[String] = Seq.empty,
      logExcludes: Seq[String] = Seq.empty,
      failureContains: Seq[String] = Seq.empty,
      allowPathWarnings: Boolean = false
  )

  case class CaseResult(
      testCase: TestCase,
      result: TrackingDiagnosticsEmit.Result,
      issues: Seq[String]
  ) {
    def passed: Boolean = issues.isEmpty
  }

  private def validate(testCase: TestCase, result: TrackingDiagnosticsEmit.Result): Seq[String] = {
    val issues = Vector.newBuilder[String]
    val sv = result.sv.getOrElse("")
    val graph = result.graph.getOrElse("")
    val log = result.log
    val failureMessage = result.failure.flatMap(e => Option(e.getMessage)).getOrElse("")

    if (testCase.shouldPass && result.failure.nonEmpty)
      issues.addOne(s"expected emit success, but failed with: $failureMessage")

    if (!testCase.shouldPass && result.failure.isEmpty)
      issues.addOne("expected emit failure, but emit succeeded")

    if (!testCase.allowPathWarnings && log.contains("[ WARN ] tracking/pathChecks"))
      issues.addOne("log unexpectedly contained tracking path warning")

    testCase.svContains.foreach { needle =>
      if (!sv.contains(needle))
        issues.addOne(s"SystemVerilog did not contain: $needle")
    }

    testCase.graphContains.foreach { needle =>
      if (!graph.contains(needle))
        issues.addOne(s"module graph did not contain: $needle")
    }

    testCase.graphExcludes.foreach { needle =>
      if (graph.contains(needle) || log.contains(needle))
        issues.addOne(s"module graph/log unexpectedly contained: $needle")
    }

    testCase.graphOccurrences.foreach { case (needle, expected) =>
      val actual = TrackingDiagnosticsUtil.count(graph, needle)
      if (actual != expected)
        issues.addOne(s"module graph contained '$needle' $actual times, expected $expected")
    }

    testCase.logContains.foreach { needle =>
      if (!log.contains(needle))
        issues.addOne(s"log did not contain: $needle")
    }

    testCase.logExcludes.foreach { needle =>
      if (log.contains(needle))
        issues.addOne(s"log unexpectedly contained: $needle")
    }

    testCase.failureContains.foreach { needle =>
      if (!failureMessage.contains(needle))
        issues.addOne(s"failure message did not contain: $needle")
    }

    issues.result()
  }

  private def reportFor(caseResult: CaseResult): String = {
    val tc = caseResult.testCase
    val result = caseResult.result
    val failureText =
      result.failure match {
        case Some(value) => TrackingDiagnosticsUtil.stackTrace(value)
        case None        => "(none)"
      }

    val status =
      if (caseResult.passed && tc.shouldPass) "PASS"
      else if (caseResult.passed) "EXPECTED FAILURE"
      else "FAIL"

    Seq(
      TrackingDiagnosticsUtil.section("TEST", tc.name),
      TrackingDiagnosticsUtil.section("DESCRIPTION", tc.description),
      TrackingDiagnosticsUtil.section("STATUS", status),
      TrackingDiagnosticsUtil.section(
        "ASSERTION ERRORS",
        if (caseResult.issues.isEmpty) "(none)" else caseResult.issues.mkString("\n")
      ),
      TrackingDiagnosticsUtil.section("SYSTEMVERILOG", result.sv.getOrElse("(not emitted)")),
      TrackingDiagnosticsUtil.section("LOG", if (result.log.nonEmpty) result.log else "(empty)"),
      TrackingDiagnosticsUtil.section("ERRORS", failureText),
      TrackingDiagnosticsUtil.section("MODULE GRAPH", result.graph.getOrElse("(not available)"))
    ).mkString
  }

  TrackingDiagnosticsOutput.init()

  private val cases = Seq(
    TestCase(
      name = "elastic_connect",
      description = "Plain elastic source-to-sink connection without any DataView usage.",
      shouldPass = true,
      gen = () => new ElasticConnectTop,
      svContains = Seq("module ElasticConnectTop"),
      graphContains = Seq("/source", "/sink"),
      logExcludes = Seq("Interface never marked!", "more than 1 times")
    ),
    TestCase(
      name = "elastic_null",
      description = "Plain elastic IO endpoints are consumed and produced by null components.",
      shouldPass = true,
      gen = () => new ElasticNullTop,
      svContains = Seq("module ElasticNullTop"),
      graphContains = Seq("/source", "/sink"),
      logExcludes = Seq("Interface never marked!", "more than 1 times")
    ),
    TestCase(
      name = "tracking_path_warning",
      description = "Two deliberately same-prefixed components verify that tracking path warnings are still reported separately.",
      shouldPass = true,
      gen = () => new ElasticPathWarningTop,
      svContains = Seq("module ElasticPathWarningTop"),
      logContains = Seq(
        "[ WARN ] tracking/pathChecks : Multiple components or containers use the same path, which should be avoided"
      ),
      allowPathWarnings = true
    ),
    TestCase(
      name = "elastic_unused",
      description = "A root elastic source is not used, so tracking should warn before FIRRTL reports incomplete initialization.",
      shouldPass = false,
      gen = () => new ElasticUnusedTop,
      logContains = Seq("Interface never marked!", "Interface '/source' is defined by"),
      failureContains = Seq("not fully initialized")
    ),
    TestCase(
      name = "elastic_duplicate_source",
      description = "One source IO feeds two sinks; hardware can emit, but tracking must report repeated source use.",
      shouldPass = true,
      gen = () => new ElasticDuplicateSourceTop,
      graphContains = Seq("/source", "/sink0", "/sink1"),
      logContains = Seq("Interface marked as source more than 1 times!", "Interface '/source' is defined by")
    ),
    TestCase(
      name = "elastic_duplicate_sink",
      description = "Two source IOs feed one sink; hardware can emit, but tracking must report repeated sink use.",
      shouldPass = true,
      gen = () => new ElasticDuplicateSinkTop,
      graphContains = Seq("/source0", "/source1", "/sink"),
      logContains = Seq("Interface marked as sink more than 1 times!", "Interface '/sink' is defined by")
    ),
    TestCase(
      name = "elastic_wrong_role",
      description = "A source endpoint is used as a sink and a sink endpoint is used as a source.",
      shouldPass = false,
      gen = () => new ElasticWrongRoleTop,
      failureContains = Seq("declared as a Sink, but marked as Source")
    ),
    TestCase(
      name = "elastic_wire_once",
      description = "An internal elastic wire is used once as a sink and once as a source.",
      shouldPass = true,
      gen = () => new ElasticWireOnceTop,
      graphContains = Seq("/wire"),
      logExcludes = Seq("Interface never marked!", "more than 1 times")
    ),
    TestCase(
      name = "elastic_wire_unused",
      description = "An internal elastic wire is never marked by a component.",
      shouldPass = false,
      gen = () => new ElasticWireUnusedTop,
      logContains = Seq("Interface never marked!", "Interface '/wire' is defined by"),
      failureContains = Seq("not fully initialized")
    ),
    TestCase(
      name = "elastic_wire_duplicate_source",
      description = "An internal elastic wire is driven once but consumed by two sinks.",
      shouldPass = true,
      gen = () => new ElasticWireDuplicateSourceTop,
      logContains = Seq("Interface marked as source more than 1 times!", "Interface '/wire' is defined by")
    ),
    TestCase(
      name = "elastic_wire_duplicate_sink",
      description = "Two sources drive the same internal elastic wire.",
      shouldPass = true,
      gen = () => new ElasticWireDuplicateSinkTop,
      logContains = Seq("Interface marked as sink more than 1 times!", "Interface '/wire' is defined by")
    ),
    TestCase(
      name = "elastic_fork_join",
      description = "Fork and join components exercise internal wires and multi-port component graph naming.",
      shouldPass = true,
      gen = () => new ElasticForkJoinTop,
      svContains = Seq("module ElasticForkJoinTop"),
      graphContains = Seq("Fork", "Join", "/source", "/sink"),
      logExcludes = Seq("Interface never marked!", "more than 1 times")
    ),
    TestCase(
      name = "elastic_nested",
      description = "Two child modules are chained through elastic IO to check child IO graph paths.",
      shouldPass = true,
      gen = () => new ElasticNestedTop,
      svContains = Seq("module ElasticNestedTop", "module ElasticLeaf"),
      graphContains = Seq("/leaf0/source", "/leaf1/sink", "/source", "/sink"),
      logExcludes = Seq("Interface never marked!", "more than 1 times")
    ),
    TestCase(
      name = "elastic_nested_duplicate",
      description = "A child module output is consumed twice by the parent, so tracking should name the child IO cleanly.",
      shouldPass = true,
      gen = () => new ElasticNestedDuplicateTop,
      graphContains = Seq("/leaf/source", "/leaf/sink", "/sink0", "/sink1"),
      logContains = Seq("Interface marked as source more than 1 times!", "Interface '/leaf/sink' is defined by")
    ),
    TestCase(
      name = "axi_full_native",
      description = "Native AXI4 full elastic interfaces connect without raw DataView conversion.",
      shouldPass = true,
      gen = () => new Axi4FullNativeTop,
      svContains = Seq("module Axi4FullNativeTop"),
      graphContains = Seq("/s_axi.ar", "/m_axi.r"),
      logExcludes = Seq("axi4View", "Interface never marked!", "more than 1 times")
    ),
    TestCase(
      name = "axi_full_native_duplicate",
      description = "Native AXI4 full interface channel is consumed twice without any raw DataView conversion.",
      shouldPass = true,
      gen = () => new Axi4FullNativeDuplicateTop,
      graphContains = Seq("/s_axi.ar", "/s_axi.r", "/s_axi.b"),
      logContains = Seq("Interface marked as source more than 1 times!", "Interface '/s_axi.ar' is defined by"),
      logExcludes = Seq("axi4View")
    ),
    TestCase(
      name = "axi4_view_root_once",
      description = "Raw AXI4 root IO is viewed once and the elastic view is reused for all channels.",
      shouldPass = true,
      gen = () => new Axi4ViewRootOnceTop,
      svContains = Seq("module Axi4ViewRootOnceTop"),
      logExcludes = Seq("[ WARN ] axi4/axi4View", "Interface never marked!", "more than 1 times")
    ),
    TestCase(
      name = "axi4_view_repeated",
      description = "The same raw AXI4 root IO is viewed twice, which should show current and previous call sites.",
      shouldPass = true,
      gen = () => new Axi4ViewRepeatedRootTop,
      logContains = Seq(
        "[ WARN ] axi4/axi4View : bad use of AXI4 view: raw interface viewed multiple times; current call is .asLite",
        "Called .asLite by",
        "Previous calls:",
        "Chisel .viewAs does not create hardware"
      ),
      logExcludes = Seq("called outside the root module", "was not called on root IO")
    ),
    TestCase(
      name = "axi4_view_child",
      description = "Raw AXI4 IO is viewed in a child module, which is unsafe even if the child hardware emits.",
      shouldPass = true,
      gen = () => new Axi4ViewChildTop,
      logContains = Seq(
        "[ WARN ] axi4/axi4View : bad use of AXI4 view: .asLite called outside the root module",
        "Safe pattern: call .asLite/.asFull exactly once at the root module on root IO"
      ),
      logExcludes = Seq("bad use of AXI4 view: .asLite was not called on root IO")
    ),
    TestCase(
      name = "axi4_view_root_wire",
      description = "Raw AXI4 root wire is viewed; it is root-module use but not root IO.",
      shouldPass = true,
      gen = () => new Axi4ViewRootWireTop,
      logContains = Seq("[ WARN ] axi4/axi4View : bad use of AXI4 view: .asLite was not called on root IO"),
      logExcludes = Seq("called outside the root module")
    ),
    TestCase(
      name = "axi4_view_nested",
      description = "A child module views each raw AXI4 IO once and the parent connects raw IO around it.",
      shouldPass = true,
      gen = () => new Axi4ViewNestedTop,
      svContains = Seq("module Axi4ViewNestedTop", "module Axi4ViewLeaf"),
      graphContains = Seq("/leaf/s_axi$view.ar", "/leaf/m_axi$view.r"),
      logContains = Seq("called outside the root module"),
      logExcludes = Seq("Encountered an interface which is neither an IO or Wire.")
    ),
    TestCase(
      name = "axi4_view_null",
      description = "Raw AXI4 root IOs are viewed once and tied off through elastic null endpoints.",
      shouldPass = true,
      gen = () => new Axi4ViewNullTop,
      svContains = Seq("module Axi4ViewNullTop"),
      graphContains = Seq("/s_axi$view.ar", "/m_axi$view.r"),
      logExcludes = Seq("raw interface viewed multiple times")
    ),
    TestCase(
      name = "axi4_view_zero",
      description = "Raw AXI4 root IOs are viewed once but not connected to tracking components.",
      shouldPass = false,
      gen = () => new Axi4ViewZeroTop,
      logContains = Seq("Interface never marked!", "Interface '/s_axi$view.ar' is defined by"),
      failureContains = Seq("not fully initialized")
    ),
    TestCase(
      name = "axi4_view_missing_channel",
      description = "A raw AXI4 root IO is viewed once but one channel is left unused.",
      shouldPass = false,
      gen = () => new Axi4ViewMissingChannelTop,
      graphContains = Seq("/s_axi$view.ar", "/s_axi$view.b"),
      logContains = Seq("Interface never marked!", "Interface '/s_axi$view.r' is defined by"),
      logExcludes = Seq("raw interface viewed multiple times"),
      failureContains = Seq("not fully initialized")
    ),
    TestCase(
      name = "axi4_view_duplicate_channel",
      description = "A raw AXI4 root IO is viewed once but one viewed channel endpoint is used twice.",
      shouldPass = true,
      gen = () => new Axi4ViewDuplicatedChannelTop,
      graphOccurrences = Seq("/s_axi$view.ar" -> 2),
      logContains = Seq(
        "Interface marked as source more than 1 times!",
        "Interface '/s_axi$view.ar' is defined by"
      ),
      logExcludes = Seq("raw interface viewed multiple times")
    ),
    TestCase(
      name = "axi4_view_multi_connect",
      description = "Two raw AXI4 root IOs are viewed once, then the viewed channels are connected twice.",
      shouldPass = true,
      gen = () => new Axi4ViewMultiConnectTop,
      graphContains = Seq("/s_axi$view.ar", "/m_axi$view.r"),
      logContains = Seq(
        "Interface marked as source more than 1 times!",
        "Interface marked as sink more than 1 times!",
        "Interface '/s_axi$view.ar' is defined by",
        "Interface '/m_axi$view.b' is defined by"
      ),
      logExcludes = Seq("raw interface viewed multiple times")
    ),
    TestCase(
      name = "axi4_view_wrong_role",
      description = "A viewed AXI4 endpoint is connected with the wrong channel role; tracking should catch the view role mismatch early.",
      shouldPass = false,
      gen = () => new Axi4ViewWrongRoleTop,
      failureContains = Seq("is declared as a Sink, but marked as Source")
    ),
    TestCase(
      name = "axi4s_view_root_once",
      description = "Raw AXI4 Stream root IO is viewed once and the elastic view is reused.",
      shouldPass = true,
      gen = () => new Axi4sViewRootOnceTop,
      svContains = Seq("module Axi4sViewRootOnceTop"),
      logExcludes = Seq("[ WARN ] axi4s/axi4sView", "Interface never marked!", "more than 1 times")
    ),
    TestCase(
      name = "axi4s_view_repeated",
      description = "The same raw AXI4 Stream root IO is viewed twice.",
      shouldPass = true,
      gen = () => new Axi4sViewRepeatedRootTop,
      logContains = Seq(
        "[ WARN ] axi4s/axi4sView : bad use of AXI4 Stream view: raw interface viewed multiple times; current call is .asFull",
        "Called .asFull by",
        "Previous calls:",
        "Chisel .viewAs does not create hardware"
      ),
      logExcludes = Seq("called outside the root module", "was not called on root IO")
    ),
    TestCase(
      name = "axi4s_view_child",
      description = "Raw AXI4 Stream IO is viewed in a child module.",
      shouldPass = true,
      gen = () => new Axi4sViewChildTop,
      logContains = Seq(
        "[ WARN ] axi4s/axi4sView : bad use of AXI4 Stream view: .asFull called outside the root module",
        "Safe pattern: call .asLite/.asFull exactly once at the root module on root IO"
      ),
      logExcludes = Seq("bad use of AXI4 Stream view: .asFull was not called on root IO")
    ),
    TestCase(
      name = "axi4s_view_root_wire",
      description = "Raw AXI4 Stream root wire is viewed, so the root-IO warning should fire.",
      shouldPass = true,
      gen = () => new Axi4sViewRootWireTop,
      logContains = Seq("[ WARN ] axi4s/axi4sView : bad use of AXI4 Stream view: .asFull was not called on root IO"),
      logExcludes = Seq("called outside the root module")
    ),
    TestCase(
      name = "axi4s_view_pass",
      description = "Raw AXI4 Stream slave and master root IOs are viewed once and connected through an elastic transform.",
      shouldPass = true,
      gen = () => new Axi4sViewPassTop,
      svContains = Seq("module Axi4sViewPassTop"),
      graphContains = Seq("/s_axis$view", "/m_axis$view"),
      logExcludes = Seq("raw interface viewed multiple times", "Interface never marked!", "more than 1 times")
    ),
    TestCase(
      name = "axi4s_view_null",
      description = "Raw AXI4 Stream root IOs are viewed once and tied off with elastic null endpoints.",
      shouldPass = true,
      gen = () => new Axi4sViewNullTop,
      svContains = Seq("module Axi4sViewNullTop"),
      graphContains = Seq("/s_axis$view", "/m_axis$view"),
      logExcludes = Seq("raw interface viewed multiple times", "Interface never marked!", "more than 1 times")
    ),
    TestCase(
      name = "axi4s_view_unused",
      description = "A raw AXI4 Stream root IO is viewed once but not connected to a tracking component.",
      shouldPass = false,
      gen = () => new Axi4sViewUnusedTop,
      logContains = Seq("Interface never marked!", "Interface '/axis$view'"),
      failureContains = Seq("not fully initialized")
    ),
    TestCase(
      name = "axi4s_view_duplicate_source",
      description = "A raw AXI4 Stream slave view is consumed twice, so tracking should report repeated source use.",
      shouldPass = true,
      gen = () => new Axi4sViewDuplicateSourceTop,
      graphContains = Seq("/axis$view"),
      logContains = Seq("Interface marked as source more than 1 times!", "Interface '/axis$view'"),
      logExcludes = Seq("raw interface viewed multiple times")
    ),
    TestCase(
      name = "axi4s_view_duplicate_sink",
      description = "A raw AXI4 Stream master view is driven twice, so tracking should report repeated sink use.",
      shouldPass = true,
      gen = () => new Axi4sViewDuplicateSinkTop,
      graphContains = Seq("/axis$view"),
      logContains = Seq("Interface marked as sink more than 1 times!", "Interface '/axis$view'"),
      logExcludes = Seq("raw interface viewed multiple times")
    )
  )

  private val results = cases.map { testCase =>
    println(s"[tracking-diagnostics] running: ${testCase.name}")
    val result = TrackingDiagnosticsEmit(testCase.gen())
    val issues = validate(testCase, result)
    val caseResult = CaseResult(testCase, result, issues)

    if (caseResult.passed) {
      val label = if (testCase.shouldPass) "pass" else "expected failure"
      println(s"[tracking-diagnostics] $label: ${testCase.name}")
    } else {
      println(s"[tracking-diagnostics] fail: ${testCase.name}")
    }

    caseResult
  }

  results.foreach { caseResult =>
    TrackingDiagnosticsOutput.write(caseResult.testCase.name, reportFor(caseResult))
  }

  val failures = results.filterNot(_.passed)
  if (failures.nonEmpty) {
    val message =
      failures
        .map { failure => s"${failure.testCase.name}:\n${failure.issues.mkString("\n")}" }
        .mkString("\n\n")
    throw new RuntimeException(
      s"${failures.length} tracking diagnostic test(s) failed; see ${TrackingDiagnosticsOutput.dir}\n$message"
    )
  }

  println(s"[tracking-diagnostics] wrote reports under: ${TrackingDiagnosticsOutput.dir}")
}
