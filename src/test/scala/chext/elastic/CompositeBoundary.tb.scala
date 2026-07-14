package chext.elastic

import chisel3._

class CompositeBoundary_Tbtop extends Module with chext.AnnotatedModule {
  val sourceLoop = IO(Source(UInt(8.W)))
  val sinkLoop = IO(Sink(UInt(8.W)))

  val loop0 = new Loop(sourceLoop, sinkLoop) {
    end { _ => true.B }
  }
  val connectLoopBody0 = new Connect(loop0.sinkCurrent, loop0.sourceNext)

  val sourceScope = IO(Source(UInt(8.W)))
  val sinkScope = IO(Sink(UInt(8.W)))

  val scope0 = new Scope(sourceScope, sinkScope)
  val connectScopeBody0 = new Connect(scope0.sinkBegin, scope0.sourceEnd)

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(sourceLoop, "LoopIn")
  declareElasticInterface(sinkLoop, "LoopOut")
  declareElasticInterface(sourceScope, "ScopeIn")
  declareElasticInterface(sinkScope, "ScopeOut")
}

object CompositeBoundary_Tb extends chext.TestBench {
  emit(new CompositeBoundary_Tbtop)
}
