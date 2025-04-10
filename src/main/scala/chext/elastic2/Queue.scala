package chext.elastic2

import chisel3._
import chisel3.experimental._

trait Queue[T <: Data] {
  def module: BaseModule

  def source: Interface[T]
  def sink: Interface[T]
}

object Queue {
  def between[T <: Data](
      source: Interface[T],
      sink: Interface[T],
      count: Int,
      pipe: Boolean = false,
      flow: Boolean = false,
      useSyncReadMem: Boolean = false,
      useVerilog: Boolean = true
  ) = {
    requireIsHardware(source, "Queue source must be hardware.")
    requireIsHardware(sink, "Queue sink must be hardware.")
    require(count >= 0, "Length must be non-negative.")

    if (count == 0) {
      import ConnectOp._
      source :=> sink
    } else if (useVerilog)
      VerilogQueue.between(source, sink, count, pipe, flow, useSyncReadMem)
    else
      ChiselQueue.between(source, sink, count, pipe, flow, useSyncReadMem)
  }

  def apply[T <: Data](
      gen: T,
      count: Int,
      pipe: Boolean = false,
      flow: Boolean = false,
      syncReadMem: Boolean = false,
      useVerilog: Boolean = true
  ): Queue[T] = {
    require(count > 0, "Length must be positive.")

    if (useVerilog)
      VerilogQueue(gen, count, pipe, flow, syncReadMem)
    else
      ChiselQueue(gen, count, pipe, flow, syncReadMem)
  }
}

class ChiselQueue[T <: Data](
    val gen: T,
    val count: Int,
    val pipe: Boolean = false,
    val flow: Boolean = false,
    val useSyncReadMem: Boolean = false
) extends Module
    with Queue[T] {
  require(count > -1, "Queue must have non-negative count.")
  require(count != 0, "Use companion object Queue.apply for empty queue.")
  requireIsChiselType(gen)

  def module: BaseModule = this

  val source = IO(Source(gen))
  val sink = IO(Sink(gen))
  val ram =
    if (useSyncReadMem) SyncReadMem(count, gen, SyncReadMem.WriteFirst) else Mem(count, gen)
  val enq_ptr = chisel3.util.Counter(count)
  val deq_ptr = chisel3.util.Counter(count)
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
    val deq_ptr_next = Mux(deq_ptr.value === (count.U - 1.U), 0.U, deq_ptr.value + 1.U)
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

  override def desiredName = s"chext_queue_${count}_${gen.typeName}"
}

object ChiselQueue {
  def apply[T <: Data](
      gen: T,
      count: Int,
      pipe: Boolean = false,
      flow: Boolean = false,
      useSyncReadMem: Boolean = false,
      hasFlush: Boolean = false
  ): Queue[T] = Module(new ChiselQueue(gen, count, pipe, flow, useSyncReadMem))

  def between[T <: Data](
      source: Interface[T],
      sink: Interface[T],
      count: Int,
      pipe: Boolean = false,
      flow: Boolean = false,
      useSyncReadMem: Boolean = false
  ): Unit = {
    val x_queue =
      Module(
        new ChiselQueue(source.bits.cloneType, count, pipe, flow, useSyncReadMem)
      )

    import ConnectOp._

    source :=> x_queue.source
    x_queue.sink :=> sink
  }
}

trait VerilogQueue extends BlackBox {
  def clock: Clock
  def reset: Reset

  def source: Interface[UInt]
  def sink: Interface[UInt]
}

object VerilogQueue {
  def apply[T <: Data](
      gen: T,
      count: Int,
      pipe: Boolean = false,
      flow: Boolean = false,
      useSyncReadMem: Boolean = false
  ): Queue[T] = {
    val addrWidth = chisel3.util.log2Ceil(count)
    val dataWidth = gen.getWidth

    val x_queue =
      if (dataWidth == 0)
        Module(
          new verilog.chext_queue_no_data(count, addrWidth, pipe, flow)
        )
      else
        Module(
          new verilog.chext_queue(count, addrWidth, dataWidth, pipe, flow, useSyncReadMem)
        )

    x_queue.clock := Module.clock
    x_queue.reset := Module.reset

    val x_source = Wire(Interface(gen))
    val x_sink = Wire(Interface(gen))

    new Transform(x_source, x_queue.source) {
      out := in.asTypeOf(out)
    }

    new Transform(x_queue.sink, x_sink) {
      out := in.asTypeOf(out)
    }

    new Queue[T] {
      val module: BaseModule = x_queue

      val source: Interface[T] = x_source
      val sink: Interface[T] = x_sink
    }
  }

  def between[T <: Data](
      source: Interface[T],
      sink: Interface[T],
      count: Int,
      pipe: Boolean = false,
      flow: Boolean = false,
      useSyncReadMem: Boolean = false
  ): Unit = {
    val addrWidth = chisel3.util.log2Ceil(count)
    val dataWidth = source.bits.getWidth

    val x_queue =
      if (dataWidth == 0)
        Module(
          new verilog.chext_queue_no_data(count, addrWidth, pipe, flow)
        )
      else
        Module(
          new verilog.chext_queue(count, addrWidth, dataWidth, pipe, flow, useSyncReadMem)
        )

    x_queue.clock := Module.clock
    x_queue.reset := Module.reset

    new Transform(source, x_queue.source) {
      out := in.asTypeOf(out)
    }

    new Transform(x_queue.sink, sink) {
      out := in.asTypeOf(out)
    }
  }
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
      )
      with VerilogQueue {
    val io = IO(new Bundle {
      val clock = Input(Clock())
      val reset = Input(Bool())
      val source = Source(UInt(dataWidth.W))
      val sink = Sink(UInt(dataWidth.W))
    })

    def clock: Clock = io.clock
    def reset: Reset = io.reset
    def source: Interface[UInt] = io.source
    def sink: Interface[UInt] = io.sink
  }

  class chext_queue_no_data(
      val count: Int,
      val addrWidth: Int,
      val pipe: Boolean,
      val flow: Boolean
  ) extends BlackBox(
        Map(
          "COUNT" -> count,
          "ADDR_WIDTH" -> addrWidth,
          "PIPE" -> (if (pipe) 1 else 0),
          "FLOW" -> (if (flow) 1 else 0)
        )
      )
      with VerilogQueue {
    val io = IO(new Bundle {
      val clock = Input(Clock())
      val reset = Input(Bool())
      val source = Source(UInt(0.W))
      val sink = Sink(UInt(0.W))
    })

    def clock: Clock = io.clock
    def reset: Reset = io.reset
    def source: Interface[UInt] = io.source
    def sink: Interface[UInt] = io.sink
  }
}
