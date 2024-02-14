package chext.axi4.full.components

import chext.axi4
import chext.elastic
import chext.{test => cht}

import chisel3._
import chisel3.util._

import chiseltest._

import chisel3.experimental.BundleLiterals._

class AddressDecoderTest extends cht.FreeSpec with cht.TestMixin {
  implicit val cfg = axi4.Config(wData = 128)
  enableVcd()

  "AddressDecoder" in test(new AddressDecoder(cfg)) { (dut) =>
    {
      dut.in.initSource()

      dut.out.ready.poke(true)
      
      dut.in.enqueue(
        (new axi4.full.ReadAddressChannel).Lit(
          _.id -> 0.U,
          _.addr -> 0x7000.U,
          _.len -> 64.U,
          _.size -> 7.U,
          _.burst -> axi4.BurstType.INCR,
          _.lock -> false.B,
          _.cache -> 0.U,
          _.prot -> 0.U,
          _.qos -> 0.U,
          _.region -> 0.U
        )
      )

      dut.in.enqueue(
        (new axi4.full.ReadAddressChannel).Lit(
          _.id -> 0.U,
          _.addr -> 0x9000.U,
          _.len -> 32.U,
          _.size -> 7.U,
          _.burst -> axi4.BurstType.INCR,
          _.lock -> false.B,
          _.cache -> 0.U,
          _.prot -> 0.U,
          _.qos -> 0.U,
          _.region -> 0.U
        )
      )

      dut.clock.step(128)
    }
  }
}
