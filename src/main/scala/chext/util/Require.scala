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
