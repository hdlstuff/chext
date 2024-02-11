package chext.elastic

import chisel3._
import chisel3.util._
import chisel3.experimental._

object Transform {
  implicit class transformDecoupled[T <: Data](input: DecoupledIO[T]) {
    def transform[TT <: Data](gen: TT)(fn: (T, TT) => Unit) = {
      val result = Wire(Flipped(new DecoupledIO(gen)))
      result.valid := input.valid
      input.ready := result.ready
      fn(input.bits, result.bits)
      result
    }
  }

  implicit class transformIrrevocable[T <: Data](input: IrrevocableIO[T]) {
    def transform[TT <: Data](gen: TT)(fn: (T, TT) => Unit) = {
      val result = Wire(Flipped(new IrrevocableIO(gen)))
      result.valid := input.valid
      input.ready := result.ready
      fn(input.bits, result.bits)
      result
    }
  }

  implicit class transform[T <: Data](input: ReadyValidIO[T]) {
    def transformAsDecoupled[TT <: Data](gen: TT)(fn: (T, TT) => Unit) = {
      val result = Wire(Flipped(new DecoupledIO(gen)))
      result.valid := input.valid
      input.ready := result.ready
      fn(input.bits, result.bits)
      result
    }

    def transformAsIrrevocable[TT <: Data](gen: TT)(fn: (T, TT) => Unit) = {
      val result = Wire(Flipped(new IrrevocableIO(gen)))
      result.valid := input.valid
      input.ready := result.ready
      fn(input.bits, result.bits)
      result
    }
  }
}
