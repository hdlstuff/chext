package chext.elastic

import chisel3._

class DataLast[T <: Data](gen: T) extends Bundle {
  val data = gen.cloneType
  val last = Bool()
}

class DataFirstLast[T <: Data](gen: T) extends Bundle {
  val data = gen.cloneType
  val first = Bool()
  val last = Bool()
}

class DataFirstLastZero[T <: Data](gen: T) extends Bundle {
  val data = gen.cloneType
  val first = Bool()
  val last = Bool()
  val zero = Bool()
}

object DataLast {
  def apply[T <: Data](gen: T) = new DataLast(gen)
  def apply(width: Int) = new DataLast(UInt(width.W))
}
