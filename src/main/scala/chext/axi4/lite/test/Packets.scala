package chext.axi4.lite.test

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import chext.elastic
import chext.axi4.lite.{
  Interface,
  AddressChannel,
  ReadDataChannel,
  WriteDataChannel,
  WriteResponseChannel
}
import chext.util.Expect

import elastic.test.{Packet, PacketTag, PacketBridge}
import elastic.test.PacketOps._

case class AddressPacket(
    val addr: Int
) extends elastic.test.Packet {
  val last: Boolean = true
}

case class ReadDataPacket(
    val data: Long
) extends elastic.test.Packet {
  val last = true
}

case class WriteDataPacket(
    val data: Long
) extends elastic.test.Packet {
  val last = true
}

case class WriteResponsePacket(
) extends elastic.test.Packet {
  def last: Boolean = false
}

trait PacketUtils {
  @annotation.nowarn /* suppress warning: Implicit definition should have explicit type */
  private implicit val tagAddressPacket =
    chext.elastic.test.PacketTag.makeTag[AddressPacket]

  @annotation.nowarn /* suppress warning: Implicit definition should have explicit type */
  private implicit val tagReadDataPacket =
    chext.elastic.test.PacketTag.makeTag[ReadDataPacket]

  @annotation.nowarn /* suppress warning: Implicit definition should have explicit type */
  private implicit val tagWriteDataPacket =
    chext.elastic.test.PacketTag.makeTag[WriteDataPacket]

  @annotation.nowarn /* suppress warning: Implicit definition should have explicit type */
  private implicit val tagWriteResponsePacket =
    chext.elastic.test.PacketTag.makeTag[WriteResponsePacket]

  @annotation.nowarn /* suppress warning: Implicit definition should have explicit type */
  private implicit val bridgeAddressPacket =
    new elastic.test.PacketBridge[AddressChannel, AddressPacket] {
      def toTester(t: AddressChannel): AddressPacket =
        AddressPacket(
          t.addr.litValue.toInt
        )

      def toLit(gen: AddressChannel, tt: AddressPacket): AddressChannel =
        gen.Lit(
          _.addr -> tt.addr.U,
          _.prot -> 0.U
        )
    }

  @annotation.nowarn /* suppress warning: Implicit definition should have explicit type */
  private implicit val bridgeReadDataPacket =
    new elastic.test.PacketBridge[ReadDataChannel, ReadDataPacket] {
      def toTester(t: ReadDataChannel): ReadDataPacket =
        ReadDataPacket(
          t.data.litValue.toInt
        )

      def toLit(
          gen: ReadDataChannel,
          tt: ReadDataPacket
      ): ReadDataChannel =
        gen.Lit(
          _.data -> tt.data.U,
          _.resp -> 0.U
        )

    }

  @annotation.nowarn /* suppress warning: Implicit definition should have explicit type */
  private implicit val bridgeWriteDataPacket =
    new elastic.test.PacketBridge[WriteDataChannel, WriteDataPacket] {
      def toTester(t: WriteDataChannel): WriteDataPacket =
        WriteDataPacket(
          t.data.litValue.toInt
        )

      def toLit(
          gen: WriteDataChannel,
          tt: WriteDataPacket
      ): WriteDataChannel =
        gen.Lit(
          _.data -> tt.data.U,
          _.strb -> 0xf.U // TODO: make this one data dependent
        )

    }

  @annotation.nowarn /* suppress warning: Implicit definition should have explicit type */
  private implicit val bridgeWriteResponsePacket =
    new elastic.test.PacketBridge[
      WriteResponseChannel,
      WriteResponsePacket
    ] {
      def toTester(t: WriteResponseChannel): WriteResponsePacket =
        WriteResponsePacket()

      def toLit(
          gen: WriteResponseChannel,
          tt: WriteResponsePacket
      ): WriteResponseChannel =
        gen.Lit(
          _.resp -> 0.U
        )

    }

  implicit class axi4_lite_interface_packet_utils(interface: Interface) {
    def receiveReadAddress() = interface.ar.receivePacket[AddressPacket]()

    def expectReadAddress(addr: Int): Unit =
      expectReadAddress(AddressPacket(addr))

    def expectReadAddress(ar: AddressPacket): Unit =
      Expect.equals(
        ar,
        receiveReadAddress(),
        "expectReadAddress"
      )

    def sendReadAddress(addr: Int): Unit =
      sendReadAddress(AddressPacket(addr))

    def sendReadAddress(ar: AddressPacket): Unit =
      interface.ar.sendPacket(ar)

    def receiveReadData() =
      interface.r.receivePacket[ReadDataPacket]()

    def expectReadData(data: Long): Unit =
      expectReadData(ReadDataPacket(data))

    def expectReadData(r: ReadDataPacket): Unit =
      Expect.equals(
        r,
        interface.r.receivePacket[ReadDataPacket](),
        "expectReadData"
      )

    def sendReadData(data: Long): Unit =
      sendReadData(ReadDataPacket(data))

    def sendReadData(r: ReadDataPacket): Unit =
      interface.r.sendPacket(r)
    def receiveWriteAddress() =
      interface.aw.receivePacket[AddressPacket]()

    def expectWriteAddress(addr: Int): Unit =
      expectWriteAddress(AddressPacket(addr))

    def expectWriteAddress(aw: AddressPacket): Unit =
      Expect.equals(
        aw,
        receiveWriteAddress(),
        "expectWriteAddress"
      )

    def sendWriteAddress(addr: Int): Unit =
      sendWriteAddress(AddressPacket(addr))

    def sendWriteAddress(aw: AddressPacket): Unit =
      interface.aw.sendPacket(aw)

    def receiveWriteData() =
      interface.w.receivePacket[WriteDataPacket]()

    def expectWriteData(data: Long): Unit =
      expectWriteData(WriteDataPacket(data))

    def expectWriteData(w: WriteDataPacket): Unit =
      Expect.equals(
        w,
        interface.w.receivePacket[WriteDataPacket](),
        "expectWriteData"
      )

    def sendWriteData(data: Long): Unit =
      sendWriteData(WriteDataPacket(data))

    def sendWriteData(w: WriteDataPacket): Unit =
      interface.w.sendPacket(w)

    def receiveWriteResponse() =
      interface.b.receivePacket[WriteResponsePacket]()

    def expectWriteResponse(): Unit =
      expectWriteResponse(WriteResponsePacket())

    def expectWriteResponse(b: WriteResponsePacket): Unit =
      Expect.equals(b, receiveWriteResponse(), "expectWriteResponse")

    def sendWriteResponse(): Unit =
      sendWriteResponse(WriteResponsePacket())

    def sendWriteResponse(b: WriteResponsePacket): Unit =
      interface.b.sendPacket(b)
  }
}

object PacketUtils extends PacketUtils
