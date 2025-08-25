package chisel3.hacks

import chisel3.Data

object CloneTypeFull {
  def apply[T <: Data](gen: T) = gen.cloneTypeFull
}
