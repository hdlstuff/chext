package chext.tracking

import chisel3.Data
import chisel3.experimental.BaseModule
import chisel3.hacks.ModuleInternals
import chisel3.reflect.DataMirror

/** Builds canonical absolute paths for tracked modules, components, and data. */
private[chext] object Path {

  /** Appends one local element without creating a double slash at the root. */
  private def append(parent: String, child: String): String = {
    val prefix = if (parent == "/") "" else parent
    s"$prefix/$child"
  }

  /** Converts Chisel aggregate notation into a slash-path-safe local name. */
  private def localDataName(data: Data): String =
    DataMirror
      .queryNameGuess(data)
      .replace('.', '_')
      .replaceAll("\\[([^\\]]+)\\]", "_$1")

  /** Returns the absolute tracking path of Chisel data. */
  def data(data: Data): String = {
    val module = ModuleInternals.getParent(data).getOrElse {
      throw new IllegalStateException("Tracked data has no containing module")
    }
    append(Manager.registerModule(module).absolutePath, localDataName(data))
  }

  /** Returns the absolute tracking path of a component. */
  def component(component: Component): String =
    append(component.moduleInfo.absolutePath, component.pathStr)

  /** Returns the absolute tracking path of a Chisel module. */
  def module(module: BaseModule): String =
    Manager.registerModule(module).absolutePath
}
