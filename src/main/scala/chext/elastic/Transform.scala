package chext.elastic

import chext.elastic.{tracking => t}

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred

import chext.deadlock
import chext.tracking.Component

abstract class Transform[Tin <: Data, Tout <: Data](
    source: Interface[Tin],
    sink: Interface[Tout]
)(implicit si_ : SourceInfo)
    extends Component
    with Fire[Tout] {
  protected def fireSink: Interface[Tout] = sink

  private val elasticState = trackingState(t.Tag)
  import elasticState._

  addSourcePort("source", source)
  addSinkPort("sink", sink)

  val sourceInfo: SourceInfo = si_
  def tpe: String = "Transform"
  def namePrefix: String = "transform"

  protected final val in = source.$bits
  protected final val out = sink.$bits

  deferred {
    sink.$valid := source.$valid
    source.$ready := sink.$ready

    val monitor0 = new deadlock.Monitor(this) {
      source.waitValid := true.B
    }
  }
}
