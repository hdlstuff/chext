package chext.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.experimental.BundleLiterals._

import chiseltest._

import chext.axi4
import chext.elastic

import axi4.ConnectTo._
import axi4.ResponseFlag
import axi4.full._
import axi4.full.test.InterconnectTester
import elastic.test.TesterSpec
import axi4.full.components.{Demux, DemuxConfig}
import axi4.test.TestedModule
import axi4.full.ReadWriteInterface
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

  val s_axi = IO(ReadWriteInterface.slave(axiCfg))
  val m_axi = IO(Vec(numMasters, ReadWriteInterface.master(axiCfg)))

  s_axi :=> demux.S_AXI
  demux.M_AXI.zip(m_axi).foreach { case (a, b) => a :=> b }

  declareSlaveInterface(s_axi)
  m_axi.foreach { declareMasterInterface(_) }
}

class DemuxSpec extends elastic.test.TesterSpec {
  def decodeFn(x: UInt) = (x >> 12) & 3.U
  def encodeFn(masterIdx: Int, offset: Int) = {
    ((masterIdx << 12) | offset)
  }

  def moduleFn = new DemuxTester(
    chext.axi4.Config(
      wId = 4,
      wAddr = 32,
      wData = 32,
      read = true,
      write = true,
      lite = false
    ),
    4,
    decodeFn
  )

  enableVcd()
  useVerilator()

  "AXI4 Full Demux (basic)" in moduleTest(moduleFn) {
    new InterconnectTester(_) {
      protected def createTasks(): Unit = {
        for (masterIdx <- (0 until 4)) {
          for (id <- (0 until 16)) {
            readTask(
              0,
              encodeFn(masterIdx, rand.nextInt(32) << 2),
              len = rand.nextInt(32),
              id = id
            )

            writeTask(
              0,
              encodeFn(masterIdx, rand.nextInt(32) << 2),
              len = rand.nextInt(32),
              id = id
            )
          }
        }
      }
    }
  }
}
