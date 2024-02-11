package chext.elastic

import chisel3._
import chisel3.util._
import chisel3.experimental.AffectsChiselPrefix

class Distributor[T <: Data](
    val gen: T,
    val n: Int,
    val chooserFn: Chooser.ChooserFn,
    val isLastFn: T => Bool = (_: T) => true.B
) extends Module with ChoosingModule {
  require(n > 0)
  override def desiredName: String = "elasticDistributor"

  val io = IO(new Bundle {
    val input = Flipped(Decoupled(gen))
    val outputs = Vec(n, Decoupled(gen))
    val select = Irrevocable(genSelect)
  })

  protected val chooser = chooserFn(VecInit(io.outputs.map { _.ready }))

  protected val inputValid = io.input.valid
  protected val inputLast = isLastFn(io.input.bits)
  protected val inputReady = io.input.ready

  protected val selectValid = io.select.valid
  protected val selectReady = io.select.ready

  protected val outputValid = Wire(Bool())
  protected val outputReady = io.outputs(choice).ready

  protected def implementDataPlane() = {
    io.outputs.foreach { x => x.bits := io.input.bits }
    io.select.bits := choice

    io.outputs.zipWithIndex.foreach { case (x, i) =>
      x.valid := outputValid && i.U === (choice)
    }
  }

  implementDataPlane()
  implementControlPlane()
  implementChoiceLogic()
}

object Distributor {
  def apply[T <: Data](
      input: ReadyValidIO[T],
      outputs: Seq[ReadyValidIO[T]],
      chooserFn: Chooser.ChooserFn,
      select: Option[ReadyValidIO[T]] = None,
      isLastFn: T => Bool = (_: T) => true.B
  ): Unit = {
    val Distributor = Module(
      new Distributor(
        chiselTypeOf(input.bits),
        outputs.length,
        chooserFn,
        isLastFn
      )
    )

    Distributor.io.input <> input
    Distributor.io.outputs.zip(outputs).foreach { case (x, y) => x <> y }

    select match {
      case None         => Disposed(Distributor.io.select)
      case Some(select) => Distributor.io.select <> select
    }
  }
}
