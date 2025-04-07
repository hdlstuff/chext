package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

import chext.amba.axi4
import chext.{elastic2 => elastic}
import elastic.ConnectOp._
import axi4.Ops._

case class UnburstConfig(
    val axiCfg: axi4.Config,
    val arQueueCapacity: Int = 8,
    val awQueueCapacity: Int = 8
) {
  require(axiCfg.wId == 0, "axiCfg.wId must be zero!")
  require(!axiCfg.lite, "axiCfg.lite must be false!")
  require(arQueueCapacity >= 2, "AR queue capacity must be >= 2!")
  require(awQueueCapacity >= 2, "AW queue capacity must be >= 2!")

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
        new AddressStrobeGenerator(wAddr, wData)
      )

    val lenQueue = elastic.Queue(UInt(axiCfg.wLen.W), arQueueCapacity)

    val arReplicated = Wire(chiselTypeOf(s_axi.ar))

    def implAR(): Unit = prefix("ar") {
      new elastic.Fork(s_axi.ar) {
        new elastic.Transform(fork(), addressStrobeGenerator.source) {
          out.addr := in.addr
          out.len := in.len
          out.size := in.size
          out.burst := in.burst
        }

        new elastic.Replicate(fork(), arReplicated) {
          len := in.len +& 1.U
          out := in
        }

        fork(in.len) :=> lenQueue.source
      }

      new elastic.Join(m_axi.ar) {
        val pkt0 = join(arReplicated)
        val pkt1 = join(addressStrobeGenerator.sink)

        out.id := pkt0.id
        out.addr := pkt1.addr
        out.len := 0.U
        out.size := pkt1.size
        out.burst := axi4.BurstType.INCR
        out.lock := pkt0.lock
        out.cache := pkt0.cache
        out.prot := pkt0.prot
        out.qos := pkt0.qos
        out.region := pkt0.region
        out.user := pkt0.user
      }
    }

    def implR(): Unit = prefix("r") {
      val lastReplicated = Wire(elastic.Interface(Bool()))

      new elastic.Replicate(lenQueue.sink, lastReplicated) {
        len := in +& 1.U
        out := last
      }

      new elastic.Join(s_axi.r) {
        val r = join(m_axi.r)
        val last = join(lastReplicated)

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
        new AddressStrobeGenerator(wAddr, wData)
      )

    val lenQueue = elastic.Queue(UInt(axiCfg.wLen.W), awQueueCapacity)

    val awReplicated = Wire(chiselTypeOf(s_axi.aw))

    def implAW(): Unit = {
      new elastic.Fork(s_axi.aw) {
        new elastic.Transform(fork(), addressStrobeGenerator.source) {
          out.addr := in.addr
          out.len := in.len
          out.size := in.size
          out.burst := in.burst

        }

        new elastic.Replicate(fork(), awReplicated) {
          len := in.len +& 1.U
          out := in

        }

        fork(in.len) :=> lenQueue.source
      }

      new elastic.Join(m_axi.aw) {
        val pkt0 = join(awReplicated)
        val pkt1 = join(addressStrobeGenerator.sink)

        out.id := pkt0.id
        out.addr := pkt1.addr
        out.len := 0.U
        out.size := pkt1.size
        out.burst := axi4.BurstType.INCR
        out.lock := pkt0.lock
        out.cache := pkt0.cache
        out.prot := pkt0.prot
        out.qos := pkt0.qos
        out.region := pkt0.region
        out.user := pkt0.user
      }
    }

    def implW(): Unit = {
      new elastic.Transform(s_axi.w, m_axi.w) {
        out := in
        out.last := true.B
      }
    }

    def implB(): Unit = {
      val lastReplicated = Wire(elastic.Interface(Bool()))
      new elastic.Replicate(lenQueue.sink, lastReplicated) {
        len := in +& 1.U
        out := last
      }

      val joined = elastic.Zip(m_axi.b, lastReplicated)

      val respReg = RegInit(0.U(2.W))

      new elastic.Arrival(joined, s_axi.b) {
        when(arrived) {
          out := in._1

          // we reduce on the largest value of response
          when(in._1.resp > respReg) {
            respReg := in._1.resp
            out.resp := in._1.resp
          }.otherwise {
            out.resp := respReg
          }

          when(in._2 /* last */ ) {
            accept()

            respReg := 0.U
          }.otherwise {
            drop()
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
