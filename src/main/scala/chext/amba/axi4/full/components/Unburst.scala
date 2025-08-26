package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

import chext.amba.axi4
import chext.elastic
import elastic.ConnectOp._
import axi4.Ops._

case class UnburstConfig(
    val axiCfg: axi4.Config,
    val numOutstandingRead: Int = 8,
    val numOutstandingWrite: Int = 8
) {
  require(axiCfg.wId == 0, "axiCfg.wId must be zero!")
  require(!axiCfg.lite, "axiCfg.lite must be false!")
  require(numOutstandingRead >= 2, "AR queue capacity must be >= 2!")
  require(numOutstandingWrite >= 2, "AW queue capacity must be >= 2!")

  require(axiCfg.wUserB == 0, "user data is not supported on channel B.")

  val wAddr = axiCfg.wAddr
  val wData = axiCfg.wData
}

class Unburst(val cfg: UnburstConfig) extends Module {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiCfg))
  val m_axi = IO(axi4.full.Master(axiCfg))

  dontTouch(s_axi)
  dontTouch(m_axi)

  private def implRead(): Unit = prefix("read") {
    val addressStrobeGenerator =
      Module(
        new AddressStrobeGenerator(wAddr, wData, chiselTypeOf(s_axi.ar.$bits))
      )

    val wire0 = elastic.EWire(UInt(axiCfg.wLen.W))

    def implAR(): Unit = prefix("ar") {
      val fork0 = new elastic.Fork(s_axi.ar) {
        val transform0 = new elastic.Transform(fork(), addressStrobeGenerator.source) {
          out.addr := in.addr
          out.len := in.len
          out.size := in.size
          out.burst := in.burst

          out.user := in
        }

        fork(in.len) :=> elastic.SinkBuffer(wire0, numOutstandingRead)
      }

      val transform1 = new elastic.Transform(addressStrobeGenerator.sink, m_axi.ar) {
        out := in.user

        out.addr := in.addr
        out.len := 0.U
        out.size := in.size
        out.burst := axi4.BurstType.INCR
      }
    }

    def implR(): Unit = prefix("r") {
      val lastRepeated = elastic.EWire(Bool())

      val repeat0 = new elastic.Repeat(wire0, lastRepeated, axiCfg.wLen + 1) {
        len { in => in +& 1.U }
        out { (in, _, _, last) => last }
      }

      val join0 = new elastic.Join(s_axi.r) {
        val r = join(m_axi.r)
        val last = join(lastRepeated)

        out := r
        out.last := last
      }
    }

    implAR()
    implR()
  }

  private def implWrite(): Unit = prefix("write") {
    val addressStrobeGenerator =
      Module(
        new AddressStrobeGenerator(wAddr, wData, chiselTypeOf(s_axi.aw.$bits))
      )

    val wire0 = elastic.EWire(UInt(axiCfg.wLen.W))

    def implAW(): Unit = {
      val fork0 = new elastic.Fork(s_axi.aw) {
        val transform0 = new elastic.Transform(fork(), addressStrobeGenerator.source) {
          out.addr := in.addr
          out.len := in.len
          out.size := in.size
          out.burst := in.burst
          out.user := in
        }

        fork(in.len) :=> wire0
      }

      val transform0 = new elastic.Transform(addressStrobeGenerator.sink, m_axi.aw) {
        out := in.user

        out.addr := in.addr
        out.len := 0.U
        out.size := in.size
        out.burst := axi4.BurstType.INCR
      }
    }

    def implW(): Unit = {
      val transform1 = new elastic.Transform(s_axi.w, m_axi.w) {
        out := in
        out.last := true.B
      }
    }

    def implB(): Unit = {
      val lastRepeated = elastic.EWire(Bool())
      
      val repeat0 = new elastic.Repeat(wire0, lastRepeated, axiCfg.wLen + 1) {
        len { in => in +& 1.U }
        out { (in, _, _, last) => last }
      }

      val joined = elastic.Zip(m_axi.b, lastRepeated)

      val transducerReduceResp = new elastic.Transducer(joined, s_axi.b) {
        val respReg = RegInit(0.U(2.W))

        out := in._1
        out.resp := Mux(in._1.resp > respReg, in._1.resp, respReg)

        packet {
          when(in._2 /* last */ ) {
            accept {
              respReg := 0.U
            }
          }.otherwise {
            consume {
              respReg := out.resp
            }
          }
        }
      }
    }

    implAW()
    implW()
    implB()
  }

  if (axiCfg.read) implRead()
  if (axiCfg.write) implWrite()
}
