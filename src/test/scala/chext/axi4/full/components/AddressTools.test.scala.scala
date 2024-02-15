package chext.axi4.full.components

import chext.axi4
import chext.elastic

import chisel3._
import chisel3.util._

import chiseltest._

import chisel3.experimental.BundleLiterals._

import elastic.test.PacketOps._

import axi4.full.test._
import axi4.full.test.PacketUtils._

class AddressGeneratorTest
    extends chext.test.FreeSpec
    with chext.test.TestMixin {
  val cfg = axi4.Config(wData = 128)

  useVerilator()
  enableVcd()

  "chext.axi4.full.components.AddressGenerator.INCR" in
    test(new AddressGenerator(cfg)) { (dut) =>
      {
        dut.arSource.initSource()
        dut.addrSink.initSink()

        fork {
          dut.arSource.sendPacket(AddressPacket(0, 0x3000, 0, 1, 4))
          dut.arSource.sendPacket(AddressPacket(0, 0x5024, 1, 1, 4))
          dut.arSource.sendPacket(AddressPacket(0, 0x6008, 3, 1, 4))
          dut.arSource.sendPacket(AddressPacket(0, 0x7000, 7, 1, 4))
          dut.arSource.sendPacket(AddressPacket(0, 0x8000, 15, 1, 4))
          dut.arSource.sendPacket(AddressPacket(0, 0xa000, 255, 1, 4))
        }.fork {
          dut.addrSink.expectDequeue(0x3000.U)

          dut.addrSink.expectDequeue(0x5024.U)
          dut.addrSink.expectDequeue(0x5030.U)

          dut.addrSink.expectDequeue(0x6008.U)
          for (i <- (0 until 3))
            dut.addrSink.expectDequeue((0x6010 + i * 0x10).U)

          for (i <- (0 until 8))
            dut.addrSink.expectDequeue((0x7000 + i * 0x10).U)

          for (i <- (0 until 16))
            dut.addrSink.expectDequeue((0x8000 + i * 0x10).U)

          for (i <- (0 until 256))
            dut.addrSink.expectDequeue((0xa000 + i * 0x10).U)
        }.join()
      }
    }

  "chext.axi4.full.components.AddressGenerator.WRAP" in
    test(new AddressGenerator(cfg)) { (dut) =>
      {
        dut.arSource.initSource()
        dut.addrSink.initSink()

        fork {
          dut.arSource.sendPacket(AddressPacket(0, 0xa010, 3, 2, 4))
        }.fork {
          dut.addrSink.expectDequeue(0xa010.U)
          dut.addrSink.expectDequeue(0xa020.U)
          dut.addrSink.expectDequeue(0xa030.U)
          dut.addrSink.expectDequeue(0xa000.U)
        }.join()
      }
    }

  "chext.axi4.full.components.AddressGenerator.FIXED" in
    test(new AddressGenerator(cfg)) { (dut) =>
      {
        dut.arSource.initSource()
        dut.addrSink.initSink()

        fork {
          dut.arSource.sendPacket(AddressPacket(0, 0xa010, 3, 0, 4))
        }.fork {
          dut.addrSink.expectDequeue(0xa010.U)
          dut.addrSink.expectDequeue(0xa010.U)
          dut.addrSink.expectDequeue(0xa010.U)
          dut.addrSink.expectDequeue(0xa010.U)
        }.join()
      }
    }
}
