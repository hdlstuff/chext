package chext.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental._

import chext.axi4
import chext.elastic

import axi4.ConnectTo._
import axi4.ResponseFlag
import axi4.full._
import axi4.full.test.InterconnectTester
import elastic.test.TesterSpec
import chext.axi4.{Config, ResponseFlag}

class MuxTester(
    val axiCfgSlave: axi4.Config,
    val numSlaves: Int = 4,
    val muxCfg: MuxConfig = MuxConfig()
) extends axi4.test.TestedModule {
  require(axiCfgSlave.read && axiCfgSlave.write)
  val mux = Module(new Mux(axiCfgSlave, numSlaves, muxCfg))

  val axiCfgMaster = mux.axiCfgMaster

  val s_axi = IO(
    Vec(numSlaves, axi4.full.ReadWriteInterface.slave(axiCfgSlave))
  )
  val m_axi = IO(axi4.full.ReadWriteInterface.master(axiCfgMaster))

  s_axi.zip(mux.S_AXI).foreach { case (a, b) => a :=> b }
  mux.M_AXI :=> m_axi

  s_axi.foreach { declareSlaveInterface(_) }
  declareMasterInterface(m_axi)
}

class MuxSpec extends elastic.test.TesterSpec {
  def moduleFn = new MuxTester(
    Config(
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

  "AXI4 Full Mux (basic)" in moduleTest(moduleFn) {
    new InterconnectTester(_) {
      protected def createTasks(): Unit = {
        for (i <- (0 until 4)) {
          for (slaveIdx <- (0 until 8)) {
            for (id <- (0 until 16)) {
              readTask(
                slaveIdx,
                (slaveIdx << 16) + (id << 12),
                len = rand.nextInt(32),
                id = id
              )

              writeTask(
                slaveIdx,
                (slaveIdx << 16) + (id << 12),
                len = rand.nextInt(32),
                id = id
              )
            }
          }
        }
      }
    }
  }
}
