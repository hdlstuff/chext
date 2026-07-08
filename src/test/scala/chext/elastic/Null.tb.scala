package chext.elastic

import chisel3._

import chext.{elastic => e}

class Null_Tbtop extends Module with chext.AnnotatedModule {
  val source = IO(e.Source(UInt(32.W)))
  val sink = IO(e.Sink(UInt(32.W)))

  val nullSink0 = new e.NullSink(source)
  val nullSource0 = new e.NullSource(sink)

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(source, "In")
  declareElasticInterface(sink, "Out")
}

object Null_Tb extends chext.TestBench {
  emit(new Null_Tbtop)
}
