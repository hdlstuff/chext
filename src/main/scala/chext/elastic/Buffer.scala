package chext.elastic

import chisel3._
import chisel3.experimental.{SourceInfo, prefix}

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
        val queue0 = new Queue(
          source,
          queueSink,
          count,
          pipe = pipe,
          flow = flow,
          useSyncReadMem = false
        )

        queueSink
      }
    }

    def apply[T <: Data](
        sources: Seq[Interface[T]],
        count: Int,
        flow: Boolean,
        pipe: Boolean,
        name: String
    )(implicit si: SourceInfo): Seq[Interface[T]] = {
      sources.zipWithIndex.map { case (source, index) =>
        prefix(index.toString) {
          apply(source, count, flow, pipe, name)
        }
      }
    }

    def apply[T <: Data](
        sources: Seq[Interface[T]],
        count: Int,
        flow: Boolean,
        pipe: Boolean
    )(implicit si: SourceInfo): Seq[Interface[T]] =
      apply(sources, count, flow, pipe, "")

    def apply[T <: Data](
        sources: Seq[Interface[T]],
        count: Int
    )(implicit si: SourceInfo): Seq[Interface[T]] =
      apply(sources, count, false, false, "")

    def apply[T <: Data](
        sources: Seq[Interface[T]]
    )(implicit si: SourceInfo): Seq[Interface[T]] =
      apply(sources, 2, false, false, "")
  }

  trait SourceBuffered_ {
    def apply[T <: Data](
        source: Interface[T],
        count: Int = 2,
        flow: Boolean = false,
        pipe: Boolean = false
    )(implicit si: SourceInfo): Interface[T] = {
      val queueSink = EWire.like(source)
      val queue0 = new Queue(
        source,
        queueSink,
        count,
        pipe = pipe,
        flow = flow,
        useSyncReadMem = false
      )

      queueSink
    }

    def apply[T <: Data](
        sources: Seq[Interface[T]],
        count: Int,
        flow: Boolean,
        pipe: Boolean
    )(implicit si: SourceInfo): Seq[Interface[T]] = {
      sources.zipWithIndex.map { case (source, index) =>
        prefix(index.toString) {
          apply(source, count, flow, pipe)
        }
      }
    }

    def apply[T <: Data](
        sources: Seq[Interface[T]],
        count: Int
    )(implicit si: SourceInfo): Seq[Interface[T]] =
      apply(sources, count, false, false)

    def apply[T <: Data](
        sources: Seq[Interface[T]]
    )(implicit si: SourceInfo): Seq[Interface[T]] =
      apply(sources, 2, false, false)
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
        val queue0 = new Queue(
          queueSource,
          sink,
          count,
          pipe = pipe,
          flow = flow,
          useSyncReadMem = false
        )

        queueSource
      }
    }

    def apply[T <: Data](
        sinks: Seq[Interface[T]],
        count: Int,
        flow: Boolean,
        pipe: Boolean,
        name: String
    )(implicit si: SourceInfo): Seq[Interface[T]] = {
      sinks.zipWithIndex.map { case (sink, index) =>
        prefix(index.toString) {
          apply(sink, count, flow, pipe, name)
        }
      }
    }

    def apply[T <: Data](
        sinks: Seq[Interface[T]],
        count: Int,
        flow: Boolean,
        pipe: Boolean
    )(implicit si: SourceInfo): Seq[Interface[T]] =
      apply(sinks, count, flow, pipe, "")

    def apply[T <: Data](
        sinks: Seq[Interface[T]],
        count: Int
    )(implicit si: SourceInfo): Seq[Interface[T]] =
      apply(sinks, count, false, false, "")

    def apply[T <: Data](
        sinks: Seq[Interface[T]]
    )(implicit si: SourceInfo): Seq[Interface[T]] =
      apply(sinks, 2, false, false, "")
  }

  trait SinkBuffered_ {
    def apply[T <: Data](
        sink: Interface[T],
        count: Int = 2,
        flow: Boolean = false,
        pipe: Boolean = false
    )(implicit si: SourceInfo): Interface[T] = {
      val queueSource = EWire.like(sink)
      val queue0 = new Queue(
        queueSource,
        sink,
        count,
        pipe = pipe,
        flow = flow,
        useSyncReadMem = false
      )

      queueSource
    }

    def apply[T <: Data](
        sinks: Seq[Interface[T]],
        count: Int,
        flow: Boolean,
        pipe: Boolean
    )(implicit si: SourceInfo): Seq[Interface[T]] = {
      sinks.zipWithIndex.map { case (sink, index) =>
        prefix(index.toString) {
          apply(sink, count, flow, pipe)
        }
      }
    }

    def apply[T <: Data](
        sinks: Seq[Interface[T]],
        count: Int
    )(implicit si: SourceInfo): Seq[Interface[T]] =
      apply(sinks, count, false, false)

    def apply[T <: Data](
        sinks: Seq[Interface[T]]
    )(implicit si: SourceInfo): Seq[Interface[T]] =
      apply(sinks, 2, false, false)
  }
}

object LeftBuffer extends detail.LeftBuffer_

object SourceBuffer extends detail.LeftBuffer_ {
  protected override val defaultName: String = "sourceBuffer"
}

object SourceBuffered extends detail.SourceBuffered_

object RightBuffer extends detail.RightBuffer_

object SinkBuffer extends detail.RightBuffer_ {
  protected override val defaultName: String = "sinkBuffer"
}

object SinkBuffered extends detail.SinkBuffered_
