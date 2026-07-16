package chext.amba.axi4.tracking

import chext.tracking

final class ModuleState private[tracking] (moduleInfo: tracking.ModuleInfo)
    extends tracking.ModuleState {
  def newComponentState(component: tracking.Component): ComponentState =
    new ComponentState(component)

  def onComplete(): Unit = ()
}
