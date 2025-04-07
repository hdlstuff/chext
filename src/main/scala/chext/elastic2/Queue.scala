package chext.elastic2

import chisel3._
import chisel3.experimental.{requireIsHardware, requireIsChiselType, AffectsChiselPrefix, prefix}

private class ChiselQueue[T <: Data](
    val gen: T,
    val entries: Int,
    val pipe: Boolean = false,
    val flow: Boolean = false,
    val useSyncReadMem: Boolean = false,
    val hasFlush: Boolean = false
) extends Module() {
  require(entries > -1, "Queue must have non-negative number of entries")
  require(entries != 0, "Use companion object Queue.apply for zero entries")
  requireIsChiselType(gen)

  val source = IO(Source(gen))
  val sink = IO(Sink(gen))
  val ram =
    if (useSyncReadMem) SyncReadMem(entries, gen, SyncReadMem.WriteFirst) else Mem(entries, gen)
  val enq_ptr = chisel3.util.Counter(entries)
  val deq_ptr = chisel3.util.Counter(entries)
  val maybe_full = RegInit(false.B)
  val ptr_match = enq_ptr.value === deq_ptr.value
  val empty = ptr_match && !maybe_full
  val full = ptr_match && maybe_full
  val do_enq = WireDefault(source.fire)
  val do_deq = WireDefault(sink.fire)

  // when flush is high, empty the queue
  // Semantically, any enqueues happen before the flush.
  when(do_enq) {
    ram(enq_ptr.value) := source.bits
    enq_ptr.inc()
  }
  when(do_deq) {
    deq_ptr.inc()
  }
  when(do_enq =/= do_deq) {
    maybe_full := do_enq
  }

  sink.valid := !empty
  source.ready := !full

  if (useSyncReadMem) {
    val deq_ptr_next = Mux(deq_ptr.value === (entries.U - 1.U), 0.U, deq_ptr.value + 1.U)
    val r_addr = WireDefault(Mux(do_deq, deq_ptr_next, deq_ptr.value))
    sink.bits := ram.read(r_addr)
  } else {
    sink.bits := ram(deq_ptr.value)
  }

  if (flow) {
    when(source.valid) { sink.valid := true.B }
    when(empty) {
      sink.bits := source.bits
      do_deq := false.B
      when(sink.ready) { do_enq := false.B }
    }
  }

  if (pipe) {
    when(sink.ready) { source.ready := true.B }
  }

  override def desiredName = s"ChiselQueue_${entries}_${gen.typeName}"
}

package verilog {
  class chext_queue(
      val count: Int,
      val addrWidth: Int,
      val dataWidth: Int,
      val pipe: Boolean,
      val flow: Boolean,
      val useSyncmem: Boolean
  ) extends BlackBox(
        Map(
          "COUNT" -> count,
          "ADDR_WIDTH" -> addrWidth,
          "DATA_WIDTH" -> dataWidth,
          "PIPE" -> (if (pipe) 1 else 0),
          "FLOW" -> (if (flow) 1 else 0),
          "USE_SYNCMEM" -> (if (useSyncmem) 1 else 0)
        )
      ) {
    val io = IO(new Bundle {
      val clock = Input(Clock())
      val reset = Input(Bool())
      val source = Source(UInt(dataWidth.W))
      val sink = Sink(UInt(dataWidth.W))
    })
  }
}

object Queue {
  def apply[T <: Data](
      source: Interface[T],
      sink: Interface[T],
      count: Int,
      pipe: Boolean = false,
      flow: Boolean = false,
      syncReadMem: Boolean = false,
      useVerilog: Boolean = true
  ) = {
    requireIsHardware(source, "Queue source must be hardware.")
    requireIsHardware(sink, "Queue sink must be hardware.")
    require(count >= 0, "Length must be non-negative.")

    if (count == 0) {
      import ConnectOp._
      source :=> sink
    } else if (useVerilog) {
      val addrWidth = chisel3.util.log2Ceil(count)
      val dataWidth = source.bits.getWidth

      val queue =
        Module(new verilog.chext_queue(count, addrWidth, dataWidth, flow, pipe, syncReadMem))

      queue.io.clock := Module.clock
      queue.io.reset := Module.reset

      new Transform(source, queue.io.source) {}
      new Transform(queue.io.sink, sink) {}
    } else {
      // TODO: Chisel queue implementation causes a verilog code size explosion
      // We should provide our own Chisel-compatible queue implementation
      // This queue must be parametrized and it must be self-contained
      val queue =
        Module(new ChiselQueue(chiselTypeOf(source.bits), count, pipe, flow, syncReadMem))

      import ConnectOp._
      source :=> queue.source
      queue.sink :=> sink
    }
  }
}
