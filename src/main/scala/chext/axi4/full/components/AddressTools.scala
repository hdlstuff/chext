package chext.axi4.full.components

import chext.{axi4, elastic}

import chisel3._
import chisel3.util._
import chisel3.experimental._

import elastic.ConnectOp._

import axi4.Casts._

import axi4.full.{AddressChannel, ReadAddressChannel, WriteAddressChannel}
import elastic.{Source, Sink}

private object addressChannel {
  def apply(cfg: axi4.Config, write: Boolean): IrrevocableIO[AddressChannel] =
    if (write) Irrevocable(new WriteAddressChannel()(cfg))
    else Irrevocable(new ReadAddressChannel()(cfg))
}

class AddressTransformer(
    val slaveCfg: axi4.Config,
    val masterCfg: axi4.Config,
    val write: Boolean = false
) extends Module {
  val arIn = Source(addressChannel(slaveCfg, write))
  val arOut = Sink(addressChannel(slaveCfg, write))

  // TODO: Perform data-width conversions

  // Should support: FIXED, INCR, and WRAP

  if (slaveCfg.wData == masterCfg.wData) {
    arIn :=> arOut
  } else if (slaveCfg.wData > masterCfg.wData) {
    // will create more bursts
    // assume that the length field never overflows, we have a burst splitter
  } else {
    // will create fewer bursts
  }
}

/** @brief
  *   Decodes an address packet by calculating the addresses corresponding to
  *   each beat of the transaction.
  */
class AddressDecoder(val cfg: axi4.Config, val write: Boolean = false)
    extends Module {
  require(!cfg.lite)
  require(write && cfg.write || !write && cfg.read)

  val in = IO(Source(addressChannel(cfg, write)))
  val out = IO(Sink(Irrevocable(UInt(cfg.wAddr.W))))

  private val in_ = elastic.SourceBuffer(in)

  out.noenq()
  in_.nodeq()

  private val numBytes = cfg.wStrobe
  private val genAddr = UInt(cfg.wAddr.W)

  private val addr = Reg(genAddr)
  private val ctr = Reg(UInt(8.W))
  private val isAligned = (addr & (numBytes - 1).U) === 0.U
  private val alignedAddr =
    Mux(isAligned, addr, (addr | (numBytes - 1).U) + 1.U)

  when(in.fire) {
    addr := in.bits.addr
    ctr := 0.U
  }

  when(in_.valid && out.ready) {
    when(ctr === in_.bits.len) {
      in_.deq()
    }.elsewhen(ctr === 0.U) {
      addr := alignedAddr + numBytes.U
    }.otherwise {
      addr := addr + numBytes.U
    }

    ctr := ctr + 1.U
    out.enq(addr)
  }
}
