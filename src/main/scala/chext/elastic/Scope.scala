package chext.elastic

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred

import chext.tracking.{Component, withComponent}

class Scope[Tinit <: Data, Texit <: Data](
    val sourceInit: Interface[Tinit],
    val sinkExit: Interface[Texit]
)(implicit si_ : SourceInfo)
    extends Component {
  def tpe: String = "Scope"
  val sourceInfo: SourceInfo = si_
  def namePrefix: String = "scope"

  private val elasticState = trackingState(tracking.Tag)
  elasticState.addSource("sourceInit", sourceInit, boundary = true)
  elasticState.addSink("sinkExit", sinkExit, boundary = true)

  final val sinkBegin = EWire.like(sourceInit)
  final val sourceEnd = EWire.like(sinkExit)

  elasticState.addSink("sinkBegin", sinkBegin, boundary = true)
  elasticState.addSource("sourceEnd", sourceEnd, boundary = true)

  type InitFn = (Tinit) => Unit
  type ExitFn = (Texit) => Unit

  private var initFn_ = Option.empty[InitFn]
  private var exitFn_ = Option.empty[ExitFn]

  private val require_ = chext.util.Require.inferred(sourceInfo)

  protected final def init(fn: => InitFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(
      initFn_.isEmpty,
      "'init { (in) => ... }' must be called at most once!"
    )
    initFn_ = Some(fn)
  }

  protected final def exit(fn: => ExitFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(
      exitFn_.isEmpty,
      "'exit { (in) => ... }' must be called at most once!"
    )
    exitFn_ = Some(fn)
  }

  deferred {
    withComponent(this) {
      val stall = RegInit(false.B)

      val stall0 = new Stall(sourceInit, SinkBuffer(sinkBegin)) {
        out := in

        cond { stall }
        fire {
          stall := true.B
          initFn_.foreach { _(in) }
        }
      }

      val connect0 = new Connect(sourceEnd, SinkBuffer(sinkExit)) {
        fire {
          stall := false.B
          exitFn_.foreach { _(in) }
        }
      }
    }
  }
}
