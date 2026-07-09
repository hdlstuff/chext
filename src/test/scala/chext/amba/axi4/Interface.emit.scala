package chext.amba.axi4

import chisel3._

import chext.amba.axi4
import axi4.Ops._

class FullInterfaceEmitDevice extends Module {
  private val cfg1 = axi4.Config(read = true, write = false)
  private val cfg2 = axi4.Config(wUserAR = 1, wUserB = 5)

  val slave1 = IO(axi4.Slave(cfg1))
  val master1 = IO(axi4.Master(cfg1))

  val slave2 = IO(axi4.Slave(cfg2))
  val master2 = IO(axi4.Master(cfg2))

  val slave3 = IO(axi4.Slave(cfg2))
  val master3 = IO(axi4.Master(cfg2))

  slave1.asFull :=> master1.asFull
  slave2.asFull :=> master2.asFull
  slave2.asFull :=> master2.asFull
  slave3.asFull :=> master3.asFull
}

object FullInterface_Emit extends App {
  emitVerilog(new FullInterfaceEmitDevice, Array("--target-dir", "output/"))
}

class LiteInterfaceEmitDevice extends Module {
  private val cfg1 = axi4.Config(read = true, write = false, lite = true)
  private val cfg2 = axi4.Config(lite = true)

  val slave1 = IO(axi4.Slave(cfg1))
  val master1 = IO(axi4.Master(cfg1))

  val slave2 = IO(axi4.Slave(cfg2))
  val master2 = IO(axi4.Master(cfg2))

  val slave3 = IO(axi4.Slave(cfg2))
  val master3 = IO(axi4.Master(cfg2))

  slave1.asLite :=> master1.asLite
  slave2.asLite :=> master2.asLite
  slave2.asLite :=> master2.asLite
  slave3.asLite :=> master3.asLite
}

object LiteInterface_Emit extends App {
  emitVerilog(new LiteInterfaceEmitDevice, Array("--target-dir", "output/"))
}
