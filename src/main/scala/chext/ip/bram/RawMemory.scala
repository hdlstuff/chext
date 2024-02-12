package chext.ip.bram

import chisel3._
import chisel3.util._

class RawInterface(
    val wAddr: Int,
    val wData: Int,
    val read: Boolean = true,
    val write: Boolean = true
) extends Bundle {
  require(isPow2(wAddr))
  require(isPow2(wData) && wData >= 8)
  require(read || write)

  val wWriteStrobe = (wData >> 3)

  val addr = Input(UInt(wAddr.W))
  val dataIn = Input(Bits(wData.W))
  val dataOut = Output(Bits(wData.W))
  val writeStrobe = Input(UInt(wWriteStrobe.W))
}

trait RawMemory extends Module {
  def getPorts: Seq[RawInterface]
}
