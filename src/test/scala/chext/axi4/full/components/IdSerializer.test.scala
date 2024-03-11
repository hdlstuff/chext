package chext.axi4.full.components

import chisel3._
import chisel3.util._
import chiseltest._

import chext.elastic
import chext.axi4

import axi4.Ops._
import axi4.full.test.PacketUtils._

class IdSerializerZeroDut extends Module {
  val axiCfg = axi4.Config(
    wId = 4,
    wAddr = 8,
    wData = 32
  )

  val s_axi = IO(Vec(4, axi4.full.Slave(axiCfg)))

  private val mux = Module(
    new axi4.full.components.Mux(
      axiCfg,
      4
    )
  )
  s_axi.zip(mux.s_axi).foreach { case (master, slave) =>
    master :=> slave
  }

  private val idSerializer = Module(new IdSerializerZero(mux.axiCfgMaster))
  mux.m_axi :=> idSerializer.s_axi

  private val memBridge = Module(
    new chext.ip.memory.Axi4FullToReadWriteBridge(idSerializer.axiMasterCfg)
  )
  idSerializer.m_axi :=> memBridge.s_axi

  private val rawMemCfg = chext.ip.memory.RawMemConfig(
    wAddr = memBridge.cfg.wAddr,
    wData = memBridge.cfg.wData,
    latencyRead = 2,
    latencyWrite = 1
  )

  private val portCfg = chext.ip.memory.PortConfig(numOutstandingRead = 4, numOutstandingWrite = 4)

  private val mem = Module(new chext.ip.memory.SimpleDualPortRAM(rawMemCfg, portCfg))
  memBridge.read <> mem.read
  memBridge.write <> mem.write

}

class IdSerializerZeroSpec extends chext.test.FreeSpec with chext.test.TestMixin {
  enableVcd()

  chext.ip.memory.Target.setCurrent(chext.ip.memory.chisel.Target)

  "AXI4 Full IdSerializerZero Basic Functionality" in test(new IdSerializerZeroDut) { dut =>
    {
      import axi4.full.test._

      val s_axi = dut.s_axi
      s_axi.foreach { _.initSlave() }

      fork {
        s_axi(0).sendWriteAddress(AddressPacket(0, 0x00, 7, 2))
        s_axi(0).sendWriteAddress(AddressPacket(1, 0x20, 1, 2))
      }.fork {
        s_axi(0).sendWriteDataBurst(
          Seq(
            WriteDataPacket(0x0_dead, 0xf, false),
            WriteDataPacket(0x1_dead, 0xf, false),
            WriteDataPacket(0x2_dead, 0xf, false),
            WriteDataPacket(0x3_dead, 0xf, false),
            WriteDataPacket(0x4_dead, 0xf, false),
            WriteDataPacket(0x5_dead, 0xf, false),
            WriteDataPacket(0x6_dead, 0xf, false),
            WriteDataPacket(0x7_dead, 0xf, true)
          )
        )

        s_axi(0).sendWriteDataBurst(
          Seq(
            WriteDataPacket(0x0_beef, 0xf, false),
            WriteDataPacket(0x1_beef, 0xf, true)
          )
        )
      }.fork {
        println(f"s_axi(0): ${s_axi(0).receiveWriteResponse()}")
        println(f"s_axi(0): ${s_axi(0).receiveWriteResponse()}")
      }.join()

      fork {
        s_axi(0).sendReadAddress(AddressPacket(0, 0x00, 9, 2))
      }.fork {
        println(f"s_axi(0): ${s_axi(0).receiveReadDataBurst()}")
      }.fork {
        s_axi(1).sendReadAddress(AddressPacket(0, 0x00, 3, 2))
        s_axi(1).sendReadAddress(AddressPacket(1, 0x08, 3, 2))
      }.fork {
        println(f"s_axi(1): ${s_axi(1).receiveReadDataBurst()}")
        println(f"s_axi(1): ${s_axi(1).receiveReadDataBurst()}")
      }.fork {
        s_axi(2).sendReadAddress(AddressPacket(0, 0x00, 3, 2))
        s_axi(2).sendReadAddress(AddressPacket(1, 0x08, 3, 2))
      }.fork {
        println(f"s_axi(2): ${s_axi(2).receiveReadDataBurst()}")
        println(f"s_axi(2): ${s_axi(2).receiveReadDataBurst()}")
      }.join()
    }
  }
}

