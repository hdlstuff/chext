package chext.elastic2


import chisel3._
import chisel3.util._

import ConnectOp._

class BasicArbiter[T <: Data](
    val gen: T,
    val n: Int,
    val chooserFn: Chooser.ChooserFn
) extends Module {
  require(n > 0)
  val genSelect = UInt(chisel3.util.log2Up(n).W)

  override def desiredName: String = "elasticBasicArbiter"

  val io = IO(new Bundle {
    val sources = Vec(n, Source(gen))
    val sink = Sink(gen)
    val select = Sink(genSelect)
  })

  private val sources = io.sources

  // TODO do not use buffers, try to use eagerFork-like structure?
  private val sink = SinkBuffer(io.sink)
  private val select = SinkBuffer(io.select)

  private val chooser = chooserFn(VecInit(sources.map { _.valid }))
  private val choice = chooser.choice

  private val fire = sources(choice).valid && sink.ready && select.ready

  sink.valid := fire
  select.valid := fire

  sink.bits := sources(choice).bits
  select.bits := choice

  sources.zipWithIndex.foreach { case (x, i) =>
    x.ready := fire && i.U === (choice)
  }

  when(fire) {
    chooser.updateState
  }
}

object BasicArbiter {
  def apply[T <: Data](
      sources: Seq[Interface[T]],
      sink: Interface[T],
      chooserFn: Chooser.ChooserFn,
      select: Option[Interface[UInt]] = None
  ): Unit = {
    val arbiter = Module(
      new BasicArbiter(
        chiselTypeOf(sources(0).bits),
        sources.length,
        chooserFn
      )
    )

    sources.zip(arbiter.io.sources).foreach { case (x, y) => x :=> y }
    arbiter.io.sink :=> sink

    select match {
      case None         => arbiter.io.select.deq() // disposed
      case Some(select) => arbiter.io.select :=> select
    }
  }
}
