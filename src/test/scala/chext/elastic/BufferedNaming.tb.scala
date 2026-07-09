package chext.elastic

import chisel3._

import chext.{elastic => e}
import e.ConnectOp._
import chext.util.NamedVec

class BufferedNaming_Tbtop extends Module with chext.AnnotatedModule {
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
  declareElasticInterface(sources, "In")
  declareElasticInterface(sinks, "Out")
}

object BufferedNaming_Tb extends chext.TestBench {
  emit(new BufferedNaming_Tbtop)
}

class BufferedNamingSeq_Tbtop extends Module with chext.AnnotatedModule {
  val sources = IO(e.Source.many(4, UInt(16.W), NamedVec.ZeroExtended()))
  val sinks = IO(e.Sink.many(4, UInt(16.W), NamedVec.ZeroExtended()))

  private val sourcesBuffered = e.SourceBuffered(sources, 2)
  private val sinksBuffered = e.SinkBuffered(sinks, 2)

  sourcesBuffered :=> sinksBuffered

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(sources, "In")
  declareElasticInterface(sinks, "Out")
}

object BufferedNamingSeq_Tb extends chext.TestBench {
  emit(new BufferedNamingSeq_Tbtop)
}

class SourceBufferedManyNaming_Tbtop extends Module with chext.AnnotatedModule {
  val sources = IO(e.Source.many(4, UInt(16.W), NamedVec.ZeroExtended()))
  val sinks = IO(e.Sink.many(4, UInt(16.W), NamedVec.ZeroExtended()))

  private val sourceBuffered0 = e.SourceBuffered(sources, 2)

  sourceBuffered0 :=> sinks

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(sources, "In")
  declareElasticInterface(sinks, "Out")
}

object SourceBufferedManyNaming_Tb extends chext.TestBench {
  emit(new SourceBufferedManyNaming_Tbtop)
}

class EWireManyNaming_Tbtop extends Module with chext.AnnotatedModule {
  val sources = IO(e.Source.many(4, UInt(16.W), NamedVec.ZeroExtended()))
  val sinks = IO(e.Sink.many(4, UInt(16.W), NamedVec.ZeroExtended()))

  private val wires = e.EWire.many(4, UInt(16.W), NamedVec.ZeroExtended())

  sources :=> wires
  wires :=> sinks

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(sources, "In")
  declareElasticInterface(sinks, "Out")
}

object EWireManyNaming_Tb extends chext.TestBench {
  emit(new EWireManyNaming_Tbtop)
}

class ManyLikeNaming_Tbtop extends Module with chext.AnnotatedModule {
  val source = IO(e.Source(UInt(16.W)))
  val sink = IO(e.Sink(UInt(16.W)))

  private val sources = IO(e.Source.manyLike(4, sink, NamedVec.ZeroExtended()))
  private val sinks = IO(e.Sink.manyLike(4, source, NamedVec.ZeroExtended()))
  private val wires = e.EWire.manyLike(4, source, NamedVec.ZeroExtended())

  sources :=> wires
  wires :=> sinks

  source.nodeq()
  sink.noenq()

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(source, "In")
  declareElasticInterface(sink, "Out")
  declareElasticInterface(sources, "In")
  declareElasticInterface(sinks, "Out")
}

object ManyLikeNaming_Tb extends chext.TestBench {
  emit(new ManyLikeNaming_Tbtop)
}
