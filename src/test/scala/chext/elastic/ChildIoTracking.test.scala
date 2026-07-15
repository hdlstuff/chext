package chext.elastic

import chisel3._

import chext.elastic.ConnectOp._
import chext.util.ElaborationTest

import java.nio.file.Path

private class PassiveElasticChild(manualTracking: Boolean)
    extends Module
    with chext.AnnotatedModule {
  if (manualTracking)
    chext.elastic.tracking.register()

  val source = IO(Source(UInt(8.W)))
  val sink = IO(Sink(UInt(8.W)))

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(source, "Data")
  declareElasticInterface(sink, "Data")

  source.$ready := sink.$ready
  sink.$valid := source.$valid
  sink.$bits := source.$bits
}

private class ChildIoTrackingTestTop(manualChildTracking: Boolean)
    extends Module
    with chext.AnnotatedModule {
  val source = IO(Source(UInt(8.W)))
  val sink = IO(Sink(UInt(8.W)))

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(source, "Data")
  declareElasticInterface(sink, "Data")

  val child = Module(new PassiveElasticChild(manualChildTracking))
  source :=> child.source
  child.sink :=> sink
}

object ChildIoTracking_Test extends App with ElaborationTest {
  suite(
    name = "child-io-tracking",
    outputDir = Path.of("output", "child_io_tracking")
  )

  test(
    name = "manual_child_tracking",
    gen = () => new ChildIoTrackingTestTop(manualChildTracking = true),
    checks = Seq(SystemVerilog contains "module PassiveElasticChild")
  )

  test(
    name = "missing_child_tracking",
    expected = Failure,
    gen = () => new ChildIoTrackingTestTop(manualChildTracking = false),
    checks = Seq(
      Errors contains "has no Elastic tracking graph",
      Errors contains "chext.elastic.tracking.register()"
    )
  )

  runTests()
}
