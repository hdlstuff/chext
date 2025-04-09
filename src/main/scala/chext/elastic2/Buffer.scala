package chext.elastic2

import chisel3._
import chisel3.experimental.prefix

package detail {
  trait LeftBuffer_ {
    def apply[T <: Data](
        source: Interface[T],
        count: Int = 2,
        flow: Boolean = false,
        pipe: Boolean = false
    ): Interface[T] = {
      val interface = Wire(chiselTypeOf(source))
      Queue.between(source, interface, count, flow, pipe, false)
      interface
    }
  }

  trait RightBuffer_ {
    def apply[T <: Data](
        sink: Interface[T],
        count: Int = 2,
        flow: Boolean = false,
        pipe: Boolean = false
    ): Interface[T] = {
      val interface = Wire(chiselTypeOf(sink))
      Queue.between(interface, sink, count, flow, pipe, false)
      interface
    }
  }
}

object LeftBuffer extends detail.LeftBuffer_
object SourceBuffer extends detail.LeftBuffer_

object RightBuffer extends detail.RightBuffer_
object SinkBuffer extends detail.RightBuffer_
