package chext.ip.bram.chisel

import chext.ip.bram

case object Target extends bram.Target {
  val name = "Chisel"
  def createSinglePortRawMem(cfg: bram.RawMemConfig): bram.RawMem = ???
  def createSimpleDualPortRawMem(cfg: bram.RawMemConfig): bram.RawMem =
    new SimpleDualPortMem(cfg)
  def createTrueDualPortRawMem(cfg: bram.RawMemConfig): bram.RawMem = ???
}
