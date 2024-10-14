package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.amba.axi4
import chext.elastic
import chext.ip.memory

import axi4.Ops._
import elastic.ConnectOp._
/*
case class DownscaleTestTopConfig(wideWidth: Int, narrowWidth: Int, desiredName: Option[String] = None) extends chext.ModuleConfig {
  // (2 ** 12) * 4B = 16 KB of memory (14 bits)
  val rawMemCfg = memory.RawMemConfig(12, narrowWidth, 4, 4)
  val portCfg = memory.PortConfig(8, 8)
  val axiCfg128 = axi4.Config(0, 14, 128)
  val axiCfg32 = axiCfg128.copy(wData = 32)
}

class DownscaleTestTop1(val cfg: DownscaleTestTop1Config = DownscaleTestTop1Config())
    extends Module {
    
      import cfg._

  val s_axi_w128 = IO(axi4.full.Slave(axiCfg128))
  val s_axi_w32 = IO(axi4.full.Slave(axiCfg32))

  private val mem = Module(
    new memory.TrueDualPortRAM(rawMemCfg, portCfg, portCfg)
  )

  private val axiBridge1 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg32))

  s_axi_w32 :=> axiBridge1.s_axi

  axiBridge1.read.req :=> mem.read1.req
  mem.read1.resp :=> axiBridge1.read.resp

  axiBridge1.write.req :=> mem.write1.req
  mem.write1.resp :=> axiBridge1.write.resp

  private val axiBridge2 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg32))

  axiBridge2.read.req :=> mem.read2.req
  mem.read2.resp :=> axiBridge2.read.resp

  axiBridge2.write.req :=> mem.write2.req
  mem.write2.resp :=> axiBridge2.write.resp

  private val downscaleCfg = axi4.full.components.DownscaleConfig(axiCfg128, 32)
  private val downscale = Module(new axi4.full.components.Downscale(downscaleCfg))
  s_axi_w128 :=> downscale.s_axi
  downscale.m_axi :=> axiBridge2.s_axi
}

class DownscaleTestTop2 extends Module {
  // (2 ** 12) * 4B = 16 KB of memory (14 bits)
  private val rawMemCfg = memory.RawMemConfig(11, 64, 4, 4)
  private val portCfg = memory.PortConfig(8, 8)
  private val axiCfg128 = axi4.Config(0, 14, 128)
  private val axiCfg64 = axiCfg128.copy(wData = 64)

  val s_axi_w128 = IO(axi4.full.Slave(axiCfg128))
  val s_axi_w64 = IO(axi4.full.Slave(axiCfg64))

  private val mem = Module(
    new memory.TrueDualPortRAM(rawMemCfg, portCfg, portCfg)
  )

  private val axiBridge1 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg64))

  s_axi_w64 :=> axiBridge1.s_axi

  axiBridge1.read.req :=> mem.read1.req
  mem.read1.resp :=> axiBridge1.read.resp

  axiBridge1.write.req :=> mem.write1.req
  mem.write1.resp :=> axiBridge1.write.resp

  private val axiBridge2 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg64))

  axiBridge2.read.req :=> mem.read2.req
  mem.read2.resp :=> axiBridge2.read.resp

  axiBridge2.write.req :=> mem.write2.req
  mem.write2.resp :=> axiBridge2.write.resp

  private val downscaleCfg = axi4.full.components.DownscaleConfig(axiCfg128, 64)
  private val downscale = Module(new axi4.full.components.Downscale(downscaleCfg))
  s_axi_w128 :=> downscale.s_axi
  downscale.m_axi :=> axiBridge2.s_axi
}
*/
