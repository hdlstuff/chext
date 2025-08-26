package chext.elastic

import chisel3._
import chisel3.experimental.SourceInfo

import chext.tracking
import tracking.Component

abstract class Transform[Tin <: Data, Tout <: Data](
    source: Interface[Tin],
    sink: Interface[Tout]
)(implicit si_ : SourceInfo)
    extends Component
    with Fire[Tout] {
  protected def fireSink: Interface[Tout] = sink

  addSourcePort("source", source)
  addSinkPort("sink", sink)

  val sourceInfo: SourceInfo = si_
  def tpe: String = "Transform"
  def namePrefix: String = "transform"

  protected final val in = source.$bits
  protected final val out = sink.$bits

  sink.$valid := source.$valid
  source.$ready := sink.$ready
}
