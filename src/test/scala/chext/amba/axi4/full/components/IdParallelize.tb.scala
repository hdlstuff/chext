package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.amba.axi4
import chext.elastic
import chext.memory

import axi4.Ops._
import elastic.ConnectOp._

class IdParallelize_Tbtop(
    override val desiredName: String
) extends Module
    with chext.TestBenchTop {
  val log2bytesTotal = 14
  val wData = 128

  val rawMemCfg = memory.RawMemConfig(log2bytesTotal - log2Ceil(wData / 8), wData, 4, 4)
  val portCfg = memory.PortConfig(8, 8)

  val axiCfg = axi4.Config(4, log2bytesTotal, wData)

  val S_AXI_NORMAL = IO(axi4.Slave(axiCfg))
  val S_AXI_TEST = IO(axi4.Slave(axiCfg.copy(wId = 0)))

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

  private val idParallelizeCfg = IdParallelizeConfig(axiCfg.copy(wId = 0), 4)
  private val idParallelize = Module(new IdParallelize(idParallelizeCfg))
  S_AXI_TEST :=> idParallelize.s_axi
  idParallelize.m_axi :=> axiBridge2.s_axi

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(S_AXI_NORMAL)
  declareAxi4Interface(S_AXI_TEST)
}

class IdParallelize_Tbtop1(
    val wId: Int,
    val wBufferIdx: Int,
    val readUseSyncMem: Boolean,
    val writeUseSyncMem: Boolean,
    override val desiredName: String
) extends Module
    with chext.TestBenchTop {

  private val cfg = IdParallelizeConfig(
    axi4.Config(wId = 0, wAddr = 32, wData = 64, wUserB = 32 /* for testing purposes */ ),
    wId,
    wBufferIdx,
    readUseSyncMem,
    writeUseSyncMem
  )
  private val dut = Module(new IdParallelize(cfg))

  val S_AXI = IO(axi4.Slave(cfg.axiSlaveCfg))
  val M_AXI = IO(axi4.Master(cfg.axiMasterCfg))

  S_AXI.asFull :=> dut.s_axi
  dut.m_axi :=> M_AXI.asFull

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(S_AXI)
  declareAxi4Interface(M_AXI)
}

object IdParallelize_Tb extends chext.TestBench {
  emit(new IdParallelize_Tbtop("IdParallelize_Tbtop_1"))

  emit(new IdParallelize_Tbtop1(2, 5, false, false, "IdParallelize_Tbtop1_1"))
  emit(new IdParallelize_Tbtop1(3, 5, false, false, "IdParallelize_Tbtop1_2"))
  emit(new IdParallelize_Tbtop1(6, 8, false, false, "IdParallelize_Tbtop1_3"))

  emit(new IdParallelize_Tbtop1(2, 5, true, true, "IdParallelize_Tbtop1_4"))
  emit(new IdParallelize_Tbtop1(3, 5, true, true, "IdParallelize_Tbtop1_5"))
  emit(new IdParallelize_Tbtop1(6, 8, true, true, "IdParallelize_Tbtop1_6"))
}
