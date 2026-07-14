package chext.deadlock

import chisel3._

import chext.{elastic => e}

import chext.tracking.Component
import chext.elastic.tracking.Tag

import scala.collection.mutable.HashMap
import chisel3.hacks.deferred

@scala.annotation.nowarn("cat=deprecation")
// TODO Replace with ExtModule
private class chext_deadlock_monitor(
    val sourceCount: Int,
    val sinkCount: Int
) extends BlackBox( // TODO Replace with ExtModule
      Map(
        "SOURCE_COUNT" -> sourceCount,
        "SINK_COUNT" -> sinkCount
      )
    ) {

  val io = IO(new Bundle {
    val source_valid = Input(UInt(sourceCount.W))
    val source_waitValid = Input(UInt(sourceCount.W))

    val sink_ready = Input(UInt(sinkCount.W))
    val sink_waitReady = Input(UInt(sinkCount.W))
  })
}

object Monitor {
  private var enabled_ = false

  def enable(enabled: Boolean): Unit = {
    enabled_ = enabled
  }

  def enabled: Boolean = enabled_
}

class Monitor(val component: Component) {
  private val require_ = chext.util.Require.inferred()

  private val elasticState = component.trackingState(Tag)
  private val sourceInterfaces = elasticState.sources.map { port => port.name -> port.interface }
  private val sinkInterfaces = elasticState.sinks.map { port => port.name -> port.interface }

  private val sourceInterfaceSet =
    sourceInterfaces
      .map[Tuple2[e.Interface[_], String]] { (x) => (x._2 -> x._1) }
      .toMap

  private val sinkInterfaceSet =
    sinkInterfaces
      .map[Tuple2[e.Interface[_], String]] { (x) => (x._2 -> x._1) }
      .toMap

  private val waitForValid_ = HashMap.empty[e.Interface[_], Bool]
  private val waitForReady_ = HashMap.empty[e.Interface[_], Bool]

  protected implicit class interface_ops[T <: Data](interface: e.Interface[T]) {
    def waitValid: Bool = {
      require_(
        sourceInterfaceSet.contains(interface),
        "unknown source interface registered for deadlock detection"
      )

      waitForValid_.getOrElseUpdate(interface, Wire(Bool()))
    }

    def waitReady: Bool = {
      require_(
        sinkInterfaceSet.contains(interface),
        "unknown sink interface registered for deadlock detection"
      )

      waitForReady_.getOrElseUpdate(interface, Wire(Bool()))
    }
  }

  deferred {
    require_(
      elasticState.sources.forall(!_.boundary) && elasticState.sinks.forall(!_.boundary),
      "A deadlock monitor must not be attached to a component with boundary interfaces."
    )

    if (Monitor.enabled) {
      val chext_dlm = Module(
        new chext_deadlock_monitor(
          Math.max(sourceInterfaces.length, 1),
          Math.max(sinkInterfaces.length, 1)
        )
      )

      if (sourceInterfaces.nonEmpty) {
        chext_dlm.io.source_valid := VecInit(
          sourceInterfaces.map { //
            case (name, interface) => interface.$valid
          }
        ).asUInt

        chext_dlm.io.source_waitValid := VecInit(
          sourceInterfaces.map {
            // if not specified, always wait for a valid
            case (name, interface) =>
              waitForValid_.getOrElse(interface, true.B)
          }
        ).asUInt
      } else {
        chext_dlm.io.source_valid := 0.U
        chext_dlm.io.source_waitValid := 0.U
      }

      if (sinkInterfaces.nonEmpty) {
        chext_dlm.io.sink_ready := VecInit(
          sinkInterfaces.map { //
            case (name, interface) => interface.$ready
          }
        ).asUInt

        chext_dlm.io.sink_waitReady := VecInit(
          sinkInterfaces.map {
            // if not specified, wait for a ready only if valid
            case (name, interface) =>
              waitForReady_.getOrElse(interface, interface.$valid)
          }
        ).asUInt
      } else {
        chext_dlm.io.sink_ready := 0.U
        chext_dlm.io.sink_waitReady := 0.U
      }
    }
  }
}
