package chext.amba.axi4.tracking

import chisel3.experimental.{SourceInfo, UnlocatableSourceInfo}

import scala.collection.mutable.ArrayBuffer

import chext.amba.axi4.tracking.{properties => p, values => v}
import chext.util.sourceInfoToString

/** Resolves, validates, and checks compatibility for AXI interface properties. */
object Checker {
  private final case class Diagnostic(
      interface: Tracked,
      property: String,
      message: String,
      details: Seq[String] = Seq.empty
  )

  private sealed trait Checked[+T]
  private final case class Value[T](
      value: T,
      steps: Seq[ResolutionStep],
      enforcementSourceInfo: Option[SourceInfo],
      enforcementOwner: Option[Owner],
      enforcementInterface: Option[Tracked]
  ) extends Checked[T]
  private case object Skip extends Checked[Nothing]
  private case object Invalid extends Checked[Nothing]

  /** Resolves and checks every supported property on every supplied interface. */
  def check(interfaces: Iterable[Tracked]): Unit = {
    val diagnostics = ArrayBuffer.empty[Diagnostic]
    interfaces.foreach(checkInterface(_, diagnostics))

    if (diagnostics.nonEmpty) {
      diagnostics.foreach { diagnostic =>
        println(s"${Tag.diagnosticName} : error: ${diagnostic.message}")
        println(s"${Tag.diagnosticName} :   Interface: ${path(diagnostic.interface)}")
        println(s"${Tag.diagnosticName} :   Property: ${diagnostic.property}")
        diagnostic.details.foreach(line => println(s"${Tag.diagnosticName} :   $line"))
      }
    }
  }

  private def checkInterface(
      interface: Tracked,
      diagnostics: ArrayBuffer[Diagnostic]
  ): Unit = {
    if (interface.cfg.read) {
      if (!interface.cfg.lite)
        checkBurst(
          interface,
          p.MasterReadBurstShape,
          p.SlaveReadBurstShape,
          diagnostics
        )
      checkThread(
        interface,
        p.MasterReadThreadMode,
        p.SlaveReadThreadMode,
        diagnostics
      )
    }

    if (interface.cfg.write) {
      if (!interface.cfg.lite)
        checkBurst(
          interface,
          p.MasterWriteBurstShape,
          p.SlaveWriteBurstShape,
          diagnostics
        )
      checkThread(
        interface,
        p.MasterWriteThreadMode,
        p.SlaveWriteThreadMode,
        diagnostics
      )
    }

    checkedValue(interface, p.SlaveMemoryMap, allowUndefined = true, diagnostics) match {
      case value @ Value(memoryMap, steps, enforcementSourceInfo, _, _) =>
        memoryMap.validate match {
          case v.MemoryMap.ValidationResult.Success => ()
          case failure: v.MemoryMap.ValidationResult.Failure =>
            diagnostics += Diagnostic(
              interface,
              p.SlaveMemoryMap.qualifiedName,
              "memory map validation failed",
              detail(failure.render) ++
                trace("Resolution trace", steps, enforcementSourceInfo) ++
                enforcementContext("Property", value)
            )
        }

        val limit = BigInt(1) << interface.cfg.wAddr
        if (memoryMap.offset + memoryMap.allocatedSize > limit)
          diagnostics += Diagnostic(
            interface,
            p.SlaveMemoryMap.qualifiedName,
            s"memory map exceeds the ${interface.cfg.wAddr}-bit address space",
            detail(
              s"offset=0x${memoryMap.offset.toString(16)}",
              s"size=0x${memoryMap.allocatedSize.toString(16)}",
              s"limit=0x${limit.toString(16)}"
            ) ++ trace("Resolution trace", steps, enforcementSourceInfo)
              ++ enforcementContext("Property", value)
          )
      case Skip | Invalid => ()
    }
  }

