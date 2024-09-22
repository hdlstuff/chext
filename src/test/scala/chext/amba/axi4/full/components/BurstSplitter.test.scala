package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.test
import chext.amba.axi4
import chext.elastic
import chext.ip.memory

import axi4.Ops._
import elastic.ConnectOp._
import chiseltest.simulator.WriteVcdAnnotation

class BurstSplitterTestModule extends Module {
  // (2 ** 10) * 16B = 16 KB of memory (14 bits)
  private val rawMemCfg = memory.RawMemConfig(10, 128, 1, 1)
  private val portCfg = memory.PortConfig(8, 8, () => new memory.BasicReadWriteArbiter(32))
  private val axiCfg = axi4.Config(0, 14, 128)
  
  val s_axi_normal = IO(axi4.full.Slave(axiCfg))
  val s_axi_split = IO(axi4.full.Slave(axiCfg))

  private val mem = Module(
    new memory.TrueDualPortRAM(rawMemCfg, portCfg, portCfg)
  )

  private val axiBridge1 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg))

  s_axi_normal :=> axiBridge1.s_axi

  axiBridge1.read.req :=> mem.read1.req
  mem.read1.resp :=> axiBridge1.read.resp

  axiBridge1.write.req :=> mem.write1.req
  mem.write1.resp :=> axiBridge1.write.resp

  private val axiBridge2 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg))

  axiBridge2.read.req :=> mem.read2.req
  mem.read2.resp :=> axiBridge2.read.resp

  axiBridge2.write.req :=> mem.write2.req
  mem.write2.resp :=> axiBridge2.write.resp

  private val burstSplitterCfg = axi4.full.components.BurstSplitterConfig(axiCfg)
  private val burstSplitter = Module(new axi4.full.components.BurstSplitter(burstSplitterCfg))
  s_axi_split :=> burstSplitter.s_axi
  burstSplitter.m_axi :=> axiBridge2.s_axi
}

class BurstSplitterTest extends test.FreeSpec with test.TestMixin {
  memory.Target.setCurrent(memory.chisel.Target)

  "BurstSplitter Basic Functionality" in test(new BurstSplitterTestModule)
    .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      {
        import chiseltest._
        import axi4.full.test.PacketUtils._
        import axi4.full.test._

        dut.s_axi_normal.initSlave()
        dut.s_axi_split.initSlave()

        def writeDataPacket(
            u32_3: BigInt,
            u32_2: BigInt,
            u32_1: BigInt,
            u32_0: BigInt,
            last: Boolean
        ) = {
          WriteDataPacket((u32_3 << 96) | (u32_2 << 64) | (u32_1 << 32) | u32_0, 0xffff, last)
        }

        fork {
          dut.s_axi_normal.sendWriteAddress(AddressPacket(0, 0x0000, 7, 4))
        }.fork {
          dut.s_axi_normal.sendWriteData(
            writeDataPacket(0x00fe1000, 0x00fe0100, 0x00fe0010, 0x00fe0001, false)
          )
          dut.s_axi_normal.sendWriteData(
            writeDataPacket(0x01fe1000, 0x01fe0100, 0x01fe0010, 0x01fe0001, false)
          )
          dut.s_axi_normal.sendWriteData(
            writeDataPacket(0x02fe1000, 0x02fe0100, 0x02fe0010, 0x02fe0001, false)
          )
          dut.s_axi_normal.sendWriteData(
            writeDataPacket(0x03fe1000, 0x03fe0100, 0x03fe0010, 0x03fe0001, false)
          )
          dut.s_axi_normal.sendWriteData(
            writeDataPacket(0x04fe1000, 0x04fe0100, 0x04fe0010, 0x04fe0001, false)
          )
          dut.s_axi_normal.sendWriteData(
            writeDataPacket(0x05fe1000, 0x05fe0100, 0x05fe0010, 0x05fe0001, false)
          )
          dut.s_axi_normal.sendWriteData(
            writeDataPacket(0x06fe1000, 0x06fe0100, 0x06fe0010, 0x06fe0001, false)
          )
          dut.s_axi_normal.sendWriteData(
            writeDataPacket(0x07fe1000, 0x07fe0100, 0x07fe0010, 0x07fe0001, true)
          )
        }.fork {
          dut.s_axi_normal.receiveWriteResponse()
        }.join()

        println("Write complete.")

        fork {
          dut.s_axi_normal.sendReadAddress(AddressPacket(0, 0x0000, 7, 4))
        }.fork {
          dut.s_axi_normal.receiveReadDataBurst().zipWithIndex.foreach {
            case (beat, index) => {
              println(f"beatIdx = $index, data = ${beat.data.toString(16)}")
            }
          }
        }.join()

        println("Read complete.")

        fork {
          dut.s_axi_split.sendReadAddress(AddressPacket(0, 0x0000, 7, 4))
        }.fork {
          dut.s_axi_split.receiveReadDataBurst().zipWithIndex.foreach {
            case (beat, index) => {
              println(f"beatIdx = $index, data = ${beat.data.toString(16)}")
            }
          }
        }.join()

        println("Read from split bus is complete.")

        fork {
          dut.s_axi_split.sendWriteAddress(AddressPacket(0, 0x0000, 7, 4))
        }.fork {
          dut.s_axi_split.sendWriteData(
            writeDataPacket(0x70fe1000, 0x70fe0100, 0x70fe0010, 0x70fe0001, false)
          )
          dut.s_axi_split.sendWriteData(
            writeDataPacket(0x71fe1000, 0x71fe0100, 0x71fe0010, 0x71fe0001, false)
          )
          dut.s_axi_split.sendWriteData(
            writeDataPacket(0x72fe1000, 0x72fe0100, 0x72fe0010, 0x72fe0001, false)
          )
          dut.s_axi_split.sendWriteData(
            writeDataPacket(0x73fe1000, 0x73fe0100, 0x73fe0010, 0x73fe0001, false)
          )
          dut.s_axi_split.sendWriteData(
            writeDataPacket(0x74fe1000, 0x74fe0100, 0x74fe0010, 0x74fe0001, false)
          )
          dut.s_axi_split.sendWriteData(
            writeDataPacket(0x75fe1000, 0x75fe0100, 0x75fe0010, 0x75fe0001, false)
          )
          dut.s_axi_split.sendWriteData(
            writeDataPacket(0x76fe1000, 0x76fe0100, 0x76fe0010, 0x76fe0001, false)
          )
          dut.s_axi_split.sendWriteData(
            writeDataPacket(0x77fe1000, 0x77fe0100, 0x77fe0010, 0x77fe0001, true)
          )
        }.fork{
          dut.s_axi_split.receiveWriteResponse()
        }.join()

        println("Write to split bus is complete.")

        fork {
          dut.s_axi_normal.sendReadAddress(AddressPacket(0, 0x0000, 7, 4))
        }.fork {
          dut.s_axi_normal.receiveReadDataBurst().zipWithIndex.foreach {
            case (beat, index) => {
              println(f"beatIdx = $index, data = ${beat.data.toString(16)}")
            }
          }
        }.join()

        println("Read complete.")
      }
    }
}
