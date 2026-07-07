package chext.elastic

import chext.elastic.{tracking => t}

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred

import chext.deadlock
import chext.tracking.Component

/** Elastic demultiplexer with no explicit select interface. The destination
  * sink is computed directly from the current input token by the `select`
  * function.
  *
  * The `Ns` suffix reflects that this is the no-select variant.
  *
  * @tparam T
  *   payload type
  * @param source
  *   input interface
  * @param sinks
  *   output interfaces
  */
class DemuxNs[Tin <: Data, Tout <: Data](
    val source: Interface[Tin],
    val sinks: Seq[Interface[Tout]]
)(implicit si_ : SourceInfo)
    extends Component {
  type SelectFn = Tin => UInt
  type OutFn = Tin => Data
  type OutExplicitFn = (Tin, Data) => Unit

  private var selectFn_ = Option.empty[SelectFn]
  private var outFn_ = Option.empty[OutFn]

  override def tpe: String = "DemuxNs"
  override def namePrefix: String = "demuxNs"
  override val sourceInfo: SourceInfo = si_

  private val require_ = chext.util.Require.inferred(sourceInfo)

  require_(sinks.nonEmpty, "requires at least one sink interface")

  private val genIn = chiselTypeOf(source.$bits)
  private val genOut = chiselTypeOf(sinks.head.$bits)

  private val elasticState = trackingState(t.Tag)
  import elasticState._

  addSourcePort("source", source)
  sinks.zipWithIndex.foreach { //
    case (sink, i) => addSinkPort(s"sink_$i", sink)
  }

  /** Sets the function that computes the sink index for the current token.
    *
    * This function must be called at most once.
    *
    * Example:
    * {{{
    * select { in => in(3, 0) }
    * }}}
    *
    * @param fn
    *   function returning the destination sink index
    */
  protected final def select(fn: => SelectFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(
      selectFn_.isEmpty,
      "'select { (in) => ... }' must be called at most once!"
    )

    selectFn_ = Some(fn)
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
    require_(
      selectFn_.nonEmpty,
      "'select { (in) => ... }' must be called at least once!"
    )

    val selectFn = selectFn_.get
    val outFn = outFn_.getOrElse((x: Tin) => x)
    val readyVector = VecInit(sinks.map { _.$ready })

    val select = selectFn(source.$bits)
    val valid = source.$valid
    val fire = valid && readyVector(select)

    source.$ready := fire

    sinks.zipWithIndex.foreach { //
      // sink ready might wait for sink valid
      // so, make sure that they do not depend on each other
      case (x, i) => x.$valid := valid && (i.U === select)
    }

    sinks.foreach { //
      sink => sink.$bits := outFn(source.$bits)
    }

    val monitor0 = new deadlock.Monitor(this) {
      source.waitValid := true.B
    }
  }
}
