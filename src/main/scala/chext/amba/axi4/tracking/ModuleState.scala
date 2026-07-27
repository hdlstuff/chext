package chext.amba.axi4.tracking

import java.util.IdentityHashMap

import scala.collection.mutable.ArrayBuffer

import chext.tracking

/** AXI-domain tracking state attached to one registered Chisel module. */
final class ModuleState private[tracking] (moduleInfo: tracking.ModuleInfo)
    extends tracking.ModuleState {
  private val registeredInterfaces = ArrayBuffer.empty[Tracked]
  private var checked = false

  /** Creates the AXI-domain state for a component owned by this module. */
  def newComponentState(component: tracking.Component): ComponentState =
    new ComponentState(component)

  /** Records a DataView-created interface, which is not discoverable as an ordinary wire or port. */
  private[axi4] def registerInterface(interface: Tracked): Unit = {
    if (!registeredInterfaces.exists(_ eq interface))
      registeredInterfaces.addOne(interface)
  }

  private def localInterfaces: Seq[Tracked] =
    registeredInterfaces.toSeq

  private def treeInterfaces: Seq[Tracked] = {
    val seen = new IdentityHashMap[Tracked, java.lang.Boolean]

    def collect(info: tracking.ModuleInfo): Seq[Tracked] = {
      val state = info.trackingState(Tag)
      state.localInterfaces ++ info.children.flatMap { case (_, child) => collect(child) }
    }

    collect(moduleInfo).filter(interface => seen.put(interface, java.lang.Boolean.TRUE) == null)
  }

  /** Runs the exhaustive pass once at the root of a complete module hierarchy. */
  private[axi4] def checkRoot(): Unit = {
    require(moduleInfo.parent.isEmpty, "AXI compatibility checking must start at the root module")
    if (!checked) {
      checked = true
      CompatibilityChecker.check(treeInterfaces)
    }
  }

  /** Child states wait for their parent so connections registered outside child bodies are visible. */
  def onComplete(): Unit =
    if (moduleInfo.parent.isEmpty)
      checkRoot()
}
