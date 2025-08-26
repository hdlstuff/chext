package chext.tracking

import chisel3.experimental.SourceInfo
import chisel3.experimental.UnlocatableSourceInfo
import chisel3.experimental.DeprecatedSourceInfo
import chisel3.experimental.SourceLine

object util {
  def sourceInfoToString(x: SourceInfo): String = {
    x match {
      case UnlocatableSourceInfo => "(unlocatable)"
      case DeprecatedSourceInfo  => "(deprecated)"
      case SourceLine(filename, line, col) =>
        if (col == 0) s"$filename $line"
        else s"$filename $line:$col"
    }
  }

  def baseComponentToString(baseComponent: BaseComponent): String = {
    assert(baseComponent.isComponent || baseComponent.isContainer)

    val str0 =
      if (baseComponent.isComponent) "Component"
      else "Container"

    f"$str0[${baseComponent.tpe}]: ${baseComponent.pathStr} @[${sourceInfoToString(baseComponent.sourceInfo)}]"
  }
}

class Logger(val name: String) {
  private def print(tpe: String, identifier: String, lines: String*): Unit = {
    assert(tpe.length == 4)

    val lineHead0 = f"[ $tpe ] $name/$identifier : "
    val lineHead1 = " " * lineHead0.length

    lines.zipWithIndex.foreach { //
      case (msg, idx) =>
        if (idx == 0) println(lineHead0 + msg)
        else println(lineHead1 + msg)
    }
  }

  def info(identifier: String, lines: String*): Unit =
    print("INFO", identifier, lines: _*)

  def error(identifier: String, lines: String*): Unit =
    print("ERR ", identifier, lines: _*)

  def warn(identifier: String, lines: String*): Unit =
    print("WARN", identifier, lines: _*)

}
