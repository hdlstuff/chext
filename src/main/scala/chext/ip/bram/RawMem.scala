package chext.ip.memory

import chisel3._
import chisel3.util._

class RawInterface(
    val wAddr: Int,
    val wData: Int,
    val supportsRead: Boolean = true,
    val supportsWrite: Boolean = true
) extends Bundle {
  require(isPow2(wData) && wData >= 8)
  require(supportsRead || supportsWrite)

  val wWriteStrobe = (wData >> 3)

  val addr = Input(UInt(wAddr.W))
  val dataIn = Input(Bits(wData.W))
  val dataOut = Output(Bits(wData.W))
  val writeStrobe = Input(UInt(wWriteStrobe.W))
}

case class RawMemConfig(
    val wAddr: Int = 6,
    val wData: Int = 32,
    val readLatency: Int = 2,
    val writeLatency: Int = 1
)

trait RawMem extends Module {
  def cfg: RawMemConfig
  def getPorts: Seq[RawInterface]
}
