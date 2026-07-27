package chext.elastic

import chext.elastic.{tracking => t}

import chisel3._
import chisel3.experimental.{SourceInfo, requireIsChiselType, requireIsHardware}

import chext.tracking.{Component, uniquePrefix}

abstract class Const[Tin <: Data, Tout <: Data](
    val sink: Interface[Tout]
)(implicit si_ : SourceInfo)
    extends Component
    with Fire[Tout] {
  protected def fireSink: Interface[Tout] = sink

  private val elasticState = trackingState(t.Tag)
  import elasticState.addSink

  addSink("sink", sink)

  val sourceInfo: SourceInfo = si_
  def tpe: String = "Const"
  def namePrefix: String = "const"

  protected final val out = sink.$bits

  sink.$valid := true.B
}

object Const {
  def apply[T <: Data](
      constant: T,
      name: String = "const"
  )(implicit si: SourceInfo): Interface[T] = {
    requireIsHardware(constant, "The constant parameter must be a Chisel hardware.")

    uniquePrefix(name) {
      val interface = EWire(chiselTypeOf(constant))

      val const0 = new Const(interface) {
        out := constant
      }

      interface
    }
  }

  def explicit[T <: Data](gen: T, name: String = "const")(fn: => (T) => Unit)(implicit
      si: SourceInfo
  ): Interface[T] = {
    requireIsChiselType(gen, "The gen parameter must be a Chisel type.")

    uniquePrefix(name) {
      val interface = EWire(gen.cloneType)

      val const0 = new Const(interface) {
        fn(out)
      }

      interface
    }
  }

}
