package chext.ip.bram.altera

import chext.ip.bram

case object Target extends bram.Target {
  val name: String = "Altera"
  def createSinglePortRawMem(cfg: bram.RawMemConfig): bram.RawMem = ???
  def createSimpleDualPortRawMem(cfg: bram.RawMemConfig): bram.RawMem = ???
  def createTrueDualPortRawMem(cfg: bram.RawMemConfig): bram.RawMem = ???
}
