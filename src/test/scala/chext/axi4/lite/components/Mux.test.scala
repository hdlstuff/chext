package chext.axi4.lite.components

import chisel3._
import chisel3.util._
import chisel3.experimental._

import chext.axi4
import chext.elastic

import axi4.ConnectTo._
import axi4.ResponseFlag
import axi4.lite._
import axi4.lite.test.InterconnectTester
import elastic.test.TesterSpec
import chext.axi4.{Config, ResponseFlag}

class MuxTester(
    val axiCfgSlave: axi4.Config,
    val numSlaves: Int = 4,
    val muxCfg: MuxConfig = MuxConfig()
) extends axi4.test.TestedModule {
  require(axiCfgSlave.read && axiCfgSlave.write)
  val mux = Module(new Mux(axiCfgSlave, numSlaves, muxCfg))

  val axiCfg = mux.axiCfg

  val s_axil = IO(Vec(numSlaves, ReadWriteInterface.slave(axiCfg)))
  val m_axil = IO(ReadWriteInterface.master(axiCfg))

  s_axil.zip(mux.S_AXIL).foreach { case (a, b) => a :=> b }
  mux.M_AXIL :=> m_axil

  s_axil.foreach { declareSlaveInterface(_) }
  declareMasterInterface(m_axil)
}

class MuxSpec extends elastic.test.TesterSpec {
  def moduleFn = new MuxTester(
    Config(
      wAddr = 32,
      wData = 32,
      read = true,
      write = true,
      lite = true
    ),
    8,
    MuxConfig(
      slaveBuffers = axi4.BufferConfig.all(1),
      masterBuffers = axi4.BufferConfig.all(1)
    )
  )

  enableVcd()
  useVerilator()

  "AXI4 Lite Mux (basic)" in moduleTest(moduleFn) {
    new InterconnectTester(_) {
      protected def createTasks(): Unit = {
        for (i <- (0 until 64)) {
          for (slaveIdx <- (0 until 8)) {
            readTask(slaveIdx, (slaveIdx << 16))
            writeTask(slaveIdx, (slaveIdx << 16))
          }
        }
      }
    }
  }
}
