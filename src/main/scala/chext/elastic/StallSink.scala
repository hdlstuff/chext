package chext.elastic

import chisel3._
import chisel3.experimental.SourceInfo

import chext.elastic.{tracking => t}
import chext.tracking.Component

/** Permanently backpressures an elastic source without consuming its packets. */
final class StallSink[T <: Data](
    source: Interface[T]
)(implicit si_ : SourceInfo)
    extends Component {
  private val elasticState = trackingState(t.Tag)
  import elasticState.addSource

  addSource("source", source)

  val sourceInfo: SourceInfo = si_
  def tpe: String = "StallSink"
  def namePrefix: String = "stallSink"

  source.nodeq()
}
