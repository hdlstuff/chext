package chext.elastic

import chisel3._
import chisel3.experimental.{AffectsChiselPrefix, SourceInfo}
import chisel3.hacks.deferred

// To avoid confusion with chisel Mux
import chext.elastic.{Mux => EMux}

import chext.naming.prefix

import ConnectOp._

/** Elastic fold (i.e., reduction) primitive.
  *
  * This module reduces a stream of elastic tokens using a user-defined operation. The fold starts
  * with an initial value (`sourceInit`) and applies a fold operation to each input token from
  * `source`. When a token is marked as `last`, the fold result is emitted to `sink`.
  *
  * The user **must** call:
  *   - `operand { (in) => ... }` or `operandExplicit { (in, out) => ... }`
  *   - `last { (in) => Bool }`
  *
  * Optionally, the user **may** call:
  *   - `first { (in) => Bool }` to indicate the first token in a sequence (to avoid FSM overhead)
  *   - `zero { (in) => Bool }` to discard irrelevant tokens before fold
  *
  * Example usage:
  * {{{
  * class MyReduce extends Fold[MyToken, UInt](source, init, sink) {
  *   operand { in => in.data }
  *   last { in => in.isLast }
  *
  *   new Join(SinkBuffer(sourceResult)) {
  *     out := join(sinkOpA) + join(sinkOpB)
  *   }
  * }
  * }}}
  *
  * Notes:
  *   - `sinkOpA` carries the **accumulator** (previous result or init)
  *   - `sinkOpB` always carries the **newest** token (current input)
  */
