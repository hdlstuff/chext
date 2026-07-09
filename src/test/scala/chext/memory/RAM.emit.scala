package chext.memory

import chisel3.emitVerilog

object RAM_Emit extends App {
  Target.setCurrent(xilinx.Target)

  emitVerilog(
    new SimpleDualPortRAM(RawMemConfig(256, 15, 8, 1), PortConfig(12, 12)),
    Array("--target-dir", "output/")
  )

  emitVerilog(
    new SinglePortRAM(RawMemConfig(256, 15, 8, 1), PortConfig(12, 12)),
    Array("--target-dir", "output/")
  )

  emitVerilog(
    new TrueDualPortRAM(RawMemConfig(256, 15, 8, 1), PortConfig(12, 12)),
    Array("--target-dir", "output/")
  )
}
