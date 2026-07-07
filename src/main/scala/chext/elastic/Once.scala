package chext.elastic

import chext.elastic.{tracking => t}

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.experimental.requireIsHardware
import chisel3.hacks.deferred

import chext.deadlock
import chext.tracking.Component

/** Sends an elastic packet only once.
  *
  * The first sink handshake emits the configured payload and all later cycles
  * hold the sink invalid.
  *
  * Inherit from this class and set `out := value`.
  *
  * @param sink
  *   elastic sink interface that receives the one-shot value
  */
class Once[T <: Data](val sink: Interface[T])(implicit si_ : SourceInfo)
    extends Component
    with Fire[T] {
  protected def fireSink: Interface[T] = sink

  override def tpe: String = "Once"
  override def namePrefix: String = "once"
  override val sourceInfo: SourceInfo = si_

  protected val out = sink.$bits

  private val elasticState = trackingState(t.Tag)
  import elasticState._

  addSinkPort("sink", sink)

  deferred {
    val sent = RegInit(false.B)

    sink.$valid := !sent

    when(sink.$ready) {
      sent := true.B
    }

    val monitor0 = new deadlock.Monitor(this)
  }

}

object Once {
  def apply[T <: Data](value: T) = {
    requireIsHardware(value, "Once: value must be hardware, not a Chisel type!")
    val sinkOnce = EWire(chiselTypeOf(value))
    val once0 = new Once(sinkOnce) { out := value }
    sinkOnce
  }
}
