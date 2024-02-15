package chext.axi4.full.components

import chext.{axi4, elastic}

import chisel3._
import chisel3.util._
import chisel3.experimental._

import elastic.ConnectOp._

import axi4.Casts._

import axi4.full.{AddressChannel, ReadAddressChannel, WriteAddressChannel}
import elastic.{Source, Sink, SourceBuffer, SinkBuffer}

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
class AddressGenerator(val cfg: axi4.Config, val write: Boolean = false)
    extends Module {
  require(!cfg.lite)
  require(write && cfg.write || !write && cfg.read)

  val arSource = IO(Source(addressChannel(cfg, write)))
  val addrSink = IO(Sink(Irrevocable(UInt(cfg.wAddr.W))))

  val arSource_ = SourceBuffer(arSource)
  val addrSink_ = SinkBuffer(addrSink)

  private val numBytes = cfg.wStrobe
  private val genAddr = UInt(cfg.wAddr.W)

  private val addr = Reg(genAddr)
  private val len = Reg(UInt(8.W))
  private val generating = RegInit(false.B)

  arSource_.nodeq()
  addrSink_.noenq()

  when(arSource_.valid && addrSink_.ready) {
    when(generating) {
      when(len === 0.U) {
        generating := false.B
        arSource_.deq()
      }.otherwise {
        len := len - 1.U
        addr := addr + numBytes.U
      }
      addrSink_.enq(addr)
    }.otherwise {
      val ar = arSource_.bits

      when(ar.len === 0.U) {
        // create a single addr
        arSource_.deq()
      }.otherwise {
        generating := true.B

        val mask = ~((numBytes - 1).U(cfg.wAddr.W))
        addr := (ar.addr & mask) + numBytes.U
        len := ar.len - 1.U
      }

      addrSink_.enq(ar.addr)
    }
  }
}
