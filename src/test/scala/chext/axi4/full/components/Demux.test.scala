package chext.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.experimental.BundleLiterals._

import chiseltest._

import chext.axi4
import axi4.full.components.{InterconnectTester, InterconnectHelper}

class DemuxSpec extends chext.test.TesterSpec {
  def decodeFn(x: UInt) = (x >> 12) & 3.U
  def encodeFn(masterIdx: Int, offset: Int) = {
    ((masterIdx << 12) | offset)
  }

  def moduleFn = new Demux(
    axi4.Config(
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

  useVerilator()
  enableVcd()

  private implicit val helper: InterconnectHelper[Demux] = new InterconnectHelper[Demux] {
    def slaveInterfaces(module: Demux): Seq[axi4.full.Interface] = Seq(module.s_axi)
    def masterInterfaces(module: Demux): Seq[axi4.full.Interface] = module.m_axi.toSeq
  }

  "AXI4 Full Demux (basic)" in testWithTester(moduleFn) {
    new InterconnectTester(_, 2000) {
      protected def createTasks(): Unit = {
        for (masterIdx <- (0 until 4)) {
          for (id <- (0 until 16)) {
            readTask(
              0,
              masterIdx,
              encodeFn(masterIdx, rand.nextInt(32) << 2),
              len = rand.nextInt(32),
              id = id
            )

            writeTask(
              0,
              masterIdx,
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
