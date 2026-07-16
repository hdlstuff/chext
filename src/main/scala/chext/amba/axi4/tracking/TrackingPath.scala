package chext.amba.axi4.tracking

import chisel3.Data
import chisel3.hacks.ModuleInternals
import chisel3.reflect.DataMirror

import chext.tracking.{Component, Manager}

private[axi4] object TrackingPath {
  private def append(parent: String, child: String): String = {
    val prefix = if (parent == "/") "" else parent
    s"$prefix/$child"
  }

  private def localDataName(data: Data): String =
    DataMirror
      .queryNameGuess(data)
      .replace('.', '_')
      .replaceAll("\\[([^\\]]+)\\]", "_$1")

  def interface(interface: Tracked): String = {
    val data = interface.asInstanceOf[Data]
    val module = ModuleInternals.getParent(data).getOrElse {
      throw new IllegalStateException("AXI interface has no containing module")
    }
    append(Manager.registerModule(module).absolutePath, localDataName(data))
  }

  def component(component: Component): String =
    append(component.moduleInfo.absolutePath, component.pathStr)

  def module(module: chisel3.experimental.BaseModule): String =
    Manager.registerModule(module).absolutePath
}
