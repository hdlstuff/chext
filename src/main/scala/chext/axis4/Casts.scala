package chext.axis4

import chisel3._
import chisel3.util._
import chisel3.experimental.dataview._

object Casts {
  implicit class viewInterfaceAs(x: Interface) {
    import FullChannel._
    import BasicChannel._

    def lite = x.viewAs[IrrevocableIO[Bits]]
    def full = x.viewAs[IrrevocableIO[FullChannel]]
  }
}
