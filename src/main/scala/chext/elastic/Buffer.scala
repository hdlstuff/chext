package chext.elastic

import chisel3._
import chisel3.experimental.SourceInfo

import chext.tracking.uniquePrefix

package detail {
  trait LeftBuffer_ {
    val defaultName: String = "leftBuffer"

    def apply[T <: Data](
        source: Interface[T],
        count: Int = 2,
        flow: Boolean = false,
        pipe: Boolean = false,
        name: String = ""
    )(implicit si: SourceInfo): Interface[T] = {
      val prefixName = if (name.nonEmpty) name else defaultName

      uniquePrefix(prefixName) {
        val interface = EWire.like(source)
        Queue.between(source, interface, count, flow, pipe, false)

        interface
      }
    }
  }

  trait RightBuffer_ {
    val defaultName: String = "leftBuffer"

    def apply[T <: Data](
        sink: Interface[T],
        count: Int = 2,
        flow: Boolean = false,
        pipe: Boolean = false,
        name: String = ""
    )(implicit si: SourceInfo): Interface[T] = {
      val prefixName = if (name.nonEmpty) name else defaultName

      uniquePrefix(prefixName) {
        val interface = EWire.like(sink)
        Queue.between(interface, sink, count, flow, pipe, false)

        interface
      }
    }
  }
}

object LeftBuffer extends detail.LeftBuffer_

object SourceBuffer extends detail.LeftBuffer_ {
  override val defaultName: String = "sourceBuffer"
}

object RightBuffer extends detail.RightBuffer_

object SinkBuffer extends detail.RightBuffer_ {
  override val defaultName: String = "sinkBuffer"
}
