package chext.ip.memory.altera

import chext.ip.memory

case object Target extends memory.Target {
  val name: String = "Altera"
  def createSinglePortRawMem(cfg: memory.RawMemConfig): memory.RawMem = ???
  def createSimpleDualPortRawMem(cfg: memory.RawMemConfig): memory.RawMem = ???
  def createTrueDualPortRawMem(cfg: memory.RawMemConfig): memory.RawMem = ???
}
