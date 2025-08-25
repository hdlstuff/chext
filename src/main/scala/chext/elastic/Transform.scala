package chext.elastic

import chisel3._
import chisel3.experimental.SourceInfo

import chext.Prefix.needsPrefix
import tracking.Component

abstract class Transform[Tin <: Data, Tout <: Data](
    source: Interface[Tin],
    sink: Interface[Tout]
)(implicit sourceInfo: SourceInfo)
    extends Fire[Tout](sink) {
  needsPrefix("Transform", "transform")
  source.markSource()
  sink.markSink()

  Component(
    chext.Prefix.currentPrefix,
    "Transform",
    Seq(("source", source)),
    Seq(("sink", sink))
  ).register()

  protected final val in = source.$bits
  protected final val out = sink.$bits

  sink.$valid := source.$valid
  source.$ready := sink.$ready
}
