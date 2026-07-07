package chext.elastic

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred

import chext.tracking.{Container, withContainer}

class Loop[Tstate <: Data](
    sourceInit: Interface[Tstate],
    sinkExit: Interface[Tstate]
)(implicit si_ : SourceInfo)
    extends Container {
  def tpe: String = "Loop"
  val sourceInfo: SourceInfo = si_
  def namePrefix: String = "loop"

  private val genState = chiselTypeOf(sourceInit.$bits)

  final val sinkCurrent = EWire(genState)
  final val sourceNext = EWire(genState)

  type EndFn = (Tstate) => Bool

  private var endFn_ = Option.empty[EndFn]

  private val require_ = chext.util.Require.inferred(sourceInfo)

  /** Declares a combinational function that maps the state to whether its
    * complete or not.
    *
    * @param fn
    */
  protected final def end(fn: => EndFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(
      endFn_.isEmpty,
      "'end { (state) => ... }' must be called at most once!"
    )
    endFn_ = Some(fn)
  }

  deferred {
    withContainer(this) {
      require_(
        endFn_.nonEmpty,
        "'end { (state) => ... }' must be called at least once!"
      )
      val endFn = endFn_.get

      val stall = RegInit(false.B)

      val sourceInit_ = EWire(genState)
      val sinkExit_ = EWire(genState)

      val stall0 = new Stall(sourceInit, sourceInit_) {
        out := in

        cond { stall }
        fire { stall := true.B }
      }

      val connect0 = new Connect(sinkExit_, sinkExit) {
        fire { stall := false.B }
      }

      val ewire0 = EWire(genState)
      val ewire1 = EWire(genState)
      val ewire2 = EWire(genState)
      val ewire3 = EWire(genState)

      val fork0 = new Fork(sourceInit_) {
        val demux0 = new Demux(
          fork(),
          Seq(ewire0, ewire1),
          fork { endFn(in).asUInt }
        )
      }

      val fork1 = new Fork(sourceNext) {
        val demux1 = new Demux(
          fork(),
          Seq(SinkBuffer(ewire2), ewire3),
          fork { endFn(in).asUInt }
        )
      }

      val merger0 = Merger(Seq(ewire0, ewire2), sinkCurrent)
      val merger1 = Merger(Seq(ewire1, ewire3), sinkExit_)
    }
  }
}
