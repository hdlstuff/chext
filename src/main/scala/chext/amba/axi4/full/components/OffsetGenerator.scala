package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.elastic
import elastic.{Source, Sink, SinkBuffer}

import chext.util.BitOps._

class AddrLenFixedBundle(val wAddr: Int) extends Bundle {
  val addr = UInt(wAddr.W)
  val len = UInt(5.W)
  val fixed = Bool()
}

class OffsetLastBundle(wOffset: Int) extends Bundle {
  val offset = UInt(wOffset.W)
  val last = Bool()
}

/** This is similar to AddressGenerator; however, simplified and specialized for up/down scale
  * modules.
  *
  * @param wAddr
  * @param wWide
  * @param wNarrow
  */
class OffsetGenerator(wWide: Int, wNarrow: Int) extends Module {
  val wAddr = log2Ceil(wWide >> 3 /* to bytes */)
  val wOffset = log2Ceil(wWide) - log2Ceil(wNarrow)

  val genSource = new AddrLenFixedBundle(wAddr)
  val genSink = new OffsetLastBundle(wOffset)

  val source = IO(Source(Irrevocable(genSource)))
  val sink = IO(Sink(Irrevocable(genSink)))

  private val source_ = source
  private val sink_ = SinkBuffer(sink)

  private val current = source_.bits
  private val offset = Reg(UInt(wOffset.W))
  private val ctr = Reg(UInt(5.W))
  private val generating = RegInit(false.B)

  source_.nodeq()
  sink_.noenq()

  when(source_.valid && sink_.ready) {
    when(generating) {
      val last = ctr === 0.U

      when(last) {
        generating := false.B
        source_.deq()
      }.otherwise {
        ctr := ctr - 1.U

        offset := offset + (!current.fixed).asUInt
      }

      // NOTE: the logic on `bits` might not depend on `source_.valid && sink_.ready`
      // Does your synthesis tool can figure out that optimization?
      // Maybe, in the future, write data and control logic separately
      sink_.enq {
        val result = Wire(genSink)

        result.offset := offset
        result.last := last

        result
      }
    }.otherwise {
      val last = current.len === 0.U
      val thisOffset = current.addr.dropLsbN(log2Ceil(wNarrow >> 3 /* to bytes */))

      when(last) {
        source_.deq()
      }.otherwise {
        generating := true.B
        offset := thisOffset + (!current.fixed).asUInt
        ctr := current.len - 1.U
      }

      sink_.enq {
        val result = Wire(genSink)

        result.offset := thisOffset
        result.last := last

        result
      }
    }
  }
}
