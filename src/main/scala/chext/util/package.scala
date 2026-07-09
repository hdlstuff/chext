package chext

import chisel3.experimental.DeprecatedSourceInfo
import chisel3.experimental.SourceInfo
import chisel3.experimental.SourceLine
import chisel3.experimental.UnlocatableSourceInfo

package object util {
  def sourceInfoToString(x: SourceInfo): String =
    x match {
      case UnlocatableSourceInfo => "(unlocatable)"
      case DeprecatedSourceInfo  => "(deprecated)"
      case SourceLine(filename, line, col) =>
        if (col == 0) s"$filename $line"
        else s"$filename $line:$col"
    }
}
