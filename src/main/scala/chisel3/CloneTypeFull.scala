package chisel3

object CloneTypeFull {
  def apply[T <: Data](gen: T) = gen.cloneTypeFull
}
