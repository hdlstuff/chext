package chext.elastic

import chisel3._
import chisel3.experimental.SourceInfo

import chext.tracking
import tracking.Component

final class NullSource[T <: Data](
    sink: Interface[T]
)(implicit si_ : SourceInfo)
    extends Component {
  addSinkPort("sink", sink)

  val sourceInfo: SourceInfo = si_
  def tpe: String = "NullSource"
  def namePrefix: String = "nullSource"

  sink.noenq()
}
