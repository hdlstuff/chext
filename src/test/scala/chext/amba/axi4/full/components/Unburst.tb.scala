package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.amba.axi4
import chext.elastic
import chext.memory

import axi4.Ops._
import elastic.ConnectOp._

class Unburst_Tbtop(
    override val desiredName: String
) extends Module
    with chext.AnnotatedModule {
  val log2bytesTotal = 14
  val wData = 128

  val rawMemCfg = memory.RawMemConfig(log2bytesTotal - log2Ceil(wData / 8), wData, 4, 4)
  val portCfg = memory.PortConfig(8, 8)

  val axiCfg = axi4.Config(0, log2bytesTotal, wData)

  val S_AXI_NORMAL = IO(axi4.Slave(axiCfg))
  val S_AXI_TEST = IO(axi4.Slave(axiCfg))

  private val mem = Module(
    new memory.TrueDualPortRAM(rawMemCfg, portCfg, portCfg)
  )

  private val axiBridge1 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg))

  S_AXI_NORMAL :=> axiBridge1.s_axi

  axiBridge1.read.req :=> mem.read1.req
  mem.read1.resp :=> axiBridge1.read.resp

  axiBridge1.write.req :=> mem.write1.req
  mem.write1.resp :=> axiBridge1.write.resp

  private val axiBridge2 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg))

  axiBridge2.read.req :=> mem.read2.req
  mem.read2.resp :=> axiBridge2.read.resp

  axiBridge2.write.req :=> mem.write2.req
  mem.write2.resp :=> axiBridge2.write.resp

  private val unburstCfg = UnburstConfig(axiCfg)
  private val unburst = Module(new Unburst(unburstCfg))
  S_AXI_TEST :=> unburst.s_axi
  unburst.m_axi :=> axiBridge2.s_axi

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(S_AXI_NORMAL)
  declareAxi4Interface(S_AXI_TEST)
}

object Unburst_Tb extends chext.TestBench {
  emit(new Unburst_Tbtop("Unburst_Tbtop_1"))
}
