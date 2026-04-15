package chext.elastic

import chisel3._
import chisel3.experimental.requireIsHardware
import chisel3.experimental.requireIsChiselType
import chisel3.experimental.SourceInfo
import chisel3.experimental.skipPrefix

import chext.tracking
import tracking.{Component, RigidComponent, uniquePrefix}

abstract class Const[Tin <: Data, Tout <: Data](
    sink: Interface[Tout]
)(implicit si_ : SourceInfo)
    extends Component
    with Fire[Tout] {
  protected def fireSink: Interface[Tout] = sink

  addSinkPort("sink", sink)

  val sourceInfo: SourceInfo = si_
  def tpe: String = "Const"
  def namePrefix: String = "const"

  protected final val out = sink.$bits

  sink.$valid := true.B
}

// TODO make it sourceInfo aware and use macros
object Const {
  def apply[T <: Data](
      constant: T,
      name: String = "const"
  )(implicit si: SourceInfo): Interface[T] = {
    requireIsHardware(constant, "The constant parameter must be a Chisel hardware.")

    uniquePrefix(name) {
      val interface = EWire(chiselTypeOf(constant))

      val component = skipPrefix { new RigidComponent("Const", "const") }
      component.sink("sink", interface)

      interface.enq(constant)
      interface
    }
  }

  def explicit[T <: Data](gen: T, name: String = "const")(fn: => (T) => Unit) = {
    requireIsChiselType(gen, "The gen parameter must be a Chisel type.")

    uniquePrefix(name) {
      val interface = EWire(gen.cloneType)

      val component = new RigidComponent("Const", "const")
      component.sink("sink", interface)

      fn(interface.$bits)
      interface.$valid := true.B
      interface
    }
  }

}
