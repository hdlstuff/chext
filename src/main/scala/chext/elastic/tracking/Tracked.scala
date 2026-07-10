package chext.elastic.tracking

import chisel3.{ChiselException, Data, Module, SpecifiedDirection}
import chisel3.experimental.{BaseModule, SourceInfo, requireIsHardware}
import chisel3.hacks.DataInternals
import chisel3.reflect.DataMirror

import scala.collection.mutable.ArrayBuffer

import chext.util.Logger
import chext.util.sourceInfoToString

sealed abstract case class DeclaredRole(str: String)

object DeclaredRole {
  object Source extends DeclaredRole("Source")
  object Sink extends DeclaredRole("Sink")
  object None extends DeclaredRole("None")

  /** Returns the opposite endpoint role, leaving `None` role-neutral.
    */
  private[chext] def invert(role: DeclaredRole): DeclaredRole =
    role match {
      case Source => Sink
      case Sink   => Source
      case None   => None
    }
}

object Tracked {
  val logger = new Logger("tracking")

  private def isOwnedByCurrentModule(source: Data): Boolean =
    Module.currentModule.exists { current =>
      DataInternals.getOwningModuleOption(source).contains(current)
    }

  /** Returns the source/sink role of an IO-like Data from the current module's perspective.
    *
    * Non-IO data is role-neutral and returns `None`. If the current module is looking at child IO,
    * the owner-side role is inverted.
    */
  private[chext] def roleFromCurrentModule(source: Data): Option[DeclaredRole] =
    if (DataInternals.isIO(source)) {
      val roleInOwner =
        DataMirror.specifiedDirectionOf(source) match {
          case SpecifiedDirection.Flip => DeclaredRole.Source
          case _                       => DeclaredRole.Sink
        }

      Some {
        if (isOwnedByCurrentModule(source))
          roleInOwner
        else
          DeclaredRole.invert(roleInOwner)
      }
    } else
      None

  /** Overrides the declared source/sink role used by tracking mark checks.
    *
    * This is used for DataView-created endpoints whose Scala object is not ordinary IO, but whose
    * backing raw interface has a protocol role.
    */
  private[chext] def enforceRole(tracked: Tracked, role: DeclaredRole): Unit =
    tracked.enforceRole_(role)
}

/** This trait brings `markSource()`, `markSink()`, `sanityCheck()` capabilities to Elastic
  * interfaces.
  */
trait Tracked extends Data {
  /** @todo
    *   Consider removing `tpe` from `Tracked`. Elastic module-graph generation is deliberately
    *   coupled to `chext.elastic.Interface`, so the graph type can be derived from that concrete
    *   interface instead of making `Tracked` look more generic than it is. Apply the same
    *   closed-world reasoning to downstream consumers such as the C++ deadlock-detection code.
    */
  def tpe: String

  def declaredRole: DeclaredRole

  def sourceInfo: SourceInfo

  private def module_ = DataInternals.getOwningModuleOption(this)

  private val markSource_ = ArrayBuffer.empty[(BaseModule, SourceInfo)]
  private val markSink_ = ArrayBuffer.empty[(BaseModule, SourceInfo)]
  private var enforcedRole_ = Option.empty[DeclaredRole]

  /** Stores the effective role for endpoints whose `declaredRole` cannot describe the backing IO.
    */
  private[tracking] final def enforceRole_(role: DeclaredRole): Unit =
    enforcedRole_ = Some(role)

  private def mark_(role: DeclaredRole, array: ArrayBuffer[(BaseModule, SourceInfo)])(implicit
      sourceInfo: SourceInfo
  ): Unit = {
    requireIsHardware(
      this,
      f"chext.elastic.tracking.Tracked: mark${role.str} must be called on a hardware!"
    )

    val currentModule = Module.currentModule.getOrElse(
      throw new ChiselException(
        f"chext.elastic.tracking.Tracked: mark${role.str} must be called from a module!"
      )
    )
    array.addOne((currentModule, sourceInfo))

    // chisel can catch this mistake too, but the error message is not great
    // check 5 below.
    assert(module_.nonEmpty)

    val effectiveRole = enforcedRole_.getOrElse(declaredRole)

    if (enforcedRole_.nonEmpty || currentModule == module_.get) {
      if (effectiveRole != DeclaredRole.None && effectiveRole != role) {
        val pos = sourceInfoToString(sourceInfo)

        throw new ChiselException(
          f"chext.elastic.tracking.Tracked: Interface '$this' is declared as a ${effectiveRole.str}, but marked as ${role.str}. @[$pos]!"
        )
      }
    }

    // ensure that the current module is tracked
    chext.tracking.Manager.registerCurrentModule()
  }

  final def markSource()(implicit sourceInfo: SourceInfo): Unit =
    mark_(DeclaredRole.Source, markSource_)

  final def markSink()(implicit sourceInfo: SourceInfo): Unit =
    mark_(DeclaredRole.Sink, markSink_)

  /** Sanity checks on the interface.
    *
    * @param isRootIO
    *   True if the interface is an IO port of the root module. Such interfaces cannot be marked
    *   both sink and source.
    *
    * @param instanceSourceInfo
    *   If an interface belongs to child, describes where it is instantiated.
    */
  private[chext] final def sanityCheck(
      logger: Logger,
      isRootIO: Boolean = false,
      instanceSourceInfo: Option[SourceInfo] = Option.empty,
      displayPath: Option[String] = Option.empty,
      displayOwner: Option[BaseModule] = Option.empty,
      displaySourceInfo: Option[SourceInfo] = Option.empty
  ): Unit = {
    Module.currentModule.getOrElse(
      throw new ChiselException(
        "chext.elastic.tracking.Tracked: sanityCheck must be called from a module!"
      )
    )

    def warn(msg: String): Unit = {
      val lines = ArrayBuffer.empty[String]

      val pos = sourceInfoToString(displaySourceInfo.getOrElse(sourceInfo))
      val moduleName = displayOwner.orElse(module_).map(_.toString()).getOrElse("(null)")
      val interfaceName = displayPath.getOrElse(this.toString())

      lines.addOne(msg)

      if (instanceSourceInfo.nonEmpty) {
        val pos = sourceInfoToString(instanceSourceInfo.get)
        lines.addOne(
          f"Module '$moduleName' with instance name '${module_.get.instanceName}' is instantiated @[$pos]"
        )
      }

      lines.addOne(f"Interface '$interfaceName' is defined by '$moduleName' @[$pos]")

      markSource_.foreach {
        case (module, si) => {
          val pos = sourceInfoToString(si)
          lines.addOne(f"Marked as source by '$module' @[$pos]")
        }
      }

      markSink_.foreach {
        case (module, si) => {
          val pos = sourceInfoToString(si)
          lines.addOne(f"Marked as sink by '$module' @[$pos]")
        }
      }

      logger.warn("sanityChecks", lines.toSeq: _*)
    }

    // Check (1)
    if (!isRootIO) {
      if (markSource_.length >= 1 && markSink_.length == 0) {
        warn("Interface is marked as a source but never as a sink!")
      }

      if (markSink_.length >= 1 && markSource_.length == 0) {
        warn("Interface is marked as a sink but never as a source!")
      }
    }

    // Check (2)
    if (markSource_.isEmpty && markSink_.isEmpty) {
      warn("Interface never marked!")
    }

    // Check (3)
    if (markSource_.length > 1) {
      warn("Interface marked as source more than 1 times!")
    }

    // Check (4)
    if (markSink_.length > 1) {
      warn("Interface marked as sink more than 1 times!")
    }
  }
}
