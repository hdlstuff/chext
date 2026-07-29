package chext.amba.axi4.tracking

import chisel3.experimental.{BaseModule, SourceInfo}
import chisel3.hacks.ModuleInternals
import chisel3.{Module => ChiselModule}

import chext.tracking.{Component, Path}

/** Module or component that owns an AXI property resolver.
  *
  * Owners provide resolver-precedence and diagnostic context. AXI module states receive module
  * instantiation information in one hierarchy pass immediately before checking.
  */
sealed trait Owner {

  /** Absolute tracking path of this owner. */
  def path: String

  /** Source location at which this owner is defined. */
  def sourceInfo: SourceInfo

  /** Chisel module containing this owner. */
  def module: BaseModule

  /** Absolute tracking path of the containing module. */
  def modulePath: String

  /** Source location at which the containing module is defined. */
  def moduleDefinitionSourceInfo: SourceInfo

  /** Source location at which the containing module is instantiated, or `None` for the root. */
  def moduleInstantiationSourceInfo: Option[SourceInfo]

  private[tracking] def hierarchyDepth: Int
}

object Owner {

  /** Resolves an implicit owner, falling back to Chisel's current module when elaborating. */
  private[tracking] def resolve(owner: Owner): Option[Owner] =
    Option(owner).orElse(ChiselModule.currentModule.map(Owner(_)))

  /** Creates ownership by a unified tracked component. */
  def apply(component: Component): Owner =
    ComponentOwner(component)

  /** Creates ownership directly by a Chisel module. */
  def apply(module: BaseModule): Owner =
    Module(module)

  /** Resolver ownership by a unified tracked component. */
  final case class ComponentOwner(component: Component) extends Owner {
    private lazy val moduleInfo = component.moduleInfo

    lazy val path: String = Path.component(component)
    val sourceInfo: SourceInfo = component.sourceInfo
    lazy val module: BaseModule = moduleInfo.module
    lazy val modulePath: String = moduleInfo.absolutePath
    lazy val moduleDefinitionSourceInfo: SourceInfo =
      ModuleInternals.getSourceInfo(module)
    lazy val moduleInstantiationSourceInfo: Option[SourceInfo] =
      moduleInfo.trackingState(Tag).instanceSourceInfo

    private[tracking] lazy val hierarchyDepth: Int = {
      @scala.annotation.tailrec
      def loop(current: Component, depth: Int): Int =
        current.parentOption match {
          case Some(parent) => loop(parent, depth + 1)
          case None         => depth
        }

      loop(component, 1)
    }
  }

  /** Resolver ownership by a Chisel module.
    *
    * Modules use depth zero because they form the enclosing resolution boundary. A null module is
    * accepted only for lightweight unit-test resolvers and is represented by the root path. Source
    * metadata is unavailable for that test-only owner.
    */
  final case class Module(module: BaseModule) extends Owner {
    private val moduleInfo =
      if (module eq null) null
      else chext.tracking.Manager.registerModule(module)

    lazy val path: String =
      if (module eq null) "/"
      else moduleInfo.absolutePath

    lazy val sourceInfo: SourceInfo =
      requireModule(ModuleInternals.getSourceInfo(module))

    lazy val modulePath: String =
      if (module eq null) "/"
      else moduleInfo.absolutePath

    lazy val moduleDefinitionSourceInfo: SourceInfo =
      requireModule(ModuleInternals.getSourceInfo(module))

    lazy val moduleInstantiationSourceInfo: Option[SourceInfo] =
      requireModule(moduleInfo.trackingState(Tag).instanceSourceInfo)

    private[tracking] val hierarchyDepth: Int = 0

    private def requireModule[T](value: => T): T = {
      require(module ne null, "Source metadata is unavailable for a null test Owner.Module")
      value
    }
  }
}
