package chext.axi4.full.components

import chext.axi4
import chext.elastic

import chisel3._
import chisel3.util._

import chiseltest._

import chisel3.experimental.BundleLiterals._

import elastic.test.{Packet, PacketTag, PacketBridge}
import elastic.test.PacketOps._

import axi4.full.test._
import axi4.full.test.PacketUtils._

abstract sealed class Burst(val name: String, val value: Int) {
  override def toString(): String = f"Burst.$name"
}

object Burst {
  case object FIXED extends Burst("FIXED", 0)
  case object INCR extends Burst("INCR", 1)
  case object WRAP extends Burst("WRAP", 2)

  def apply(value: Int): Burst = {
    value match {
      case 0 => FIXED
      case 1 => INCR
      case 2 => WRAP
      case _ => throw new RuntimeException("invalid burst value")
    }
  }
}

case class AddrLenSizeBurstPacket(
    val addr: Int,
    val len: Int,
    val size: Int,
    val burst: Burst
) extends Packet {
  val last = true
}

case class AddressSizeLastPacket(
    val addr: Int,
    val size: Int,
    val last: Boolean
) extends Packet

trait AddressGeneratorTestPacketOps {
  @annotation.nowarn
  implicit val tagAddrLenSizeBurstPacket =
    PacketTag.makeTag[AddrLenSizeBurstPacket]

  @annotation.nowarn
  implicit val tagAddressSizeLastPacket =
    PacketTag.makeTag[AddressSizeLastPacket]

  @annotation.nowarn
  implicit val bridgeAddrLenSizeBurst =
    new PacketBridge[AddrLenSizeBurstBundle, AddrLenSizeBurstPacket] {
      def toTester(t: AddrLenSizeBurstBundle): AddrLenSizeBurstPacket =
        AddrLenSizeBurstPacket(
          addr = t.addr.litValue.toInt,
          len = t.len.litValue.toInt,
          size = t.size.litValue.toInt,
          burst = Burst(t.burst.litValue.toInt)
        )

      def toLit(
          gen: AddrLenSizeBurstBundle,
          tt: AddrLenSizeBurstPacket
      ): AddrLenSizeBurstBundle = gen.Lit(
        _.addr -> tt.addr.U,
        _.len -> tt.len.U,
        _.size -> tt.size.U,
        _.burst -> tt.burst.value.U
      )
    }

  @annotation.nowarn
  implicit val bridgeAddressSizeLastPacket =
    new PacketBridge[AddrSizeLastBundle, AddressSizeLastPacket] {
      def toTester(t: AddrSizeLastBundle): AddressSizeLastPacket =
        AddressSizeLastPacket(
          addr = t.addr.litValue.toInt,
          size = t.size.litValue.toInt,
          last = t.last.litValue == 1
        )

      def toLit(
          gen: AddrSizeLastBundle,
          tt: AddressSizeLastPacket
      ): AddrSizeLastBundle = gen.Lit(
        _.addr -> tt.addr.U,
        _.size -> tt.size.U,
        _.last -> tt.last.B
      )
    }
}

object AddressGeneratorTestPacketOps extends AddressGeneratorTestPacketOps

class AddressGeneratorTest extends chext.test.FreeSpec with chext.test.TestMixin {
  import AddressGeneratorTestPacketOps._

  val wAddr = 32

  useVerilator()
  enableVcd()

  "chext.axi4.full.components.AddressGenerator.INCR" in
    test(new AddressGenerator(wAddr)) { (dut) =>
      {
        dut.source.initSource()
        dut.sink.initSink()

        fork {
          dut.source.sendPacket(AddrLenSizeBurstPacket(0x3000, 0, 4, Burst.INCR))
          dut.source.sendPacket(AddrLenSizeBurstPacket(0x5024, 1, 4, Burst.INCR))
          dut.source.sendPacket(AddrLenSizeBurstPacket(0x6008, 3, 4, Burst.INCR))
          dut.source.sendPacket(AddrLenSizeBurstPacket(0x7000, 7, 4, Burst.INCR))
          dut.source.sendPacket(AddrLenSizeBurstPacket(0x8000, 15, 4, Burst.INCR))
          dut.source.sendPacket(AddrLenSizeBurstPacket(0xa000, 255, 4, Burst.INCR))

        }.fork {
          dut.sink.expectPacket(AddressSizeLastPacket(0x3000, 4, true))

          dut.sink.expectPacket(AddressSizeLastPacket(0x5024, 4, false))
          dut.sink.expectPacket(AddressSizeLastPacket(0x5030, 4, true))

          dut.sink.expectPacket(AddressSizeLastPacket(0x6008, 4, false))
          for (i <- (0 until 3))
            dut.sink.expectPacket(AddressSizeLastPacket(0x6010 + i * 0x10, 4, i == 2))

          for (i <- (0 until 8))
            dut.sink.expectPacket(AddressSizeLastPacket(0x7000 + i * 0x10, 4, i == 7))

          for (i <- (0 until 16))
            dut.sink.expectPacket(AddressSizeLastPacket(0x8000 + i * 0x10, 4, i == 15))

          for (i <- (0 until 256))
            dut.sink.expectPacket(AddressSizeLastPacket(0xa000 + i * 0x10, 4, i == 255))

        }.join()
      }
    }

  "chext.axi4.full.components.AddressGenerator.WRAP" in
    test(new AddressGenerator(wAddr)) { (dut) =>
      {
        dut.source.initSource()
        dut.sink.initSink()

        fork {
          /* for WRAP-type bursts, the start address MUST BE aligned (A3.4, ARM IHI 0022G). */
          dut.source.sendPacket(AddrLenSizeBurstPacket(0xa010, 3, 4, Burst.WRAP))

        }.fork {
          dut.sink.expectPacket(AddressSizeLastPacket(0xa010, 4, false))
          dut.sink.expectPacket(AddressSizeLastPacket(0xa020, 4, false))
          dut.sink.expectPacket(AddressSizeLastPacket(0xa030, 4, false))
          dut.sink.expectPacket(AddressSizeLastPacket(0xa000, 4, true))

        }.join()
      }
    }

  "chext.axi4.full.components.AddressGenerator.FIXED" in
    test(new AddressGenerator(wAddr)) { (dut) =>
      {
        dut.source.initSource()
        dut.sink.initSink()

        fork {
          dut.source.sendPacket(AddrLenSizeBurstPacket(0xa010, 3, 4, Burst.FIXED))

        }.fork {
          dut.sink.expectPacket(AddressSizeLastPacket(0xa010, 4, false))
          dut.sink.expectPacket(AddressSizeLastPacket(0xa010, 4, false))
          dut.sink.expectPacket(AddressSizeLastPacket(0xa010, 4, false))
          dut.sink.expectPacket(AddressSizeLastPacket(0xa010, 4, true))

        }.join()
      }
    }
}
