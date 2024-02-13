package chext.util

object Expect {
  def equals[T](a: T, b: T, msg: String = "") = {
    if (a != b) {
      doThrow(f"${a} != ${b}. Message: ${msg}")
    }
  }

  def condition(b: Boolean, msg: String = "") = {
    if (!b) {
      doThrow(f"Condition failed. Message: ${msg}")
    }
  }

  protected def doThrow(msg: String) = {
    Console.err.println(f"Expect Failed: ${msg}")
    throw new Exception(msg)
  }
}
