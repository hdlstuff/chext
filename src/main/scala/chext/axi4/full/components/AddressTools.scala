package chext.axi4.full.components

import chext.{axi4, elastic}

import chisel3._
import chisel3.util._
import chisel3.experimental._

import elastic.ConnectOp._

import axi4.Casts._

import axi4.BurstType
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

  private val ar = arSource_.bits

  /** @brief Current address to emit (INCR bursts). */
  private val addr = Reg(UInt(cfg.wAddr.W))

  /** @brief Beat counter. */
  private val ctr = Reg(UInt(8.W))

  /** @brief Flag for generating right now. */
  private val generating = RegInit(false.B)

  arSource_.nodeq()
  addrSink_.noenq()

  when(arSource_.valid && addrSink_.ready) {
    when(generating) {
      when(ctr === 0.U) {
        generating := false.B
        arSource_.deq()
      }.otherwise {
        ctr := ctr - 1.U

        when(ar.burst === BurstType.INCR) {
          addr := addr + 1.U
        }.elsewhen(ar.burst === BurstType.WRAP) {
          val mask1 = ar.len + 0.U(cfg.wAddr.W)
          val mask2 = ~mask1
          addr := (addr & mask2) | (((addr + 1.U) & mask1))
        }
      }

      when(ar.burst === BurstType.FIXED) {
        addrSink_.enq(ar.addr)
      }.otherwise {
        addrSink_.enq(addr << ar.size)
      }
    }.otherwise {
      when(ar.len === 0.U) {
        // create a single addr
        arSource_.deq()
      }.otherwise {
        generating := true.B
        addr := ((ar.addr >> ar.size) + 1.U)
        ctr := ar.len - 1.U
      }

      addrSink_.enq(ar.addr)
    }
  }
}
