package chext.elastic

import chext.elastic.{tracking => t}

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred

import chext.tracking.Component

abstract class Count[Tstate <: Data, Tin <: Data, Tout <: Data](
    val source: Interface[Tin],
    val sink: Interface[Tout],
    val genState: Tstate
)(implicit si_ : SourceInfo)
    extends Component
    with Fire[Tout] {
  protected def fireSink: Interface[Tout] = sink

  private val elasticState = trackingState(t.Tag)
  import elasticState.{addSource, addSink}

  addSource("source", source)
  addSink("sink", sink)

  val sourceInfo: SourceInfo = si_
  def tpe: String = "Count"
  def namePrefix: String = "count"

  type InitFn = (Tin) => Tstate
  type InitExplicitFn = (Tin, Tstate) => Unit
  type CondFn = (Tin, Tstate) => Bool
  type NextFn = (Tin, Tstate) => Tstate
  type NextExplicitFn = (Tin, Tstate, Tstate) => Unit
  type OutFn = (Tin, Tstate, Bool, Bool) => Tout
  type OutExplicitFn = (Tin, Tstate, Bool, Bool, Tout) => Unit

  private var initFn_ = Option.empty[InitFn]
  private var condFn_ = Option.empty[CondFn]
  private var nextFn_ = Option.empty[NextFn]
  private var outFn_ = Option.empty[OutFn]

  private val require_ = chext.util.Require.inferred(sourceInfo)

  protected final def in: Tin = require_.fail("`in` field shall not be used!")

  // format: off

  /** 
   * Sets the pure functional "init" state initialization function.
   * The function takes the current input and returns the initial state.
   * Must be called exactly once.
   * 
   * Example:
   * {{{
   * init { (in) => 0.U }
   * }}}
   */
  protected final def init(fn: => InitFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(initFn_.isEmpty, "'init { (in) => ... }' must be called at most once!")
    initFn_ = Some(fn)
  }

  /** 
   * Sets the imperative "init" state initialization function.
   * The function takes the current input and a mutable state register to assign.
   * Must be called exactly once.
   * 
   * Example:
   * {{{
   * initExplicit { (in, state) => state := in }
   * }}}
   */
  protected final def initExplicit(fn: => InitExplicitFn)(implicit si_ : SourceInfo): Unit = {
    init { //
      (in) => { //
        val initResult = Wire(genState)
        fn(in, initResult)
        initResult
      }
    }
  }

  /**
   * Sets the condition function that determines whether the state is valid for generating an output token.
   * The function takes the current input and state and returns a Bool indicating if the counting should continue.
   * Must be called at most once.
   * 
   * Example:
   * {{{
   * cond { (in, state) => state =/= 0.U }
   * }}}
   */
  protected final def cond(fn: => CondFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(
      condFn_.isEmpty,
      "'cond { (in, state) => ... }' must be called at most once!"
    )
    condFn_ = Some(fn)
  }

  /** 
   * Sets the pure functional "next" state transition function.
   * The function takes the current input and state and returns the next state.
   * Must be called at most once.
   * 
   * Example:
   * {{{
   * next { (in, state) => state - 1.U }
   * }}}
   */
  protected final def next(fn: => NextFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(
      nextFn_.isEmpty,
      "'next { (in, state) => ... }' must be called at most once!"
    )
    nextFn_ = Some(fn)
  }

  /** 
   * Sets the imperative "next" state update function.
   * The function takes the current input, the current state, and a mutable state register to update.
   * Must be called at most once.
   * 
   * Example:
   * {{{
   * nextExplicit { (in, state, stateNext) => stateNext := state - 1.U }
   * }}}
   */
  protected final def nextExplicit(fn: => NextExplicitFn)(implicit si_ : SourceInfo): Unit = {
    next {
      (in, state) => {
        val nextResult = Wire(genState)
        fn(in, state, nextResult)
        nextResult
      }
    }
  }

  /** 
   * Sets the pure functional output generation function.
   * The function takes the current input, state, and flags indicating first and last cycles, and returns the output.
   * Must be called at most once.
   * 
   * Example:
   * {{{
   * out { (in, state, first, last) => state }
   * }}}
   */
  protected final def out(fn: => OutFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(
      outFn_.isEmpty,
      "'out { (in, state, first, last) => ... }' must be called at most once!"
    )
    outFn_ = Some(fn)
  }

  /** 
   * Sets the imperative output generation function.
   * The function takes the current input, state, flags for first and last cycles, and a mutable output register to assign.
   * Must be called at most once.
   * 
   * Example:
   * {{{
   * outExplicit { (in, state, first, last, out) => out := ... }
   * }}}
   */
  protected final def outExplicit(fn: => OutExplicitFn)(implicit si_ : SourceInfo): Unit = {
    out {
      (in, state, first, last) => {
        val outResult = Wire(chiselTypeOf(sink.$bits))
        fn(in, state, first, last, outResult)
        outResult
      }
    }
  }

  // format: on

  deferred {
    // format: off
    require_(initFn_.nonEmpty, "'init { (in) => ... }' must be called at least once!")
    require_(condFn_.nonEmpty, "'cond { (in, state) => ... }' must be called at least once!")
    require_(nextFn_.nonEmpty, "'next { (in, state) => ... }' must be called at least once!")
    require_(outFn_.nonEmpty, "'out { (in, state, first, last) => ... }' must be called at least once!")
    // format: on

    val initFn = initFn_.get
    val nextFn = nextFn_.get
    val condFn = condFn_.get
    val outFn = outFn_.get

    val state = Reg(genState)
    val valid = RegInit(false.B)

    val in = source.$bits
    val out = sink.$bits

    source.nodeq()
    sink.noenq()

    when(source.$valid) {
      when(valid) {
        val nextState = nextFn(in, state)

        when(!condFn(in, nextState)) {
          sink.enq(outFn(in, state, false.B, true.B))

          when(sink.$ready) {
            // update the state only if the sink packet could be generated
            valid := false.B
            state := 0.U.asTypeOf(state)

            // deq source only if the sink packet could be generated
            source.deq()
          }
        }.otherwise {
          sink.enq(outFn(in, state, false.B, false.B))

          when(sink.$ready) {
            // update the state only if the sink packet could be generated
            state := nextState
          }
        }
      }.otherwise {
        val initState = initFn(in)
        val nextState = nextFn(in, initState)

        when(!condFn(in, initState)) {
          // do nothing, do not generate anything
          source.deq()

        }.elsewhen(!condFn(in, nextState)) {
          // generate something
          // sink.enq must not depend on sink.$ready to satisfy the elastic protocol
          sink.enq(outFn(in, initState, true.B, true.B))

          when(sink.$ready) {
            // deq source only if the sink packet could be generated
            source.deq()
          }
        }.otherwise {
          sink.enq(outFn(in, initState, true.B, false.B))

          when(sink.$ready) {
            // update the state only if the sink packet could be generated
            state := nextState
            valid := true.B
          }
        }
      }
    }
  }
}
