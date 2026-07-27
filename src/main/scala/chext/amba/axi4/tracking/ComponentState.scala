package chext.amba.axi4.tracking

/** AXI-domain marker state attached to one unified Chext component.
  *
  * AXI property state lives directly on [[Tracked]] interfaces.
  */
final class ComponentState private[tracking] (component: chext.tracking.Component)
    extends chext.tracking.ComponentState
