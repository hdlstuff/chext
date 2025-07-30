package chext.elastic

import chisel3._
import chisel3.util._

import ConnectOp._

class Demux[T <: Data](
    val gen: T,
    val n: Int,
    val isLastFn: T => Bool = (_: T) => true.B
) extends Module {
  require(n > 0)
  override def desiredName: String = "elasticDemux"

  val genSelect = UInt(chisel3.util.log2Up(n).W)

  val io = IO(new Bundle {
    val source = Source(gen)
    val sinks = Vec(n, Sink(gen))
    val select = Source(genSelect)
  })

  io.source.markSource()
  io.sinks.foreach { _.markSink() }
  io.select.markSource()

  private val valid = io.select.valid && io.source.valid
  private val fire = valid && io.sinks(io.select.bits).ready
  private val isLast = isLastFn(io.source.bits)

  io.source.ready := fire
  io.sinks.zipWithIndex.foreach { case (x, i) =>
    // sink ready might wait for sink valid
    // so, make sure that they do not depend on each other
    x.valid := valid && (i.U === (io.select.bits))
  }
  io.select.ready := fire && isLast

  io.sinks.foreach { _.bits := io.source.bits }
}

object Demux {
  def apply[T <: Data](
      source: Interface[T],
      sinks: Seq[Interface[T]],
      select: Interface[UInt],
      isLastFn: T => Bool = (_: T) => true.B
  ): Unit = {
    val demux = Module(
      new Demux(chiselTypeOf(source.bits), sinks.length, isLastFn)
    )

    source :=> demux.io.source
    demux.io.sinks.zip(sinks).foreach { case (x, y) => { x :=> y } }
    select :=> demux.io.select
  }
}
