package chext.elastic

import chisel3._
import chisel3.experimental.{requireIsHardware, requireIsChiselType}

import chext.naming.prefix

// TODO make this one the default
object Const {
  def apply[T <: Data](constant: T, name: String = "") = {
    requireIsHardware(constant, "The constant parameter must be a Chisel hardware.")

    prefix(name) {
      val interface = Wire(Interface(chiselTypeOf(constant)))
      interface.markSink()

      interface.enq(constant)
      interface
    }
  }

  def explicit[T <: Data](gen: T, name: String = "")(fn: => (T) => Unit) = {
    requireIsChiselType(gen, "The gen parameter must be a Chisel type.")

    prefix(name) {
      val interface = Wire(Interface(gen.cloneType))
      interface.markSink()

      fn(interface.bits)
      interface.valid := true.B
      interface
    }
  }
}
