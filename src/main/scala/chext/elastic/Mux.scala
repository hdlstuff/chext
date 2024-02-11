package chext.elastic

import chisel3._
import chisel3.util._

class Mux[T <: Data](
    val gen: T,
    val n: Int,
    val isLastFn: T => Bool = (_: T) => true.B
) extends Module {
  require(n > 0)
  override def desiredName: String = "elasticMux"

  val genSelect = UInt(chisel3.util.log2Up(n).W)

  val io = IO(new Bundle {
    val inputs = Vec(n, Flipped(Decoupled(gen)))
    val output = Decoupled(gen)
    val select = Flipped(Irrevocable(genSelect))
  })

  private val valid = io.select.valid && io.inputs(io.select.bits).valid
  private val fire = valid && io.output.ready
  private val isLast = isLastFn(io.output.bits)

  io.inputs.zipWithIndex.foreach { case (x, i) =>
    x.ready := fire && i.U === (io.select.bits)
  }

  // output ready might wait for output valid
  // so, make sure that they do not depend on each other
  io.output.valid := valid
  
  io.select.ready := fire && isLast

  io.output.bits := io.inputs(io.select.bits.asUInt).bits
}

object Mux {
  def apply[T <: Data](
      inputs: Seq[ReadyValidIO[T]],
      output: ReadyValidIO[T],
      select: ReadyValidIO[UInt],
      isLastFn: T => Bool = (_: T) => true.B
  ): Unit = {
    val mux = Module(
      new Mux(chiselTypeOf(inputs(0).bits), inputs.length, isLastFn)
    )
    mux.io.inputs.zip(inputs).foreach { case (x, y) => x <> y }
    output <> mux.io.output
    mux.io.select <> select
  }
}
