package chext.elastic

import chisel3._

import chisel3.experimental.AffectsChiselPrefix
import chisel3.experimental.SourceInfo
import chisel3.experimental.requireIsChiselType
import chisel3.experimental.requireIsHardware
import chisel3.experimental.skipPrefix

import chisel3.util.log2Ceil

import ConnectOp._

import chext.util.Naming

private object memory_impl {
  trait Memory {
    val count: Int
    val addrWidth: Int
    val dataWidth: Int

    def noRead(): Unit
    def read(addr: UInt): UInt

    def noWrite(): Unit
    def write(addr: UInt, data: UInt): Unit

  }

  class chisel_mem_1w1r(
      val count: Int,
      val addrWidth: Int,
      val dataWidth: Int
  ) extends AffectsChiselPrefix
      with Memory {
    private val mem = Mem(count, UInt(dataWidth.W))

    val addrA = Wire(UInt(addrWidth.W))
    val writeEnA = Wire(Bool())
    val dataInA = Wire(UInt(dataWidth.W))

    val addrB = Wire(UInt(addrWidth.W))
    val dataOutB = mem(addrB)

    when(writeEnA) {
      mem(addrA) := dataInA
    }

    def noRead(): Unit = {
      addrB := DontCare
    }

    def read(addr: UInt): UInt = {
      addrB := addr
      dataOutB
    }

    def noWrite(): Unit = {
      addrA := DontCare
      writeEnA := false.B
      dataInA := DontCare
    }

    def write(addr: UInt, data: UInt): Unit = {
      addrA := addr
      writeEnA := true.B
      dataInA := data
    }
  }

  class chisel_syncmem_1w1r(
      val count: Int,
      val addrWidth: Int,
      val dataWidth: Int
  ) extends AffectsChiselPrefix
      with Memory {
    private val mem = SyncReadMem(count, UInt(dataWidth.W), SyncReadMem.WriteFirst)

    val addrA = Wire(UInt(addrWidth.W))
    val writeEnA = Wire(Bool())
    val dataInA = Wire(UInt(dataWidth.W))

    val addrB = Wire(UInt(addrWidth.W))
    val dataOutB = mem(addrB)

    when(writeEnA) {
      mem(addrA) := dataInA
    }

    def noRead(): Unit = {
      addrB := DontCare
    }

    def read(addr: UInt): UInt = {
      addrB := addr
      dataOutB
    }

    def noWrite(): Unit = {
      addrA := DontCare
      writeEnA := false.B
      dataInA := DontCare
    }

    def write(addr: UInt, data: UInt): Unit = {
      addrA := addr
      writeEnA := true.B
      dataInA := data
    }
  }

  class chext_mem_1w1r(
      val count: Int,
      val addrWidth: Int,
      val dataWidth: Int
  ) extends BlackBox(
        Map(
          "COUNT" -> count,
          "ADDR_WIDTH" -> addrWidth,
          "DATA_WIDTH" -> dataWidth
        )
      )
      with Memory {
    val io = IO(new Bundle {
      val clock = Input(Clock())

      val addrA = Input(UInt(addrWidth.W))
      val writeEnA = Input(Bool())
      val dataInA = Input(UInt(dataWidth.W))

      val addrB = Input(UInt(addrWidth.W))
      val dataOutB = Output(UInt(dataWidth.W))
    })

    def noRead(): Unit = {
      io.addrB := DontCare
    }

    def read(addr: UInt): UInt = {
      io.addrB := addr
      io.dataOutB
    }

    def noWrite(): Unit = {
      io.addrA := DontCare
      io.writeEnA := false.B
      io.dataInA := DontCare
    }

    def write(addr: UInt, data: UInt): Unit = {
      io.addrA := addr
      io.writeEnA := true.B
      io.dataInA := data
    }
  }

