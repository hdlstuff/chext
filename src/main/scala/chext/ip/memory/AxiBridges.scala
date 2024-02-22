package chext.ip.memory

import chisel3._
import chisel3.util._

import chext.elastic
import chext.axi4

import axi4.full.components.addrgen.{
  AddressStrobeGenerator,
  AddrLenSizeBurstBundle,
  AddrSizeStrobeLastBundle
}
import elastic._

private[memory] object connectAddressStrobeGenerator {
  def apply(
      source: ReadyValidIO[axi4.full.AddressChannel],
      sink: ReadyValidIO[AddrLenSizeBurstBundle]
  ): Unit = {}
}

class AxiToReadWriteBridge(val cfg: axi4.Config) {
  private val wAddr = cfg.wAddr >> (cfg.wStrobe)
  private val wData = cfg.wData
  assert(cfg.read && cfg.write)

  val s_axi = IO(axi4.full.Slave(cfg))
  val read = IO(Flipped(new ReadInterface(wAddr, wData)))
  val write = IO(Flipped(new WriteInterface(wAddr, wData)))

  def implRead() = {
    val addressStrobeGenerator = Module(new AddressStrobeGenerator(wAddr, wData))

    new Transform(s_axi.ar, addressStrobeGenerator.source) {
      protected def onTransform: Unit = {
        out.addr := in.addr
        out.len := in.len
        out.size := in.size
        out.burst := in.burst
      }
    }

    new Transform(addressStrobeGenerator.sink, read.req) {
      protected def onTransform: Unit = {
        out := in.addr
      }
    }

    new Join(s_axi.r) {
      protected def onJoin: Unit = {
        val resp = join(read.resp)
        val ar = join(fork())
      }
    }
  }

  def implWrite() = {
    val addressStrobeGenerator = Module(new AddressStrobeGenerator(wAddr, wData))
    connectAddressStrobeGenerator(s_axi.aw, addressStrobeGenerator.source)
  }

  implRead()
  implWrite()
}
