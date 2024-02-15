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

class AddressGeneratorTest extends chext.test.FreeSpec with chext.test.TestMixin {
  val cfg = axi4.Config(wData = 128)

  useVerilator()
  enableVcd()

  "AddressGenerator" in test(new AddressGenerator(cfg)) { (dut) =>
    {
      dut.arSource.initSource()
      dut.addrSink.initSink()
      
      fork {
        dut.arSource.sendPacket(AddressPacket(0, 0x5024, 0, 1))
        dut.arSource.sendPacket(AddressPacket(0, 0x6008, 1, 1))
        dut.arSource.sendPacket(AddressPacket(0, 0x7000, 2, 1))
        dut.arSource.sendPacket(AddressPacket(0, 0x9004, 4, 1))
      }.fork {
        dut.addrSink.expectDequeue(0x5024.U)

        dut.addrSink.expectDequeue(0x6008.U)
        dut.addrSink.expectDequeue(0x6010.U)

        dut.addrSink.expectDequeue(0x7000.U)
        dut.addrSink.expectDequeue(0x7010.U)
        dut.addrSink.expectDequeue(0x7020.U)

        dut.addrSink.expectDequeue(0x9004.U)
        dut.addrSink.expectDequeue(0x9010.U)
        dut.addrSink.expectDequeue(0x9020.U)
        dut.addrSink.expectDequeue(0x9030.U)
        dut.addrSink.expectDequeue(0x9040.U)
      }.join()
    }
  }
}
