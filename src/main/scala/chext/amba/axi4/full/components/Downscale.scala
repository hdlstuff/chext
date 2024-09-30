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
    val readOffsetLastQueueLength: Int = 16,
    val writeOffsetLastQueueLength: Int = 16
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

private class AddrLenBundle(val wAddr: Int) extends Bundle {
  val addr = UInt(wAddr.W)
  val len = UInt(5.W)
}

private class OffsetLastBundle(wOffset: Int) extends Bundle {
  val offset = UInt(wOffset.W)
  val last = Bool()
}

private class OffsetLastGenerator(wWide: Int, wNarrow: Int) extends Module {
  override def desiredName: String = f"OffsetLastGenerator_${wWide}_${wNarrow}"

  val wAddr = log2Ceil(wWide >> 3 /* to bytes */ )
  val wOffset = log2Ceil(wWide) - log2Ceil(wNarrow)

  val genSource = new AddrLenBundle(wAddr)
  val genSink = new OffsetLastBundle(wOffset)

  val source = IO(elastic.Source(Irrevocable(genSource)))
  val sink = IO(elastic.Sink(Irrevocable(genSink)))

  private val source_ = source
  private val sink_ = elastic.SinkBuffer(sink)

  private val current = source_.bits
  private val offset = Reg(UInt(wOffset.W))
  private val ctr = Reg(UInt(5.W))
  private val generating = RegInit(false.B)

  source_.nodeq()
  sink_.noenq()

  when(source_.valid && sink_.ready) {
    when(generating) {
      val last = ctr === 0.U

      when(last) {
        generating := false.B
        source_.deq()
      }.otherwise {
        ctr := ctr - 1.U
        offset := offset + 1.U
      }

      // NOTE: the logic on `bits` might not depend on `source_.valid && sink_.ready`
      // Does your synthesis tool can figure out that optimization?
      // Maybe, in the future, write data and control logic separately
      sink_.enq {
        val result = Wire(genSink)

        result.offset := offset
        result.last := last

        result
      }
    }.otherwise {
      val last = current.len === 0.U
      val thisOffset = current.addr.dropLsbN(log2Ceil(wNarrow >> 3 /* to bytes */ ))

      when(last) {
        source_.deq()
      }.otherwise {
        generating := true.B
        offset := thisOffset + 1.U
        ctr := current.len - 1.U
      }

      sink_.enq {
        val result = Wire(genSink)

        result.offset := thisOffset
        result.last := last

        result
      }
    }
  }
}

class Downscale(val cfg: DownscaleConfig) extends Module {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.full.Master(axiMasterCfg))

  private def implRead(): Unit = prefix("read") {
    val offsetLastGenerator = Module(new OffsetLastGenerator(wDataSlave, wDataMaster))
    val offsetLastQueue = Module(new Queue(offsetLastGenerator.genSink, readOffsetLastQueueLength))

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
          new elastic.Transform(fork(), offsetLastGenerator.source) {
            override protected def onTransform: Unit = {
              out.addr := in.addr
              out.len := in.len
            }
          }

          fork() :=> m_axi.ar
        }
      }

      offsetLastGenerator.sink :=> offsetLastQueue.io.enq
    }

    def implR(): Unit = prefix("r") {
      val zipped = elastic.Zip(m_axi.r, offsetLastQueue.io.deq)

      val dataReg = RegInit(0.U(axiSlaveCfg.wData.W))
      val respReg = RegInit(0.U(2.W))

      val steerLeft = Module(new SteerLeft(wDataMaster, wDataSlave))

      new elastic.Arrival(zipped, s_axi.r) {
        steerLeft.dataIn := in._1.data
        steerLeft.offsetIn := in._2.offset

        protected def onArrival: Unit = {
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
    val offsetLastGenerator = Module(new OffsetLastGenerator(wDataSlave, wDataMaster))
    val offsetLastQueue = Module(new Queue(offsetLastGenerator.genSink, writeOffsetLastQueueLength))

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
          new elastic.Transform(fork(), offsetLastGenerator.source) {
            override protected def onTransform: Unit = {
              out.addr := in.addr
              out.len := in.len
            }
          }

          fork() :=> m_axi.aw
        }
      }

      offsetLastGenerator.sink :=> offsetLastQueue.io.enq
    }

    def implW(): Unit = prefix("w") {
      val offsetLastQueueDeq = offsetLastQueue.io.deq
      offsetLastQueueDeq.nodeq()

      val steerRight = Module(new SteerRight(wDataSlave, wDataMaster))
      val steerRightStrobe = Module(new SteerRight(wStrobeSlave, wStrobeMaster))

      new elastic.Arrival(s_axi.w, m_axi.w) {
        val offset = offsetLastQueueDeq.bits

        steerRight.dataIn := in.data
        steerRight.offsetIn := offset.offset

        steerRightStrobe.dataIn := in.strb
        steerRightStrobe.offsetIn := offset.offset

        protected def onArrival: Unit = {
          out.data := steerRight.dataOut
          out.strb := steerRightStrobe.dataOut

          out.last := offsetLastQueueDeq.bits.last
          out.user := in.user

          when(offsetLastQueueDeq.valid) {
            offsetLastQueueDeq.deq()
            produce()

            when(offsetLastQueueDeq.bits.last) {
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
