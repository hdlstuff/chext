package chext.memory.xilinx

import chisel3.emitVerilog

import chext.memory

object xpm_memory_sdpram_Emit extends App {
  emitVerilog(
    new SimpleDualPortRawMem(memory.RawMemConfig(20, 32, 4, 1)),
    Array("--target-dir", "output/")
  )
}
