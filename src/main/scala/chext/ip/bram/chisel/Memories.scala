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

private object pack {
  def apply(in: Vec[UInt]): UInt = {
    assert(in.length > 0)
    val length = in.length
    val elemWidth = in(0).getWidth
    val packed = Wire(UInt((in.length * elemWidth).W))
    in.zipWithIndex.foreach {
      case (elem, idx) => {
        packed(elemWidth * (idx + 1) - 1, elemWidth * idx) := elem
      }
    }
    packed
  }
}

class SimpleDualPortMem(
    val cfg: bram.RawMemConfig
) extends Module
    with bram.RawMem {
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

  interfaceRd.dataOut := pack(mem.read(interfaceRd.addr, true.B))

  def getPorts: Seq[bram.RawInterface] = Seq(interfaceRd, interfaceWr)
}
