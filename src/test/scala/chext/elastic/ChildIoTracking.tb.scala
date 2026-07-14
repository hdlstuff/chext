package chext.elastic

import chisel3._
import circt.stage.ChiselStage

import chext.elastic.ConnectOp._

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

private class ChildIoTracking_Tbtop(manualChildTracking: Boolean)
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

object ChildIoTracking_Tb extends App {
  val systemVerilog = ChiselStage.emitSystemVerilog(
    new ChildIoTracking_Tbtop(manualChildTracking = true)
  )
  assert(systemVerilog.contains("module PassiveElasticChild"))

  val missingTrackingFailure =
    try {
      ChiselStage.emitSystemVerilog(
        new ChildIoTracking_Tbtop(manualChildTracking = false)
      )
      Option.empty[Throwable]
    } catch {
      case exception: Throwable => Some(exception)
    }

  val failureMessages = Iterator
    .iterate(missingTrackingFailure.orNull)(_.getCause)
    .takeWhile(_ != null)
    .flatMap(exception => Option(exception.getMessage))
    .mkString("\n")

  assert(missingTrackingFailure.nonEmpty)
  assert(failureMessages.contains("has no Elastic tracking graph"))
  assert(failureMessages.contains("chext.elastic.tracking.register()"))
}
