package chext.util

object Expect {
  def equals[T](a: T, b: T, msg: String = "") = {
    if (a != b) {
      throw new Exception(f"${a} != ${b}. Message: ${msg}")
    }
  }

  protected def condition(b: Boolean, msg: String = "") = {
    if (!b) {
      throw new Exception(f"Condition failed. Message: ${msg}")
    }
  }
}
