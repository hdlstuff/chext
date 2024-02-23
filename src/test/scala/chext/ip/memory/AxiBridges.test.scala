package chext.ip.memory

import chisel3._
import chisel3.util._

import chiseltest._

import chext.axi4

import axi4.Ops._
import axi4.full.test.PacketUtils._

class Axi4FullTestModule extends Module {
  val wAddr = 8
  val wData = 32

  private val axiCfg = axi4.Config(wId = 4, wAddr = wAddr, wData = wData)
  private val memCfg = MemConfig(
    wAddr = wAddr,
    wData = wData,
    latencyRead = 4,
    latencyWrite = 2,
    numOutstandingRead = 4,
    numOutstandingWrite = 4
  )

  val s_axi = IO(axi4.full.Slave(axiCfg))
  val s_axi_ = axi4.full.SlaveBuffer(s_axi, axi4.BufferConfig.all(1))

  private val memory = Module(new SinglePortRAM(memCfg))
  private val axi4fullBridge = Module(new Axi4FullToReadWriteBridge(axiCfg))

  s_axi_ :=> axi4fullBridge.s_axi

  // TODO: change <> with a better operator
  axi4fullBridge.read <> memory.read
  axi4fullBridge.write <> memory.write
}

class Axi4FullToReadWriteBridgeSpec extends chext.test.FreeSpec with chext.test.TestMixin {
  Target.setCurrent(chisel.Target)

  useVerilator()
  enableVcd()

  "chext.ip.memory.Axi4FullToReadWriteBridge.Basic1" in test(new Axi4FullTestModule) { dut =>
    {
      import axi4.full.test._

      dut.s_axi.initSlave()

      dut.s_axi.b.ready.poke(true.B)
      dut.s_axi.sendWriteAddress(AddressPacket(0, 0x0000, 3, 5, 1))
      dut.s_axi.sendWriteData(0x0ded_beef)
      dut.s_axi.sendWriteData(0x1ded_beef)
      dut.s_axi.sendWriteData(0x2ded_beef)
      dut.s_axi.sendWriteData(0x3ded_beef)

      step(10)
    }
  }
}
