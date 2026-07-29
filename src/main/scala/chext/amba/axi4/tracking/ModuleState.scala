package chext.amba.axi4.tracking

import chisel3.RawModule
import chisel3.experimental.SourceInfo
import chisel3.hacks.ModuleInternals

import java.util.IdentityHashMap

import scala.collection.mutable.ArrayBuffer

import chext.tracking

/** AXI-domain tracking state attached to one registered Chisel module. */
final class ModuleState private[tracking] (moduleInfo: tracking.ModuleInfo)
    extends tracking.ModuleState {
  private val registeredInterfaces = ArrayBuffer.empty[Tracked]
  private var instanceSourceInfo_ = Option.empty[SourceInfo]
  private var instanceSourceInfoAssigned = false
  private var checked = false

  /** Source location at which this module is instantiated, or `None` for the root module. */
  private[tracking] def instanceSourceInfo: Option[SourceInfo] = {
    if (!instanceSourceInfoAssigned)
      throw new IllegalStateException(
        s"Instantiation SourceInfo for ${moduleInfo.module} was requested before AXI hierarchy assignment"
      )
    instanceSourceInfo_
  }

  /** Assigns instantiation locations to this state and all descendant AXI module states.
    *
    * Chisel records child instantiation locations as `DefInstance` commands in the parent. Running
    * this once from the completed root hierarchy scans every tracked parent at most once.
    */
  private def assignInstanceSourceInfos(sourceInfo: Option[SourceInfo]): Unit = {
    require(
      !instanceSourceInfoAssigned,
      s"Instantiation SourceInfo for ${moduleInfo.module} was assigned more than once"
    )
    instanceSourceInfo_ = sourceInfo
    instanceSourceInfoAssigned = true

    if (moduleInfo.children.nonEmpty) {
      val parentModule =
        moduleInfo.module match {
          case rawModule: RawModule => rawModule
          case other =>
            throw new IllegalStateException(
              s"Cannot obtain child instantiation SourceInfos from non-RawModule parent $other"
            )
        }
      val childSourceInfos = ModuleInternals.getChildrenSourceInfo(parentModule)

      moduleInfo.children.foreach { case (childModule, childInfo) =>
        val childSourceInfo =
          childSourceInfos.getOrElse(
            childModule,
            throw new IllegalStateException(
              s"Instantiation SourceInfo for $childModule is missing from its completed parent $parentModule"
            )
          )
        childInfo
          .trackingState(Tag)
          .assignInstanceSourceInfos(Some(childSourceInfo))
      }
    }
  }

  /** Creates the AXI-domain state for a component owned by this module. */
  def newComponentState(component: tracking.Component): ComponentState =
    new ComponentState(component)

  /** Records a DataView-created interface, which is not discoverable as an ordinary wire or port.
    */
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
      assignInstanceSourceInfos(None)
      Checker.check(treeInterfaces)
    }
  }

  /** Child states wait for their parent so connections registered outside child bodies are visible.
    */
  def onComplete(): Unit =
    if (moduleInfo.parent.isEmpty)
      checkRoot()
}
