package chext.elastic2

import chisel3._
import chisel3.experimental.AffectsChiselPrefix

abstract class Scope[T1 <: Data, T2 <: Data](
    sourceIn: Interface[T1],
    sinkOut: Interface[T2],
    numOutstanding: Int = 1
) extends AffectsChiselPrefix {
  require(numOutstanding >= 1)

  protected val source = Wire(chiselTypeOf(sourceIn))
  protected val sink = Wire(chiselTypeOf(sinkOut))

  private val counter = Module(new chext.util.Counter(numOutstanding + 1))

  source.valid := sourceIn.valid
  source.bits := sourceIn.bits
  sourceIn.ready := source.ready && counter.notFull

  sinkOut.valid := sink.valid
  sinkOut.bits := sink.bits
  sink.ready := sinkOut.ready

  when(sourceIn.fire) {
    counter.inc()
  }

  when(sourceIn.fire) {
    counter.dec()
  }
}
