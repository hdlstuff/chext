package chext.elastic

import chext.elastic.{tracking => t}

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred
import chisel3.util.log2Ceil

import chext.deadlock
import chext.tracking.Component

/** Elastic arbiter that selects one valid source and forwards it to the sink.
  *
  * The arbitration decision is delegated to `chooser`, which receives the
  * vector of source valid signals and a readiness hint.
  *
  * @tparam T
  *   payload type
  * @param sources
  *   input source interfaces
  * @param sink
  *   output sink interface
  * @param select
  *   output interface carrying the chosen source index
  * @param chooser
  *   arbitration function
  */
final class Arbiter[Tin <: Data, Tout <: Data](
    val sources: Seq[Interface[Tin]],
    val sink: Interface[Tout],
    val sinkSelect: Interface[UInt],
    val chooser: Chooser
)(implicit si_ : SourceInfo)
    extends Component
    with Fire[Tout] {
  protected def fireSink: Interface[Tout] = sink

  type OutFn = Tin => Data
  type OutExplicitFn = (Tin, Data) => Unit

  private var outFn_ = Option.empty[OutFn]

  override def tpe: String = "Arbiter"
  override def namePrefix: String = "arbiter"
  override val sourceInfo: SourceInfo = si_

  private val require_ = chext.util.Require.inferred(sourceInfo)

  require_(sources.nonEmpty, "requires at least one source interface")
  require_(
    sinkSelect.$bits.widthKnown,
    "`sinkSelect.$bits` width must be statically known"
  )
  require_(
    sinkSelect.$bits.getWidth >= log2Ceil(sources.length),
    "`sinkSelect.$bits` width is too small for the number of sources"
  )

  private val genIn = chiselTypeOf(sources.head.$bits)
  private val genOut = chiselTypeOf(sink.$bits)

  private val elasticState = trackingState(t.Tag)
  import elasticState._

  sources.zipWithIndex.foreach { //
    case (source, i) => addSourcePort(s"source_$i", source)
  }
  addSinkPort("sink", sink)
  addSinkPort("sinkSelect", sinkSelect)

  /** Sets a pure functional transformation for the value driven on the sink.
    * The function takes the arbitrated input data and returns the transformed
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
    * function takes the arbitrated input data and a mutable output wire.
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
    val outFn = outFn_.getOrElse((x: Tin) => x)
    val bitsVector = VecInit(sources.map { _.$bits })
    val validVector = VecInit(sources.map { _.$valid })

    val regSink = RegInit(false.B)
    val regSelect = RegInit(false.B)

    val ready = (sink.$ready || regSink) && (sinkSelect.$ready || regSelect)
    val choice = chooser(validVector, ready)

    sources.zipWithIndex.foreach { //
      case (x, i) => x.$ready := ready && (i.U === choice)
    }

    sink.$valid := validVector(choice) && !regSink
    sinkSelect.$valid := validVector(choice) && !regSelect

    regSink := (sink.$ready || regSink) && validVector(choice) && !ready
    regSelect := (sinkSelect.$ready || regSelect) &&
      validVector(choice) && !ready

    sink.$bits := outFn(bitsVector(choice))
    sinkSelect.$bits := choice

    val monitor0 = new deadlock.Monitor(this) {
      sources.zipWithIndex.foreach { //
        case (x, i) =>
          // if none of the sources are valid, we waitValid on them all
          x.waitValid := VecInit(sources.map { !_.$valid }).asUInt.andR
      }
    }
  }
}

object Arbiter {
  def apply[T <: Data](
      sources: Seq[Interface[T]],
      sink: Interface[T],
      chooserFn: Chooser,
      select: Option[Interface[UInt]] = None
  )(implicit si: SourceInfo): Component = {

    if (select.nonEmpty) {
      new Arbiter(
        sources,
        sink,
        select.get,
        chooserFn
      )
    } else {
      new ArbiterNs(
        sources,
        sink,
        chooserFn
      )
    }
  }
}
