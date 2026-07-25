package chext.elastic

import chext.elastic.{tracking => t}

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred

import scala.collection.mutable.ArrayBuffer

import chext.deadlock
import chext.tracking.Component

/** `Transducer` creates a finite-state transducer between a source and a sink.
  *
  * It defines four actions: `stall`, `accept`, `consume`, and `produce`, only one action must be
  * taken at a time. Each action can be associated with a state transition, which is executed only
  * if the action is taken.
  *
  * The user **must not** update any state outside actions.
  *
  * Exactly one action must be taken in every clock cycle when the source is valid.
  *
  *   - If more than one action is taken, `Transducer` emits a simulation-time warning.
  *   - If no action is taken, the default behavior is `stall`. However, `Transducer` emits a
  *     simulation-time warning in this case.
  *
  * Usage:
  * {{{
  * packet {
  *   when(in.flag) {
  *     accept { ... }
  *   }.otherwise {
  *     consume { ... }
  *   }
  * }
  * }}}
  *
  * @param source
  *   The source elastic interface.
  * @param sink
  *   The sink elastic interface.
  * @tparam Tin
  *   The type of the source's data payload (`source.$bits`).
  * @tparam Tout
  *   The type of the sink's data payload (`sink.$bits`).
  */
abstract class Transducer[Tin <: Data, Tout <: Data](
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
  def tpe: String = "Transducer"
  def namePrefix: String = "transducer"

  protected final val in = source.$bits
  protected final val out = sink.$bits

  type PacketFn = () => Unit
  type ActionFn = () => Unit

  private var packetFn_ = Option.empty[PacketFn]

  private var dryRun_ = true

  private var actions_ = ArrayBuffer.empty[(String, SourceInfo)]
  private var actionIndex_ = 0
  private var actionConds_ = Seq.empty[Bool]

  private var deferredContext_ = false

  private val require_ = chext.util.Require.inferred(sourceInfo)

  private def requireDeferredContext_(funcName: String)(implicit sourceInfo: SourceInfo): Unit = {
    require_.here(
      deferredContext_,
      f"$funcName must be called within 'packet { ... }'"
    )(sourceInfo)
  }

  // format: off

  /**
   * Defines the transducer’s per-packet behavior.
   * Must be called exactly once.
   * 
   * All actions (`accept`, `stall`, `consume`, `produce`) must appear within this block.
   */
  protected final def packet(fn: => Unit)(implicit si_ : SourceInfo): Unit = {
    if (!deferredContext_)
      require_.here(packetFn_.isEmpty, "'packet { ... }' must be defined at most once!")

    packetFn_ = Some(() => fn)
  }

  /**
   * Neither consumes input nor produces output; holds state.
   * 
   * Executed only if the source is valid.
   * Must be used inside a `packet { ... }` block.
   */
  protected final def stall(action: => Unit)(implicit si_ : SourceInfo): Unit = {
    requireDeferredContext_("stall")

    if (dryRun_) {
      actions_.addOne(("stall", sourceInfo))
      return
    }

    actionConds_(actionIndex_) := true.B
    actionIndex_ += 1

    source.$ready := false.B
    sink.$valid := false.B

    action
  }

  /**
   * Accepts a source packet and produces a sink packet in the same cycle.
   * 
   * Executed only if the source is valid and the sink is ready.
   * Must be used inside a `packet { ... }` block.
   */
  protected final def accept(action: => Unit)(implicit si_ : SourceInfo): Unit = {
    requireDeferredContext_("accept")

    if (dryRun_) {
      actions_.addOne(("accept", sourceInfo))
      return
    }

    actionConds_(actionIndex_) := true.B
    actionIndex_ += 1

    source.$ready := sink.$ready
    sink.$valid := source.$valid

    when(sink.$ready) {
      action
    }
  }

  /**
   * Consumes a source packet without producing an output.
   * 
   * Executed only if the source is valid.
   * Must be used inside a `packet { ... }` block.
   */
  protected final def consume(action: => Unit)(implicit si_ : SourceInfo): Unit = {
    requireDeferredContext_("consume")

    if (dryRun_) {
      actions_.addOne(("consume", sourceInfo))
      return
    }

    actionConds_(actionIndex_) := true.B
    actionIndex_ += 1

    source.$ready := true.B
    sink.$valid := false.B

    action
  }

  /**
   * Produces a sink packet without consuming the source packet.
   * 
   * Executed only if the source is valid and the sink is ready.
   * Must be used inside a `packet { ... }` block.
   */
  protected final def produce(action: => Unit)(implicit si_ : SourceInfo): Unit = {
    requireDeferredContext_("produce")

    if (dryRun_) {
      actions_.addOne(("produce", sourceInfo))
      return
    }

    actionConds_(actionIndex_) := true.B
    actionIndex_ += 1

    source.$ready := false.B
    sink.$valid := source.$valid

    when(sink.$ready) {
      action
    }
  }

  // format: on

  deferred {
    require_(packetFn_.nonEmpty, "'packet { ... }' must be called at least once!")

    deferredContext_ = true

    dryRun_ = true
    when(source.$valid) {
      // Note: even for a dry run, we should have source.$valid
      // in case there are some wire-assignments inside packetFn
      packetFn_.get()
    }

    dryRun_ = false

    actionConds_ = Seq.fill(actions_.length) { WireInit(false.B) }

    source.$ready := false.B
    sink.$valid := false.B

    when(source.$valid) {
      packetFn_.get()
    }.otherwise {
      out := DontCare
    }

    require_(
      actionConds_.nonEmpty,
      "elastic.Transducer: There must be at least one action taken in the transducer. Maybe call accept{} in it?"
    )

    val cond = VecInit(actionConds_).asUInt

    val errorAtLeastTwoActions = WireInit(source.$valid && ((cond & (cond -% 1.U)) =/= 0.U))
    val errorNoAction = WireInit(source.$valid && (cond === 0.U))

    dontTouch(errorAtLeastTwoActions)
    dontTouch(errorNoAction)

    when(errorAtLeastTwoActions) {
      printf("elastic.Transducer: at least two actions are taken in the same clock cycle!\n")
      actionConds_.zip(actions_).foreach {
        case (cond, (funcName, sourceInfo)) => {
          when(cond) {
            printf(sourceInfo.makeMessage { (x) => f"elastic.Transducer: action '$funcName' $x\n" })
          }
        }
      }
    }

    when(errorNoAction) {
      printf(sourceInfo.makeMessage { (x) => f"elastic.Transducer: no action was taken! $x\n" })
    }

    assert(actionIndex_ == actions_.length)

    val monitor0 = new deadlock.Monitor(this) {
      source.waitValid := true.B
    }
  }
}
