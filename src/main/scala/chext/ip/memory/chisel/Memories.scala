package chext.ip.memory.chisel

import chisel3._
import chisel3.util._

import chext.ip.memory

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

class SimpleDualPortRawRAM(
    val cfg: memory.RawMemConfig
) extends Module
    with memory.RawMem {
  override val desiredName = "ChiselSimpleDualPortMem"

  assert(cfg.latencyRead >= 1)
  assert(cfg.latencyWrite >= 1)

  val interfaceRd = IO(
    new memory.RawInterface(cfg.wAddr, cfg.wData, true, false)
  )
  val interfaceWr = IO(
    new memory.RawInterface(cfg.wAddr, cfg.wData, false, true)
  )

  private val numBytes =
    (cfg.wData >> 3) // NOTE: same as the write strobe width

  private val mem =
    SyncReadMem(1 << cfg.wAddr, Vec(numBytes, UInt(8.W)))
  private class WrReq(val wAddr: Int, val wData: Int) extends Bundle {
    val wStrobe = (wData >> 3)

    val addr = UInt(wAddr.W)
    val dIn = Bits(wData.W)
    val wstrb = UInt(wStrobe.W)
  }

  private val wrReq_ = Wire(new WrReq(wAddr = cfg.wAddr, wData = cfg.wData))
  wrReq_.addr := interfaceWr.addr
  wrReq_.dIn := interfaceWr.dIn
  wrReq_.wstrb := interfaceWr.wstrb

  private val wrReqDelayed_ =
    if (cfg.latencyRead > 1) ShiftRegister(wrReq_, cfg.latencyWrite - 1)
    else wrReq_

  mem.write(
    wrReqDelayed_.addr,
    unpack(wrReqDelayed_.dIn, 8),
    wrReqDelayed_.wstrb.asBools
  )
  interfaceWr.dOut := 0.U

  private val dOut_ = mem.read(interfaceRd.addr, true.B).asUInt

  interfaceRd.dOut := {
    if (cfg.latencyRead > 1) ShiftRegister(dOut_, cfg.latencyWrite - 1)
    else dOut_
  }

  def getPorts: Seq[memory.RawInterface] = Seq(interfaceRd, interfaceWr)
}
