package chext.amba.axi4.tracking

import chext.tracking

/** Unified-tracking tag that attaches AXI-specific state to modules and components. */
object Tag extends tracking.Tag {
  type CS = ComponentState
  type MS = ModuleState

  /** Stable domain name used by the unified tracking manager. */
  val name: String = "axi4"

  /** Creates the AXI tracking state associated with one registered module. */
  def newModuleState(moduleInfo: tracking.ModuleInfo): ModuleState =
    new ModuleState(moduleInfo)
}