  private def checkBurst(
      interface: Tracked,
      masterKey: p.Key[v.BurstShape],
      slaveKey: p.Key[v.BurstShape],
      diagnostics: ArrayBuffer[Diagnostic]
  ): Unit = {
    val master = checkedValue(interface, masterKey, allowUndefined = false, diagnostics)
    val slave = checkedValue(interface, slaveKey, allowUndefined = false, diagnostics)

    if (!eitherEnforced(interface, masterKey, slaveKey))
      return

    master match {
      case checked @ Value(value, steps, enforcementSourceInfo, _, _) =>
        v.BurstShape.checkConfig(value, interface.cfg).errors.foreach { message =>
          diagnostics += Diagnostic(
            interface,
            masterKey.qualifiedName,
            message,
            trace("Resolution trace", steps, enforcementSourceInfo) ++
              enforcementContext("Property", checked)
          )
        }
      case _ => ()
    }
    slave match {
      case checked @ Value(value, steps, enforcementSourceInfo, _, _) =>
        v.BurstShape.checkConfig(value, interface.cfg).errors.foreach { message =>
          diagnostics += Diagnostic(
            interface,
            slaveKey.qualifiedName,
            message,
            trace("Resolution trace", steps, enforcementSourceInfo) ++
              enforcementContext("Property", checked)
          )
        }
      case _ => ()
    }

    (master, slave) match {
      case (
            master @ Value(masterValue, masterSteps, masterSourceInfo, _, _),
            slave @ Value(slaveValue, slaveSteps, slaveSourceInfo, _, _)
          ) =>
        val result = v.BurstShape.checkCompatible(masterValue, slaveValue)
        if (!result.isSuccess)
          diagnostics += Diagnostic(
            interface,
            s"${masterKey.qualifiedName} -> ${slaveKey.qualifiedName}",
            "burst shapes are incompatible",
            detail(result.errors: _*) ++
              trace("Master trace", masterSteps, masterSourceInfo) ++
              trace("Slave trace", slaveSteps, slaveSourceInfo) ++
              enforcementContext("Master", master) ++
              enforcementContext("Slave", slave)
          )
      case _ => ()
    }
  }

  private def checkThread(
      interface: Tracked,
      masterKey: p.Key[v.ThreadMode],
      slaveKey: p.Key[v.ThreadMode],
      diagnostics: ArrayBuffer[Diagnostic]
  ): Unit = {
    val master = checkedValue(interface, masterKey, allowUndefined = false, diagnostics)
    val slave = checkedValue(interface, slaveKey, allowUndefined = false, diagnostics)

    if (!eitherEnforced(interface, masterKey, slaveKey))
      return

    def validate(
        checked: Checked[v.ThreadMode],
        key: p.Key[v.ThreadMode]
    ): Boolean =
      checked match {
        case checked @ Value(value, steps, enforcementSourceInfo, _, _) =>
          val result = v.ThreadMode.checkConfig(value, interface.cfg)
          result.errors.foreach { message =>
            diagnostics += Diagnostic(
              interface,
              key.qualifiedName,
              message,
              trace("Resolution trace", steps, enforcementSourceInfo) ++
                enforcementContext("Property", checked)
            )
          }
          result.isSuccess
        case _ => false
      }

    val masterValid = validate(master, masterKey)
    val slaveValid = validate(slave, slaveKey)

    (master, slave) match {
      case (
            master @ Value(masterValue, masterSteps, masterSourceInfo, _, _),
            slave @ Value(slaveValue, slaveSteps, slaveSourceInfo, _, _)
          ) if masterValid && slaveValid =>
        v.ThreadMode.checkCompatible(masterValue, slaveValue).errors.foreach { message =>
          diagnostics += Diagnostic(
            interface,
            s"${masterKey.qualifiedName} -> ${slaveKey.qualifiedName}",
            message,
            trace("Master trace", masterSteps, masterSourceInfo) ++
              trace("Slave trace", slaveSteps, slaveSourceInfo) ++
              enforcementContext("Master", master) ++
              enforcementContext("Slave", slave)
          )
        }
      case _ => ()
    }
  }

  private def checkedValue[T](
      interface: Tracked,
      key: p.Key[T],
      allowUndefined: Boolean,
      diagnostics: ArrayBuffer[Diagnostic]
  ): Checked[T] = {
    val request = ResolveRequest(interface, key)
    val resolution = Resolver.resolve(request)

    resolution.result match {
      case ResolveResult.Failure(message, _) =>
        diagnostics += Diagnostic(
          interface,
          key.qualifiedName,
          s"resolution failed: $message"
        )
        Invalid
      case ResolveResult.Success() =>
        request.cell.state match {
          case p.State.Enforced(value) =>
            checkedValue(value, Seq.empty, request.cell)
          case p.State.Calculated(value, _) =>
            checkedValue(value, resolution.steps, request.cell)
          case p.State.DontCare(_) =>
            Skip
          case p.State.Undefined if allowUndefined =>
            Skip
          case p.State.Undefined =>
            diagnostics += Diagnostic(
              interface,
              key.qualifiedName,
              "property is undefined on an enabled channel group"
            )
            Invalid
          case p.State.Incomplete =>
            diagnostics += Diagnostic(
              interface,
              key.qualifiedName,
              "property resolution is incomplete"
            )
            Invalid
          case p.State.Unresolved =>
            diagnostics += Diagnostic(
              interface,
              key.qualifiedName,
              "property remained unresolved"
            )
            Invalid
        }
      case ResolveResult.Retry(_) =>
        diagnostics += Diagnostic(
          interface,
          key.qualifiedName,
          "resolution returned an unprocessed retry"
        )
        Invalid
    }
  }

