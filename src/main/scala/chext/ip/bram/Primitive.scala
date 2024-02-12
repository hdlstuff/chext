package chext.ip.bram

import chisel3._
import chisel3.util._

case class PrimitiveConfig(val wData: Int, val wAddr: Int) {
  require(isPow2(wData) && wData >= 8)
  require(isPow2(wAddr))

  val wWriteStrobe = (wData >> 3)
}

class PrimitiveIO(val cfg: PrimitiveConfig) extends Bundle {
  val en = Input(Bool())
  val addr = Input(UInt(cfg.wAddr.W))
  val dataIn = Input(Bits(cfg.wData.W))
  val dataOut = Output(Bits(cfg.wData.W))
  val writeStrobe = Input(UInt(cfg.wWriteStrobe.W))
}

trait Primitive extends Module {
  def getPorts: Seq[PrimitiveIO]
}
