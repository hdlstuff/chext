package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.amba.axi4
import chext.elastic
import chext.ip.memory

import axi4.Ops._
import elastic.ConnectOp._

case class BurstSplitterTestDutConfig(
    val desiredName: Option[String] = None
) extends chext.ModuleConfig {
  // (2 ** 10) * 16B = 16 KB of memory (14 bits)
  val rawMemCfg = memory.RawMemConfig(10, 128, 1, 1)
  val portCfg = memory.PortConfig(8, 8, () => new memory.BasicReadWriteArbiter(32))
  val axiCfg = axi4.Config(0, 14, 128)

  def moduleName: String = desiredName.getOrElse("BurstSplitterTestDut")

  def hdlinfoModule: hdlinfo.Module = ???
}

class BurstSplitterTestDut(val cfg: BurstSplitterTestDutConfig = BurstSplitterTestDutConfig())
    extends Module
    with chext.Module {
  import cfg._

  def this(desiredName: Option[String]) = {
    this(BurstSplitterTestDutConfig(desiredName))
  }

  val s_axi_normal = IO(axi4.full.Slave(axiCfg))
  val s_axi_split = IO(axi4.full.Slave(axiCfg))

  private val mem = Module(
    new memory.TrueDualPortRAM(rawMemCfg, portCfg, portCfg)
  )

  private val axiBridge1 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg))

  s_axi_normal :=> axiBridge1.s_axi

  axiBridge1.read.req :=> mem.read1.req
  mem.read1.resp :=> axiBridge1.read.resp

  axiBridge1.write.req :=> mem.write1.req
  mem.write1.resp :=> axiBridge1.write.resp

  private val axiBridge2 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg))

  axiBridge2.read.req :=> mem.read2.req
  mem.read2.resp :=> axiBridge2.read.resp

  axiBridge2.write.req :=> mem.write2.req
  mem.write2.resp :=> axiBridge2.write.resp

  private val burstSplitterCfg = axi4.full.components.BurstSplitterConfig(axiCfg)
  private val burstSplitter = Module(new axi4.full.components.BurstSplitter(burstSplitterCfg))
  s_axi_split :=> burstSplitter.s_axi
  burstSplitter.m_axi :=> axiBridge2.s_axi
}

object BurstSplitter_TB extends chext.TestBench {
  emit(new BurstSplitterTestDut(Some("BurstSplitterTestDut_1")))
}
