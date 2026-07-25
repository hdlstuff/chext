package chext.elastic

import chext.elastic.{tracking => t}
import chisel3._
import chisel3.experimental.SourceInfo

import chext.tracking.Component

final class NullSink[T <: Data](
    val source: Interface[T]
)(implicit si_ : SourceInfo)
    extends Component
    with Fire[T] {
  protected def fireSink: Interface[T] = source

  private val elasticState = trackingState(t.Tag)
  import elasticState.{addSource, addSink}

  addSource("source", source)

  val sourceInfo: SourceInfo = si_
  def tpe: String = "NullSink"
  def namePrefix: String = "nullSink"

  source.deq()
}
