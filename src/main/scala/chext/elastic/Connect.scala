package chext.elastic

import chisel3._
import chisel3.experimental.prefix
import chisel3.experimental.SourceInfo
import chisel3.experimental.skipPrefix

import chisel3.hacks.deferred

import chext.tracking
import tracking.Component
import tracking.RigidComponent
import tracking.uniquePrefix

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
  private val require_ = chext.util.Require.inferred()

  private def connect_[T <: Data](
      source: Interface[T],
      sink: Interface[T]
  )(implicit sourceInfo: SourceInfo) = {
    uniquePrefix {
      source.$ready := sink.$ready
      sink.$valid := source.$valid
      sink.$bits := source.$bits

      val component = skipPrefix { new RigidComponent("Connect", "connect") }
      component.source("source", source)
      component.sink("sink", sink)
    }
  }

  implicit class elastic_connect_op[T <: Data](val source: Interface[T]) extends AnyVal {
    def :=>(sink: Interface[T])(implicit sourceInfo: SourceInfo): Unit = {
      connect_(source, sink)
    }
  }

  implicit class elastic_connect_seq_op[T <: Data](val sources: Seq[Interface[T]]) extends AnyVal {
    def :=>(sinks: Seq[Interface[T]])(implicit sourceInfo: SourceInfo): Unit = {
      require_(
        sources.length == sinks.length,
        f"source/sink sequence length mismatch: ${sources.length} != ${sinks.length}"
      )

      sources.zip(sinks).zipWithIndex.foreach { case ((source, sink), index) =>
        prefix(index.toString) {
          connect_(source, sink)
        }
      }
    }
  }
}
