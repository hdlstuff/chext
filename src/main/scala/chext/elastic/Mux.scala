package chext.elastic

import chext.elastic.{tracking => t}

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred
import chisel3.util.log2Ceil

import chext.deadlock
import chext.tracking.Component

/** Elastic multiplexer with an optional `last` predicate.
  *
  * One of the input sources is selected by `sourceSelect` and forwarded to the
  * sink. The `last` function determines whether the current sink token is the
  * last one for the current sequence. If `last` is not provided, it defaults to
  * `true.B`.
  *
  * @tparam T
  *   payload type
  * @param sources
  *   input source interfaces
  * @param sink
  *   output sink interface
  * @param sourceSelect
  *   select interface indicating which source should be forwarded
  */
class Mux[Tin <: Data, Tout <: Data](
    val sources: Seq[Interface[Tin]],
    val sink: Interface[Tout],
    val sourceSelect: Interface[UInt]
)(implicit si_ : SourceInfo)
    extends Component
    with Fire[Tout] {
  protected def fireSink: Interface[Tout] = sink

  type LastFn = Tin => Bool
  type OutFn = Tin => Data
  type OutExplicitFn = (Tin, Data) => Unit

  private var lastFn_ = Option.empty[LastFn]
  private var outFn_ = Option.empty[OutFn]

  override def tpe: String = "Mux"
  override def namePrefix: String = "mux"
  override val sourceInfo: SourceInfo = si_

  private val require_ = chext.util.Require.inferred(sourceInfo)

  require_(sources.nonEmpty, "requires at least one source interface")
  require_(
    sourceSelect.$bits.widthKnown,
    "`sourceSelect.$bits` width must be statically known"
  )
  require_(
    sourceSelect.$bits.getWidth >= log2Ceil(sources.length),
    "`sourceSelect.$bits` width is too small for the number of sources"
  )

  private val genIn = chiselTypeOf(sources.head.$bits)
  private val genOut = chiselTypeOf(sink.$bits)

  private val elasticState = trackingState(t.Tag)
  import elasticState.{addSource, addSink}

  sources.zipWithIndex.foreach { //
    case (source, i) => addSource(s"source_$i", source)
  }
  addSink("sink", sink)
  addSource("sourceSelect", sourceSelect)

  /** Sets the predicate that marks the current token as the last one.
    *
    * If this function is not called, the default is `(_: Tin) => true.B`. This
    * function must be called at most once.
    *
    * Example:
    * {{{
    * last { out => out === 0.U }
    * }}}
    *
    * @param fn
    *   predicate returning `true.B` when the current token is the last one
    */
  protected final def last(fn: => LastFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(
      lastFn_.isEmpty,
      "'last { (out) => ... }' must be called at most once!"
    )

    lastFn_ = Some(fn)
  }

  /** Sets a pure functional transformation for the value driven on the sink.
    * The function takes the selected input data and returns the transformed
    * value.
    */
  protected final def out(fn: => OutFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(
      outFn_.isEmpty,
      "'out { (in) => ... }' must be called at most once!"
    )

    outFn_ = Some(fn)
  }

  /** Sets an imperative transformation for the value driven on the sink. The
    * function takes the selected input data and a mutable output wire.
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

    val bitsVector = VecInit(sources.map { _.$bits })
    val validVector = VecInit(sources.map { _.$valid })

    val valid = sourceSelect.$valid && validVector(sourceSelect.$bits)
    val fire = valid && sink.$ready
    val isLast = lastFn(bitsVector(sourceSelect.$bits))

    sources.zipWithIndex.foreach { //
      case (x, i) => x.$ready := fire && (i.U === sourceSelect.$bits)
    }

    // sink ready might wait for sink valid
    // so, make sure that they do not depend on each other
    sink.$valid := valid

    sourceSelect.$ready := fire && isLast

    sink.$bits := outFn(bitsVector(sourceSelect.$bits))

    val monitor0 = new deadlock.Monitor(this) {
      sources.zipWithIndex.foreach { //
        case (x, i) =>
          x.waitValid := sourceSelect.$valid && (i.U === sourceSelect.$bits)
      }

      sink.waitReady := sourceSelect.$valid

      sourceSelect.waitValid := true.B
    }
  }
}

object Mux {
  def apply[T <: Data](
      sources: Seq[Interface[T]],
      sink: Interface[T],
      select: Interface[UInt],
      isLastFn: T => Bool = (_: T) => true.B
  )(implicit si: SourceInfo): Component = {
    new Mux(sources, sink, select) {
      last { isLastFn(_) }
    }
  }
}
