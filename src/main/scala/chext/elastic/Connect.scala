package chext.elastic

import chisel3._
import chisel3.experimental.AffectsChiselPrefix
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred

/** `Connect` connects two elastic interfaces, with an optional combinational transformation given
  * by:
  * {{{
  * out := in // the user can choose whichever transformation
  * }}}
  *
  * @param source
  *   The upstream interface providing tokens.
  * @param sink
  *   The downstream interface accepting tokens which are optionally modified.
  * @tparam Tin
  *   Type of the input token.
  * @tparam Tout
  *   Type of the output token.
  */
abstract class Connect[Tin <: Data, Tout <: Data](
    source: Interface[Tin],
    sink: Interface[Tout]
)(implicit sourceInfo: SourceInfo)
    extends Fire[Tout](sink) {
  protected final val in = source.bits
  protected final val out = sink.bits

  out := in

  sink.valid := source.valid
  source.ready := sink.ready
}

object Connect {
  def apply[T <: Data](source: Interface[T], sink: Interface[T]) = {
    source.ready := sink.ready
    sink.valid := source.valid
    sink.bits := source.bits
  }
}

object ConnectOp {
  implicit class elastic_connect_op[T <: Data](source: Interface[T]) {
    def :=>(sink: Interface[T]): Unit = {
      Connect(source, sink)
    }
  }
}
