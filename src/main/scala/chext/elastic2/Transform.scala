package chext.elastic2

import chisel3._
import chisel3.experimental.AffectsChiselPrefix

abstract class Transform[SourceT <: Data, SinkT <: Data](
    source: Interface[SourceT],
    sink: Interface[SinkT]
) extends AffectsChiselPrefix {
  protected val in = source.bits
  protected val out = sink.bits

  protected final def onTransform: Unit = throw new NotImplementedError("Shall not be used!")

  sink.valid := source.valid
  source.ready := sink.ready
}
