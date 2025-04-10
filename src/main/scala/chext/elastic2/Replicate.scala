package chext.elastic2

import chisel3._
import chisel3.util._
import chisel3.experimental.AffectsChiselPrefix

/** Replicates an input stream "source" to an output stream "sink".
  *
  * @param source
  * @param sink
  * @param wIdx
  */
abstract class Replicate[SourceT <: Data, SinkT <: Data](
    source: Interface[SourceT],
    sink: Interface[SinkT],
    val wIdx: Int = 16
) extends AffectsChiselPrefix {
  private val sinkBuffered_ = SinkBuffer(sink)
  private val generating_ = RegInit(false.B)
  private val idx_ = RegInit(0.U(wIdx.W))

  protected val in = source.bits
  protected val out = sinkBuffered_.bits
  protected val len = WireInit(1.U(wIdx.W))
  protected val idx = Wire(UInt(wIdx.W))
  protected val last = (idx === (len - 1.U))

  protected final def onReplicate: Unit = throw new NotImplementedError("Shall not be used!")

  source.ready := false.B
  sinkBuffered_.valid := false.B

  when(source.valid && sinkBuffered_.ready) {
    when(generating_) {
      when(last) {
        // complete
        generating_ := false.B
        idx_ := 0.U
        source.ready := true.B
      }

      sinkBuffered_.valid := true.B
      idx_ := idx_ + 1.U
    }.otherwise {
      when(len === 0.U) {
        source.deq()
      }.elsewhen(len === 1.U) {
        sinkBuffered_.valid := true.B
        source.ready := true.B
      }.otherwise {
        sinkBuffered_.valid := true.B
        generating_ := true.B
        idx_ := 1.U
      }
    }
  }

  when(!generating_) {
    idx := 0.U
  }.otherwise {
    idx := idx_
  }
}
