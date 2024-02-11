package chext.axi4.utils

object NotSupported {
  def apply(str: String) = new RuntimeException(f"Not supported: $str")
}

object BadConfig {
  def apply(str: String) = new RuntimeException(f"Bad config: $str")
}
