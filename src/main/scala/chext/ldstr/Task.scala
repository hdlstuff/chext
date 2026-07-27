package chext.ldstr

import chisel3._


class Task[T <: Data](genUser: T, wAddress: Int) extends Bundle {
  val address = UInt(wAddress.W)
  val user = genUser.cloneType
}
