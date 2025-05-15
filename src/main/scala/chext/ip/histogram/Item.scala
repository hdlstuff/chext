package chext.ip.histogram

import chisel3._
import chisel3.util._

class Item extends Bundle {
  val zero = Bool()
  val last = Bool()

  val bucket = UInt(64.W)
  val value = UInt(64.W)
}
