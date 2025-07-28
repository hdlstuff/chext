package chext.memory

import chisel3._
import chisel3.util._

import chext.elastic
import chext.amba.axi4

import axi4.full.components._

import elastic.ConnectOp._
import chisel3.experimental.prefix

private class IdLastBundle(wId: Int) extends Bundle {
  val id = UInt(wId.W)
  val last = Bool()
}

class Axi4FullToReadWriteBridge(val axiCfg: axi4.Config) extends Module {
  private val numOutstandingRead: Int = 4
  private val numOutstandingWrite: Int = 4
  private val addrShift = log2Ceil(axiCfg.wData >> 3)
  private val wWordAddr = axiCfg.wAddr - addrShift
  private val wData = axiCfg.wData

  assert(axiCfg.read && axiCfg.write && !axiCfg.lite)

  val s_axi = IO(axi4.full.Slave(axiCfg))
  val read = IO(Flipped(new ReadInterface(wWordAddr, wData)))
  val write = IO(Flipped(new WriteInterface(wWordAddr, wData)))

  private def implRead() = prefix("read") {
    val addressGenerator = Module(new AddressGenerator(axiCfg.wAddr))
    val idLast = Wire(elastic.Interface(new IdLastBundle(axiCfg.wId)))

    val fork0 = new elastic.Fork(s_axi.ar) {
      val repeat0 =
        new elastic.Repeat(
          elastic.SourceBuffer(fork(), numOutstandingRead),
          idLast,
          axiCfg.wLen + 1
        ) {
          len { (in) => in.len +& 1.U }

          outExplicit {
            case (in, _, _, last, out) => {
              out.id := in.id
              out.last := last
            }
          }
        }

      val transform0 = new elastic.Transform(fork(), addressGenerator.source) {
        out.addr := in.addr
        out.len := in.len
        out.size := in.size
        out.burst := in.burst
        out.user := 0.U
      }
    }

    val transform0 = new elastic.Transform(addressGenerator.sink, read.req) {
      out := in.addr >> addrShift
    }

    val join0 = new elastic.Join(s_axi.r) {
      val resp = join(read.resp)
      val id = join(idLast)

      out.data := resp
      out.id := id.id
      out.resp := axi4.ResponseFlag.OKAY
      out.user := 0.U // TODO: propagate the user data, maybe?
      out.last := id.last
    }
  }

  private def implWrite() = prefix("write") {
    val addressStrobeGenerator = Module(new AddressStrobeGenerator(axiCfg.wAddr, wData))
    val idLast = Wire(elastic.Interface(new IdLastBundle(axiCfg.wId)))

    val fork1 = new elastic.Fork(s_axi.aw) {
      val repeat0 =
        new elastic.Repeat(
          elastic.SourceBuffer(fork(), numOutstandingWrite),
          idLast,
          axiCfg.wLen + 1
        ) {
          len { (in) => in.len +& 1.U }

          outExplicit {
            case (in, _, _, last, out) => {
              out.id := in.id
              out.last := last
            }
          }
        }

      val transform0 = new elastic.Transform(fork(), addressStrobeGenerator.source) {
        out.addr := in.addr
        out.len := in.len
        out.size := in.size
        out.burst := in.burst
        out.user := 0.U
      }
    }

    val join0 = new elastic.Join(write.req) {
      val addrStrobe = join(addressStrobeGenerator.sink)
      val w = join(s_axi.w)

      out.addr := addrStrobe.addr >> addrShift
      out.data := w.data
      out.strb := addrStrobe.strb & w.strb
    }

    val idLastJoined = Wire(elastic.Interface(new IdLastBundle(axiCfg.wId)))

    val join1 = new elastic.Join(idLastJoined) {
      out := join(idLast)
      join(write.resp)
    }

    val drop0 = new elastic.Drop(idLastJoined, s_axi.b) {
      out.id := in.id
      out.resp := axi4.ResponseFlag.OKAY
      out.user := 0.U // TODO: propagate the user data, maybe?

      cond { !in.last }
    }
  }

  implRead()
  implWrite()
}

class Axi4LiteToReadWriteBridge(val axiCfg: axi4.Config) extends Module {
  private val addrShift = log2Ceil(axiCfg.wData >> 3)
  private val wWordAddr = axiCfg.wAddr - addrShift
  private val wData = axiCfg.wData

  assert(axiCfg.read && axiCfg.write && axiCfg.lite)

  val s_axil = IO(axi4.lite.Slave(axiCfg))
  val read = IO(Flipped(new ReadInterface(wWordAddr, wData)))
  val write = IO(Flipped(new WriteInterface(wWordAddr, wData)))

  private def implRead() = prefix("read") {
    val transform1 = new elastic.Transform(s_axil.ar, read.req) {
      out := in.addr >> addrShift
    }

    val transform2 = new elastic.Transform(read.resp, s_axil.r) {
      out.data := in
      out.resp := axi4.ResponseFlag.OKAY
    }
  }
  implRead()

  private def implWrite() = prefix("write") {
    val join1 = new elastic.Join(write.req) {
      val aw = join(s_axil.aw)
      val w = join(s_axil.w)

      out.addr := aw.addr >> addrShift
      out.data := w.data
      out.strb := w.strb
    }

    val transform1 = new elastic.Transform(write.resp, s_axil.b) {
      out.resp := axi4.ResponseFlag.OKAY
    }
  }
  implWrite()
}
