package chext.elastic

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.experimental.skipPrefix

import chisel3.hacks.deferred

import chext.tracking
import tracking.Component
import tracking.RigidComponent
import tracking.uniquePath

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
class Connect[Tin <: Data, Tout <: Data](
    source: Interface[Tin],
    sink: Interface[Tout]
)(implicit si_ : SourceInfo)
    extends Component
    with Fire[Tout] {
  protected def fireSink: Interface[Tout] = sink

  addSourcePort("source", source)
  addSinkPort("sink", sink)

  val sourceInfo: SourceInfo = si_
  def tpe: String = "Connect"
  def namePrefix: String = "connect"

  protected final val in = source.$bits
  protected final val out = sink.$bits

  out := in

  sink.$valid := source.$valid
  source.$ready := sink.$ready

}

object ConnectOp {
  private def connect_[T <: Data](
      source: Interface[T],
      sink: Interface[T]
  )(implicit sourceInfo: SourceInfo) = {
    uniquePath {
      source.$ready := sink.$ready
      sink.$valid := source.$valid
      sink.$bits := source.$bits

      val component = skipPrefix { new RigidComponent("Connect", "connect") }
      component.source("source", source)
      component.sink("sink", sink)
    }
  }

  implicit class elastic_connect_op[T <: Data](source: Interface[T]) {
    def :=>(sink: Interface[T])(implicit sourceInfo: SourceInfo): Unit = {
      connect_(source, sink)
    }
  }
}
