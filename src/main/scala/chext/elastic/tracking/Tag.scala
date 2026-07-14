package chext.elastic.tracking

import chext.tracking

object Tag extends tracking.Tag {
  type CS = ComponentState
  type MS = ModuleState

  val name: String = "elastic"

  // Explicitly forces this tag to register with the tracking registry.
  private[tracking] def initialize(): Unit = ()

  def newModuleState(moduleInfo: tracking.ModuleInfo): ModuleState =
    new ModuleState(moduleInfo)
}
