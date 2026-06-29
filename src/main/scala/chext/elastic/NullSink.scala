package chext.elastic

import chisel3._
import chisel3.experimental.SourceInfo

import chext.tracking
import tracking.Component

final class NullSink[T <: Data](
    source: Interface[T]
)(implicit si_ : SourceInfo)
    extends Component {
  addSourcePort("source", source)

  val sourceInfo: SourceInfo = si_
  def tpe: String = "NullSink"
  def namePrefix: String = "nullSink"

  source.nodeq()
}
