package chext.ip.memory.chisel

import chext.ip.memory

case object Target extends memory.Target {
  val name = "Chisel"
  def createSinglePortRawMem(cfg: memory.RawMemConfig): memory.RawMem = ???
  def createSimpleDualPortRawMem(cfg: memory.RawMemConfig): memory.RawMem =
    new SimpleDualPortMem(cfg)
  def createTrueDualPortRawMem(cfg: memory.RawMemConfig): memory.RawMem = ???
}
