package chext.elastic

import chisel3._
import chisel3.experimental.AffectsChiselPrefix
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred

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
    source: Interface[Tin],
    sink: Interface[Tout]
)(implicit sourceInfo: SourceInfo)
    extends Fire[Tout](sink) {
  protected final val in = source.bits
  protected final val out = sink.bits

  private var condFn_ = Option.empty[() => Bool]

  private def require_(cond: Boolean, msg: String): Unit = {
    require(cond, sourceInfo.makeMessage((x) => f"Drop: $msg @[$x]"))
  }

  /** Sets the condition under which an input token should be dropped.
    *
    * This must be called exactly once. The condition is evaluated every cycle. If it returns
    * `true`, the current input token is dropped (not forwarded). If it returns `false`, the token
    * is passed to the output normally.
    *
    * @example
    *   cond { in.shouldBeDropped }
    */
  protected final def cond(fn: => Bool): Unit = {
    require_(condFn_.isEmpty, "'cond { ... }' must be called at most once!")
    condFn_ = Some(() => { fn })
  }

  deferred {
    require_(condFn_.nonEmpty, "'cond { ... }' must be called at least once!")

    val cond = condFn_.get()

    source.ready := cond || sink.ready
    sink.valid := !cond && source.valid
  }
}
