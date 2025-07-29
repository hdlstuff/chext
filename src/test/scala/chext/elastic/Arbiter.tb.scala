package chext.elastic

import chisel3._
import chisel3.util._

import chext.elastic

import chext.util.VecCustomNamed

class Arbiter_Tbtop extends Module with chext.TestBenchTop {
  val rr_sources = IO(VecCustomNamed.zeroExtended(16, Source(UInt(32.W))))
  val rr_sink = IO(Sink(UInt(32.W)))
  val rr_select = IO(Sink(UInt(4.W)))

  val priority_sources = IO(VecCustomNamed.zeroExtended(16, Source(UInt(32.W))))
  val priority_sink = IO(Sink(UInt(32.W)))
  val priority_select = IO(Sink(UInt(4.W)))

  {
    val arbiter0 = Arbiter(rr_sources.toSeq, rr_sink, Chooser.rr, Some(rr_select))
    val arbiter1 = Arbiter(priority_sources.toSeq, priority_sink, Chooser.rr, Some(priority_select))
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
