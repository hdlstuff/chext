package chext.elastic

import chisel3._
import chisel3.util._

class Arbiter[T <: Data](
    val gen: T,
    val n: Int,
    val chooserFn: Chooser.ChooserFn,
    val isLastFn: T => Bool = (_: T) => true.B
) extends Module with ChoosingModule {
  require(n > 0)
  override def desiredName: String = "elasticArbiter"

  val io = IO(new Bundle {
    val sources = Vec(n, Source(Decoupled(gen)))
    val sink = Sink(Decoupled(gen))
    val select = Sink(Irrevocable(genSelect))
  })

  protected val chooser = chooserFn(VecInit(io.sources.map { _.valid }))

  protected val sourceValid = io.sources(choice).valid
  protected val sourceLast = isLastFn(io.sources(choice).bits)
  protected val sourceReady = Wire(Bool())

  protected val selectValid = io.select.valid
  protected val selectReady = io.select.ready

  protected val sinkValid = io.sink.valid
  protected val sinkReady = io.sink.ready

  protected def implementDataPlane() = {
    io.sink.bits := io.sources(choice).bits
    io.select.bits := choice

    io.sources.zipWithIndex.foreach { case (x, i) =>
      x.ready := sourceReady && i.U === (choice)
    }
  }

  implementDataPlane()
  implementControlPlane()
  implementChoiceLogic()
}

object Arbiter {
  def apply[T <: Data](
      sources: Seq[ReadyValidIO[T]],
      sink: ReadyValidIO[T],
      chooserFn: Chooser.ChooserFn,
      select: Option[ReadyValidIO[UInt]] = None,
      isLastFn: T => Bool = (_: T) => true.B
  ): Unit = {
    val arbiter = Module(
      new Arbiter(
        chiselTypeOf(sources(0).bits),
        sources.length,
        chooserFn,
        isLastFn
      )
    )

    arbiter.io.sources.zip(sources).foreach { case (x, y) => x <> y }
    arbiter.io.sink <> sink

    select match {
      case None         => Disposed(arbiter.io.select)
      case Some(select) => arbiter.io.select <> select
    }
  }
}
