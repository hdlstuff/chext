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

class DownscaleTestModule extends Module {
  // (2 ** 12) * 4B = 16 KB of memory (14 bits)
  private val rawMemCfg = memory.RawMemConfig(12, 32, 4, 4)
  private val portCfg = memory.PortConfig(8, 8)
  private val axiCfg128 = axi4.Config(0, 14, 128)
  private val axiCfg32 = axiCfg128.copy(wData = 32)

  val s_axi_w128 = IO(axi4.full.Slave(axiCfg128))
  val s_axi_w32 = IO(axi4.full.Slave(axiCfg32))

  private val mem = Module(
    new memory.TrueDualPortRAM(rawMemCfg, portCfg, portCfg)
  )

  private val axiBridge1 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg32))

  s_axi_w32 :=> axiBridge1.s_axi

  axiBridge1.read.req :=> mem.read1.req
  mem.read1.resp :=> axiBridge1.read.resp

  axiBridge1.write.req :=> mem.write1.req
  mem.write1.resp :=> axiBridge1.write.resp

  private val axiBridge2 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg32))

  axiBridge2.read.req :=> mem.read2.req
  mem.read2.resp :=> axiBridge2.read.resp

  axiBridge2.write.req :=> mem.write2.req
  mem.write2.resp :=> axiBridge2.write.resp

  private val downscaleCfg = axi4.full.components.DownscaleConfig(axiCfg128, 32)
  private val downscale = Module(new axi4.full.components.Downscale(downscaleCfg))
  s_axi_w128 :=> downscale.s_axi
  downscale.m_axi :=> axiBridge2.s_axi
}

class DownscaleTest extends test.FreeSpec with test.TestMixin {
  memory.Target.setCurrent(memory.chisel.Target)

  "Downscale Basic Functionality" in test(new DownscaleTestModule)
    .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      {
        import chiseltest._
        import axi4.full.test.PacketUtils._
        import axi4.full.test._

        dut.s_axi_w32.initSlave()
        dut.s_axi_w128.initSlave()
        /*
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
          dut.s_axi_w128.sendWriteAddress(AddressPacket(0, 0x0000, 3, 4))
        }.fork {
          dut.s_axi_w128.sendWriteData(
            writeDataPacket(0x00fe1000, 0x00fe0100, 0x00fe0010, 0x00fe0001, false)
          )
          dut.s_axi_w128.sendWriteData(
            writeDataPacket(0x01fe1000, 0x01fe0100, 0x01fe0010, 0x01fe0001, false)
          )
          dut.s_axi_w128.sendWriteData(
            writeDataPacket(0x02fe1000, 0x02fe0100, 0x02fe0010, 0x02fe0001, false)
          )
          dut.s_axi_w128.sendWriteData(
            writeDataPacket(0x03fe1000, 0x03fe0100, 0x03fe0010, 0x03fe0001, true)
          )
        }.fork {
          dut.s_axi_w128.receiveWriteResponse()
        }.join()

        println("Write complete.")
         */

        fork {
          dut.s_axi_w128.sendReadAddress(AddressPacket(0, 0x0000, 0, 4))
        }.fork {
          dut.s_axi_w128.receiveReadDataBurst().zipWithIndex.foreach {
            case (beat, index) => {
              println(f"beatIdx = $index, data = ${beat.data.toString(16)}")
            }
          }
        }.join()

        println("Read complete.")
        /*
        fork {
          dut.s_axi_w32.sendReadAddress(AddressPacket(0, 0x0000, 15, 2 /* 4B */))
        }.fork {
          dut.s_axi_w32.receiveReadDataBurst().zipWithIndex.foreach {
            case (beat, index) => {
              println(f"beatIdx = $index, data = ${beat.data.toString(16)}")
            }
          }
        }.join()

        println("Read from narrow bus is complete.")

        fork {
          dut.s_axi_w32.sendWriteAddress(AddressPacket(0, 0x0000, 15, 2))
        }.fork {
          for (i <- (0 until 16)) {
            dut.s_axi_w32.sendWriteData(WriteDataPacket(0x7000_0000 | (0x1000 * i + i), 0xF, i == 15))
          }
        }.fork{
          dut.s_axi_w32.receiveWriteResponse()
        }.join()

        println("Write to narrow bus is complete.")

        fork {
          dut.s_axi_w128.sendReadAddress(AddressPacket(0, 0x0000, 3, 4))
        }.fork {
          dut.s_axi_w128.receiveReadDataBurst().zipWithIndex.foreach {
            case (beat, index) => {
              println(f"beatIdx = $index, data = ${beat.data.toString(16)}")
            }
          }
        }.join()

        println("Read complete.")
        */
      }
    }
}

object EmitDownscaleTest extends App {
  memory.Target.setCurrent(memory.chisel.Target)
  emitVerilog(new DownscaleTestModule)
}
