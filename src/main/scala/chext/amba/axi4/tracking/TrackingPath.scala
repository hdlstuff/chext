package chext.amba.axi4.tracking

import chisel3.Data
import chisel3.hacks.ModuleInternals
import chisel3.reflect.DataMirror

import chext.tracking.{Component, Manager}

/** Builds canonical absolute paths used by AXI resolution traces.
  *
  * Interface paths combine their containing module's unified tracking path with a Chisel-derived
  * local data name. Component and module paths reuse the corresponding unified tracking identities.
  */
private[axi4] object TrackingPath {
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

  /** Returns the absolute tracking path of an AXI interface. */
  def interface(interface: Tracked): String = {
    val data = interface.asInstanceOf[Data]
    val module = ModuleInternals.getParent(data).getOrElse {
      throw new IllegalStateException("AXI interface has no containing module")
    }
    append(Manager.registerModule(module).absolutePath, localDataName(data))
  }

  /** Returns the absolute tracking path of a unified component. */
  def component(component: Component): String =
    append(component.moduleInfo.absolutePath, component.pathStr)

  /** Returns the absolute tracking path of a Chisel module. */
  def module(module: chisel3.experimental.BaseModule): String =
    Manager.registerModule(module).absolutePath
}
