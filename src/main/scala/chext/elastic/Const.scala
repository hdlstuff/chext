package chext.elastic

import chisel3._
import chisel3.experimental.{requireIsHardware, requireIsChiselType}

import chext.Prefix.prefix
import tracking.Component

// TODO make this one the default
object Const {
  def apply[T <: Data](constant: T, name: String = "const") = {
    requireIsHardware(constant, "The constant parameter must be a Chisel hardware.")

    prefix(name) {
      val interface = Wire(Interface(chiselTypeOf(constant)))
      interface.markSink()

      Component(
        chext.Prefix.currentPrefix,
        "Const",
        Seq(),
        Seq(("sink", interface))
      ).register()

      interface.enq(constant)
      interface
    }
  }

  def explicit[T <: Data](gen: T, name: String = "const")(fn: => (T) => Unit) = {
    requireIsChiselType(gen, "The gen parameter must be a Chisel type.")

    prefix(name) {
      val interface = Wire(Interface(gen.cloneType))
      interface.markSink()

      Component(
        chext.Prefix.currentPrefix,
        "Const",
        Seq(),
        Seq(("sink", interface))
      ).register()

      fn(interface.$bits)
      interface.$valid := true.B
      interface
    }
  }

}
