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
  def in_a: T
  def in_b: T
  def out: T
}
