package chext.ip.bram

abstract class Target {
  def name: String
  def createSinglePortRawMem(cfg: RawMemConfig): RawMem
  def createSimpleDualPortRawMem(cfg: RawMemConfig): RawMem
  def createTrueDualPortRawMem(cfg: RawMemConfig): RawMem
}

object Target {
  def current: Target = {
    if (current_.isEmpty)
      throw new RuntimeException("Current target is not set!")
    current_.get
  }

  def setCurrent(target: Target): Unit = current_ = Some(target)

  private var current_ = Option.empty[Target]
}
