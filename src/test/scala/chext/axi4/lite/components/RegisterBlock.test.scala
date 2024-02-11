package chext.axi4.lite.components

import chext.axi4
import chext.elastic

import chisel3._
import chisel3.experimental.prefix
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import axi4.ResponseFlag
import axi4.lite._
import axi4.lite.{WriteDataChannel, AddressChannel, ReadDataChannel, WriteResponseChannel}
import axi4.lite.components.RegisterBlock
import chext.axi4.ResponseFlag

class RegisterBlockTestDevice extends Module {
  private val regBlock = prefix("regBlock") { new RegisterBlock(32, 32, 8) }
  
  @annotation.nowarn /* suppress warning: Implicit definition should have explicit type */
  implicit val axiConfig = regBlock.cfgAxi

  val saxil = IO(chext.axi4.lite.ReadWriteInterface.slave)

  saxil <> regBlock.s_axil

  private val reg1 = RegInit(0.U(32.W))
  private val reg2 = RegInit(0.U(32.W))

  regBlock.base(0x00)
  regBlock.reg(reg1, read = true, write = true)
  regBlock.reg(reg2, read = true, write = true)
  regBlock.reg(48.U, read = true, write = false)

  private val region1 = regBlock.reserve(64)

  when(regBlock.rdReq) {
    when(regBlock.rdAddr >= region1.U) {
      regBlock.rdOk(regBlock.rdAddr - region1.U)
    }.otherwise {
      regBlock.rdOk()
    }
  }

  when(regBlock.wrReq) {
    regBlock.wrOk()
  }
}

class RegisterBundleSpec extends AnyFreeSpec with ChiselScalatestTester {
  "RegisterBlockTestDevice should perform correctly" in {
    test(new RegisterBlockTestDevice).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      {
        implicit val axiConfig = dut.axiConfig

        dut.saxil.ar.initSource()
        dut.saxil.ar.setSourceClock(dut.clock)

        dut.saxil.r.initSink()
        dut.saxil.r.setSinkClock(dut.clock)

        dut.saxil.aw.initSource()
        dut.saxil.aw.setSourceClock(dut.clock)

        dut.saxil.w.initSource()
        dut.saxil.w.setSourceClock(dut.clock)

        dut.saxil.b.initSink()
        dut.saxil.b.setSinkClock(dut.clock)

        fork {
          dut.saxil.ar.enqueue((new AddressChannel).Lit(_.addr -> 24.U, _.prot -> 0.U))
          dut.saxil.aw.enqueue((new AddressChannel).Lit(_.addr -> 0.U, _.prot -> 0.U))
          dut.saxil.w.enqueue((new WriteDataChannel).Lit(_.data -> 0x0FEE_DEAD.U, _.strb -> 0x0C.U))
        }.fork {
          dut.saxil.r.expectDequeue((new ReadDataChannel).Lit(_.data -> 12.U, _.resp -> ResponseFlag.OKAY))
          dut.saxil.b.expectDequeue((new WriteResponseChannel).Lit(_.resp -> ResponseFlag.OKAY))
        }.join()

        fork {
          dut.saxil.ar.enqueue((new AddressChannel).Lit(_.addr -> 0.U, _.prot -> 0.U))
          dut.saxil.ar.enqueue((new AddressChannel).Lit(_.addr -> 4.U, _.prot -> 0.U))
          dut.saxil.ar.enqueue((new AddressChannel).Lit(_.addr -> 8.U, _.prot -> 0.U))
        }.fork {
          dut.saxil.r.expectDequeue((new ReadDataChannel).Lit(_.data -> 0x0FEE_0000.U, _.resp -> ResponseFlag.OKAY))
          dut.saxil.r.expectDequeue((new ReadDataChannel).Lit(_.data -> 0.U, _.resp -> ResponseFlag.OKAY))
          dut.saxil.r.expectDequeue((new ReadDataChannel).Lit(_.data -> 48.U, _.resp -> ResponseFlag.OKAY))
        }.join()
      }
    }
  }
}

