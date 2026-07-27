package chext.amba.axi4.tracking

import scala.collection.mutable.ArrayBuffer

import chext.amba.axi4.tracking.properties.{Master, Slave}
import chext.amba.axi4.tracking.values.{BurstShape, MemoryMap, ThreadMode}

/** Exhaustive validation and master/slave compatibility checking for AXI interfaces. */
object CompatibilityChecker {
  private final case class Diagnostic(
      interface: Tracked,
      property: String,
      message: String,
      details: Seq[String] = Seq.empty
  )

  private sealed trait Checked[+T]
  private final case class Value[T](value: T, steps: Seq[ResolutionStep]) extends Checked[T]
  private case object Skip extends Checked[Nothing]
  private case object Invalid extends Checked[Nothing]

  /** Resolves and checks every supported property on every supplied interface. */
  def check(interfaces: Iterable[Tracked]): Unit = {
    val diagnostics = ArrayBuffer.empty[Diagnostic]
    interfaces.foreach(checkInterface(_, diagnostics))

    if (diagnostics.nonEmpty) {
      diagnostics.foreach { diagnostic =>
        println(
          s"axi4/tracking : error: ${path(diagnostic.interface)} " +
            s"${diagnostic.property}: ${diagnostic.message}"
        )
        diagnostic.details.foreach(line => println(s"axi4/tracking :   $line"))
      }
      throw new IllegalArgumentException(
        s"AXI4 property checking failed with ${diagnostics.length} diagnostic(s)"
      )
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
          Master.ReadBurstShape,
          Slave.ReadBurstShape,
          diagnostics
        )
      checkThread(
        interface,
        Master.ReadThreadMode,
        Slave.ReadThreadMode,
        diagnostics
      )
    }

    if (interface.cfg.write) {
      if (!interface.cfg.lite)
        checkBurst(
          interface,
          Master.WriteBurstShape,
          Slave.WriteBurstShape,
          diagnostics
        )
      checkThread(
        interface,
        Master.WriteThreadMode,
        Slave.WriteThreadMode,
        diagnostics
      )
    }

    checkedValue(interface, Slave.MemoryMap, allowUndefined = true, diagnostics) match {
      case Value(memoryMap, _) =>
        memoryMap.validate match {
          case MemoryMap.ValidationResult.Success => ()
          case failure: MemoryMap.ValidationResult.Failure =>
            diagnostics += Diagnostic(
              interface,
              Slave.MemoryMap.qualifiedName,
              "memory map validation failed",
              Seq(failure.render)
            )
        }

        val limit = BigInt(1) << interface.cfg.wAddr
        if (memoryMap.offset + memoryMap.allocatedSize > limit)
          diagnostics += Diagnostic(
            interface,
            Slave.MemoryMap.qualifiedName,
            s"memory map exceeds the ${interface.cfg.wAddr}-bit address space",
            Seq(
              s"offset=0x${memoryMap.offset.toString(16)}",
              s"size=0x${memoryMap.allocatedSize.toString(16)}",
              s"limit=0x${limit.toString(16)}"
            )
          )
      case Skip | Invalid => ()
    }
  }

  private def checkBurst(
      interface: Tracked,
      masterKey: PropertyKey[BurstShape],
      slaveKey: PropertyKey[BurstShape],
      diagnostics: ArrayBuffer[Diagnostic]
  ): Unit = {
    val master = checkedValue(interface, masterKey, allowUndefined = false, diagnostics)
    val slave = checkedValue(interface, slaveKey, allowUndefined = false, diagnostics)

    master match {
      case Value(value, _) =>
        BurstShape.validationErrors(value, interface.cfg).foreach { message =>
          diagnostics += Diagnostic(interface, masterKey.qualifiedName, message)
        }
      case _ => ()
    }
    slave match {
      case Value(value, _) =>
        BurstShape.validationErrors(value, interface.cfg).foreach { message =>
          diagnostics += Diagnostic(interface, slaveKey.qualifiedName, message)
        }
      case _ => ()
    }

    (master, slave) match {
      case (Value(masterValue, masterSteps), Value(slaveValue, slaveSteps)) =>
        val errors = BurstShape.compatibilityErrors(masterValue, slaveValue)
        if (errors.nonEmpty)
          diagnostics += Diagnostic(
            interface,
            s"${masterKey.qualifiedName} -> ${slaveKey.qualifiedName}",
            "burst shapes are incompatible",
            errors ++ trace("master trace", masterSteps) ++ trace("slave trace", slaveSteps)
          )
      case _ => ()
    }
  }

  private def checkThread(
      interface: Tracked,
      masterKey: PropertyKey[ThreadMode],
      slaveKey: PropertyKey[ThreadMode],
      diagnostics: ArrayBuffer[Diagnostic]
  ): Unit = {
    val master = checkedValue(interface, masterKey, allowUndefined = false, diagnostics)
    val slave = checkedValue(interface, slaveKey, allowUndefined = false, diagnostics)

    def validate(
        checked: Checked[ThreadMode],
        key: PropertyKey[ThreadMode]
    ): Boolean =
      checked match {
        case Value(value, steps) =>
          val errors = ThreadMode.validationErrors(value, interface.cfg)
          errors.foreach { message =>
            diagnostics += Diagnostic(
              interface,
              key.qualifiedName,
              message,
              trace("resolution trace", steps)
            )
          }
          errors.isEmpty
        case _ => false
      }

    val masterValid = validate(master, masterKey)
    val slaveValid = validate(slave, slaveKey)

    (master, slave) match {
      case (Value(masterValue, masterSteps), Value(slaveValue, slaveSteps))
          if masterValid &&
            slaveValid &&
            !ThreadMode.compatible(masterValue, slaveValue) =>
        diagnostics += Diagnostic(
          interface,
          s"${masterKey.qualifiedName} -> ${slaveKey.qualifiedName}",
          s"thread modes are incompatible: master=$masterValue, slave=$slaveValue",
          trace("master trace", masterSteps) ++ trace("slave trace", slaveSteps)
        )
      case _ => ()
    }
  }

  private def checkedValue[T](
      interface: Tracked,
      key: PropertyKey[T],
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
        request.state match {
          case PropertyState.Enforced(value) =>
            Value(value, Seq.empty)
          case PropertyState.Calculated(value, _) =>
            Value(value, resolution.steps)
          case PropertyState.DontCare(_) =>
            Skip
          case PropertyState.Undefined if allowUndefined =>
            Skip
          case PropertyState.Undefined =>
            diagnostics += Diagnostic(
              interface,
              key.qualifiedName,
              "property is undefined on an enabled channel group"
            )
            Invalid
          case PropertyState.Incomplete =>
            diagnostics += Diagnostic(
              interface,
              key.qualifiedName,
              "property resolution is incomplete"
            )
            Invalid
          case PropertyState.Unresolved =>
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

  private def trace(label: String, steps: Seq[ResolutionStep]): Seq[String] =
    if (steps.isEmpty)
      Seq(s"$label: enforced locally")
    else
      Seq(s"$label:") ++ steps.map { step =>
        s"${step.interfaceFrom} -> ${step.interfaceTo} " +
          s"(${step.kind}/${step.resolver} at ${step.resolverPath})"
      }

  private def path(interface: Tracked): String =
    try TrackingPath.interface(interface)
    catch {
      case _: RuntimeException => interface.toString
    }
}
