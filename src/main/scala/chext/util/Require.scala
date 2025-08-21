package chext.util

import chisel3.experimental.SourceInfo

case class Require(val identifier: String) {
  val indent = " " * identifier.length

  private def printFirst(message: String, si: SourceInfo): Unit = {
    val source = si.makeMessage((x) => (x))
    println(f"${identifier} : $message $source")
  }

  private def printNext(message: String): Unit = {
    println(f"${indent}   $message")
  }

  def apply(cond: Boolean, message: String)(implicit si: SourceInfo): Unit = {
    if (!cond) {
      printFirst(message, si)
      throw new IllegalArgumentException("requirement failed")
    }
  }

  def apply(cond: Boolean, message: String, lines: Seq[String])(implicit si: SourceInfo): Unit = {
    if (!cond) {
      printFirst(message, si)
      lines.foreach { printNext(_) }
      throw new IllegalArgumentException("requirement failed")
    }
  }
}

private object MyModule_Emit extends App {
  import chisel3._

  import chext.elastic
  import elastic.ConnectOp._

  import chext.amba.axi4
  import axi4.Ops._

  class MyModule extends Module {
    val io = IO(new Bundle {
      val s_axi = axi4.full.Slave(axi4.Config(wAddr = 8, wData = 256, wId = 5))
      val m_axi = axi4.full.Master(axi4.Config(wAddr = 8, wData = 256, wId = 8))
      val source = elastic.Source(UInt(32.W))
      val sink = elastic.Sink(UInt(32.W))
    })

    val master = io.s_axi
    val slave = io.m_axi

    axi4.full.LeftBuffer(master) :=> slave
    // master :=> slave

    // should fail without the following line
    io.source :=> io.sink

    // this is not OK, throws a warning
    io.sink.bits := 0.U

    // this is not OK, supresses the warning
    io.sink.$bits := 0.U
  }

  // System.out.println("Working Directory = " + System.getProperty("user.dir"));
  emitVerilog(new MyModule, Array("--target-dir", "output/"))
}
