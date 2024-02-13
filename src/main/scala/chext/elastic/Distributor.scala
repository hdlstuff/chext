package chext.elastic

import chext.elastic

import chisel3._
import chisel3.util._

import elastic.ConnectOp._

class Distributor[T <: Data](
    val gen: T,
    val n: Int,
    val chooserFn: Chooser.ChooserFn,
    val isLastFn: T => Bool = (_: T) => true.B
) extends Module with ChoosingModule {
  require(n > 0)
  override def desiredName: String = "elasticDistributor"

  val io = IO(new Bundle {
    val source = Source(Decoupled(gen))
    val sinks = Vec(n, Sink(Decoupled(gen)))
    val select = Sink(Irrevocable(genSelect))
  })

  protected val chooser = chooserFn(VecInit(io.sinks.map { _.ready }))

  protected val sourceValid = io.source.valid
  protected val sourceLast = isLastFn(io.source.bits)
  protected val sourceReady = io.source.ready

  protected val selectValid = io.select.valid
  protected val selectReady = io.select.ready

  protected val sinkValid = Wire(Bool())
  protected val sinkReady = io.sinks(choice).ready

  protected def implementDataPlane() = {
    io.sinks.foreach { x => x.bits := io.source.bits }
    io.select.bits := choice

    io.sinks.zipWithIndex.foreach { case (x, i) =>
      x.valid := sinkValid && i.U === (choice)
    }
  }

  implementDataPlane()
  implementControlPlane()
  implementChoiceLogic()
}

object Distributor {
  def apply[T <: Data](
      source: ReadyValidIO[T],
      sinks: Seq[ReadyValidIO[T]],
      chooserFn: Chooser.ChooserFn,
      select: Option[ReadyValidIO[UInt]] = None,
      isLastFn: T => Bool = (_: T) => true.B
  ): Unit = {
    val Distributor = Module(
      new Distributor(
        chiselTypeOf(source.bits),
        sinks.length,
        chooserFn,
        isLastFn
      )
    )

    source :=> Distributor.io.source
    Distributor.io.sinks.zip(sinks).foreach { case (x, y) => x :=> y }

    select match {
      case None         => Disposed(Distributor.io.select)
      case Some(select) => Distributor.io.select :=> select
    }
  }
}
