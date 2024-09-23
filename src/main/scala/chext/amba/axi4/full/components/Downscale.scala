package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

import chext.amba.axi4
import chext.elastic
import elastic.ConnectOp._
import axi4.Ops._

import axi4.full.components.addrgen

case class DownscaleConfig(
    val axiCfgSlave: axi4.Config,
    val wDataMaster: Int,
    val readAddressStrobeQueueLength: Int = 16,
    val writeAddressStrobeQueueLength: Int = 16
) {
  require(axiCfgSlave.wId == 0, "axiCfgSlave.wId must be zero!")
  require(!axiCfgSlave.lite, "axiCfgSlave.lite must be false!")
  require(wDataMaster < axiCfgSlave.wData, "wDataMaster must be < axiCfgSlave.wData")

  require(wDataMaster >= 8)
  require(isPow2(wDataMaster))

  require(axiCfgSlave.wUserR == 0, "user data is not supported on channel R.")
  require(axiCfgSlave.wUserB == 0, "user data is not supported on channel B.")

  val wDataSlave = axiCfgSlave.wData
  val wAddr = axiCfgSlave.wAddr
  val axsizeMaxMaster = log2Ceil(wDataMaster >> 3)
  val axiCfgMaster = axiCfgSlave.copy(wData = wDataMaster)
}

class Downscale(val cfg: DownscaleConfig) extends Module {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiCfgSlave))
  val m_axi = IO(axi4.full.Master(axiCfgMaster))

  private def implRead(): Unit = prefix("read") {
    val addressStrobeGenerator =
      Module(
        new addrgen.AddressStrobeGenerator(wAddr, wDataSlave)
      )

    val addressStrobeQueue =
      Module(
        new Queue(
          addressStrobeGenerator.genOutput,
          readAddressStrobeQueueLength
        )
      )

    def implAR(): Unit = prefix("ar") {
      val arTransformed = Wire(chiselTypeOf(m_axi.ar))

      new elastic.Transform(s_axi.ar, arTransformed) {
        protected def onTransform: Unit = {
          out := in

          out.burst := axi4.BurstType.INCR

          when(in.size <= axsizeMaxMaster.U) {
            out.size := in.size
            out.len := 0.U
          }.otherwise {
            out.size := axsizeMaxMaster.U

            // TODO: optimize this calculation
            out.len := (1.U << (in.size - axsizeMaxMaster.U)) - 1.U
          }
        }
      }

      new elastic.Fork(arTransformed) {
        override protected def onFork: Unit = {
          new elastic.Transform(fork(), addressStrobeGenerator.source) {
            override protected def onTransform: Unit = {
              out.addr := in.addr
              out.len := in.len
              out.size := in.size
              out.burst := in.burst
            }
          }

          fork() :=> m_axi.ar
        }
      }

      addressStrobeGenerator.sink :=> addressStrobeQueue.io.enq
    }

    def implR(): Unit = prefix("r") {
      val zipped = elastic.Zip(m_axi.r, addressStrobeQueue.io.deq)

      new elastic.Arrival(zipped, s_axi.r) {
        protected def onArrival: Unit = {
          val dataReg = RegInit(0.U(axiCfgSlave.wData.W))
          val respReg = RegInit(0.U(2.W))

          // we reduce on the largest value of response

          out.id := in._1.id
          out.data := dataReg

          when(in._1.resp > respReg) {
            respReg := in._1.resp
            out.resp := in._1.resp
          }.otherwise {
            out.resp := respReg
          }

          // do the lane steering in a better way
          out.data := dataReg | (in._1.data << (in._2.lowerByteIndex << 3))

          out.last := true.B

          when(in._2.last) {
            dataReg := 0.U

            consume()
            produce()
          }.otherwise {
            consume()
          }
        }
      }
    }

    implAR()
    implR()
  }

  private def implWrite(): Unit = prefix("write") {

    println("warning: fix these:")

    s_axi.aw.nodeq()
    m_axi.aw.noenq()

    s_axi.w.nodeq()
    m_axi.w.noenq()

    m_axi.b.nodeq()
    s_axi.b.noenq()
  }

  if (axiCfgSlave.read) implRead()
  if (axiCfgSlave.write) implWrite()
}
