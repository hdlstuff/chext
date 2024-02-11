package chext.elastic

import chisel3._
import chisel3.util._
import chisel3.experimental._

object TransformOp {
  implicit class transformDecoupled[T <: Data](source: DecoupledIO[T]) {
    def transform[TT <: Data](gen: TT)(fn: (T, TT) => Unit) = {
      val result = Wire(Source(new DecoupledIO(gen)))
      result.valid := source.valid
      source.ready := result.ready
      fn(source.bits, result.bits)
      result
    }
  }

  implicit class transformIrrevocable[T <: Data](source: IrrevocableIO[T]) {
    def transform[TT <: Data](gen: TT)(fn: (T, TT) => Unit) = {
      val result = Wire(Source(new IrrevocableIO(gen)))
      result.valid := source.valid
      source.ready := result.ready
      fn(source.bits, result.bits)
      result
    }
  }

  implicit class transform[T <: Data](source: ReadyValidIO[T]) {
    def transformAsDecoupled[TT <: Data](gen: TT)(fn: (T, TT) => Unit) = {
      val result = Wire(Source(new DecoupledIO(gen)))
      result.valid := source.valid
      source.ready := result.ready
      fn(source.bits, result.bits)
      result
    }

    def transformAsIrrevocable[TT <: Data](gen: TT)(fn: (T, TT) => Unit) = {
      val result = Wire(Source(new IrrevocableIO(gen)))
      result.valid := source.valid
      source.ready := result.ready
      fn(source.bits, result.bits)
      result
    }
  }
}
