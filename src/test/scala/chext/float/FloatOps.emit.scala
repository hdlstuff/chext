package chext.float

import chisel3.emitVerilog

private object EmitStupidModule extends App {
  emitVerilog(new StupidModule, Array("--target-dir", "output/"))
}

object OpAdd_Emit extends App {
  emitVerilog(new OpAdd(FloatingPoint.ieeeFp32), Array("--target-dir", "output/"))
}

object OpMultiply_Emit extends App {
  emitVerilog(new OpMultiply(FloatingPoint.ieeeFp32), Array("--target-dir", "output/"))
}

object ElasticFloat_Emit extends App {
  emitVerilog(new ElasticAdd(FloatingPoint.ieeeFp32), Array("--target-dir", "output/"))
  emitVerilog(new ElasticMultiply(FloatingPoint.ieeeFp32), Array("--target-dir", "output/"))
}
