package chext.axi4.utils

private[axi4] object NotSupported {
  def apply(str: String) = new RuntimeException(f"Not supported: $str")
}

private[axi4] object BadConfig {
  def apply(str: String) = new RuntimeException(f"Bad config: $str")
}
