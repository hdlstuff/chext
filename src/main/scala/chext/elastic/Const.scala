package chext.elastic

import chisel3._
import chisel3.experimental.requireIsHardware
import chisel3.experimental.requireIsChiselType
import chisel3.experimental.SourceInfo
import chisel3.experimental.skipPrefix

import chext.tracking
import tracking.RigidComponent
import tracking.namedUniquePath

// TODO make it sourceInfo aware and use macros
object Const {
  def apply[T <: Data](constant: T, name: String = "const") = {
    requireIsHardware(constant, "The constant parameter must be a Chisel hardware.")

    namedUniquePath(name) {
      val interface = Wire(Interface(chiselTypeOf(constant)))

      val component = skipPrefix { new RigidComponent("Const", "const") }
      component.sink("sink", interface)

      interface.enq(constant)
      interface
    }
  }

  def explicit[T <: Data](gen: T, name: String = "const")(fn: => (T) => Unit) = {
    requireIsChiselType(gen, "The gen parameter must be a Chisel type.")

    namedUniquePath(name) {
      val interface = Wire(Interface(gen.cloneType))

      val component = new RigidComponent("Const", "const")
      component.sink("sink", interface)

      fn(interface.$bits)
      interface.$valid := true.B
      interface
    }
  }

}
