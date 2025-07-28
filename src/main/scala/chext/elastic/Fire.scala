package chext.elastic

import chisel3._
import chisel3.experimental.AffectsChiselPrefix
import chisel3.hacks.deferred

import scala.collection.mutable.ArrayBuffer

/** Provides the `fire { ... }` method that is executed when a packet is transferred at the sink
  * interface.
  *
  * @param sink
  */
private[elastic] abstract class Fire[Tout <: Data](sink: Interface[Tout])
    extends AffectsChiselPrefix {
  private var fireFns_ = ArrayBuffer.empty[() => Unit]

  /** Sets an action that is executed when the sink interface fires. Can be called multiple times.
    */
  protected final def fire(fn: => Unit): Unit = {
    fireFns_.addOne(() => fn)
  }

  deferred {
    when(sink.fire) {
      fireFns_.foreach { _() }
    }
  }
}
