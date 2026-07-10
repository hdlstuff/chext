package chext.elastic

import chext.elastic.{tracking => t}

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred

import chext.deadlock
import chext.tracking.Component

/** Applies a user-defined combinational transformation between two elastic interfaces.
  *
  * `Transform` forwards the ready/valid handshake and exposes the source payload as protected
  * [[in]] and the sink payload as protected [[out]]. The subclass body must drive `out`, typically
  * with an assignment such as `out := f(in)`. Unlike [[Connect]], no payload connection is added
  * automatically.
  *
  * @param source
  *   The upstream interface providing input tokens.
  * @param sink
  *   The downstream interface accepting transformed tokens.
  * @tparam Tin
  *   Type of the input token.
  * @tparam Tout
  *   Type of the transformed output token.
  */
abstract class Transform[Tin <: Data, Tout <: Data](
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
  def tpe: String = "Transform"
  def namePrefix: String = "transform"

  protected final val in = source.$bits
  protected final val out = sink.$bits

  deferred {
    sink.$valid := source.$valid
    source.$ready := sink.$ready

    val monitor0 = new deadlock.Monitor(this) {
      source.waitValid := true.B
    }
  }
}
