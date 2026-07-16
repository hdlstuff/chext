package chext.amba.axi4.tracking

import chext.tracking

object Tag extends tracking.Tag {
  type CS = ComponentState
  type MS = ModuleState

  val name: String = "axi4"

  def newModuleState(moduleInfo: tracking.ModuleInfo): ModuleState =
    new ModuleState(moduleInfo)
}
