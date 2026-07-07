package chext.elastic

import chext.elastic.{tracking => t}

import chisel3._
import chisel3.experimental.{
  AffectsChiselPrefix,
  SourceInfo,
  requireIsChiselType,
  requireIsHardware,
  skipPrefix
}
import chisel3.hacks.deferred
import chisel3.util.log2Ceil

import chext.deadlock
import chext.elastic.ConnectOp._
import chext.tracking.Component

private object memory_impl {
  private val require_ = chext.util.Require.inferred()

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
    private val mem =
      SyncReadMem(count, UInt(dataWidth.W), SyncReadMem.WriteFirst)

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

  @scala.annotation.nowarn("cat=deprecation")
  // TODO Replace with ExtModule
  class chext_mem_1w1r(
      val count: Int,
      val addrWidth: Int,
      val dataWidth: Int
  ) extends BlackBox( // TODO Replace with ExtModule
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

  @scala.annotation.nowarn("cat=deprecation")
  // TODO Replace with ExtModule
  class chext_syncmem_1w1r(
      val count: Int,
      val addrWidth: Int,
      val dataWidth: Int
  ) extends BlackBox( // TODO Replace with ExtModule
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

  class no_data_mem(
      val count: Int,
      val addrWidth: Int,
      val dataWidth: Int
  ) extends Memory {
    require_(dataWidth == 0, "no_data_mem needs dataWidth == 0")

    def noRead(): Unit = {
      // nop
    }

    def read(addr: UInt): UInt = {
      0.U
    }

    def noWrite(): Unit = {
      // nop
    }

    def write(addr: UInt, data: UInt): Unit = {
      // nop
    }
  }

  class single_elem_mem(
      val count: Int,
      val addrWidth: Int,
      val dataWidth: Int
  ) extends Memory
      with AffectsChiselPrefix {
    require_(count == 1, "single_elem_mem needs count == 1")

    val mem = RegInit(0.U(dataWidth.W))

    def noRead(): Unit = {
      // nop
    }

    def read(addr: UInt): UInt = {
      mem
    }

    def noWrite(): Unit = {
      // nop
    }

    def write(addr: UInt, data: UInt): Unit = {
      mem := data
    }
  }
}

object Queue {
  private val require_ = chext.util.Require.inferred()

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
    require_(count >= 0, "Length must be non-negative.")

    if (count == 0) {
      source :=> sink
    } else {
      val queueSink = EWire.like(source)
      val queue = skipPrefix {
        new Queue(source, queueSink, count, pipe, flow, useSyncReadMem)
      }
      queue.sink :=> sink
    }
  }

  def apply[T <: Data](
      gen: T,
      count: Int,
      pipe: Boolean = false,
      flow: Boolean = false,
      useSyncReadMem: Boolean = false
  )(implicit si: SourceInfo): Queue[T, T] = {
    require_(count > 0, "Length must be positive.")
    requireIsChiselType(gen)

    val source = EWire(gen)
    val sink = EWire(gen)
    new Queue(source, sink, count, pipe, flow, useSyncReadMem)
  }

  private[Queue] var useVerilogMem_ = true

  def useVerilogMem(enabled: Boolean = true): Unit = {
    useVerilogMem_ = enabled
  }
}

class Queue[Tin <: Data, Tout <: Data](
    val source: Interface[Tin],
    val sink: Interface[Tout],
    val count: Int,
    val pipe: Boolean = false,
    val flow: Boolean = false,
    val useSyncReadMem: Boolean = false
)(implicit si_ : SourceInfo)
    extends Component
    with Fire[Tout] {
  protected def fireSink: Interface[Tout] = sink

  private val genIn = chiselTypeOf(source.$bits)
  private val genOut = chiselTypeOf(sink.$bits)

  private val elasticState = trackingState(t.Tag)
  import elasticState._

  addSourcePort("source", source)
  addSinkPort("sink", sink)

  val sourceInfo: SourceInfo = si_
  def tpe: String = "Queue"
  def namePrefix: String = "queue"

  type OutFn = Tin => Tout
  type OutExplicitFn = (Tin, Tout) => Unit

  private var outFn_ = Option.empty[OutFn]

  private val require_ = chext.util.Require.inferred(sourceInfo)

  require_(count >= 0, "Length must be non-negative.")

  protected final def in: Tin = require_.fail("`in` field shall not be used!")

  /** Sets a pure functional transformation for the value driven on sink.$bits.
    * The function takes the raw queue output and returns the transformed value.
    */
  protected final def out(fn: => OutFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(
      outFn_.isEmpty,
      "'out { (in) => ... }' must be called at most once!"
    )

    outFn_ = Some(fn)
  }

  /** Sets an imperative transformation for the value driven on sink.$bits. The
    * function takes the raw queue output and a mutable output wire.
    */
  protected final def outExplicit(fn: => OutExplicitFn)(implicit si_ : SourceInfo): Unit = {
    require_.here(
      outFn_.isEmpty,
      "'outExplicit { (in, out) => ... }' must be called at most once!"
    )

    out { (in) =>
      {
        val outResult = Wire(genOut)
        fn(in, outResult)
        outResult
      }
    }
  }

  deferred {
    val outFn = outFn_.getOrElse((x: Tin) => x)

    dontTouch(source)
    dontTouch(sink)

    if (count == 0) {
      sink.$valid := source.$valid
      source.$ready := sink.$ready
      sink.$bits := outFn(source.$bits)

      val monitor0 = new deadlock.Monitor(this) {
        source.waitValid := true.B
      }
    } else {
      val wAddr = log2Ceil(count)
      val wData = genIn.getWidth

      val ram =
        (wData, wAddr, Queue.useVerilogMem_, useSyncReadMem) match {
          case (0, _, _, _) => new memory_impl.no_data_mem(count, wAddr, wData)
          case (_, 0, _, _) =>
            new memory_impl.single_elem_mem(count, wAddr, wData)
          case (_, _, false, false) =>
            new memory_impl.chisel_mem_1w1r(count, wAddr, wData)
          case (_, _, false, true) =>
            new memory_impl.chisel_syncmem_1w1r(count, wAddr, wData)

          case (_, _, true, false) => {
            val ram = Module(new memory_impl.chext_mem_1w1r(count, wAddr, wData))
            ram.io.clock := Module.clock
            ram
          }

          case (_, _, true, true) => {
            val ram = Module(
              new memory_impl.chext_syncmem_1w1r(count, wAddr, wData)
            )
            ram.io.clock := Module.clock
            ram
          }
        }

      ram.noRead()
      ram.noWrite()

      val enqPtr = chisel3.util.Counter(count)
      val deqPtr = chisel3.util.Counter(count)
      val maybeFull = RegInit(false.B)

      val ptrMatch = enqPtr.value === deqPtr.value
      val empty = ptrMatch && !maybeFull
      val full = ptrMatch && maybeFull

      val doEnq = WireDefault(source.fire)
      val doDeq = WireDefault(sink.fire)

      when(doEnq) {
        ram.write(enqPtr.value, source.$bits.asUInt)
        enqPtr.inc()
      }

      when(doDeq) {
        deqPtr.inc()
      }

      when(doEnq =/= doDeq) {
        maybeFull := doEnq
      }

      sink.$valid := !empty
      source.$ready := !full

      val rawOut =
        if (useSyncReadMem) {
          val deqPtrNext =
            Mux(deqPtr.value === (count.U - 1.U), 0.U, deqPtr.value + 1.U)
          val rAddr = WireDefault(Mux(doDeq, deqPtrNext, deqPtr.value))
          ram.read(rAddr).asTypeOf(genIn)
        } else {
          ram.read(deqPtr.value).asTypeOf(genIn)
        }

      sink.$bits := outFn(rawOut)

      if (flow) {
        when(source.$valid) {
          sink.$valid := true.B
        }

        when(empty) {
          sink.$bits := outFn(source.$bits)
          doDeq := false.B

          when(sink.$ready) {
            doEnq := false.B
          }
        }
      }

      if (pipe) {
        when(sink.$ready) {
          source.$ready := true.B
        }
      }

      val monitor0 = new deadlock.Monitor(this) {
        source.waitValid := empty
        sink.waitReady := full
      }
    }
  }
}
