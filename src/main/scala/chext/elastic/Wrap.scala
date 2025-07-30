package chext.elastic

import chisel3._

import chisel3.util.ShiftRegister
import chisel3.experimental.AffectsChiselPrefix
import chisel3.hacks.deferred

import chext.util.Counter

abstract class Wrap[T1 <: Data, T2 <: Data](source: Interface[T1], sink: Interface[T2])
    extends Fire[T2](sink) {
  source.markSource()
  sink.markSink()

  protected val in = Wire(chiselTypeOf(source.bits))
  protected val out = Wire(chiselTypeOf(sink.bits))

  protected def delay: Int
  protected def queueLength: Int = delay + 1

  in := source.bits

  deferred {
    if (delay == 0) {
      sink.valid := source.valid
      sink.bits := out
      source.ready := sink.ready
    } else {
      import ConnectOp._

      // TODO counter maybe should not be a separate module?
      val ctr = Module(new Counter(queueLength + 1))
      ctr.noInc()
      ctr.noDec()

      val qOutput = Queue(chiselTypeOf(sink.bits), queueLength)

      qOutput.source.noenq()
      source.nodeq()

      in := source.bits
      qOutput.source.bits := out

      source.ready := ctr.notFull && source.valid

      when(source.fire) {
        ctr.inc()
      }

      qOutput.source.valid := ShiftRegister(source.fire, delay)
      qOutput.sink :=> sink

      when(qOutput.sink.fire) {
        ctr.dec()
      }
    }
  }
}
