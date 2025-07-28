package chext.elastic

import chisel3._
import chisel3.experimental.{requireIsHardware, requireIsChiselType}

// TODO make this one the default
object Const {
  def apply[T <: Data](constant: T) = {
    requireIsHardware(constant, "The constant parameter must be a Chisel hardware.")
    val interface = Wire(Interface(chiselTypeOf(constant)))
    interface.enq(constant)
    interface
  }

  def explicit[T <: Data](gen: T)(fn: => (T) => Unit) = {
    requireIsChiselType(gen, "The gen parameter must be a Chisel type.")
    val interface = Wire(Interface(gen.cloneType))
    fn(interface.bits)
    interface.valid := true.B
    interface
  }
}