  private def detail(lines: String*): Seq[String] =
    lines.map(line => s"Detail: $line")

  private def checkedValue[T](
      value: T,
      steps: Seq[ResolutionStep],
      cell: p.Cell[T]
  ): Value[T] =
    Value(
      value,
      steps,
      cell.enforcementSourceInfo,
      cell.enforcementOwner,
      cell.enforcementInterface
    )

  private def eitherEnforced[T](
      interface: Tracked,
      masterKey: p.Key[T],
      slaveKey: p.Key[T]
  ): Boolean =
    Seq(masterKey, slaveKey).exists { key =>
      interface.properties(key).state match {
        case p.State.Enforced(_) => true
        case _                   => false
      }
    }

  private def trace(
      label: String,
      steps: Seq[ResolutionStep],
      enforcementSourceInfo: Option[SourceInfo]
  ): Seq[String] = {
    val hops = steps.map { step =>
      s"  ${step.interfaceFrom} -> ${step.interfaceTo} " +
        s"(${step.kind}/${step.resolver} at ${step.resolverPath})"
    }
    val originLine =
      enforcementSourceInfo match {
        case Some(sourceInfo) =>
          if (steps.isEmpty)
            s"  enforced locally at ${sourceInfoToString(sourceInfo)}"
          else
            s"  enforced at ${sourceInfoToString(sourceInfo)}"
        case None =>
          if (steps.isEmpty) "  calculated locally"
          else "  calculated at end of trace"
      }

    Seq(s"$label:") ++ hops :+ originLine
  }

  private def enforcementContext(
      label: String,
      value: Value[_]
  ): Seq[String] =
    value.enforcementSourceInfo match {
      case None => Seq.empty
      case Some(sourceInfo) =>
        val header = Seq(s"$label property enforced by:")
        val interfaceLines =
          value.enforcementInterface match {
            case Some(interface) =>
              Seq(s"  Interface: ${path(interface)}")
            case None =>
              Seq("  Interface: (unavailable)")
          }
        val enforcementLine =
          Seq(s"  Property enforced at: ${sourceInfoToString(sourceInfo)}")
        val ownerLines =
          value.enforcementOwner match {
            case Some(owner @ Owner.ComponentOwner(component)) =>
              Seq(
                s"  Component: ${className(component)}",
                s"  Component path: ${owner.path}",
                s"  Component instantiated at: ${sourceInfoToString(owner.sourceInfo)}",
                s"  Module: ${className(owner.module)}",
                s"  Module path: ${owner.modulePath}",
                s"  Module defined at: ${sourceInfoToString(owner.moduleDefinitionSourceInfo)}",
                s"  Module instantiated at: ${owner.moduleInstantiationSourceInfo
                    .map(sourceInfoToString)
                    .getOrElse("(root elaboration)")}"
              )
            case Some(owner: Owner.Module) =>
              Seq(
                s"  Module: ${className(owner.module)}",
                s"  Module path: ${owner.modulePath}",
                s"  Module defined at: ${sourceInfoToString(owner.moduleDefinitionSourceInfo)}",
                s"  Module instantiated at: ${owner.moduleInstantiationSourceInfo
                    .map(sourceInfoToString)
                    .getOrElse("(root elaboration)")}"
              )
            case None =>
              Seq("  Owner: (property was enforced outside a Resolver)")
          }
        val declarationLines =
          value.enforcementInterface.toSeq.map { interface =>
            s"  Interface declared at: ${sourceInfoToString(interfaceSourceInfo(interface))}"
          }

        header ++ interfaceLines ++ enforcementLine ++ ownerLines ++ declarationLines
    }

  private def className(value: AnyRef): String =
    if (value eq null) "(null)"
    else value.getClass.getName.stripSuffix("$").replace('$', '.')

  private def interfaceSourceInfo(interface: Tracked): SourceInfo =
    interface match {
      case full: chext.amba.axi4.full.Interface => full.sourceInfo
      case lite: chext.amba.axi4.lite.Interface => lite.sourceInfo
      case raw: chext.amba.axi4.RawInterface     => raw.sourceInfo
      case _                                     => UnlocatableSourceInfo
    }

  private def path(interface: Tracked): String =
    try interface.trackingPath
    catch {
      case _: RuntimeException => interface.toString
    }
}
