package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.amba.axi4
import chext.elastic

import chext.ip.memory._

import elastic.ConnectOp._
import axi4.Ops._

case class ParallelizeTopConfig() {
  val axiCfg = axi4.Config(wId = 0, wAddr = 14, wData = 64)

  val rawMemCfg = RawMemConfig(
    axiCfg.wAddr - log2Ceil(axiCfg.wData) + 3,
    axiCfg.wData
  )

  val portCfg = PortConfig(16, 16)

  val axiMemCfg = axiCfg.copy(wId = 4)
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

  val parallelize = Module(new Parallelize(ParallelizeConfig(axiSlaveCfg = axiCfg, wIdMaster = 4)))

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
        dut.s_axi_0.sendWriteAddress(AddressPacket(0, 0x000, 3, 3, 1))
      }.fork {
        dut.s_axi_0.sendWriteData(WriteDataPacket(0x0fff_0000_aaaa_0000L, 0xff, false))
        dut.s_axi_0.sendWriteData(WriteDataPacket(0x0fff_1111_aaaa_1000L, 0xff, false))
        dut.s_axi_0.sendWriteData(WriteDataPacket(0x0fff_2222_aaaa_2000L, 0xff, false))
        dut.s_axi_0.sendWriteData(WriteDataPacket(0x0fff_3333_aaaa_3000L, 0xff, true))
      }.fork {
        println(dut.s_axi_0.receiveWriteResponse())
      }.joinAndStep()

      for (idx <- (0 until 4)) {
        dut.s_axi_1.sendReadAddress(AddressPacket(0, 0x000 + 8 * idx, 0, 3, 1))
      }

      dut.s_axi_1.r.ready.poke(true)

      dut.clock.step(30)
    }
  }
}
