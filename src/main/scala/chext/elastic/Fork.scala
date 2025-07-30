package chext.elastic

import chisel3._
import chisel3.experimental.AffectsChiselPrefix
import chisel3.experimental.SourceInfo
import chisel3.hacks._

import scala.collection.mutable.ListBuffer

import chext.naming

abstract class Fork[T <: Data](
    source: Interface[T],
    eager: Boolean = true
)(implicit si: SourceInfo)
    extends AffectsChiselPrefix {
  chext.naming.checkPrefix("Fork", "fork")

  private val sinkList = ListBuffer.empty[Interface[Data]]

  protected val in = source.bits

  protected final def onFork: Unit = throw new NotImplementedError("Shall not be used!")

  /** Branches a new elastic interface from the fork.
    *
    * @param tt
    * @return
    */
  protected final def fork[TT <: Data](tt: TT = in): Interface[TT] = {
    val result = Wire(new Interface(chiselTypeOf(tt)))
    result.bits := tt
    sinkList.addOne(result)
    result
  }

  deferred {
    if (eager)
      forkImpl.eagerFork(source, sinkList.toSeq)
    else
      forkImpl.lazyFork(source, sinkList.toSeq)
  }

}

private[elastic] object forkImpl {
  def eagerFork[T <: Data](
      source: Interface[T],
      sinks: Seq[Interface[Data]]
  )(implicit si: SourceInfo): Unit = {
    source.markSource()
    sinks.foreach { _.markSink() }

    // registers to remember if transmission already took place
    val regs = RegInit(VecInit(Seq.fill(sinks.length) { false.B }))

    val ready = VecInit(sinks.zip(regs).map {
      case (sink, reg) => {
        sink.ready || reg
      }
    }).reduceTree(_ && _)
    source.ready := ready

    sinks.zip(regs).foreach {
      case (sink, reg) => {
        sink.valid := source.valid && !reg
      }
    }

    sinks.zip(regs).foreach {
      case (sink, reg) => {
        // the next value for the register
        reg := (sink.ready || reg) && source.valid && !source.ready
      }
    }
  }

  def lazyFork[T <: Data](
      source: Interface[T],
      sinks: Seq[Interface[Data]]
  )(implicit si: SourceInfo): Unit = {
    source.markSource()
    sinks.foreach { _.markSink() }

    sinks.foreach { //
      case (sink) => sink.valid := source.valid && source.ready
    }

    val ready = VecInit(sinks.map { _.ready }).reduceTree(_ && _)
    source.ready := ready
  }
}
