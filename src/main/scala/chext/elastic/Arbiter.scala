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
    val inputs = Vec(n, Flipped(Decoupled(gen)))
    val output = Decoupled(gen)
    val select = Irrevocable(genSelect)
  })

  protected val chooser = chooserFn(VecInit(io.inputs.map { _.valid }))

  protected val inputValid = io.inputs(choice).valid
  protected val inputLast = isLastFn(io.inputs(choice).bits)
  protected val inputReady = Wire(Bool())

  protected val selectValid = io.select.valid
  protected val selectReady = io.select.ready

  protected val outputValid = io.output.valid
  protected val outputReady = io.output.ready

  protected def implementDataPlane() = {
    io.output.bits := io.inputs(choice).bits
    io.select.bits := choice

    io.inputs.zipWithIndex.foreach { case (x, i) =>
      x.ready := inputReady && i.U === (choice)
    }
  }

  implementDataPlane()
  implementControlPlane()
  implementChoiceLogic()
}

object Arbiter {
  def apply[T <: Data](
      inputs: Seq[ReadyValidIO[T]],
      output: ReadyValidIO[T],
      chooserFn: Chooser.ChooserFn,
      select: Option[ReadyValidIO[UInt]] = None,
      isLastFn: T => Bool = (_: T) => true.B
  ): Unit = {
    val arbiter = Module(
      new Arbiter(
        chiselTypeOf(inputs(0).bits),
        inputs.length,
        chooserFn,
        isLastFn
      )
    )

    arbiter.io.inputs.zip(inputs).foreach { case (x, y) => x <> y }
    arbiter.io.output <> output

    select match {
      case None         => Disposed(arbiter.io.select)
      case Some(select) => arbiter.io.select <> select
    }
  }
}
