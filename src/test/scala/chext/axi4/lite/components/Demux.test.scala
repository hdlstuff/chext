package chext.axi4.lite.components

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.experimental.BundleLiterals._

import chiseltest._

import chext.axi4
import chext.elastic

import axi4.ConnectTo._
import axi4.ResponseFlag
import axi4.lite._
import axi4.lite.test.InterconnectTester
import elastic.test.TesterSpec
import axi4.lite.components.{Demux, DemuxConfig}
import axi4.test.TestedModule
import axi4.lite.ReadWriteInterface
import axi4.Config
import chext.axi4.{Config, ResponseFlag}

class DemuxTester(
    val axiCfg: axi4.Config,
    val numMasters: Int = 4,
    val decoder: (UInt) => (UInt),
    val demuxCfg: DemuxConfig = DemuxConfig()
) extends axi4.test.TestedModule {
  require(axiCfg.read && axiCfg.write)
  val demux = Module(new Demux(axiCfg, numMasters, decoder, demuxCfg))

  val s_axil = IO(ReadWriteInterface.slave(axiCfg))
  val m_axil = IO(Vec(numMasters, ReadWriteInterface.master(axiCfg)))

  s_axil :=> demux.S_AXIL
  demux.M_AXIL.zip(m_axil).foreach { case (a, b) => a :=> b }

  declareSlaveInterface(s_axil)
  m_axil.foreach { declareMasterInterface(_) }
}

class DemuxSpec extends elastic.test.TesterSpec {
  def decodeFn(x: UInt) = (x >> 12) & 3.U
  def encodeFn(masterIdx: Int, offset: Int) = {
    ((masterIdx << 12) | offset)
  }

  def moduleFn = new DemuxTester(
    chext.axi4.Config(
      wAddr = 32,
      wData = 32,
      read = true,
      write = true,
      lite = true
    ),
    8,
    decodeFn
  )

  enableVcd()
  useVerilator()

  "AXI4 Lite Demux (basic)" in moduleTest(moduleFn) {
    new InterconnectTester(_) {
      protected def createTasks(): Unit = {
        for (i <- (0 until 64)) {
          for (masterIdx <- (0 until 8)) {
            readTask(0, encodeFn(masterIdx, rand.nextInt(32) << 5))
            writeTask(0, encodeFn(masterIdx, rand.nextInt(32) << 5))
          }
        }
      }
    }
  }
}
