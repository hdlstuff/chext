package chext.elastic


import chisel3._
import chisel3.experimental.requireIsHardware

object Constant {
  def apply[T <: Data](constant: T) = {
    requireIsHardware(constant, "The constant parameter must be a Chisel hardware.")
    val interface = EWire(chiselTypeOf(constant))
    interface.enq(constant)
    interface
  }
}
