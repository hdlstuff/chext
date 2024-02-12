package chext.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.experimental.BundleLiterals._

import chiseltest._

import chext.axi4
import axi4.full.components.{InterconnectTester, InterconnectHelper}

class MuxSpec extends chext.test.TesterSpec {
  def moduleFn = new Mux(
    axi4.Config(
      wId = 4,
      wAddr = 32,
      wData = 32,
      read = true,
      write = true,
      lite = false
    ),
    8,
    MuxConfig(
      slaveBuffers = axi4.BufferConfig.all(1),
      masterBuffers = axi4.BufferConfig.all(1)
    )
  )

  enableVcd()
  useVerilator()

  private implicit val helper: InterconnectHelper[Mux] =
    new InterconnectHelper[Mux] {
      def slaveInterfaces(module: Mux): Seq[axi4.full.Interface] =
        module.s_axi.toSeq

      def masterInterfaces(module: Mux): Seq[axi4.full.Interface] =
        Seq(module.m_axi)
    }

  "AXI4 Full Mux (basic)" in test(moduleFn) {
    new InterconnectTester(_, true) {
      protected def createTasks(): Unit = {
        for (i <- (0 until 4)) {
          for (slaveIdx <- (0 until 8)) {
            for (id <- (0 until 16)) {
              for (n <- (0 until 2)) {
                readTask(
                  slaveIdx,
                  0,
                  (slaveIdx << 16) + (id << 12),
                  len = rand.nextInt(32),
                  id = id
                )

                writeTask(
                  slaveIdx,
                  0,
                  (slaveIdx << 16) + (id << 12),
                  len = rand.nextInt(32),
                  id = id
                )
              }
            }
          }
        }
      }
    }.run()
  }
}
