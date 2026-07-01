package chext.amba.axi4s

import chisel3._
import chisel3.Module
import chisel3.experimental.BaseModule
import chisel3.experimental.SourceInfo
import chisel3.experimental.dataview._

import chisel3.hacks.DataInternals
import chisel3.hacks.ModuleInternals
import chext.elastic
import chext.tracking
import chext.tracking.Logger
import chext.tracking.util.sourceInfoToString

private[axi4s] case class ViewCall(
    conversion: String,
    sourceInfo: SourceInfo,
    module: Option[BaseModule]
)

private object ViewWarnings {
  private val logger = new Logger("axi4s")

  private def moduleString(module: Option[BaseModule]): String =
    module match {
      case Some(value) => s"'${value.toString()}'"
      case None => "(no current module)"
    }

  private def callString(call: ViewCall): String =
    s"Called .${call.conversion} by ${moduleString(call.module)} @[${sourceInfoToString(call.sourceInfo)}]"

  private def warningLines(x: Interface, current: ViewCall): Seq[String] =
    Seq(
      s"Interface '$x' is defined @[${sourceInfoToString(x.sourceInfo)}]",
      callString(current),
      "Chisel .viewAs does not create hardware, but each call may create a distinct Chext elastic wrapper.",
      "Repeated or non-root views can break Chext source/sink bookkeeping and endpoint identity.",
      "Safe pattern: call .asLite/.asFull exactly once at the root module on root IO, then reuse that elastic interface."
    )

  private def warn(x: Interface, message: String, current: ViewCall): Unit =
    logger.warn("axi4sView", (Seq(message) ++ warningLines(x, current)): _*)

  def record(x: Interface, conversion: String)(implicit si: SourceInfo): Unit = {
    val module = Module.currentModule
    val rootModule = module.exists(ModuleInternals.getParent(_).isEmpty)
    val rootIo =
      module.exists { current =>
        rootModule &&
        DataInternals.isIO(x) &&
        DataInternals.getOwningModuleOption(x).contains(current)
      }

    val call = ViewCall(conversion, si, module)
    val previous = x.viewCalls.toSeq
    x.viewCalls.addOne(call)

    if (!rootModule)
      warn(x, s"bad use of AXI4 Stream view: .$conversion called outside the root module", call)

    if (rootModule && !rootIo)
      warn(x, s"bad use of AXI4 Stream view: .$conversion was not called on root IO", call)

    if (previous.nonEmpty) {
      logger.warn(
        "axi4sView",
        (Seq(s"bad use of AXI4 Stream view: raw interface viewed multiple times; current call is .$conversion") ++
          warningLines(x, call) ++
          Seq("Previous calls:") ++
          previous.map(prev => s"  ${callString(prev)}")): _*
      )
    }
  }
}

object Casts {
  implicit class viewAxisInterfaceAs(x: Interface) {
    import FullChannel._
    import BasicChannel._

    @deprecated("Please use asLite instead, this will be removed.")
    def lite(implicit si: SourceInfo) = asLite

    @deprecated("Please use asFull instead, this will be removed.")
    def full(implicit si: SourceInfo) = asFull

    def asLite(implicit si: SourceInfo) = {
      ViewWarnings.record(x, "asLite")
      val view = x.viewAs[elastic.Interface[Bits]]
      tracking.registerView(view, x, Some("$view"), Some(x.sourceInfo))
      view
    }

    def asFull(implicit si: SourceInfo) = {
      ViewWarnings.record(x, "asFull")
      val view = x.viewAs[elastic.Interface[FullChannel]]
      tracking.registerView(view, x, Some("$view"), Some(x.sourceInfo))
      view
    }
  }
}