  class chext_syncmem_1w1r(
      val count: Int,
      val addrWidth: Int,
      val dataWidth: Int
  ) extends BlackBox(
        Map(
          "COUNT" -> count,
          "ADDR_WIDTH" -> addrWidth,
          "DATA_WIDTH" -> dataWidth
        )
      )
      with Memory {
    val io = IO(new Bundle {
      val clock = Input(Clock())

      val addrA = Input(UInt(addrWidth.W))
      val writeEnA = Input(Bool())
      val dataInA = Input(UInt(dataWidth.W))

      val addrB = Input(UInt(addrWidth.W))
      val dataOutB = Output(UInt(dataWidth.W))
    })

    def noRead(): Unit = {
      io.addrB := DontCare
    }

    def read(addr: UInt): UInt = {
      io.addrB := addr
      io.dataOutB
    }

    def noWrite(): Unit = {
      io.addrA := DontCare
      io.writeEnA := false.B
      io.dataInA := DontCare
    }

    def write(addr: UInt, data: UInt): Unit = {
      io.addrA := addr
      io.writeEnA := true.B
      io.dataInA := data
    }
  }
}

object Queue {
  def between[T <: Data](
      source: Interface[T],
      sink: Interface[T],
      count: Int,
      pipe: Boolean = false,
      flow: Boolean = false,
      useSyncReadMem: Boolean = false
  )(implicit si: SourceInfo): Unit = {
    requireIsHardware(source, "Queue source must be hardware.")
    requireIsHardware(sink, "Queue sink must be hardware.")
    require(count >= 0, "Length must be non-negative.")

    if (count == 0) {
      source :=> sink
    } else {
      val gen = chiselTypeOf(source.bits)
      val queue = skipPrefix { new Queue(gen, count, pipe, flow, useSyncReadMem) }
      source :=> queue.source
      queue.sink :=> sink
    }
  }

  def apply[T <: Data](
      gen: T,
      count: Int,
      pipe: Boolean = false,
      flow: Boolean = false,
      useSyncReadMem: Boolean = false
  )(implicit si: SourceInfo): Queue[T] = {
    require(count > 0, "Length must be positive.")
    new Queue(gen, count, pipe, flow, useSyncReadMem)
  }

  private[Queue] var useVerilogMem_ = true

  def useVerilogMem(enabled: Boolean = true): Unit = {
    useVerilogMem_ = enabled
  }
}

class Queue[T <: Data](
    val gen: T,
    val count: Int,
    val pipe: Boolean = false,
    val flow: Boolean = false,
    val useSyncReadMem: Boolean = false
)(implicit si: SourceInfo)
    extends AffectsChiselPrefix {
  Naming.needsUniquePrefix("Queue")

  require(count > -1, "Queue must have non-negative count.")
  require(count != 0, "Use companion object Queue.apply for empty queue.")
  requireIsChiselType(gen)

  val source = Wire(Source(gen))
  val sink = Wire(Sink(gen))

  {
    dontTouch(source)
    dontTouch(sink)

    val wAddr = log2Ceil(count)
    val wData = gen.getWidth

    val ram =
      (Queue.useVerilogMem_, useSyncReadMem) match {
        case (false, false) => new memory_impl.chisel_mem_1w1r(count, wAddr, wData)
        case (false, true)  => new memory_impl.chisel_syncmem_1w1r(count, wAddr, wData)

        case (true, false) => {
          val ram = Module(new memory_impl.chext_mem_1w1r(count, wAddr, wData))
          ram.io.clock := Module.clock
          ram
        }

        case (true, true) => {
          val ram = Module(new memory_impl.chext_syncmem_1w1r(count, wAddr, wData))
          ram.io.clock := Module.clock
          ram
        }
      }

    ram.noRead()
    ram.noWrite()

    val enq_ptr = chisel3.util.Counter(count)
    val deq_ptr = chisel3.util.Counter(count)
    val maybe_full = RegInit(false.B)
    val ptr_match = enq_ptr.value === deq_ptr.value
    val empty = ptr_match && !maybe_full
    val full = ptr_match && maybe_full
    val do_enq = WireDefault(source.fire)
    val do_deq = WireDefault(sink.fire)

    when(do_enq) {
      ram.write(enq_ptr.value, source.bits.asUInt)
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
      sink.bits := ram.read(r_addr).asTypeOf(sink.bits)
    } else {
      sink.bits := ram.read(deq_ptr.value).asTypeOf(sink.bits)
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
  }
}

object TestQueue extends App {
  Queue.useVerilogMem(true)

  emitVerilog(new Module {
    private val gen = new Bundle {
      val a = UInt(37.W)
      val b = UInt(30.W)
    }
    val source = IO(Source(gen))
    val sink = IO(Sink(gen))

    Queue.between(source, sink, 9)
  })
}
