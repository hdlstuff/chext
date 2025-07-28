package chisel3

object VecSampleElement {
  def apply[T <: Data](gen: Vec[T]) = gen.sample_element
}

object CloneTypeFull {
  def apply[T <: Data](gen: T) = gen.cloneTypeFull
}
