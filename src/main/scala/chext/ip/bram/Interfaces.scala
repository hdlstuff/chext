package chext.ip.bram

import chisel3._
import chisel3.util._

class RdInterface(wAddr: Int, wData: Int) extends Bundle {
  require(isPow2(wData))

  val addr = Flipped(new IrrevocableIO(UInt(wAddr.W)))
  val data = new IrrevocableIO(UInt(wData.W))
}

class WrBundle(wAddr: Int, wData: Int) extends Bundle {
  require(isPow2(wData))

  val wWriteStrobe = (wData >> 3)

  val addr = UInt(wAddr.W)
  val data = UInt(wData.W)
  val writeStrobe = UInt(wWriteStrobe.W)
}

class WrInterface(wAddr: Int, wData: Int) extends Bundle {
  val payload = Flipped(new IrrevocableIO(new WrBundle(wAddr, wData)))
  val response = new IrrevocableIO(Bool())
}
