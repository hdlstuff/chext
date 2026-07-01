package chext.elastic

import chisel3._

import chext.elastic

class Arbiter_Tbtop extends Module with chext.TestBenchTop {
  val rr_sources = IO(Source.many(16, UInt(32.W)))
  val rr_sink = IO(Sink(UInt(32.W)))
  val rr_select = IO(Sink(UInt(4.W)))

  val priority_sources = IO(Source.many(16, UInt(32.W)))
  val priority_sink = IO(Sink(UInt(32.W)))
  val priority_select = IO(Sink(UInt(4.W)))

  {
    val arbiter0 = new Arbiter(rr_sources.toSeq, rr_sink, rr_select, Chooser.rr)
    val arbiter1 =
      new Arbiter(priority_sources.toSeq, priority_sink, priority_select, Chooser.rr)
  }

  declareClock(clock)
  declareReset(reset)

  rr_sources.foreach { declareElasticInterface(_, "In") }
  declareElasticInterface(rr_sink, "Out")
  declareElasticInterface(rr_select, "Index")

  priority_sources.foreach { declareElasticInterface(_, "In") }
  declareElasticInterface(priority_sink, "Out")
  declareElasticInterface(priority_select, "Index")

}

object Arbiter_Tb extends chext.TestBench {
  emit(new Arbiter_Tbtop)
}
