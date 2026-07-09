package chext.util

import chisel3._

object BitOps_Emit extends App {
  class MyModule extends Module {
    val in = IO(Input(UInt(8.W)))
    val out1 = IO(Output(UInt(8.W)))
    val out2 = IO(Output(UInt(8.W)))
    val out3 = IO(Output(UInt(8.W)))
    val out4 = IO(Output(UInt(8.W)))
    val out5 = IO(Output(UInt(8.W)))
    val out6 = IO(Output(UInt(8.W)))
    val out7 = IO(Output(UInt(8.W)))
    val out8 = IO(Output(UInt(8.W)))

    import BitOps._

    out1 := in.setLsbN(3)
    out2 := in.resetLsbN(3)
    out3 := in.setMsbN(3)
    out4 := in.resetMsbN(3)
    out5 := in.extractBig(Seq(4, 3, 0))
    out6 := in.extractBig(Seq(7, 5, 2))
    out7 := in.extract(Seq(7, 5, 2))
    out8 := in.drop(Seq(7, 5, 2))
  }

  emitVerilog(new MyModule, Array("--target-dir", "output/"))
}
