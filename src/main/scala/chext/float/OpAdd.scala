package chext.float

import chisel3._
import chisel3.util._

class OpAdd(val genFp: FloatingPoint, val combinational: Boolean = false) //
    extends Module //
    with BinaryOp[FloatingPoint] //
    with Delay {
  override val delay = if (combinational) 0 else 4

  val io = IO(new Bundle {
    val inA = Input(genFp)
    val inB = Input(genFp)

    val out = Output(genFp)
  })

  def inA: FloatingPoint = io.inA
  def inB: FloatingPoint = io.inB
  def out: FloatingPoint = io.out

  val inA__ = inA.asUInt
  val inB__ = inB.asUInt
  val out__ = out.asUInt

  dontTouch(inA__)
  dontTouch(inB__)
  dontTouch(out__)

  if (combinational) {
    val module = Module(new AddFp_Combinational(genFp))

    module.io.inA := io.inA
    module.io.inB := io.inB
    io.out := module.io.out
  } else {
    val module = Module(new AddFp_Pipelined(genFp))

    module.io.inA := io.inA
    module.io.inB := io.inB
    io.out := module.io.out
  }
}
