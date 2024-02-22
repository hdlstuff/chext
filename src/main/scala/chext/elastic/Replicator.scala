package chext.elastic

import chisel3._
import chisel3.util._

/** Replicates an input stream "source" to an output stream "sink".
  *
  * @param source
  * @param sink
  * @param wIdx
  */
abstract class Replicate[SourceT <: Data, SinkT <: Data](
    source: ReadyValidIO[SourceT],
    sink: ReadyValidIO[SinkT],
    val wIdx: Int = 16
) {
  protected val in = source.bits
  protected val out = sink.bits
  protected val len = WireInit(1.U(wIdx.W))
  protected val idx = Wire(UInt(wIdx.W))

  protected def onReplicate: Unit

  onReplicate

  private val generating_ = RegInit(false.B)
  private val idx_ = RegInit(0.U(wIdx.W))

  when(source.valid && sink.ready) {
    when(generating_) {
      when(idx_ === len - 1.U) {
        // complete
        generating_ := false.B
        idx_ := 0.U
        source.deq()
      }

      sink.enq(out)
    }.otherwise {
      when(len === 0.U) {
        source.deq()
      }.elsewhen(len === 1.U) {
        sink.enq(out)
        source.deq()
      }.otherwise {
        sink.enq(out)
        generating_ := true.B
        idx_ := 1.U
      }
    }
  }

  idx := idx_
}
