package chext.ip.bram.xilinx

import chext.ip.bram

case object Target extends bram.Target {
  val name = "Xilinx"
  def createSinglePortRawMem(cfg: bram.RawMemConfig): bram.RawMem =
    new SinglePortRawMem(cfg)
  def createSimpleDualPortRawMem(cfg: bram.RawMemConfig): bram.RawMem =
    new SimpleDualPortRawMem(cfg)
  def createTrueDualPortRawMem(cfg: bram.RawMemConfig): bram.RawMem =
    new TrueDualPortRawMem(cfg)
}
