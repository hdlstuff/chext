package chext.amba.axi4.tracking

import chext.tracking

/** AXI-domain tracking state attached to one registered Chisel module. */
final class ModuleState private[tracking] (moduleInfo: tracking.ModuleInfo)
    extends tracking.ModuleState {
  /** Creates the AXI-domain state for a component owned by this module. */
  def newComponentState(component: tracking.Component): ComponentState =
    new ComponentState(component)

  /** Completes AXI-domain tracking.
    *
    * AXI property resolution is demand-driven, so the domain currently has no end-of-module pass.
    */
  def onComplete(): Unit = ()
}
