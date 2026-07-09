package chext.elastic

import chisel3._
import chisel3.experimental.{SourceInfo, prefix}
import chisel3.hacks.deferred
import chisel3.util._

import chext.elastic
import chext.elastic.ConnectOp._
import chext.tracking.{Container, withContainer}

/** `Switch` conditionally routes tokens to one of multiple branches.
  *
  * It examines each input token using user-defined `condFn` predicates and forwards it to the first
  * matching branch. Each branch has its own subgraph defined via `branch` or `namedBranch`.
  *
  * Internally, `Switch` uses a demultiplexer to fork the token into branches and a multiplexer to
  * recombine their outputs. An optional `last` function can be used to signal the end of a branch.
  *
  * @param source
  *   The upstream interface providing tokens.
  * @param sink
  *   The downstream interface receiving the selected and transformed token.
  * @param numOutstanding
  *   Optional max number of in-flight tokens. Defaults to number of branches.
  * @tparam Tin
  *   Input token type.
  * @tparam Tout
  *   Output token type.
  *
  * @example
  *   {{{
  *   val switch = new Switch(source, sink) {
  *     last { _.isLast }
  *
  *     namedBranch("zero") { _.opcode === 0.U } { (source, sink) =>
  *       val t = new Transform(source, sink) { out := in }
  *     }
  *
  *     namedBranch("one") { _.opcode === 1.U } { (source, sink) =>
  *       val t = new Transform(source, sink) { out := in }
  *     }
  *   }
  *   }}}
  */
abstract class Switch[Tin <: Data, Tout <: Data](
    val source: elastic.Interface[Tin],
    val sink: elastic.Interface[Tout],
    val numOutstanding: Int = -1
)(implicit si_ : SourceInfo)
    extends Container
    with Fire[Tout] {
  protected def fireSink: Interface[Tout] = sink

  val sourceInfo: SourceInfo = si_
  def tpe: String = "Switch"
  def namePrefix: String = "switch"

  private val require_ = chext.util.Require.inferred(sourceInfo)

  private val genIn = chiselTypeOf(source.$bits)
  private val genOut = chiselTypeOf(sink.$bits)

  type LastFn = (Tout) => Bool
  type CondFn = (Tin) => Bool
  type BranchFn = (elastic.Interface[Tin], elastic.Interface[Tout]) => Unit

  private var lastFn_ = Option.empty[LastFn]

  private case class Branch(
      condFn: CondFn,
      branchFn: BranchFn,
      name: String,
      sourceInfo: SourceInfo
  )

  private val usedNames = scala.collection.mutable.HashSet.empty[String]
  private val branchBuffer = scala.collection.mutable.ArrayBuffer.empty[Branch]

  /** Sets the function that determines the last token for the selected branch.
    *
    * This is used to tell the switch when a branch is complete.
    *
    * Must be called at most once.
    *
    * @example
   *   last { _.isLast }
    */
  protected final def last(fn: => LastFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(
      lastFn_.isEmpty,
      "'last { (out) => ... }' must be called at most once!"
    )
    lastFn_ = Some(fn)
  }

  /** Adds a conditional unnamed branch to the switch.
    *
    * The branch is activated when `condFn` returns true. Tokens are routed to this branch and used
    * by `branchFn`.
    *
    * @example
    *   {{{
    *   branch { _.opcode === 0.U } { (source, sink) =>
    *     val transform = new Transform(source, sink) {
    *       out := in
    *     }
    *   }
    *   }}}
    */
  protected final def branch
    // format: off
    (condFn: CondFn)
    (branchFn: BranchFn)
    (implicit sourceInfo: SourceInfo)
    // format: on
      : Unit = {
    val name = f"b${branchBuffer.length}"
    require_.here(
      !usedNames.contains(name),
      s"Branch name '$name' is already used!"
    )(sourceInfo)
    usedNames.addOne(name)
    branchBuffer.addOne(Branch(condFn, branchFn, name, sourceInfo))
  }

  /** Adds a conditional named branch to the switch.
    *
    * @param name
    *   A unique name identifying the branch (used for prefixing).
    *
    * @throws java.lang.IllegalArgumentException
    *   if the name was already used for another branch.
    *
    * @example
    *   {{{
    *   namedBranch("add") { _.opcode === 0.U } { (source, sink) =>
    *     val transform = new Transform(source, sink) {
    *       out := in
    *     }
    *   }
    *   }}}
    */
  protected final def namedBranch
    // format: off
    (name: String)
    (condFn: CondFn)
    (branchFn: BranchFn)
    (implicit sourceInfo: SourceInfo)
    // format: on
      : Unit = {
    require_.here(
      !usedNames.contains(name),
      s"Branch name '$name' is already used!"
    )(sourceInfo)
    usedNames.addOne(name)
    branchBuffer.addOne(Branch(condFn, branchFn, name, sourceInfo))
  }

  deferred {
    withContainer(this) {
      val branches = branchBuffer.toSeq

      val wireRvDemuxN = Wire(Vec(branches.length, elastic.Interface(genIn)))
      val wireRvMuxN = Wire(Vec(branches.length, elastic.Interface(genOut)))

      val queueIndex = elastic.Queue(
        UInt(log2Ceil(branches.length).W),
        if (numOutstanding > 0) numOutstanding
        else branches.length
      )

      val fork0 = new elastic.Fork(source) {
        val select = PriorityEncoder(branches.map(_.condFn(in)))
        val demux0 = new elastic.Demux(fork(), wireRvDemuxN, fork { select })
        fork { select } :=> queueIndex.source
      }

      wireRvDemuxN.zip(wireRvMuxN).zip(branches).zipWithIndex.foreach { //
        case (((branchSource, branchSink), branch), index) => {
          prefix(branch.name) {
            branch.branchFn(branchSource, branchSink)
          }
        }
      }

      val mux0 = new elastic.Mux(wireRvMuxN, sink, queueIndex.sink) {
        this.last { lastFn_.getOrElse((x: Tout) => true.B) }
      }
    }
  }
}
