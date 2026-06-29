package chext.elastic

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred
import chisel3.util.log2Ceil

import chext.tracking.Component

/** Elastic arbiter with no explicit select interface. One valid source is
  * chosen by the `chooser` function and forwarded to the sink.
  *
  * The `Ns` suffix reflects that this is the no-select variant.
  *
  * @tparam T
  *   payload type
  * @param sources
  *   input source interfaces
  * @param sink
  *   output sink interface
  * @param chooser
  *   arbitration function
  */
final class ArbiterNs[Tin <: Data, Tout <: Data](
    val sources: Seq[Interface[Tin]],
    val sink: Interface[Tout],
    val chooser: Chooser
)(implicit si_ : SourceInfo)
    extends Component {
  type OutFn = Tin => Data
  type OutExplicitFn = (Tin, Data) => Unit

  private var outFn_ = Option.empty[OutFn]

  override def tpe: String = "ArbiterNs"
  override def namePrefix: String = "arbiter"
  override val sourceInfo: SourceInfo = si_

  private def require_(cond: Boolean, msg: String): Unit = {
    require(cond, sourceInfo.makeMessage((x) => s"ArbiterNs: $msg $x"))
  }

  require_(sources.nonEmpty, "requires at least one source interface")

  private val genIn = chiselTypeOf(sources.head.$bits)
  private val genOut = chiselTypeOf(sink.$bits)

  sources.zipWithIndex.foreach { //
    case (source, i) => addSourcePort(s"source_$i", source)
  }
  addSinkPort("sink", sink)

  /** Sets a pure functional transformation for the value driven on the sink.
    * The function takes the arbitrated input data and returns the transformed
    * value.
    */
  protected final def out(fn: => OutFn): Unit = {
    require_(
      outFn_.isEmpty,
      "'out { (in) => ... }' must be called at most once!"
    )

    outFn_ = Some(fn)
  }

  /** Sets an imperative transformation for the value driven on the sink. The
    * function takes the arbitrated input data and a mutable output wire.
    */
  protected final def outExplicit(fn: => OutExplicitFn): Unit = {
    require_(
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

    val ready = sink.$ready
    val choice = chooser(validVector, ready)

    sources.zipWithIndex.foreach { //
      case (x, i) => x.$ready := ready && (i.U === choice)
    }

    sink.$valid := validVector(choice)
    sink.$bits := outFn(bitsVector(choice))

    new chext.deadlock.DeadlockMonitor(this) {
      sources.zipWithIndex.foreach { //
        case (x, i) =>
          x.waitValid := VecInit(sources.map { !_.$valid }).asUInt.andR
      }
    }
  }
}
