package chext.amba.axi4.full.components

import chisel3._
import chisel3.util.log2Ceil

import chext.elastic
import elastic.ConnectOp._

import chext.amba.axi4
import axi4.Ops._

import chext.memory

class Widen_Tbtop extends Module with chext.TestBenchTop {
  val wAddr = 14
  val wData = 64

  val rawMemCfg = memory.RawMemConfig(wAddr - log2Ceil(wData / 8), wData, 4, 4)
  val portCfg = memory.PortConfig(8, 8)

  val axiCfg = axi4.Config(0, wAddr, wData)

  val S_AXI_NORMAL = IO(axi4.Slave(axiCfg))
  val S_AXI_TEST = IO(axi4.Slave(axiCfg))

  private val mem = Module(
    new memory.TrueDualPortRAM(rawMemCfg, portCfg, portCfg)
  )

  private val axiBridge1 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg))

  S_AXI_NORMAL :=> axi4.full.MasterBuffer(axiBridge1.s_axi, axi4.BufferConfig.all(2))

  axiBridge1.read.req :=> mem.read1.req
  mem.read1.resp :=> axiBridge1.read.resp

  axiBridge1.write.req :=> mem.write1.req
  mem.write1.resp :=> axiBridge1.write.resp

  private val axiBridge2 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg))

  axiBridge2.read.req :=> mem.read2.req
  mem.read2.resp :=> axiBridge2.read.resp

  axiBridge2.write.req :=> mem.write2.req
  mem.write2.resp :=> axiBridge2.write.resp

  private val widenCfg = WidenConfig(axiCfg)
  private val widen = Module(new Widen(widenCfg))
  S_AXI_TEST :=> axi4.full.MasterBuffer(widen.s_axi, axi4.BufferConfig.all(2))
  widen.m_axi :=> axi4.full.MasterBuffer(axiBridge2.s_axi, axi4.BufferConfig.all(2))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(S_AXI_NORMAL)
  declareAxi4Interface(S_AXI_TEST)
}

object Widen_Tb extends chext.TestBench {
  emit(new Widen_Tbtop)
}
