package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

import chext.amba.axi4
import chext.{elastic2 => elastic}

import chext.util.BitOps._
import elastic.ConnectOp._
import axi4.Ops._

import helpers.{SteerLeft, SteerRight}

case class DownscaleConfig(
    val axiSlaveCfg: axi4.Config,
    val wDataMaster: Int,
    val readOffsetLastQueueLength: Int = 16,
    val writeOffsetLastQueueLength: Int = 16
) {
  require(axiSlaveCfg.wId == 0, "axiSlaveCfg.wId must be zero!")
  require(!axiSlaveCfg.lite, "axiSlaveCfg.lite must be false!")
  require(wDataMaster < axiSlaveCfg.wData, "wDataMaster must be < axiSlaveCfg.wData")

  require(wDataMaster >= 8)
  require(isPow2(wDataMaster))

  require(axiSlaveCfg.wUserR == 0, "User data is not supported on channel R.")

  require(
    axiSlaveCfg.wData / wDataMaster <=
      (if (axiSlaveCfg.axi3Compat) 16 else 256),
    "axiSlaveCfg.wData / wDataMaster must be less than 16 or 256 depending on the AXI version!"
  )

  val wDataSlave = axiSlaveCfg.wData
  val wStrobeMaster = wDataMaster / 8
  val wStrobeSlave = wDataSlave / 8
  val wOffset = log2Ceil(wDataSlave) - log2Ceil(wDataMaster)
  val wAddr = axiSlaveCfg.wAddr
  val axsizeMaxMaster = log2Ceil(wDataMaster >> 3)
  val axiMasterCfg = axiSlaveCfg.copy(wData = wDataMaster)
}

class Downscale(val cfg: DownscaleConfig) extends Module {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.full.Master(axiMasterCfg))

  private val genOffsetLast = chext.bundles.BundleN(UInt(wOffset.W), Bool())

  private def implRead(): Unit = prefix("read") {
    val addressGenerator = Module(new AddressGenerator(log2Ceil(wDataSlave >> 3)))
    val offsetLastQueue = elastic.Queue(genOffsetLast, readOffsetLastQueueLength)

    def implAR(): Unit = prefix("ar") {
      val arTransformed = Wire(chiselTypeOf(m_axi.ar))

      new elastic.Transform(s_axi.ar, arTransformed) {
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

      new elastic.Fork(arTransformed) {
        new elastic.Transform(fork(), addressGenerator.source) {
          out.addr := in.addr
          out.len := in.len
          out.size := in.size
          out.burst := axi4.BurstType.INCR
        }

        fork() :=> m_axi.ar
      }

      new elastic.Transform(addressGenerator.sink, offsetLastQueue.source) {
        out._1 := in.addr.dropLsbN(log2Ceil(wDataMaster >> 3))
        out._2 := in.last
      }
    }

    def implR(): Unit = prefix("r") {
      val zipped = elastic.Zip(m_axi.r, offsetLastQueue.sink)

      val dataReg = RegInit(0.U(axiSlaveCfg.wData.W))
      val respReg = RegInit(0.U(2.W))

      val steerLeft = Module(new SteerLeft(wDataMaster, wDataSlave))

      new elastic.Arrival(zipped, s_axi.r) {
        steerLeft.dataIn := in._1.data
        steerLeft.offsetIn := in._2._1 /* offset */

        when(arrived) {
          // we reduce on the largest value of response
          out.id := in._1.id

          when(in._1.resp > respReg) {
            respReg := in._1.resp
            out.resp := in._1.resp
          }.otherwise {
            out.resp := respReg
          }

          val outputData = dataReg | steerLeft.dataOut

          out.data := outputData
          dataReg := outputData

          out.last := true.B

          when(in._1.last) {
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
    val addressGenerator = Module(new AddressGenerator(log2Ceil(wDataSlave >> 3)))
    val offsetLastQueue = elastic.Queue(genOffsetLast, writeOffsetLastQueueLength)

    def implAW(): Unit = prefix("aw") {
      val awTransformed = Wire(chiselTypeOf(m_axi.aw))

      new elastic.Transform(s_axi.aw, awTransformed) {
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

      new elastic.Fork(awTransformed) {
        new elastic.Transform(fork(), addressGenerator.source) {
          out.addr := in.addr
          out.len := in.len
          out.size := in.size
          out.burst := axi4.BurstType.INCR
        }

        fork() :=> m_axi.aw
      }

      new elastic.Transform(addressGenerator.sink, offsetLastQueue.source) {
        out._1 := in.addr.dropLsbN(log2Ceil(wDataMaster >> 3))
        out._2 := in.last
      }
    }

    def implW(): Unit = prefix("w") {
      val offsetLastQueueDeq = offsetLastQueue.sink
      offsetLastQueueDeq.nodeq()

      val steerRight = Module(new SteerRight(wDataSlave, wDataMaster))
      val steerRightStrobe = Module(new SteerRight(wStrobeSlave, wStrobeMaster))

      new elastic.Arrival(s_axi.w, m_axi.w) {
        val offset = offsetLastQueueDeq.bits

        steerRight.dataIn := in.data
        steerRight.offsetIn := offset._1 /* offset */

        steerRightStrobe.dataIn := in.strb
        steerRightStrobe.offsetIn := offset._1 /* offset */

        when(arrived) {
          out.data := steerRight.dataOut
          out.strb := steerRightStrobe.dataOut

          out.last := offsetLastQueueDeq.bits._2 /* last */
          out.user := in.user

          when(offsetLastQueueDeq.valid) {
            offsetLastQueueDeq.deq()
            produce()

            when(offsetLastQueueDeq.bits._2 /* last */ ) {
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
