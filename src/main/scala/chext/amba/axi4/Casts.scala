package chext.amba.axi4

import chisel3.Module
import chisel3.experimental.BaseModule
import chisel3.experimental.SourceInfo
import chisel3.experimental.dataview._

import chisel3.hacks.DataInternals
import chisel3.hacks.ModuleInternals
import chext.tracking
import chext.tracking.DeclaredRole
import chext.tracking.Logger
import chext.tracking.util.sourceInfoToString

private[axi4] case class ViewCall(
    conversion: String,
    sourceInfo: SourceInfo,
    module: Option[BaseModule]
)

private object ViewWarnings {
  private val logger = new Logger("axi4")

  private def moduleString(module: Option[BaseModule]): String =
    module match {
      case Some(value) => s"'${value.toString()}'"
      case None => "(no current module)"
    }

  private def callString(call: ViewCall): String =
    s"Called .${call.conversion} by ${moduleString(call.module)} @[${sourceInfoToString(call.sourceInfo)}]"

  private def warningLines(x: RawInterface, current: ViewCall): Seq[String] =
    Seq(
      s"Interface '$x' is defined @[${sourceInfoToString(x.sourceInfo)}]",
      callString(current),
      "Chisel .viewAs does not create hardware, but each call may create a distinct Chext elastic wrapper.",
      "Repeated or non-root views can break Chext source/sink bookkeeping and endpoint identity.",
      "Safe pattern: call .asLite/.asFull exactly once at the root module on root IO, then reuse that elastic interface."
    )

  private def warn(x: RawInterface, message: String, current: ViewCall): Unit =
    logger.warn("axi4View", (Seq(message) ++ warningLines(x, current)): _*)

  def record(x: RawInterface, conversion: String)(implicit si: SourceInfo): Unit = {
    val module = Module.currentModule
    val rootModule = module.exists(ModuleInternals.getParent(_).isEmpty)
    val rootIo =
      module.exists { current =>
        rootModule &&
        DataInternals.isIO(x) &&
        DataInternals.getOwningModuleOption(x).contains(current)
      }

    val call = ViewCall(conversion, si, module)
    val previous = x.viewCalls_.toSeq
    x.viewCalls_.addOne(call)

    if (!rootModule)
      warn(x, s"bad use of AXI4 view: .$conversion called outside the root module", call)

    if (rootModule && !rootIo)
      warn(x, s"bad use of AXI4 view: .$conversion was not called on root IO", call)

    if (previous.nonEmpty) {
      logger.warn(
        "axi4View",
        (Seq(s"bad use of AXI4 view: raw interface viewed multiple times; current call is .$conversion") ++
          warningLines(x, call) ++
          Seq("Previous calls:") ++
          previous.map(prev => s"  ${callString(prev)}")): _*
      )
    }
  }
}

trait Casts {
  private def requestRole(source: RawInterface): Option[DeclaredRole] =
    tracking.Tracked.roleFromCurrentModule(source)

  private def enforceAxi4Roles(
      source: RawInterface,
      ar: => tracking.Tracked,
      r: => tracking.Tracked,
      aw: => tracking.Tracked,
      w: => tracking.Tracked,
      b: => tracking.Tracked
  ): Unit =
    requestRole(source) match {
      case None => ()
      case Some(requestRole) =>
        val responseRole = DeclaredRole.invert(requestRole)

        if (source.cfg.read) {
          tracking.Tracked.enforceRole(ar, requestRole)
          tracking.Tracked.enforceRole(r, responseRole)
        }

        if (source.cfg.write) {
          tracking.Tracked.enforceRole(aw, requestRole)
          tracking.Tracked.enforceRole(w, requestRole)
          tracking.Tracked.enforceRole(b, responseRole)
        }
    }

  private def enforceFullRoles(view: full.Interface, source: RawInterface): Unit =
    enforceAxi4Roles(source, view.ar, view.r, view.aw, view.w, view.b)

  private def enforceLiteRoles(view: lite.Interface, source: RawInterface): Unit =
    enforceAxi4Roles(source, view.ar, view.r, view.aw, view.w, view.b)

  implicit class viewAxiInterfaceAs(x: RawInterface) {
    def asFull(implicit si: SourceInfo) = {
      ViewWarnings.record(x, "asFull")
      val view = x.viewAs[full.Interface]
      enforceFullRoles(view, x)
      tracking.registerView(view, x, sourceInfo = Some(si))
      view
    }

    def asLite(implicit si: SourceInfo) = {
      ViewWarnings.record(x, "asLite")
      val view = x.viewAs[lite.Interface]
      enforceLiteRoles(view, x)
      tracking.registerView(view, x, sourceInfo = Some(si))
      view
    }
  }
}

object Casts extends Casts
