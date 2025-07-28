package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.test
import chext.amba.axi4
import chext.elastic
import chext.memory

import axi4.Ops._
import elastic.ConnectOp._

case class Upscale_Tbtop(
    val wDataNarrow: Int,
    val wDataWide: Int,
    override val desiredName: String
) extends Module
    with chext.TestBenchTop {
  val log2bytesTotal = 14

  val rawMemCfg = memory.RawMemConfig(log2bytesTotal - log2Ceil(wDataWide / 8), wDataWide, 4, 4)
  val portCfg = memory.PortConfig(8, 8)

  val axiCfgNarrow = axi4.Config(0, log2bytesTotal, wDataNarrow)
  val axiCfgWide = axi4.Config(0, log2bytesTotal, wDataWide)

  val S_AXI_NORMAL = IO(axi4.Slave(axiCfgWide))
  val S_AXI_TEST = IO(axi4.Slave(axiCfgNarrow))

  private val mem = Module(
    new memory.TrueDualPortRAM(rawMemCfg, portCfg, portCfg)
  )

  private val axiBridge1 = Module(new memory.Axi4FullToReadWriteBridge(axiCfgWide))

  S_AXI_NORMAL :=> axiBridge1.s_axi

  axiBridge1.read.req :=> mem.read1.req
  mem.read1.resp :=> axiBridge1.read.resp

  axiBridge1.write.req :=> mem.write1.req
  mem.write1.resp :=> axiBridge1.write.resp

  private val axiBridge2 = Module(new memory.Axi4FullToReadWriteBridge(axiCfgWide))

  axiBridge2.read.req :=> mem.read2.req
  mem.read2.resp :=> axiBridge2.read.resp

  axiBridge2.write.req :=> mem.write2.req
  mem.write2.resp :=> axiBridge2.write.resp

  private val upscaleCfg = UpscaleConfig(axiCfgNarrow, wDataWide)
  private val upscale = Module(new Upscale(upscaleCfg))
  S_AXI_TEST :=> upscale.s_axi
  upscale.m_axi :=> axiBridge2.s_axi

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(S_AXI_NORMAL)
  declareAxi4Interface(S_AXI_TEST)
}

object Upscale_Tb extends chext.TestBench {
  emit(new Upscale_Tbtop(32, 128, "Upscale_Tbtop_1"))
  emit(new Upscale_Tbtop(64, 128, "Upscale_Tbtop_2"))
}
