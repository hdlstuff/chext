package chext.elastic

import chisel3._
import chisel3.experimental.SourceInfo

import chext.tracking.uniquePrefix

package detail {
  trait LeftBuffer_ {
    protected val defaultName: String = "leftBuffer"

    def apply[T <: Data](
        source: Interface[T],
        count: Int = 2,
        flow: Boolean = false,
        pipe: Boolean = false,
        name: String = ""
    )(implicit si: SourceInfo): Interface[T] = {
      val prefixName = if (name.nonEmpty) name else defaultName

      uniquePrefix(prefixName) {
        val queueSink = EWire.like(source)
        val queue0 = new Queue(source, queueSink, count, flow, pipe, false)

        queueSink
      }
    }
  }

  trait RightBuffer_ {
    protected val defaultName: String = "rightBuffer"

    def apply[T <: Data](
        sink: Interface[T],
        count: Int = 2,
        flow: Boolean = false,
        pipe: Boolean = false,
        name: String = ""
    )(implicit si: SourceInfo): Interface[T] = {
      val prefixName = if (name.nonEmpty) name else defaultName

      uniquePrefix(prefixName) {
        val queueSource = EWire.like(sink)
        val queue0 = new Queue(queueSource, sink, count, flow, pipe, false)

        queueSource
      }
    }
  }
}

object LeftBuffer extends detail.LeftBuffer_

object SourceBuffer extends detail.LeftBuffer_ {
  protected override val defaultName: String = "sourceBuffer"
}

object RightBuffer extends detail.RightBuffer_

object SinkBuffer extends detail.RightBuffer_ {
  protected override val defaultName: String = "sinkBuffer"
}
