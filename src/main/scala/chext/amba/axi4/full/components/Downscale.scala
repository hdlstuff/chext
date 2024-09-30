package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

import chext.amba.axi4
import chext.elastic

import chext.util.BitOps._
import elastic.ConnectOp._
import axi4.Ops._

import helpers.{SteerLeft, SteerRight}

case class DownscaleConfig(
    val axiSlaveCfg: axi4.Config,
    val wDataMaster: Int,
    val readOffsetQueueLength: Int = 16,
    val writeAddressStrobeQueueLength: Int = 16
) {
  require(axiSlaveCfg.wId == 0, "axiSlaveCfg.wId must be zero!")
  require(!axiSlaveCfg.lite, "axiSlaveCfg.lite must be false!")
  require(wDataMaster < axiSlaveCfg.wData, "wDataMaster must be < axiSlaveCfg.wData")

  require(wDataMaster >= 8)
  require(isPow2(wDataMaster))

  require(axiSlaveCfg.wUserR == 0, "User data is not supported on channel R.")
  require(!axiSlaveCfg.axi3Compat, "Downscale cannot work in Axi3 compatibility mode!")

  val wDataSlave = axiSlaveCfg.wData
  val wStrobeMaster = wDataMaster / 8
  val wStrobeSlave = wDataSlave / 8
  val wAddr = axiSlaveCfg.wAddr
  val axsizeMaxMaster = log2Ceil(wDataMaster >> 3)
  val axiMasterCfg = axiSlaveCfg.copy(wData = wDataMaster)
}

class Downscale(val cfg: DownscaleConfig) extends Module {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.full.Master(axiMasterCfg))

  private def implRead(): Unit = prefix("read") {
    val offsetGenerator = Module(new OffsetGenerator(wDataSlave, wDataMaster))

    val offsetQueue = Module(new Queue(offsetGenerator.genSink, readOffsetQueueLength))

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
            out.len := (1.U << (in.size - axsizeMaxMaster.U)) - 1.U
          }
        }
      }

      new elastic.Fork(arTransformed) {
        override protected def onFork: Unit = {
          new elastic.Transform(fork(), offsetGenerator.source) {
            override protected def onTransform: Unit = {
              out.addr := in.addr
              out.len := in.len
              out.fixed := in.burst === axi4.BurstType.FIXED
            }
          }

          fork() :=> m_axi.ar
        }
      }

      offsetGenerator.sink :=> offsetQueue.io.enq
    }

    def implR(): Unit = prefix("r") {
      val zipped = elastic.Zip(m_axi.r, offsetQueue.io.deq)

      val dataReg = RegInit(0.U(axiSlaveCfg.wData.W))
      val respReg = RegInit(0.U(2.W))

      val steerLeftData = Module(new SteerLeft(wDataMaster, wDataSlave))

      new elastic.Arrival(zipped, s_axi.r) {
        steerLeftData.dataIn := in._1.data
        steerLeftData.offsetIn := in._2.offset

        protected def onArrival: Unit = {
          // we reduce on the largest value of response
          out.id := in._1.id

          when(in._1.resp > respReg) {
            respReg := in._1.resp
            out.resp := in._1.resp
          }.otherwise {
            out.resp := respReg
          }

          val outputData = dataReg | steerLeftData.dataOut

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
          readOffsetQueueLength
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

      val steerRightData = Module(new SteerRight(wDataSlave, wDataMaster))
      val steerRightStrobe = Module(new SteerRight(wStrobeSlave, wStrobeMaster))

      new elastic.Arrival(s_axi.w, m_axi.w) {
        val offset = addressStrobeDeq.bits.lowerByteIndex.dropLsbN(log2Ceil(wStrobeMaster))

        steerRightData.dataIn := in.data
        steerRightData.offsetIn := offset

        steerRightStrobe.dataIn := in.strb & addressStrobeDeq.bits.strb
        steerRightStrobe.offsetIn := offset

        protected def onArrival: Unit = {
          out.data := steerRightData.dataOut
          out.strb := steerRightStrobe.dataOut

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

  if (axiSlaveCfg.read) implRead()
  if (axiSlaveCfg.write) implWrite()
}
