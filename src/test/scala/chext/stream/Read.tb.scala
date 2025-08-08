package chext.stream

import chisel3._
import chisel3.util._

import chext.elastic
import elastic.ConnectOp._

import chext.amba.axi4
import axi4.Ops._
import chext.elastic.RandomStall

private class AxiTestSlave(axiCfg: axi4.Config) extends Module {
  val s_axi = IO(axi4.full.Slave(axiCfg))

  require(axiCfg.wData == 64)

  private def mkData(addr: UInt, index: UInt): UInt = {
    val genUInt32 = UInt(32.W)
    val index0 = addr + index * 8.U

    Cat(index0 + 4.U, index0)
  }

  val repeat0 = new elastic.Repeat(s_axi.ar, elastic.SinkBuffer(s_axi.r), 9) {
    len { (in) => in.len +& 1.U }
    outExplicit { (in, index, _, last, out) =>
      {
        out := 0.U.asTypeOf(out)

        out.last := last
        out.data := mkData(in.addr, index)
      }
    }
  }

  if (axiCfg.write) {
    s_axi.aw.nodeq()
    s_axi.w.nodeq()
    s_axi.b.noenq()

    s_axi.aw.markSource()
    s_axi.w.markSource()
    s_axi.b.markSink()
  }
}

class Read_Tbtop(
    val cfg: ReadConfig[UInt],
    override val desiredName: String
) extends Module
    with chext.TestBenchTop {
  private val read = Module(new Read(cfg))
  private val axiTestSlave = Module(new AxiTestSlave(cfg.axiCfg))

  val rd_sourceTask = IO(elastic.Source(cfg.genTask))
  val rd_sinkResult = IO(elastic.Sink(cfg.genResult))

  rd_sourceTask :=> read.sourceTask
  read.sinkResult :=> rd_sinkResult

  val randomStallAR = new elastic.RandomStall(read.m_axi.ar, axiTestSlave.s_axi.ar, 8)
  val randomStallR = new elastic.RandomStall(axiTestSlave.s_axi.r, read.m_axi.r, 8)

  read.m_axi.aw :=> axiTestSlave.s_axi.aw
  read.m_axi.w :=> axiTestSlave.s_axi.w
  axiTestSlave.s_axi.b :=> read.m_axi.b

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(rd_sourceTask, "Rd_Task")
  declareElasticInterface(rd_sinkResult, "Rd_Result")

}

object Read_TB extends chext.TestBench {
  emit(
    new Read_Tbtop(
      ReadConfig(axi4.Config(wAddr = 32, wData = 64), resultMode = ReadResultMode.DropEmpty),
      "Read_Tbtop_DropEmpty"
    )
  )

  emit(
    new Read_Tbtop(
      ReadConfig(
        axi4.Config(wAddr = 32, wData = 64),
        resultMode = ReadResultMode.LastAlwaysInvalid
      ),
      "Read_Tbtop_LastAlwaysInvalid"
    )
  )

  emit(
    new Read_Tbtop(
      ReadConfig(
        axi4.Config(wAddr = 32, wData = 64),
        resultMode = ReadResultMode.LastSometimesInvalid
      ),
      "Read_Tbtop_LastSometimesInvalid"
    )
  )
}