class IdSerializerDut extends Module {
  val axiCfg = axi4.Config(
    wId = 4,
    wAddr = 8,
    wData = 32
  )

  val s_axi = IO(Vec(4, axi4.full.Slave(axiCfg)))

  private val mux = Module(
    new axi4.full.components.Mux(
      axiCfg,
      4
    )
  )
  s_axi.zip(mux.s_axi).foreach { case (master, slave) =>
    master :=> slave
  }

  private val idSerializer = Module(
    new IdSerializer(mux.axiCfgMaster, IdSerializerConfig(wIdSelect = 2))
  )
  mux.m_axi :=> idSerializer.s_axi

  private val memBridge = Module(
    new chext.ip.memory.Axi4FullToReadWriteBridge(idSerializer.axiMasterCfg)
  )
  idSerializer.m_axi :=> memBridge.s_axi

  private val rawMemCfg = chext.ip.memory.RawMemConfig(
    wAddr = memBridge.cfg.wAddr,
    wData = memBridge.cfg.wData,
    latencyRead = 2,
    latencyWrite = 1
  )

  private val portCfg = chext.ip.memory.PortConfig(numOutstandingRead = 4, numOutstandingWrite = 4)

  private val mem = Module(new chext.ip.memory.SimpleDualPortRAM(rawMemCfg, portCfg))
  memBridge.read <> mem.read
  memBridge.write <> mem.write

}

class IdSerializerSpec extends chext.test.FreeSpec with chext.test.TestMixin {
  useVerilator()
  enableVcd()

  chext.ip.memory.Target.setCurrent(chext.ip.memory.chisel.Target)

  "AXI4 Full IdSerializer Basic Functionality" in test(new IdSerializerDut) { dut =>
    {
      import axi4.full.test._

      val s_axi = dut.s_axi
      s_axi.foreach { _.initSlave() }

      fork {
        s_axi(0).sendWriteAddress(AddressPacket(0, 0x00, 7, 2))
        s_axi(0).sendWriteAddress(AddressPacket(1, 0x20, 1, 2))
      }.fork {
        s_axi(0).sendWriteDataBurst(
          Seq(
            WriteDataPacket(0x0_dead, 0xf, false),
            WriteDataPacket(0x1_dead, 0xf, false),
            WriteDataPacket(0x2_dead, 0xf, false),
            WriteDataPacket(0x3_dead, 0xf, false),
            WriteDataPacket(0x4_dead, 0xf, false),
            WriteDataPacket(0x5_dead, 0xf, false),
            WriteDataPacket(0x6_dead, 0xf, false),
            WriteDataPacket(0x7_dead, 0xf, true)
          )
        )

        s_axi(0).sendWriteDataBurst(
          Seq(
            WriteDataPacket(0x0_beef, 0xf, false),
            WriteDataPacket(0x1_beef, 0xf, true)
          )
        )
      }.fork {
        println(f"s_axi(0): ${s_axi(0).receiveWriteResponse()}")
        println(f"s_axi(0): ${s_axi(0).receiveWriteResponse()}")
      }.join()

      fork {
        s_axi(0).sendReadAddress(AddressPacket(0, 0x00, 9, 2))
      }.fork {
        println(f"s_axi(0): ${s_axi(0).receiveReadDataBurst()}")
      }.fork {
        s_axi(1).sendReadAddress(AddressPacket(0, 0x00, 3, 2))
        s_axi(1).sendReadAddress(AddressPacket(1, 0x08, 3, 2))
      }.fork {
        println(f"s_axi(1): ${s_axi(1).receiveReadDataBurst()}")
        println(f"s_axi(1): ${s_axi(1).receiveReadDataBurst()}")
      }.fork {
        s_axi(2).sendReadAddress(AddressPacket(0, 0x00, 3, 2))
        s_axi(2).sendReadAddress(AddressPacket(1, 0x08, 3, 2))
      }.fork {
        println(f"s_axi(2): ${s_axi(2).receiveReadDataBurst()}")
        println(f"s_axi(2): ${s_axi(2).receiveReadDataBurst()}")
      }.join()
    }
  }
}
