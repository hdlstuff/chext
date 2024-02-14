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

class SimpleDualPortMem(
    val cfg: memory.RawMemConfig
) extends Module
    with memory.RawMem {
  override val desiredName = "ChiselSimpleDualPortMem"

  assert(cfg.readLatency >= 1)
  assert(cfg.writeLatency >= 1)

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
    val wWriteStrobe = (wData >> 3)

    val addr = UInt(wAddr.W)
    val dataIn = Bits(wData.W)
    val writeStrobe = UInt(wWriteStrobe.W)
  }

  private val wrReq_ = Wire(new WrReq(wAddr = cfg.wAddr, wData = cfg.wData))
  wrReq_.addr := interfaceWr.addr
  wrReq_.dataIn := interfaceWr.dataIn
  wrReq_.writeStrobe := interfaceWr.writeStrobe

  private val wrReqDelayed_ =
    if (cfg.writeLatency > 1) ShiftRegister(wrReq_, cfg.writeLatency - 1)
    else wrReq_

  mem.write(
    wrReqDelayed_.addr,
    unpack(wrReqDelayed_.dataIn, 8),
    wrReqDelayed_.writeStrobe.asBools
  )
  interfaceWr.dataOut := 0.U

  private val dataOut_ = mem.read(interfaceRd.addr, true.B).asUInt

  interfaceRd.dataOut := {
    if (cfg.readLatency > 1) ShiftRegister(dataOut_, cfg.readLatency - 1)
    else dataOut_
  }

  def getPorts: Seq[memory.RawInterface] = Seq(interfaceRd, interfaceWr)
}
