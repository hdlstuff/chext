package chext.ip.bram.chisel

import chisel3._
import chisel3.util._

import chext.ip.bram

private object unpack {
  def apply(in: UInt, elemWidth: Int): Vec[UInt] = {
    assert(in.getWidth >= elemWidth && in.getWidth % elemWidth == 0)
    val length = in.getWidth / elemWidth
    val unpacked = Wire(Vec(length, UInt(elemWidth.W)))
    unpacked.zipWithIndex.foreach {
      case (elem, idx) => {
        elem := in(elemWidth * (idx + 1) - 1, elemWidth * idx)
      }
    }
    unpacked
  }
}

class SimpleDualPortMem(
    val cfg: bram.RawMemConfig
) extends Module
    with bram.RawMem {
  override val desiredName = "ChiselSimpleDualPortMem"

  assert(cfg.readLatency == 1)
  assert(cfg.writeLatency == 1)

  val interfaceRd = IO(new bram.RawInterface(cfg.wAddr, cfg.wData, true, false))
  val interfaceWr = IO(new bram.RawInterface(cfg.wAddr, cfg.wData, false, true))

  private val numBytes = cfg.wData >> 3

  private val mem =
    SyncReadMem(1 << cfg.wAddr, Vec(numBytes, UInt(8.W)))

  mem.write(
    interfaceWr.addr,
    unpack(interfaceWr.dataIn, 8),
    interfaceWr.writeStrobe.asBools
  )
  interfaceWr.dataOut := 0.U

  interfaceRd.dataOut := mem.read(interfaceRd.addr, true.B).asUInt

  def getPorts: Seq[bram.RawInterface] = Seq(interfaceRd, interfaceWr)
}
