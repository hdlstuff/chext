package chext.util

import chisel3.experimental.SourceInfo

case class Require(val identifier: String) {
  val indent = " " * identifier.length

  private def printFirst(message: String, si: SourceInfo): Unit = {
    val source = si.makeMessage((x) => (x))
    println(f"${identifier} : $message $source")
  }

  private def printNext(message: String): Unit = {
    println(f"${indent} : $message")
  }

  def apply(cond: Boolean, message: String)(implicit si: SourceInfo): Unit = {
    if (!cond)
      printFirst(message, si)
  }

  def apply(cond: Boolean, message: String, lines: Seq[String])(implicit si: SourceInfo): Unit = {
    if (!cond) {
      printFirst(message, si)
      lines.foreach { printNext(_) }
    }
  }
}

import chisel3._

import chext.elastic
import elastic.ConnectOp._

import chext.amba.axi4
import axi4.Ops._

class MyModule extends Module {
  val s_axi = IO(axi4.full.Slave(axi4.Config(wAddr = 8, wData = 256, wId = 8)))
  val m_axi = IO(axi4.full.Master(axi4.Config(wAddr = 8, wData = 256, wId = 8)))
  val source = IO(elastic.Source(UInt(32.W)))
  val sink = IO(elastic.Sink(UInt(32.W)))

  val master = s_axi
  val slave = m_axi

  axi4.full.LeftBuffer(master) :=> slave
  // master :=> slave
}

object MyModule_Emit extends App {
  emitVerilog(new MyModule)
}
