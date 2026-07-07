package chext.elastic.tracking

import chext.tracking

object Tag extends tracking.Tag {
  type CS = ComponentState
  type MS = ModuleState

  val name: String = "elastic"

  def newModuleState(moduleInfo: tracking.ModuleInfo): ModuleState =
    new ModuleState(moduleInfo)
}
