package chext.elastic

import chisel3._
import chisel3.experimental.SourceInfo

import chisel3.hacks.deferred

import chext.tracking
import tracking.Container
import tracking.withContainer

/** `Repeat` replicates each input token multiple times at the output.
  *
  * The number of repetitions is determined dynamically using a user-defined `len` function. Each
  * output instance is generated using a user-defined `out` function, which is passed the input,
  * current repeat index, and cycle flags (`first`, `last`).
  *
  * Internally, `Repeat` is implemented as a specialization of `Count`, where the count range is
  * defined by the result of `len(in)`, starting from 0 up to (but not including) `len(in)`.
  *
  * Usage:
  * {{{
  * len { in => in.rep }
  * out { (in, index, first, last) => in.data }
  * }}}
  *
  * @param source
  *   The input elastic interface. Must provide `valid` and `bits`, and will be driven with `ready`.
  * @param sink
  *   The output elastic interface. Must provide `ready` and `bits`, and will be driven with
  *   `valid`.
  * @param wIndex
  *   Bit width of the internal repeat counter.
  *
  * @tparam Tin
  *   The type of the source data (`source.$bits`).
  * @tparam Tout
  *   The type of the sink data (`sink.$bits`).
  */
abstract class Repeat[Tin <: Data, Tout <: Data](
    source: Interface[Tin],
    sink: Interface[Tout],
    wIndex: Int
)(implicit si_ : SourceInfo)
    extends Container
    with Fire[Tout] {
  protected def fireSink: Interface[Tout] = sink

  val sourceInfo: SourceInfo = si_
  def tpe: String = "Repeat"
  def namePrefix: String = "repeat"

  type LenFn = (Tin) => UInt
  type OutFn = (Tin, UInt, Bool, Bool) => Tout
  type OutExplicitFn = (Tin, UInt, Bool, Bool, Tout) => Unit

  private var lenFn_ = Option.empty[LenFn]
  private var outFn_ = Option.empty[OutFn]

  private val require_ = chext.util.Require(s"chext.elastic.$tpe", Some(sourceInfo))

  protected final def in: Tin = require_.fail("in field shall not be used!")

  /** Sets the pure functional length function. Takes the input token and returns how many times it
    * should be repeated. Must be called exactly once.
    *
    * Example:
    * {{{
    * len { (in) => in.len }
    * }}}
    */
  protected final def len(fn: => LenFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(lenFn_.isEmpty, "'len { (in) => ... }' must be called at most once!")
    lenFn_ = Some(fn)
  }

  /** Sets the pure functional output generation function. Takes the input, the current repeat
    * index, and first/last flags. Must be called exactly once.
    *
    * Example:
    * {{{
    * out { (in, index, first, last) => in }
    * }}}
    */
  protected final def out(fn: => OutFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(
      outFn_.isEmpty,
      "'out { (in, index, first, last) => ... }' must be called at most once!"
    )
    outFn_ = Some(fn)
  }

  /** Sets the imperative output generation function. Must be called at most once.
    *
    * Example:
    * {{{
    * outExplicit { (in, index, first, last, out) => out := in }
    * }}}
    */
  protected final def outExplicit(fn: => OutExplicitFn)(implicit si_ : SourceInfo): Unit = {
    out { (in, index, first, last) =>
      {
        val outResult = Wire(chiselTypeOf(sink.$bits))
        fn(in, index, first, last, outResult)
        outResult
      }
    }
  }

  deferred {
    require_(lenFn_.nonEmpty, "'len { (in) => ... }' must be called at least once!")
    require_(
      outFn_.nonEmpty,
      "'out { (in, index, first, last) => ... }' must be called at least once!"
    )

    val lenFn = lenFn_.get
    val outFn = outFn_.get

    withContainer(this) {
      val count = new Count(source, sink, UInt(wIndex.W)) { count =>
        count.init { (_) => 0.U }

        count.cond { (in, state) => state =/= lenFn(in) }

        count.next { (_, state) => state + 1.U }

        count.out { (in, state, first, last) => outFn(in, state, first, last) }
      }
    }
  }
}
