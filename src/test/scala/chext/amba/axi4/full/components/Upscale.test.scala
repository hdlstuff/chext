package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.test
import chext.amba.axi4
import chext.elastic
import chext.ip.memory

import axi4.Ops._
import elastic.ConnectOp._

class UpscaleTestModule extends Module {
  // (2 ** 10) * 16B = 16 KB of memory (14 bits)
  private val rawMemCfg = memory.RawMemConfig(10, 128, 4, 4)
  private val portCfg = memory.PortConfig(8, 8)
  private val axiCfg128 = axi4.Config(0, 14, 128)
  private val axiCfg32 = axiCfg128.copy(wData = 32)

  val s_axi_w128 = IO(axi4.full.Slave(axiCfg128))
  val s_axi_w32 = IO(axi4.full.Slave(axiCfg32))

  private val mem = Module(
    new memory.TrueDualPortRAM(rawMemCfg, portCfg, portCfg)
  )

  private val axiBridge1 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg128))

  s_axi_w128 :=> axiBridge1.s_axi

  axiBridge1.read.req :=> mem.read1.req
  mem.read1.resp :=> axiBridge1.read.resp

  axiBridge1.write.req :=> mem.write1.req
  mem.write1.resp :=> axiBridge1.write.resp

  private val axiBridge2 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg128))

  axiBridge2.read.req :=> mem.read2.req
  mem.read2.resp :=> axiBridge2.read.resp

  axiBridge2.write.req :=> mem.write2.req
  mem.write2.resp :=> axiBridge2.write.resp

  private val upscaleCfg = axi4.full.components.UpscaleConfig(axiCfg32, 128)
  private val upscale = Module(new axi4.full.components.Upscale(upscaleCfg))
  s_axi_w32 :=> upscale.s_axi
  upscale.m_axi :=> axiBridge2.s_axi
}

class UpscaleTest extends test.FreeSpec with test.TestMixin {
  memory.Target.setCurrent(memory.chisel.Target)

  import chiseltest._
  import axi4.full.test.PacketUtils._
  import axi4.full.test._

  "Upscale basic" in test(new UpscaleTestModule)
    .withAnnotations(Seq(chiseltest.WriteVcdAnnotation)) { dut =>
      {
        dut.s_axi_w32.initSlave()
        dut.s_axi_w128.initSlave()

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

        fork {
          dut.s_axi_w32.sendReadAddress(AddressPacket(0, 0x0000, 15, 2 /* 4B */ ))
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
            dut.s_axi_w32.sendWriteData(
              WriteDataPacket(0x7000_0000 | (0x1000 * i + i), 0xf, i == 15)
            )
          }
        }.fork {
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
      }
    }

