package chext.axi4.lite.components

import chext.axi4
import chext.elastic

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

import axi4.lite.TestUtils._
import chext.axi4.Config

class SyncReadMemControllerTest extends AnyFreeSpec with ChiselScalatestTester {
  "SyncReadMemController" in {
    test (new SyncReadMemController(4, Config(wData = 32, wAddr = 32, lite = true), debugEnabled = true)).withAnnotations(Seq(WriteVcdAnnotation)) {
      dut => {
        implicit val axiConfig = dut.axiCfg

        dut.s_axil.initReadInterface(dut.clock)
        dut.s_axil.initWriteInterface(dut.clock)

        dut.s_axil.writeRegister(0x04, 0xdead)
        println(dut.s_axil.readRegister(0x04))

        dut.s_axil.writeRegister(0x08, 0xbeef)
        dut.s_axil.writeRegister(0x0C, 0xdead)
        dut.s_axil.writeRegister(0x10, 0xbeef)
        println(dut.s_axil.readRegister(0x08))

        dut.clock.step(1)
      }
    }
  }
}
