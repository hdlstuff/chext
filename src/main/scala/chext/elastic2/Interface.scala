package chext.elastic2

import chisel3._

class Interface[+T <: Data](gen: T) extends util.ReadyValidIO[T](gen)

object Interface {
  def apply[T <: Data](gen: T): Interface[T] = new Interface(gen)
}

object Source {
  def apply[T <: Data](gen: T): Interface[T] = Flipped(new Interface(gen))
}

object Sink {
  def apply[T <: Data](gen: T): Interface[T] = new Interface(gen)
}
