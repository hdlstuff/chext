package chext.ip.memory

import chisel3._
import chisel3.util._

import chext.{elastic2 => elastic}

// TODO: also support EN
class RawInterface(
    val wAddr: Int,
    val wData: Int,
    val supportsRead: Boolean = true,
    val supportsWrite: Boolean = true
) extends Bundle {
  require(wData >= 8 && (wData % 8) == 0)
  require(supportsRead || supportsWrite)

  val wStrobe = (wData >> 3)

  /** Address (index RAM words of size wData) */
  val addr = Input(UInt(wAddr.W))

  /** Data input */
  val dIn = Input(Bits(wData.W))

  /** Data output */
  val dOut = Output(Bits(wData.W))

  /** Write strobe */
  val wstrb = Input(UInt(wStrobe.W))
}

class ReadInterface(wAddr: Int, wData: Int) extends Bundle {
  require(isPow2(wData))

  val req = Flipped(new elastic.Interface(UInt(wAddr.W)))
  val resp = new elastic.Interface(UInt(wData.W))
}

class WriteRequest(wAddr: Int, wData: Int) extends Bundle {
  require(wData >= 8 && (wData % 8) == 0)

  val wStrobe = (wData >> 3)

  val addr = UInt(wAddr.W)
  val data = UInt(wData.W)
  val strb = UInt(wStrobe.W)
}

class WriteInterface(wAddr: Int, wData: Int) extends Bundle {
  val req = Flipped(new elastic.Interface(new WriteRequest(wAddr, wData)))
  val resp = new elastic.Interface(UInt(0.W))
}
