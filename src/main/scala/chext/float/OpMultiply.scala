package chext.float

import chisel3._

class OpMultiply(val genFp: FloatingPoint, val combinational: Boolean = false)
    extends Module //
    with BinaryOp[FloatingPoint] //
    with Delay {
  override val delay = if (combinational) 0 else 3

  val io = IO(new Bundle {
    val inA = Input(genFp)
    val inB = Input(genFp)

    val out = Output(genFp)
  })

  val inA = io.inA
  val inB = io.inB
  val out = io.out

  val inA__ = inA.asUInt
  val inB__ = inB.asUInt
  val out__ = out.asUInt

  dontTouch(inA__)
  dontTouch(inB__)
  dontTouch(out__)

  if (combinational) {
    val module = Module(new MulFp_Combinational(genFp))

    module.io.inA := io.inA
    module.io.inB := io.inB
    io.out := module.io.out
  } else {
    val module = Module(new MulFp_Pipelined(genFp))

    module.io.inA := io.inA
    module.io.inB := io.inB
    io.out := module.io.out
  }
}
