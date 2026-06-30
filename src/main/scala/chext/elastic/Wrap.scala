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

  protected val in = Wire(chiselTypeOf(source.$bits))
  protected val out = Wire(chiselTypeOf(sink.$bits))

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

      val ctr = new Counter(queueLength + 1)
      ctr.noInc()
      ctr.noDec()

      val queueOutput = Queue(chiselTypeOf(sink.$bits), queueLength)

      queueOutput.source.noenq()
      source.nodeq()

      in := source.$bits
      queueOutput.source.$bits := out

      source.$ready := ctr.notFull && source.$valid

      when(source.fire) {
        ctr.inc()
      }

      queueOutput.source.$valid := ShiftRegister(source.fire, delay)
      queueOutput.sink :=> sink

      when(queueOutput.sink.fire) {
        ctr.dec()
      }
    }
  }
}
