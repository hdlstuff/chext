package chext.util

import chisel3.experimental.SourceInfo

case class Require(
    val identifier: String,
    val sourceInfo: Option[SourceInfo] = None
) {
  private val indent = " " * identifier.length

  private def sourceSuffix(si: SourceInfo): String = {
    if (si == null) ""
    else si.makeMessage(identity)
  }

  private def format(message: String, lines: Seq[String], si: SourceInfo): String = {
    val first = f"${identifier} : $message ${sourceSuffix(si)}"
    val rest = lines.map { line => f"${indent}   $line" }
    (first +: rest).mkString(System.lineSeparator())
  }

  private def raise(message: String, lines: Seq[String], si: SourceInfo): Nothing =
    throw new IllegalArgumentException(format(message, lines, si))

  def apply(cond: Boolean, message: String)(implicit si: SourceInfo = null): Unit = {
    if (!cond)
      raise(message, Seq.empty, sourceInfo.orNull)
  }

  def apply(
      cond: Boolean,
      message: String,
      lines: Seq[String]
  )(implicit si: SourceInfo): Unit = {
    if (!cond)
      raise(message, lines, sourceInfo.orNull)
  }

  def here(cond: Boolean, message: String)(implicit si: SourceInfo): Unit =
    if (!cond)
      failHere(message)

  def here(
      cond: Boolean,
      message: String,
      lines: Seq[String]
  )(implicit si: SourceInfo): Unit =
    if (!cond)
      failHere(message, lines)

  def fail(message: String): Nothing =
    fail(message, Seq.empty)

  def fail(message: String, lines: Seq[String]): Nothing =
    raise(message, lines, sourceInfo.orNull)

  def failHere(message: String)(implicit si: SourceInfo): Nothing =
    failHere(message, Seq.empty)

  def failHere(message: String, lines: Seq[String])(implicit
      si: SourceInfo
  ): Nothing =
    raise(message, lines, si)
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
