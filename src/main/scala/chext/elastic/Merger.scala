package chext.elastic

import chext.elastic.{tracking => t}

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred
import chisel3.util._

import chext.deadlock
import chext.tracking.Component

/** Merges multiple elastic streams into one. It **must** be guaranteed that at
  * any given time at most a single `source` is active.
  *
  * @tparam T
  *   payload type
  * @param sources
  *   input source interfaces
  * @param sink
  *   output sink interface
  */
final class Merger[T <: Data](
    val sources: Seq[Interface[T]],
    val sink: Interface[T]
)(implicit si_ : SourceInfo)
    extends Component
    with Fire[T] {
  protected def fireSink: Interface[T] = sink

  override def tpe: String = "Merger"
  override def namePrefix: String = "merger"
  override val sourceInfo: SourceInfo = si_

  private val require_ = chext.util.Require.inferred(sourceInfo)

  require_(sources.nonEmpty, "requires at least one source interface")

  private val genIn = chiselTypeOf(sources.head.$bits)

  private val elasticState = trackingState(t.Tag)
  import elasticState._

  sources.zipWithIndex.foreach { //
    case (source, i) => addSourcePort(s"source_$i", source)
  }
  addSinkPort("sink", sink)

  deferred {
    sources.zipWithIndex.foreach { //
      case (x, i) => x.$ready := sink.$ready
    }

    val chosen = PriorityEncoder(VecInit(sources.map { _.$valid }))
    sink.$bits := VecInit(sources.map { _.$bits })(chosen)
    sink.$valid := VecInit(sources.map { _.$valid }).reduceTree(_ || _)

    assert(
      PopCount(sources.map(_.$valid)) <= 1.U,
      "Merger: more than one source is active (multiple $valid asserted)"
    )

    val monitor0 = new deadlock.Monitor(this) {
      sources.zipWithIndex.foreach { //
        case (x, i) =>
          x.waitValid := VecInit(sources.map { !_.$valid }).asUInt.andR
      }
    }
  }
}

object Merger {
  def apply[T <: Data](
      sources: Seq[Interface[T]],
      sink: Interface[T]
  )(implicit si: SourceInfo): Component = {
    new Merger(sources, sink)
  }
}
