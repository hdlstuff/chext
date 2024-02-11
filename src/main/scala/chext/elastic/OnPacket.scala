package chext.elastic

import chisel3._
import chisel3.util._
import chisel3.experimental._

abstract class OnPacket[Tin <: Data, T <: Data](
    source: ReadyValidIO[Tin],
    sink: ReadyValidIO[T]
) extends AffectsChiselPrefix {
  protected val bits = source.bits
  protected val sinkBuffered = SinkBuffer.decoupled(sink)

  /** Accepts the current packet, optionally transforming it.
    *
    * @param t
    */
  protected def accept(t: T = bits.asInstanceOf[T]): Unit = {
    source.ready := true.B
    sinkBuffered.valid := true.B
    sinkBuffered.bits := t
  }

  /** Does not accept the packet yet. The packet stay as long as not dropped.
    */
  protected def noAccept(): Unit = {
    source.ready := false.B
    sinkBuffered.valid := false.B
    sinkBuffered.bits := DontCare
  }

  /** Drops the current packet.
    */
  protected def drop(): Unit = {
    source.ready := true.B
    sinkBuffered.valid := false.B
    sinkBuffered.bits := DontCare
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
  protected def produce(t: T): Unit = {
    sinkBuffered.valid := true.B
    sinkBuffered.bits := t
  }

  /** Called when a packet might be accepted.
    */
  protected def onPacket: Unit

  noAccept()
  when(sinkBuffered.ready && source.valid) {
    onPacket
  }
}
