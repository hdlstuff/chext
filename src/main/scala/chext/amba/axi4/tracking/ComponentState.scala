package chext.amba.axi4.tracking

/** AXI-domain tracking state attached to one unified Chext component.
  *
  * The class is currently a domain marker; AXI property state lives directly on [[Tracked]]
  * interfaces.
  */
final class ComponentState private[tracking] (component: chext.tracking.Component)
    extends chext.tracking.ComponentState
