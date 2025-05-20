package chext.testbench_testing

import chisel3._
import chext.amba.axi4
import chext.{elastic2 => elastic}

class TestBenchTop1 extends Module with chext.TestBenchTop {
  val axiSlaveCfg = axi4.Config(8, 32, 128)
  val axiMasterCfg = axi4.Config(wAddr = 32, wData = 64, lite = true)

  class Task1 extends Bundle {
    val addr = UInt(64.W)
    val len = UInt(32.W)
  }

  class Task2 extends Bundle {
    val x0 = UInt(16.W)
    val x1 = UInt(32.W)
  }

  class Result1 extends Bundle {
    val cycles = UInt(32.W)
    val data0 = UInt(0.W)
    val data1 = UInt(1.W)
    val data2 = UInt(2.W)
    val data3 = UInt(8.W)
    val data4 = UInt(9.W)
    val data5 = UInt(16.W)
    val data6 = UInt(32.W)
  }

  class Result2 extends Bundle {
    /* empty */
  }

  val s_axi = IO(axi4.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.Master(axiMasterCfg))

  val irqIn = IO(Input(Bool()))
  val irqOut = IO(Output(Bool()))

  val sourceTask1_a = IO(elastic.Source(new Task1))
  val sourceTask1_b = IO(elastic.Source(new Task1))

  val sourceTask2_a = IO(elastic.Source(new Task2))
  val sourceTask2_b = IO(elastic.Source(new Task2))

  val sinkResult1 = IO(elastic.Sink(new Result1))
  val sinkResult2 = IO(elastic.Sink(new Result2))
  val sinkResult3 = IO(elastic.Sink(UInt(16.W)))
  val sinkResult4 = IO(elastic.Sink(UInt(64.W)))
  val sinkResult5 = IO(elastic.Sink(UInt(0.W))) /*  zero-length */

  declareClock(clock)
  declareReset(reset)

  declareAxi4Interface(s_axi)
  declareAxi4Interface(m_axi)
  declareInterrupt(irqIn, hdlinfo.PortSensitivity.interruptFalling)
  declareInterrupt(irqOut, hdlinfo.PortSensitivity.interruptHigh)
  declareElasticInterface(sourceTask1_a, "Task1")
  declareElasticInterface(sourceTask1_b, "Task1")
  declareElasticInterface(sourceTask2_a, "Task2")
  declareElasticInterface(sourceTask2_b, "Task2")
  declareElasticInterface(sinkResult1, "Result1")
  declareElasticInterface(sinkResult2, "Result2")
  declareElasticInterface(sinkResult3, "Result3")
  declareElasticInterface(sinkResult4, "Result4")
  declareElasticInterface(sinkResult5, "Result5")

  import axi4.Ops._

  s_axi.asFull.ar.nodeq()
  s_axi.asFull.r.noenq()

  s_axi.asFull.aw.nodeq()
  s_axi.asFull.w.nodeq()
  s_axi.asFull.b.noenq()

  m_axi.asLite.ar.noenq()
  m_axi.asLite.r.nodeq()

  m_axi.asLite.aw.noenq()
  m_axi.asLite.w.noenq()
  m_axi.asLite.b.nodeq()

  irqOut := false.B

  sourceTask1_a.nodeq()
  sourceTask1_b.nodeq()
  sourceTask2_a.nodeq()
  sourceTask2_b.nodeq()

  sinkResult1.noenq()
  sinkResult2.noenq()
  sinkResult3.noenq()
  sinkResult4.noenq()
  sinkResult5.noenq()
}

object TestBenchTop1_TB extends chext.TestBench {
  emit(new TestBenchTop1())
}
