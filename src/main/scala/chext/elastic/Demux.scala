package chext.elastic

import chext.elastic.{tracking => t}

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred
import chisel3.util.log2Ceil

import chext.deadlock
import chext.tracking.Component

/** Elastic demultiplexer with an optional `last` predicate.
  *
  * The input token is routed to exactly one sink selected by `sourceSelect`.
  * The `last` function determines whether the current source token is the last
  * one for the current sequence. If `last` is not provided, it defaults to
  * `true.B`.
  *
  * @tparam T
  *   payload type
  * @param source
  *   input interface
  * @param sinks
  *   output interfaces
  * @param sourceSelect
  *   select interface indicating which sink should receive the token
  */
class Demux[Tin <: Data, Tout <: Data](
    val source: Interface[Tin],
    val sinks: Seq[Interface[Tout]],
    val sourceSelect: Interface[UInt]
)(implicit si_ : SourceInfo)
    extends Component {
  type LastFn = Tin => Bool
  type OutFn = Tin => Data
  type OutExplicitFn = (Tin, Data) => Unit

  private var lastFn_ = Option.empty[LastFn]
  private var outFn_ = Option.empty[OutFn]

  override def tpe: String = "Demux"
  override def namePrefix: String = "demux"
  override val sourceInfo: SourceInfo = si_

  private val require_ = chext.util.Require.inferred(sourceInfo)

  require_(sinks.nonEmpty, "requires at least one sink interface")
  require_(
    sourceSelect.$bits.widthKnown,
    "`sourceSelect.$bits` width must be statically known"
  )
  require_(
    sourceSelect.$bits.getWidth >= log2Ceil(sinks.length),
    "`sourceSelect.$bits` width is too small for the number of sinks"
  )

  private val genIn = chiselTypeOf(source.$bits)
  private val genOut = chiselTypeOf(sinks.head.$bits)

  private val elasticState = trackingState(t.Tag)
  import elasticState.{addSource, addSink}

  addSource("source", source)
  sinks.zipWithIndex.foreach { //
    case (sink, i) => addSink(s"sink_$i", sink)
  }
  addSource("sourceSelect", sourceSelect)

  /** Sets the predicate that marks the current token as the last one.
    *
    * If this function is not called, the default is `(_: Tin) => true.B`. This
    * function must be called at most once.
    *
    * Example:
    * {{{
    * last { in => in === 0.U }
    * }}}
    *
    * @param fn
    *   predicate returning `true.B` when the current token is the last one
    */
  protected final def last(fn: => LastFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(
      lastFn_.isEmpty,
      "'last { (in) => ... }' must be called at most once!"
    )

    lastFn_ = Some(fn)
  }

  /** Sets a pure functional transformation for the value driven on each sink.
    * The function takes the input data and returns the transformed value.
    */
  protected final def out(fn: => OutFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(
      outFn_.isEmpty,
      "'out { (in) => ... }' must be called at most once!"
    )

    outFn_ = Some(fn)
  }

  /** Sets an imperative transformation for the value driven on each sink. The
    * function takes the input data and a mutable output wire.
    */
  protected final def outExplicit(fn: => OutExplicitFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(
      outFn_.isEmpty,
      "'outExplicit { (in, out) => ... }' must be called at most once!"
    )

    out { (in) =>
      {
        val outResult = Wire(genOut)
        fn(in, outResult)
        outResult
      }
    }
  }

  deferred {
    val lastFn = lastFn_.getOrElse((_: Tin) => true.B)
    val outFn = outFn_.getOrElse((x: Tin) => x)
    val readyVector = VecInit(sinks.map { _.$ready })

    val valid = sourceSelect.$valid && source.$valid
    val fire = valid && readyVector(sourceSelect.$bits)
    val isLast = lastFn(source.$bits)

    source.$ready := fire

    sinks.zipWithIndex.foreach { //
      // sink ready might wait for sink valid
      // so, make sure that they do not depend on each other
      case (x, i) => x.$valid := valid && (i.U === sourceSelect.$bits)
    }

    sourceSelect.$ready := fire && isLast

    sinks.foreach { //
      sink => sink.$bits := outFn(source.$bits)
    }

    val monitor0 = new deadlock.Monitor(this) {
      source.waitValid := sourceSelect.$valid
      sourceSelect.waitValid := true.B
    }
  }
}

object Demux {
  def apply[T <: Data](
      source: Interface[T],
      sinks: Seq[Interface[T]],
      select: Interface[UInt],
      isLastFn: T => Bool = (_: T) => true.B
  )(implicit si: SourceInfo): Component = {
    new Demux(source, sinks, select) {
      last { isLastFn(_) }
    }
  }
}
