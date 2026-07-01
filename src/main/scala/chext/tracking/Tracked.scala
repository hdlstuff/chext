package chext.tracking

import chisel3.{ChiselException, Data, Module, RawModule}
import chisel3.experimental.{SourceInfo, BaseModule, requireIsHardware}
import chisel3.hacks.{DataInternals, ModuleInternals}

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import chext.tracking.util.sourceInfoToString

sealed abstract case class DeclaredRole(str: String)

object DeclaredRole {
  object Source extends DeclaredRole("Source")
  object Sink extends DeclaredRole("Sink")
  object None extends DeclaredRole("None")
}

private[tracking] object Tracked {
  val logger = new Logger("tracking")
}

/** This trait brings `markSource()`, `markSink()`, `sanityCheck()` capabilities to `Interface`.
  */
trait Tracked extends Data {
  def tpe: String
  def declaredRole: DeclaredRole

  def sourceInfo: SourceInfo

  private def module_ = DataInternals.getOwningModuleOption(this)

  private val markSource_ = ArrayBuffer.empty[(BaseModule, SourceInfo)]
  private val markSink_ = ArrayBuffer.empty[(BaseModule, SourceInfo)]

  private def mark_(role: DeclaredRole, array: ArrayBuffer[(BaseModule, SourceInfo)])(implicit
      sourceInfo: SourceInfo
  ): Unit = {
    requireIsHardware(
      this,
      f"chext.tracking.Tracked: mark${role.str} must be called on a hardware!"
    )

    val currentModule = Module.currentModule.getOrElse(
      throw new ChiselException(
        f"chext.tracking.Tracked: mark${role.str} must be called from a module!"
      )
    )
    array.addOne((currentModule, sourceInfo))

    // chisel can catch this mistake too, but the error message is not great
    // check 5 below.
    assert(module_.nonEmpty)

    if (currentModule == module_.get) {
      if (declaredRole != DeclaredRole.None && declaredRole != role) {
        val pos = sourceInfoToString(sourceInfo)

        throw new ChiselException(
          f"chext.tracking.Tracked: Interface '$this' is declared as a ${declaredRole.str}, but marked as ${role.str}. @[$pos]!"
        )
      }
    }

    // ensure that the current module is tracked
    Manager.registerCurrentModule()
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
  private[tracking] final def sanityCheck(
      logger: Logger,
      isRootIO: Boolean = false,
      instanceSourceInfo: Option[SourceInfo] = Option.empty,
      displayPath: Option[String] = Option.empty,
      displayOwner: Option[BaseModule] = Option.empty,
      displaySourceInfo: Option[SourceInfo] = Option.empty
  ): Unit = {
    val currentModule = Module.currentModule.getOrElse(
      throw new ChiselException(
        "chext.tracking.Tracked: sanityCheck must be called from a module!"
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

    // log("debug")

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
