package chext.elastic

import chisel3._
import chisel3.experimental.{requireIsHardware, requireIsChiselType}

// TODO make this one the default
object Const {
  def apply[T <: Data](constant: T) = {
    chext.naming.checkPrefix("Const", "const")
    requireIsHardware(constant, "The constant parameter must be a Chisel hardware.")

    val interface = Wire(Interface(chiselTypeOf(constant)))
    interface.markSink()

    interface.enq(constant)
    interface
  }

  def explicit[T <: Data](gen: T)(fn: => (T) => Unit) = {
    chext.naming.checkPrefix("Const", "const")
    requireIsChiselType(gen, "The gen parameter must be a Chisel type.")

    val interface = Wire(Interface(gen.cloneType))
    interface.markSink()

    fn(interface.bits)
    interface.valid := true.B
    interface
  }
}
