package chext.elastic

import chext.elastic.{tracking => t}

import chisel3._
import chisel3.experimental.{SourceInfo, prefix}

import chext.tracking.{Component, uniquePrefix}

/** Connects two elastic interfaces without transforming the payload.
  *
  * The payload is connected with Chisel's standard `:=` connection (`sink.bits := source.bits`).
  * Consequently, compatible aggregate types use Chisel's normal name-based field matching. Use
  * [[Transform]] when the payload needs an explicit combinational transformation.
  *
  * @param source
  *   The upstream interface providing tokens.
  * @param sink
  *   The downstream interface accepting tokens connected from `source`.
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

  private val elasticState = trackingState(t.Tag)
  import elasticState._

  addSourcePort("source", source)
  addSinkPort("sink", sink)

  val sourceInfo: SourceInfo = si_
  def tpe: String = "Connect"
  def namePrefix: String = "connect"

  protected final val in = source.$bits
  private final val out = sink.$bits

  out := in

  sink.$valid := source.$valid
  source.$ready := sink.$ready

}

object ConnectOp {
  private val require_ = chext.util.Require.inferred()

  private def connect_[T <: Data](
      source: Interface[T],
      sink: Interface[T]
  )(implicit sourceInfo: SourceInfo): Connect[T, T] =
    uniquePrefix("connect") {
      new Connect(source, sink)
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

      uniquePrefix("connectMany") {
        sources.zip(sinks).zipWithIndex.foreach { case ((source, sink), index) =>
          prefix(index.toString) {
            val connect0 = new Connect(source, sink)
          }
        }
      }
    }
  }
}
