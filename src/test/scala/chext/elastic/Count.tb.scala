package chext.elastic

import chisel3._

import chext.{elastic => e}

class Count_Tbtop extends Module with chext.TestBenchTop {
  val source = IO(e.Source(UInt(32.W)))
  val sink = IO(e.Sink(UInt(32.W)))

  val repeat0 = new e.Repeat(source, sink, 32) {
    len { (in) => in }
    outExplicit { (in, index, first, last, out) => out := 0.U }
  }

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(source, "In")
  declareElasticInterface(sink, "Out")
}

object Count_Tb extends chext.TestBench {
  emit(new Count_Tbtop)
}