abstract class Fold[Tin <: Data, Tout <: Data](
    source: Interface[Tin],
    sourceInit: Interface[Tout],
    sink: Interface[Tout]
)(implicit sourceInfo: SourceInfo)
    extends Fire[Tout](sink) {
  chext.naming.checkPrefix("Fold", "fold")

  protected final val gen = chiselTypeOf(sink.$bits)

  protected final val sinkA = Wire(Interface(gen)) // newest input
  protected final val sinkB = Wire(Interface(gen)) // accumulator
  protected final val sourceResult = Wire(Interface(gen)) // fold result

  type OperandFn = (Tin) => Tout
  type OperandExplicitFn = (Tin, Tout) => Unit

  type FirstFn = (Tin) => Bool
  type LastFn = (Tin) => Bool
  type ZeroFn = (Tin) => Bool

  private var operandFn_ = Option.empty[OperandFn]
  private var firstFn_ = Option.empty[FirstFn]
  private var lastFn_ = Option.empty[LastFn]
  private var zeroFn_ = Option.empty[ZeroFn]

  private def require_(cond: Boolean, msg: String): Unit = {
    require(cond, sourceInfo.makeMessage(x => s"Reduce: $msg $x"))
  }

  private def throw_(msg: String) =
    throw new IllegalArgumentException(sourceInfo.makeMessage(x => s"Reduce: $msg $x"))

  protected final def elem: Tin = throw_("`elem` field shall not be used!")

  /** Sets the pure transformation function to apply on each input token. Must be called exactly
    * once.
    *
    * Example:
    * {{{
    * operand { in => in.data }
    * }}}
    */
  protected final def operand(fn: => OperandFn): Unit = {
    require_(operandFn_.isEmpty, "'operand { (in) => ... }' must be called at most once!")
    operandFn_ = Some(fn)
  }

  /** Sets the imperative version of the transformation logic. Allows wiring with side effects in
    * the body. Must be called at most once.
    *
    * Example:
    * {{{
    * operandExplicit { (in, out) => out := in.data }
    * }}}
    */
  protected final def operandExplicit(fn: => OperandExplicitFn): Unit = {
    operand {
      case (in) => {
        val opResult = Wire(gen)
        fn(in, opResult)
        opResult
      }
    }
  }

  /** Optionally sets the logic to determine whether a token starts a new fold group. If not set,
    * internal FSM logic is used.
    *
    * Example:
    * {{{
    * first { in => in.isFirst }
    * }}}
    */
  protected final def first(fn: => FirstFn): Unit = {
    require_(firstFn_.isEmpty, "'first { (in) => ... }' must be called at most once!")
    firstFn_ = Some(fn)
  }

  /** Sets the logic to determine whether a token ends the fold group. Must be called exactly once.
    *
    * Example:
    * {{{
    * last { in => in.isLast }
    * }}}
    */
  protected final def last(fn: => LastFn): Unit = {
    require_(lastFn_.isEmpty, "'last { (in) => ... }' must be called at most once!")
    lastFn_ = Some(fn)
  }

  /** Optionally sets the logic to determine whether a token is a zero (discarded). Useful for
    * sparse folds.
    *
    * Example:
    * {{{
    * zero { in => in.isZero }
    * }}}
    */
  protected final def zero(fn: => ZeroFn): Unit = {
    require_(zeroFn_.isEmpty, "'zero { (in) => ... }' must be called at most once!")
    zeroFn_ = Some(fn)
  }

  deferred {
    require_(operandFn_.nonEmpty, "Missing required 'op' function!")
    require_(lastFn_.nonEmpty, "Missing required 'last' function!")

    val genStage0 = new Bundle {
      val operand = gen.cloneType

      val first = Bool()
      val last = Bool()
      val zero = zeroFn_.map(_ => Bool())
    }

    val genStage1 = new Bundle {
      val operand = gen.cloneType

      val zero = zeroFn_.map(_ => Bool())
    }

    val stage0 = Wire(Interface(genStage0))
    val stage0_init = Wire(Interface(gen))
    val stage0_result = Wire(Interface(gen))

    val stage1_opA = Wire(Interface(gen))
    val stage1_opB = Wire(Interface(genStage1))
    val stage1_result = Wire(Interface(gen))

    prefix("stage0") {
      if (firstFn_.isEmpty) {
        val transducerFirstLogic = new Transducer(source, stage0) {
          val state = RegInit(true.B)

          val last = lastFn_.get(in)

          out.operand := operandFn_.get(in)
          out.first := state
          out.last := last

          if (zeroFn_.nonEmpty)
            out.zero.get := zeroFn_.get(in)

          packet {
            when(state) {
              when(last) {
                accept {}
              }.otherwise {
                accept { state := false.B }
              }
            }.otherwise {
              accept { state := last }
            }
          }
        }
      } else {
        val transform0 = new Transform(source, stage0) {
          out.operand := operandFn_.get(in)
          out.first := firstFn_.get(in)
          out.last := lastFn_.get(in)

          if (zeroFn_.nonEmpty)
            out.zero.get := zeroFn_.get(in)
        }
      }

      sourceInit :=> stage0_init
      stage0_result :=> sink
    }

    prefix("stage1") {
      // stage1 implements the reduce logic

      // sink buffer is used to break the combinational loops
      // as a result, sink buffer introduces a single cycle delay
      // this delay might cause stalls.
      // therefore, fork() buffers are used to avoid stalls

      val fork0 = new Fork(stage0) {
        val transform0 = new Transform(
          SourceBuffer(fork(), flow = true),
          stage1_opB
        ) {
          out.operand := in.operand

          if (zeroFn_.nonEmpty)
            out.zero.get := in.zero.get
        }

        val temp = Wire(Interface(gen))

        val mux0 = EMux(
          Seq(temp, stage0_init),
          stage1_opA,
          SourceBuffer(fork { in.first }, flow = true)
        )

        val demux0 =
          Demux(
            stage1_result,
            Seq(SinkBuffer(temp), stage0_result),
            SourceBuffer(fork { in.last }, flow = true)
          )
      }
    }

    prefix("stage2") {
      // stage2 implements the zero logic

      if (zeroFn_.nonEmpty) {
        val fork0 = new Fork(stage1_opB) {
          val disposed = Wire(Interface(gen))
          val temp = Wire(Interface(gen))

          val demux0 = Demux(
            fork { in.operand },
            Seq(sinkA, disposed),
            fork { in.zero.get }
          )

          val demux1 = Demux(
            stage1_opA,
            Seq(sinkB, temp),
            fork { in.zero.get }
          )

          val mux0 = EMux(
            Seq(sourceResult, temp),
            stage1_result,
            fork { in.zero.get }
          )

          disposed.deq()
          disposed.markSource()

        }
      } else {
        val transform0 = new Transform(stage1_opB, sinkA) {
          out := in.operand
        }

        stage1_opA :=> sinkB
        sourceResult :=> stage1_result
      }
    }
  }
}
