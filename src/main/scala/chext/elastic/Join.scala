package chext.elastic

import chisel3._
import chisel3.experimental.AffectsChiselPrefix
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred

import scala.collection.mutable.ListBuffer

abstract class Join[T <: Data](
    val sink: Interface[T]
)(implicit si: SourceInfo)
    extends AffectsChiselPrefix {
  chext.naming.checkPrefix("Join", "join")

  private val sourceList = ListBuffer.empty[Interface[Data]]

  protected val out: T = sink.$bits

  protected final def onJoin: Unit = throw new NotImplementedError("Shall not be used!")

  /** Adds a new elastic interface to join.
    *
    * @param sink
    * @return
    */
  def join[TT <: Data](source: Interface[TT]): TT = {
    sourceList.addOne(source)
    source.$bits
  }

  deferred {
    // TODO warn if no sources, but do not fail

    joinImpl.join(sourceList.toSeq, sink)
  }

}

private[elastic] object joinImpl {

  /** Implements a join.
    *
    * @param sources
    * @param sink
    */
  def join[T <: Data](
      sources: Seq[Interface[Data]],
      sink: Interface[Data]
  )(implicit si: SourceInfo): Unit = {
    sources.foreach { _.markSource() }
    sink.markSink()

    val allValid =
      VecInit(sources.map { _.$valid }).reduceTree(_ && _)
    val fire = sink.$ready && allValid
    sources.foreach { _.$ready := fire }
    sink.$valid := allValid
  }
}
