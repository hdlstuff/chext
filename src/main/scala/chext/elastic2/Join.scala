package chext.elastic2

import chisel3._
import chisel3.experimental.AffectsChiselPrefix
import chisel3.hacks.deferred

import scala.collection.mutable.ListBuffer

abstract class Join[T <: Data](val sink: Interface[T]) extends AffectsChiselPrefix {
  private val sourceList = ListBuffer.empty[Interface[Data]]

  protected val out: T = sink.bits

  protected final def onJoin: Unit = throw new NotImplementedError("Shall not be used!")

  /** Adds a new elastic interface to join.
    *
    * @param sink
    * @return
    */
  def join[TT <: Data](source: Interface[TT]): TT = {
    sourceList.addOne(source)
    source.bits
  }

  deferred {
    joinImpl.join(sourceList.toSeq, sink)
  }
}

private[elastic2] object joinImpl {

  /** Implements a join.
    *
    * @param sources
    * @param sink
    */
  def join[T <: Data](
      sources: Seq[Interface[Data]],
      sink: Interface[Data]
  ): Unit = {
    val allValid =
      VecInit(sources.map { _.valid }).reduceTree(_ && _)
    val fire = sink.ready && allValid
    sources.foreach { _.ready := fire }
    sink.valid := allValid
  }
}
