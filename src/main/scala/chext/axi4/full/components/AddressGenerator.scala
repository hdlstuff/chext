package chext.axi4.full.components

import chext.{axi4, elastic}

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.experimental.BundleLiterals._

import elastic.ConnectOp._

import axi4.Casts._

import axi4.BurstType
import axi4.full.{AddressChannel, ReadAddressChannel, WriteAddressChannel}
import elastic.{Source, Sink, SourceBuffer, SinkBuffer}

class AddrLenSizeBurstBundle(val wAddr: Int) extends Bundle {
  val addr = UInt(wAddr.W)
  val len = UInt(8.W)
  val size = UInt(3.W)
  val burst = UInt(2.W)
}

class AddrSizeLastBundle(val wAddr: Int) extends Bundle {
  val addr = UInt(wAddr.W)
  val size = UInt(3.W)
  val last = Bool()
}

/** @brief
  *   Decodes an address packet by calculating the addresses corresponding to each beat of the
  *   transaction.
  */
class AddressGenerator(val wAddr: Int) extends Module {

  val genSource = new AddrLenSizeBurstBundle(wAddr)
  val genSink = new AddrSizeLastBundle(wAddr)

  val source = IO(Source(Irrevocable(genSource)))
  val sink = IO(Sink(Irrevocable(genSink)))

  val source_ = SourceBuffer(source)
  val sink_ = SinkBuffer(sink)

  private val current = source_.bits

  /** @brief Current address to emit (INCR bursts). */
  private val addr = Reg(UInt(wAddr.W))

  /** @brief Beat counter. */
  private val ctr = Reg(UInt(8.W))

  /** @brief Flag for generating right now. */
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

        when(current.burst === BurstType.INCR) {
          addr := addr + 1.U
        }.elsewhen(current.burst === BurstType.WRAP) {
          val mask1 = current.len + 0.U(wAddr.W)
          val mask2 = ~mask1
          addr := (addr & mask2) | (((addr + 1.U) & mask1))
        }

      }

      when(current.burst === BurstType.FIXED) {
        val result = Wire(genSink)
        result.addr := current.addr
        result.size := current.size
        result.last := last

        sink_.enq(result)

      }.otherwise {
        val result = Wire(genSink)
        result.addr := addr << current.size
        result.size := current.size
        result.last := last

        sink_.enq(result)

      }
    }.otherwise {
      val last = current.len === 0.U

      when(last) {
        source_.deq()
      }.otherwise {
        generating := true.B
        addr := ((current.addr >> current.size) + 1.U)
        ctr := current.len - 1.U
      }

      val result = Wire(genSink)
      result.addr := current.addr
      result.size := current.size
      result.last := last

      sink_.enq(result)
    }
  }
}

class StrobeGenerator {}
