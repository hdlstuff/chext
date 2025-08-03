package chext.stream

import chisel3._
import chisel3.util._

import chext.elastic
import elastic.ConnectOp._

import chext.amba.axi4
import axi4.Ops._

class Write_Tbtop(
    val cfg: WriteConfig[UInt],
    override val desiredName: String
) extends Module
    with chext.TestBenchTop {
  private val readCfg = ReadConfig(cfg.axiCfg, resultMode = ReadResultMode.DropEmpty)
  private val read = Module(new Read(readCfg))
  private val write = Module(new Write(cfg))

  require(cfg.axiCfg == axi4.Config(wAddr = 20, wData = 32))

  val rd_sourceTask = IO(elastic.Source(readCfg.genTask))
  val rd_sinkResult = IO(elastic.Sink(readCfg.genResult))

  val wr_sourceTask = IO(elastic.Source(cfg.genTask))
  val wr_sinkResult = IO(elastic.Sink(cfg.genResult))

  val wr_sourceData = IO(elastic.Source(cfg.genData))

  rd_sourceTask :=> read.sourceTask
  read.sinkResult :=> rd_sinkResult

  wr_sourceTask :=> write.sourceTask
  write.sinkResult :=> wr_sinkResult

  wr_sourceData :=> write.sourceData

  private val (s_axi1, s_axi2) = {
    import chext.memory._
    import cfg.axiCfg

    val rawMemCfg = RawMemConfig(axiCfg.wAddr - (log2Ceil(axiCfg.wData) - 3), axiCfg.wData, 1, 1)
    val portCfg = PortConfig(4, 4, () => new BasicReadWriteArbiter(8))

    val mem = Module(new TrueDualPortRAM(rawMemCfg, portCfg, portCfg))

    val axiBridge1 = Module(new Axi4FullToReadWriteBridge(axiCfg))
    val axiBridge2 = Module(new Axi4FullToReadWriteBridge(axiCfg))

    axiBridge1.read.req :=> mem.read1.req
    mem.read1.resp :=> axiBridge1.read.resp

    axiBridge1.write.req :=> mem.write1.req
    mem.write1.resp :=> axiBridge1.write.resp

    axiBridge2.read.req :=> mem.read2.req
    mem.read2.resp :=> axiBridge2.read.resp

    axiBridge2.write.req :=> mem.write2.req
    mem.write2.resp :=> axiBridge2.write.resp

    (axiBridge1.s_axi, axiBridge2.s_axi)
  }

  read.m_axi :=> s_axi1
  write.m_axi :=> s_axi2

  dontTouch(s_axi1)
  dontTouch(s_axi2)

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(wr_sourceTask, "Wr_Task")
  declareElasticInterface(wr_sinkResult, "Wr_Result")
  declareElasticInterface(wr_sourceData, "Wr_Data")
  declareElasticInterface(rd_sourceTask, "Rd_Task")
  declareElasticInterface(rd_sinkResult, "Rd_Result")
}

object Write_TB extends chext.TestBench {
  emit(
    new Write_Tbtop(
      WriteConfig(axi4.Config(wAddr = 20, wData = 32), resultMode = WriteResultMode.DropEmpty),
      "Write_Tbtop_DropEmpty"
    )
  )

  emit(
    new Write_Tbtop(
      WriteConfig(axi4.Config(wAddr = 20, wData = 32), resultMode = WriteResultMode.KeepAll),
      "Write_Tbtop_KeepAll"
    )
  )
}