  "Upscale unaligned bursts" in test(new UpscaleTestModule)
    .withAnnotations(Seq(chiseltest.WriteVcdAnnotation)) { dut =>
      {
        dut.s_axi_w32.initSlave()
        dut.s_axi_w128.initSlave()

        fork {
          dut.s_axi_w32.sendWriteAddress(AddressPacket(0, 0x0006, 15, 2))
        }.fork {
          for (i <- (0 until 16)) {
            dut.s_axi_w32.sendWriteData(
              WriteDataPacket(0x7099_0f00 | (0x1000 * i + i), 0xf, i == 15)
            )
          }
        }.fork {
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

        fork {
          dut.s_axi_w32.sendReadAddress(AddressPacket(0, 0x0006, 15, 2 /* 4B */ ))
        }.fork {
          dut.s_axi_w32.receiveReadDataBurst().zipWithIndex.foreach {
            case (beat, index) => {
              println(f"beatIdx = $index, data = ${beat.data.toString(16)}")
            }
          }
        }.join()

        println("Read from narrow bus is complete.")

        fork {
          dut.s_axi_w32.sendWriteAddress(AddressPacket(0, 0x0003, 15, 2))
        }.fork {
          for (i <- (0 until 16)) {
            dut.s_axi_w32.sendWriteData(
              WriteDataPacket(0x7099_0f00 | (0x1000 * i + i), 0xf, i == 15)
            )
          }
        }.fork {
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

        fork {
          dut.s_axi_w32.sendReadAddress(AddressPacket(0, 0x0003, 15, 2 /* 4B */ ))
        }.fork {
          dut.s_axi_w32.receiveReadDataBurst().zipWithIndex.foreach {
            case (beat, index) => {
              println(f"beatIdx = $index, data = ${beat.data.toString(16)}")
            }
          }
        }.join()

        println("Read from narrow bus is complete.")
      }
    }

  "Upscale narrow bursts" in test(new UpscaleTestModule)
    .withAnnotations(Seq(chiseltest.WriteVcdAnnotation)) { dut =>
      {
        dut.s_axi_w32.initSlave()
        dut.s_axi_w128.initSlave()

        fork {
          dut.s_axi_w32.sendWriteAddress(AddressPacket(0, 0x0000, 15, 1 /* 2B */))
        }.fork {
          for (i <- (0 until 16)) {
            dut.s_axi_w32.sendWriteData(
              WriteDataPacket(0x7099_0f00 | (0x0100_0000 * i + 0x1000 * i + i), 0x3 << ((i % 2) * 2), i == 15)
            )
          }
        }.fork {
          dut.s_axi_w32.receiveWriteResponse()
        }.join()

        println("Write to narrow bus is complete.")

        fork {
          dut.s_axi_w128.sendReadAddress(AddressPacket(0, 0x0000, 1, 4))
        }.fork {
          dut.s_axi_w128.receiveReadDataBurst().zipWithIndex.foreach {
            case (beat, index) => {
              println(f"beatIdx = $index, data = ${beat.data.toString(16)}")
            }
          }
        }.join()

        println("Read complete.")

        fork {
          dut.s_axi_w32.sendReadAddress(AddressPacket(0, 0x0000, 15, 1 /* 2B */ ))
        }.fork {
          dut.s_axi_w32.receiveReadDataBurst().zipWithIndex.foreach {
            case (beat, index) => {
              println(f"beatIdx = $index, data = ${beat.data.toString(16)}")
            }
          }
        }.join()

        println("Read from narrow bus is complete.")
      }
    }

  "Upscale narrow unaligned bursts" in test(new UpscaleTestModule)
    .withAnnotations(Seq(chiseltest.WriteVcdAnnotation)) { dut =>
      {
        dut.s_axi_w32.initSlave()
        dut.s_axi_w128.initSlave()

        fork {
          dut.s_axi_w32.sendWriteAddress(AddressPacket(0, 0x0003, 15, 2 /* 2B */))
        }.fork {
          for (i <- (0 until 16)) {
            dut.s_axi_w32.sendWriteData(
              // + 1 because now things are reverted
              WriteDataPacket(0x7099_0f00 | (0x0100_0000 * i + 0x1000 * i + i), 0x3 << (((i + 1) % 2) * 2), i == 15)
            )
          }
        }.fork {
          dut.s_axi_w32.receiveWriteResponse()
        }.join()

        println("Write to narrow bus is complete.")

        fork {
          dut.s_axi_w128.sendReadAddress(AddressPacket(0, 0x0000, 1, 4))
        }.fork {
          dut.s_axi_w128.receiveReadDataBurst().zipWithIndex.foreach {
            case (beat, index) => {
              println(f"beatIdx = $index, data = ${beat.data.toString(16)}")
            }
          }
        }.join()

        println("Read complete.")

        fork {
          dut.s_axi_w32.sendReadAddress(AddressPacket(0, 0x0003, 15, 1 /* 2B */ ))
        }.fork {
          dut.s_axi_w32.receiveReadDataBurst().zipWithIndex.foreach {
            case (beat, index) => {
              println(f"beatIdx = $index, data = ${beat.data.toString(16)}")
            }
          }
        }.join()

        println("Read from narrow bus is complete.")
      }
    }
}

object EmitUpscaleTest extends App {
  memory.Target.setCurrent(memory.chisel.Target)
  emitVerilog(new UpscaleTestModule)
}
