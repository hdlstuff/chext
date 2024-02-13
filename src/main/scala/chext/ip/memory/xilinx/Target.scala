package chext.ip.memory.xilinx

import chext.ip.memory

case object Target extends memory.Target {
  val name = "Xilinx"
  def createSinglePortRawMem(cfg: memory.RawMemConfig): memory.RawMem =
    new SinglePortRawMem(cfg)
  def createSimpleDualPortRawMem(cfg: memory.RawMemConfig): memory.RawMem =
    new SimpleDualPortRawMem(cfg)
  def createTrueDualPortRawMem(cfg: memory.RawMemConfig): memory.RawMem =
    new TrueDualPortRawMem(cfg)
}
