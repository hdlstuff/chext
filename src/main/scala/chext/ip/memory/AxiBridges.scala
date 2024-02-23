package chext.ip.memory

import chisel3._
import chisel3.util._

import chext.elastic
import chext.axi4

import axi4.full.components.addrgen._

import elastic.{Fork, Join, Replicate, Transform}
import elastic.ConnectOp._

class Axi4FullToReadWriteBridge(val cfg: axi4.Config) {
  private val wAddr = cfg.wAddr >> (cfg.wStrobe)
  private val wData = cfg.wData

  assert(cfg.read && cfg.write && !cfg.lite)

  val s_axi = IO(axi4.full.Slave(cfg))
  val read = IO(Flipped(new ReadInterface(wAddr, wData)))
  val write = IO(Flipped(new WriteInterface(wAddr, wData)))

  private def implRead() = {
    val addressGenerator = Module(new AddressGenerator(wAddr))
    val respIds = Wire(Irrevocable(UInt(cfg.wId.W)))

    new Fork(s_axi.ar) {
      protected def onFork: Unit = {
        new Replicate(fork(), respIds) {
          protected def onReplicate: Unit = {
            len := in.len
            out := in.id
          }
        }

        new Transform(fork(), addressGenerator.source) {
          protected def onTransform: Unit = {
            out.addr := (in.addr >> (wData >> 3 /* in bytes */ ))
            out.len := in.len
            out.size := in.size
            out.burst := in.burst
          }
        }
      }
    }

    new Transform(addressGenerator.sink, read.req) {
      protected def onTransform: Unit = {
        out := in.addr
      }
    }

    new Join(s_axi.r) {
      protected def onJoin: Unit = {
        val resp = join(read.resp)
        val id = join(respIds)

        out.data := resp
        out.id := id
        out.resp := axi4.ResponseFlag.OKAY
        out.user := 0.U /* TODO: propagate the user data, maybe? */
      }
    }
  }

  private def implWrite() = {
    val addressStrobeGenerator = Module(new AddressStrobeGenerator(wAddr, wData))
    val respId = Wire(Irrevocable(UInt(cfg.wId.W)))

    new Fork(s_axi.aw) {
      fork { in.id } :=> respId

      protected def onFork: Unit = {
        new Transform(fork(), addressStrobeGenerator.source) {
          protected def onTransform: Unit = {
            out.addr := in.addr
            out.len := in.len
            out.size := in.size
            out.burst := in.burst
          }
        }
      }
    }

    new Join(write.req) {
      protected def onJoin: Unit = {
        val addrStrobe = join(addressStrobeGenerator.sink)
        val w = join(s_axi.w)

        out.addr := addrStrobe.addr
        out.data := w.data
        out.strb := addrStrobe.strb & w.strb
      }
    }

    new Join(s_axi.b) {
      protected def onJoin: Unit = {
        out.id := join(respId)
        out.resp := axi4.ResponseFlag.OKAY
        out.user := 0.U /* TODO: propagate the user data, maybe? */

        join(write.resp)
      }
    }
  }

  implRead()
  implWrite()
}

class Axi4LiteToReadWriteBridge(cfg: axi4.Config) extends Module {
  private val wAddr = cfg.wAddr >> (cfg.wStrobe)
  private val wData = cfg.wData

  assert(cfg.read && cfg.write && cfg.lite)

  val s_axi = IO(axi4.lite.Slave(cfg))
  val read = IO(Flipped(new ReadInterface(wAddr, wData)))
  val write = IO(Flipped(new WriteInterface(wAddr, wData)))

  private def implRead() = ???

  private def implWrite() = ???

  implRead()
  implWrite()
}
