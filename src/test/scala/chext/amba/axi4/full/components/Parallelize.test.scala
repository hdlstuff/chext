package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.amba.axi4
import chext.elastic

import chext.ip.memory._

import elastic.ConnectOp._
import axi4.Ops._

case class ParallelizeTopConfig(val wId: Int = 4) {
  val axiCfg = axi4.Config(wId = 0, wAddr = 14, wData = 64)

  val rawMemCfg = RawMemConfig(
    axiCfg.wAddr - log2Ceil(axiCfg.wData) + 3,
    axiCfg.wData
  )

  val portCfg = PortConfig(256, 256)

  val axiMemCfg = axiCfg.copy(wId = wId)
}

class ParallelizeTop(cfg: ParallelizeTopConfig = ParallelizeTopConfig()) extends Module {
  import cfg._

  val s_axi_0 = IO(axi4.full.Slave(axiCfg))
  val s_axi_1 = IO(axi4.full.Slave(axiCfg))

  private val mem = Module(new TrueDualPortRAM(rawMemCfg, portCfg, portCfg))

  private val bridge1 = Module(new Axi4FullToReadWriteBridge(axiMemCfg))
  private val bridge2 = Module(new Axi4FullToReadWriteBridge(axiMemCfg))

  bridge1.read.req :=> mem.read2.req
  mem.read2.resp :=> bridge1.read.resp

  bridge1.write.req :=> mem.write1.req
  mem.write1.resp :=> bridge1.write.resp

  bridge2.read.req :=> mem.read1.req
  mem.read1.resp :=> bridge2.read.resp

  bridge2.write.req :=> mem.write2.req
  mem.write2.resp :=> bridge2.write.resp

  s_axi_0 :=> bridge1.s_axi

  val parallelize = Module(
    new Parallelize(ParallelizeConfig(axiSlaveCfg = axiCfg, wIdMaster = cfg.wId))
  )

  s_axi_1 :=> parallelize.s_axi
  parallelize.m_axi :=> bridge2.s_axi
}

class ParallelizeTest extends chext.test.FreeSpec with chext.test.TestMixin {
  enableVcd()

  "parallelize basic" in test(new ParallelizeTop) { dut =>
    {
      import axi4.full.test._
      import axi4.full.test.PacketUtils._
      import chiseltest._

      dut.s_axi_0.initSlave()
      dut.s_axi_1.initSlave()

      fork {
        dut.s_axi_0.sendWriteAddress(AddressPacket(0, 0x000, 255, 3, 1))
        println(f"[AW]")
      }.fork {
        for (idx <- (0 until 256)) {
          println(f"[W] idx = ${idx}%03d")
          dut.s_axi_0.sendWriteData(
            WriteDataPacket(0x0fff_0000_aaaa_0000L + (idx << 8) + (idx), 0xff, idx == 255)
          )
        }
      }.fork {
        println(f"[W] ${dut.s_axi_0.receiveWriteResponse()}")
      }.joinAndStep()

      fork {
        for (idx <- (0 until 256)) {
          println(f"[AR] idx = ${idx}%03d")
          dut.s_axi_1.sendReadAddress(AddressPacket(0, 0x000 + 8 * idx, 0, 3, 1))
        }
      }.fork {
        for (idx <- (0 until 256)) {
          val readData = dut.s_axi_1.receiveReadData()
          println(f"[R] idx = ${idx}%03d, data = ${readData.data}%016x")
        }
      }.joinAndStep()

    }
  }
}
