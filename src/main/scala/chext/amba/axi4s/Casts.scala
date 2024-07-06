package chext.amba.axi4s

import chisel3._
import chisel3.util._
import chisel3.experimental.dataview._

object Casts {
  implicit class viewAxisInterfaceAs(x: Interface) {
    import FullChannel._
    import BasicChannel._

    def lite = x.viewAs[IrrevocableIO[Bits]]
    def full = x.viewAs[IrrevocableIO[FullChannel]]
  }
}
