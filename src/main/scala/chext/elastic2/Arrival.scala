package chext.elastic2


import chisel3._
import chisel3.experimental.{AffectsChiselPrefix, prefix}

/** @note
  *   Never declare registers inside `onAccept`!
  *
  * @param source
  * @param sink
  */
abstract class Arrival[Tin <: Data, T <: Data](
    source: Interface[Tin],
    sink: Interface[T],
    count: Int = 2,
    flow: Boolean = false,
    pipe: Boolean = false
) extends AffectsChiselPrefix {
  private val sinkBuffered_ = SinkBuffer(sink, count = count, flow = flow, pipe = pipe)

  protected val in = source.bits
  protected val out = sinkBuffered_.bits
  protected val arrived = sinkBuffered_.ready && source.valid

  protected final def onArrival: Unit = throw new NotImplementedError("Shall not be used!")

  out := in.asTypeOf(out)

  /** Accepts the current packet, optionally transforming it.
    *
    * @param t
    */
  protected def accept(): Unit = {
    source.ready := true.B
    sinkBuffered_.valid := true.B
  }

  /** Does not accept the packet yet. The packet stay as long as not dropped.
    */
  protected def noAccept(): Unit = {
    source.ready := false.B
    sinkBuffered_.valid := false.B
  }

  /** Drops the current packet.
    */
  protected def drop(): Unit = {
    source.ready := true.B
    sinkBuffered_.valid := false.B
  }

  /** Consumes the current packet from the source.
    */
  protected def consume(): Unit = {
    source.ready := true.B
  }

  /** Produces a new packet to the sink.
    *
    * @param t
    */
  protected def produce(): Unit = {
    sinkBuffered_.valid := true.B
  }

  noAccept()
}
