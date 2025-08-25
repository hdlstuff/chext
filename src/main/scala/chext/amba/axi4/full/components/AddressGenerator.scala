package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import chext.elastic
import elastic.{Source, Sink, SourceBuffer, SinkBuffer}
import elastic.ConnectOp._

import chext.amba.axi4
import axi4.BurstType
import axi4.full.{AddressChannel, ReadAddressChannel, WriteAddressChannel}
import axi4.Casts._

import chext.Prefix.prefix

class AddrLenSizeBurstBundle[T <: Data](
    val wAddr: Int,
    genUser: T = UInt(0.W)
) extends Bundle {
  val addr = UInt(wAddr.W)
  val len = UInt(8.W)
  val size = UInt(3.W)
  val burst = UInt(2.W)

  val user = genUser.cloneType
}

class AddrSizeLastBundle[T <: Data](
    val wAddr: Int,
    genUser: T = UInt(0.W)
) extends Bundle {
  val addr = UInt(wAddr.W)
  val size = UInt(3.W)
  val last = Bool()

  val user = genUser.cloneType
}

class AddrSizeStrobeLastBundle[T <: Data](
    val wAddr: Int,
    val wData: Int,
    genUser: T = UInt(0.W)
) extends Bundle {
  assert(isPow2(wData) && wData >= 32)

  val wStrobe = wData / 8

  val addr = UInt(wAddr.W)
  val size = UInt(3.W)
  val strb = UInt(wStrobe.W)
  val lowerByteIndex = UInt(log2Ceil(wStrobe).W)
  val upperByteIndex = UInt(log2Ceil(wStrobe).W)

  val last = Bool()

  val user = genUser.cloneType
}

/** @brief
  *   Decodes an address packet by calculating the addresses corresponding to each beat of the
  *   transaction.
  */
class AddressGenerator[T <: Data](
    val wAddr: Int,
    val genUser: T = UInt(0.W)
) extends Module {
  val genSource = new AddrLenSizeBurstBundle(wAddr, genUser)
  val genSink = new AddrSizeLastBundle(wAddr, genUser)

  val source = IO(Source(genSource))
  val sink = IO(Sink(genSink))

  val transducer = new elastic.Transducer(source, sink) {

    /** Current address to emit (INCR bursts). */
    val addr = Reg(UInt(wAddr.W))

    /** Beat counter. */
    val ctr = Reg(UInt(8.W))

    /** Flag for generating right now. */
    val generating = RegInit(false.B)

    packet {
      val last = Wire(Bool())

      out.size := in.size
      out.last := last
      out.user := in.user

      when(generating) {
        last := ctr === 0.U

        when(last) {
          accept {
            generating := false.B
          }
        }.otherwise {
          produce {
            ctr := ctr - 1.U

            when(in.burst === BurstType.INCR) {
              addr := addr + 1.U
            }.elsewhen(in.burst === BurstType.WRAP) {
              val mask1 = in.len + 0.U(wAddr.W)
              val mask2 = ~mask1
              addr := (addr & mask2) | (((addr + 1.U) & mask1))
            }
          }

        }

        when(in.burst === BurstType.FIXED) {
          out.addr := in.addr
        }.otherwise {
          out.addr := addr << in.size
        }

      }.otherwise {
        last := in.len === 0.U

        out.addr := in.addr

        when(last) {
          accept {}
        }.otherwise {
          produce {
            generating := true.B
            addr := ((in.addr >> in.size) + 1.U)
            ctr := in.len - 1.U
          }
        }

      }
    }
  }
}

class StrobeGenerator[T <: Data](
    val wAddr: Int,
    val wData: Int,
    val genUser: T = UInt(0.W)
) extends Module {
  val genInput = new AddrSizeLastBundle(wAddr, genUser)
  val genOutput = new AddrSizeStrobeLastBundle(wAddr, wData, genUser)

  val source = IO(Source(genInput))
  val sink = IO(Sink(genOutput))

  private val wStrobe = genOutput.wStrobe
  private val log2strobe = log2Ceil(wStrobe)

  val transform0 = new elastic.Transform(source, sink) {
    val addr = in.addr(log2strobe - 1, 0)

    // we should preserve the lower bits for unaligned transactions
    val lowerByteIndex = addr

    // we should not preserve the lower bits
    val upperByteIndex = ((1.U + (addr >> in.size)) << in.size) - 1.U

    /* pass through */
    out.addr := in.addr
    out.size := in.size
    out.last := in.last
    out.user := in.user

    out.lowerByteIndex := lowerByteIndex
    out.upperByteIndex := upperByteIndex

    /* TODO: is there a better way to optimize this? */
    out.strb := VecInit
      .tabulate(wStrobe) { (idx) =>
        (idx.U <= upperByteIndex) && (idx.U >= lowerByteIndex)
      }
      .asUInt
  }
}

class AddressStrobeGenerator[T <: Data](
    val wAddr: Int,
    val wData: Int,
    val genUser: T = UInt(0.W)
) extends Module {
  private val addressGenerator = Module(new AddressGenerator(wAddr, genUser))
  private val strobeGenerator = Module(new StrobeGenerator(wAddr, wData, genUser))

  val genInput = addressGenerator.genSource
  val genOutput = strobeGenerator.genOutput

  val source = IO(Source(genInput))
  val sink = IO(Sink(genOutput))

  source :=> addressGenerator.source
  addressGenerator.sink :=> strobeGenerator.source
  strobeGenerator.sink :=> sink
}
