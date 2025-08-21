package chext.elastic

import chisel3._
import chisel3.experimental.SourceInfo

abstract class Transform[Tin <: Data, Tout <: Data](
    source: Interface[Tin],
    sink: Interface[Tout]
)(implicit sourceInfo: SourceInfo)
    extends Fire[Tout](sink) {
  chext.naming.checkPrefix("Transform", "transform")
  source.markSource()
  sink.markSink()

  protected final val in = source.$bits
  protected final val out = sink.$bits

  sink.$valid := source.$valid
  source.$ready := sink.$ready
}
