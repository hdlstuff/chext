package chisel3.hacks

import chisel3.{Data, Vec}

object VecSampleElement {
  def apply[T <: Data](gen: Vec[T]) = gen.sample_element
}
