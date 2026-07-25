package chext.elastic

import chext.elastic.{tracking => t}

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred

import chext.tracking.Component

/** `Drop` conditionally drops tokens.
  *
  * It connects a source and sink interface, and forwards tokens from source to sink only when a
  * user-defined condition is **false**. If the condition is **true**, the token is dropped
  * silently.
  *
  * @param source
  *   The upstream interface providing tokens.
  * @param sink
  *   The downstream interface accepting filtered tokens.
  * @tparam Tin
  *   Type of the input token.
  * @tparam Tout
  *   Type of the output token.
  */
abstract class Drop[Tin <: Data, Tout <: Data](
    val source: Interface[Tin],
    val sink: Interface[Tout]
)(implicit si_ : SourceInfo)
    extends Component
    with Fire[Tout] {
  protected def fireSink: Interface[Tout] = sink

  private val elasticState = trackingState(t.Tag)
  import elasticState.{addSource, addSink}

  addSource("source", source)
  addSink("sink", sink)

  val sourceInfo: SourceInfo = si_
  def tpe: String = "Drop"
  def namePrefix: String = "drop"

  protected final val in = source.$bits
  protected final val out = sink.$bits

  private var condFn_ = Option.empty[() => Bool]

  private val require_ = chext.util.Require.inferred(sourceInfo)

  /** Sets the condition under which an input token should be dropped.
    *
    * This must be called exactly once. The condition is evaluated every cycle. If it returns
    * `true`, the current input token is dropped (not forwarded). If it returns `false`, the token
    * is passed to the output normally.
    *
    * @example
    *   cond { in.shouldBeDropped }
    */
  protected final def cond(fn: => Bool)(implicit si_ : SourceInfo): Unit = {
    require_.here(condFn_.isEmpty, "'cond { ... }' must be called at most once!")
    condFn_ = Some(() => { fn })
  }

  deferred {
    require_(condFn_.nonEmpty, "'cond { ... }' must be called at least once!")

    val cond = condFn_.get()

    source.$ready := cond || sink.$ready
    sink.$valid := !cond && source.$valid
  }
}
