package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

import chext.amba.axi4
import chext.elastic

import elastic.ConnectOp._
import axi4.Ops._
import chext.util.BitOps._

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

  val wDataSlave = axiCfgSlave.wData
  val wAddr = axiCfgSlave.wAddr
  val axsizeMaxMaster = log2Ceil(wDataMaster >> 3)
  val axiCfgMaster = axiCfgSlave.copy(wData = wDataMaster)
}

class Downscale(val cfg: DownscaleConfig) extends Module with DataWidthConverterLike {
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

      val dataReg = RegInit(0.U(axiCfgSlave.wData.W))
      val respReg = RegInit(0.U(2.W))

      new elastic.Arrival(zipped, s_axi.r) {
        protected def onArrival: Unit = {
          // we reduce on the largest value of response

          out.id := in._1.id

          when(in._1.resp > respReg) {
            respReg := in._1.resp
            out.resp := in._1.resp
          }.otherwise {
            out.resp := respReg
          }

          // TODO: Use a better line steering module
          val shiftBytes = in._2.lowerByteIndex.resetLastN(log2Ceil(wDataMaster / 8))
          val outputData = dataReg | (in._1.data << (shiftBytes << 3))

          out.data := outputData
          dataReg := outputData

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

    def implAW(): Unit = prefix("aw") {
      val awTransformed = Wire(chiselTypeOf(m_axi.aw))

      new elastic.Transform(s_axi.aw, awTransformed) {
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

      new elastic.Fork(awTransformed) {
        override protected def onFork: Unit = {
          new elastic.Transform(fork(), addressStrobeGenerator.source) {
            override protected def onTransform: Unit = {
              out.addr := in.addr
              out.len := in.len
              out.size := in.size
              out.burst := in.burst
            }
          }

          fork() :=> m_axi.aw
        }
      }

      addressStrobeGenerator.sink :=> addressStrobeQueue.io.enq
    }

    def implW(): Unit = prefix("w") {
      val addressStrobeDeq = addressStrobeQueue.io.deq
      addressStrobeDeq.nodeq()

      new elastic.Arrival(s_axi.w, m_axi.w) {
        protected def onArrival: Unit = {
          val shiftBytes =
            addressStrobeDeq.bits.lowerByteIndex.resetLastN(log2Ceil(wDataMaster / 8))

          // TODO: Lane steering module
          out.data := (in.data >> (shiftBytes << 3))
          out.strb := ((in.strb & addressStrobeDeq.bits.strb) >> shiftBytes)

          out.last := addressStrobeDeq.bits.last
          out.user := in.user

          when(addressStrobeDeq.valid) {
            addressStrobeDeq.deq()
            produce()

            when(addressStrobeDeq.bits.last) {
              consume()
            }
          }
        }
      }
    }

    def implB(): Unit = {
      m_axi.b :=> s_axi.b
    }

    implAW()
    implW()
    implB()
  }

  if (axiCfgSlave.read) implRead()
  if (axiCfgSlave.write) implWrite()
}
