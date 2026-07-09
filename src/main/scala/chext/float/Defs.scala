package chext.float

import chisel3.Data

trait Delay {
  val delay: Int
}

trait UnaryOp[T <: Data] {
  def in: T
  def out: T
}

trait BinaryOp[T <: Data] {
  def inA: T
  def inB: T
  def out: T
}
