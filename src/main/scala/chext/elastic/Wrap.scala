package chext.elastic

import chisel3._
import chisel3.util.ShiftRegister

import chisel3.experimental.SourceInfo

import chisel3.hacks.deferred

import chext.util.Counter

import chext.tracking
import tracking.Component

abstract class Wrap[T1 <: Data, T2 <: Data](source: Interface[T1], sink: Interface[T2])(implicit
    val sourceInfo: SourceInfo
) extends Component
    with Fire[T2] {
  protected def fireSink: Interface[T2] = sink

  addSourcePort("source", source)
  addSinkPort("sink", sink)

  def tpe: String = "Wrap"
  def namePrefix: String = "wrap"

  protected val in = EWire.like(source)
  protected val out = EWire.like(sink)

  protected def delay: Int
  protected def queueLength: Int = delay + 1

  in := source.$bits

  deferred {
    if (delay == 0) {
      sink.$valid := source.$valid
      sink.$bits := out
      source.$ready := sink.$ready
    } else {
      import ConnectOp._

      // TODO counter maybe should not be a separate module?
      val ctr = Module(new Counter(queueLength + 1))
      ctr.noInc()
      ctr.noDec()

      val qOutput = Queue(chiselTypeOf(sink.$bits), queueLength)

      qOutput.source.noenq()
      source.nodeq()

      in := source.$bits
      qOutput.source.$bits := out

      source.$ready := ctr.notFull && source.$valid

      when(source.fire) {
        ctr.inc()
      }

      qOutput.source.$valid := ShiftRegister(source.fire, delay)
      qOutput.sink :=> sink

      when(qOutput.sink.fire) {
        ctr.dec()
      }
    }
  }
}
