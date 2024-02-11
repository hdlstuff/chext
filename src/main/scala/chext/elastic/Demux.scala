package chext.elastic

import chisel3._
import chisel3.util._

class Demux[T <: Data](
    val gen: T,
    val n: Int,
    val isLastFn: T => Bool = (_: T) => true.B
) extends Module {
  require(n > 0)
  override def desiredName: String = "elasticDemux"

  val genSelect = UInt(chisel3.util.log2Up(n).W)

  val io = IO(new Bundle {
    val input = Flipped(Decoupled(gen))
    val outputs = Vec(n, Decoupled(gen))
    val select = Flipped(Irrevocable(genSelect))
  })

  private val valid = io.select.valid && io.input.valid
  private val fire = valid && io.outputs(io.select.bits).ready
  private val isLast = isLastFn(io.input.bits)

  io.input.ready := fire
  io.outputs.zipWithIndex.foreach { case (x, i) =>
    // output ready might wait for output valid
    // so, make sure that they do not depend on each other
    x.valid := valid && (i.U === (io.select.bits))
  }
  io.select.ready := fire && isLast

  io.outputs.foreach { _.bits <> io.input.bits }
}

object Demux {
  def apply[T <: Data](
      input: ReadyValidIO[T],
      outputs: Seq[ReadyValidIO[T]],
      select: ReadyValidIO[UInt],
      isLastFn: T => Bool = (_: T) => true.B
  ): Unit = {
    val demux = Module(
      new Demux(chiselTypeOf(input.bits), outputs.length, isLastFn)
    )
    demux.io.input <> input
    demux.io.outputs.zip(outputs).foreach { case (x, y) => { x <> y } }
    demux.io.select <> select
  }
}
