package chext.elastic

import chext.elastic.{tracking => t}
import chisel3._
import chisel3.experimental.SourceInfo

import chext.tracking.Component

final class NullSource[T <: Data](
    sink: Interface[T]
)(implicit si_ : SourceInfo)
    extends Component {
  private val elasticState = trackingState(t.Tag)
  import elasticState.{addSource, addSink}

  addSink("sink", sink)

  val sourceInfo: SourceInfo = si_
  def tpe: String = "NullSource"
  def namePrefix: String = "nullSource"

  sink.noenq()
}
