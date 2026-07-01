package chext.elastic

import chisel3._

import chext.{elastic => e}
import e.ConnectOp._
import chext.util.NamedVec

class BufferedNaming_Tbtop extends Module with chext.TestBenchTop {
  val sources = IO(e.Source.many(4, UInt(16.W)))
  val sinks = IO(e.Sink.many(4, UInt(16.W)))

  private val sourcesBuffered = sources.map { source =>
    e.SourceBuffered(source, 2)
  }

  private val sinksBuffered = sinks.map { sink =>
    e.SinkBuffered(sink, 2)
  }

  sourcesBuffered :=> sinksBuffered

  declareClock(clock)
  declareReset(reset)
  sources.foreach { declareElasticInterface(_, "In") }
  sinks.foreach { declareElasticInterface(_, "Out") }
}

object BufferedNaming_Tb extends chext.TestBench {
  emit(new BufferedNaming_Tbtop)
}

class BufferedNamingSeq_Tbtop extends Module with chext.TestBenchTop {
  val sources = IO(e.Source.many(4, UInt(16.W), NamedVec.ZeroExtended()))
  val sinks = IO(e.Sink.many(4, UInt(16.W), NamedVec.ZeroExtended()))

  private val sourcesBuffered = e.SourceBuffered(sources, 2)
  private val sinksBuffered = e.SinkBuffered(sinks, 2)

  sourcesBuffered :=> sinksBuffered

  declareClock(clock)
  declareReset(reset)
  sources.foreach { declareElasticInterface(_, "In") }
  sinks.foreach { declareElasticInterface(_, "Out") }
}

object BufferedNamingSeq_Tb extends chext.TestBench {
  emit(new BufferedNamingSeq_Tbtop)
}
