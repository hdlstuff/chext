package chext.amba.axi4s

import chisel3._
import chisel3.experimental.dataview._

import chext.{elastic2 => elastic}

object Casts {
  implicit class viewAxisInterfaceAs(x: Interface) {
    import FullChannel._
    import BasicChannel._

    @deprecated("Please use asLite instead, this will be removed.")
    def lite = x.viewAs[elastic.Interface[Bits]]

    @deprecated("Please use asFull instead, this will be removed.")
    def full = x.viewAs[elastic.Interface[FullChannel]]

    def asLite = x.viewAs[elastic.Interface[Bits]]
    def asFull = x.viewAs[elastic.Interface[FullChannel]]
  }
}
