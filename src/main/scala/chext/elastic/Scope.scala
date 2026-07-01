package chext.elastic

import chisel3._

import chisel3.hacks.deferred

import chisel3.experimental.SourceInfo

import chext.tracking
import tracking.Container
import tracking.withContainer

class Scope[Tinit <: Data, Texit <: Data](
    sourceInit: Interface[Tinit],
    sinkExit: Interface[Texit]
)(implicit si_ : SourceInfo)
    extends Container {
  def tpe: String = "Scope"
  val sourceInfo: SourceInfo = si_
  def namePrefix: String = "scope"

  final val sinkBegin = EWire.like(sourceInit)
  final val sourceEnd = EWire.like(sinkExit)

  type InitFn = (Tinit) => Unit
  type ExitFn = (Texit) => Unit

  private var initFn_ = Option.empty[InitFn]
  private var exitFn_ = Option.empty[ExitFn]

  private val require_ = chext.util.Require(s"chext.elastic.$tpe", Some(sourceInfo))

  protected final def init(fn: => InitFn): Unit = {
    require_(
      initFn_.isEmpty,
      "'init { (in) => ... }' must be called at most once!"
    )
    initFn_ = Some(fn)
  }

  protected final def exit(fn: => ExitFn): Unit = {
    require_(
      exitFn_.isEmpty,
      "'exit { (in) => ... }' must be called at most once!"
    )
    exitFn_ = Some(fn)
  }

  deferred {
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
