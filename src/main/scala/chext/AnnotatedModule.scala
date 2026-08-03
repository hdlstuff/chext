package chext

import chisel3._
import chisel3.experimental.SourceInfo

trait HasHdlinfoModule extends RawModule {
  def hdlinfoModule: hdlinfo.Module
}

/** Extracts hdlinfo metadata from the supported AXI4 interface representations.
  *
  * Chext has three Scala shapes that can describe an AXI4 interface: the flat raw bundle,
  * the hierarchical full view, and the hierarchical lite view. `declareAxi4Interface`
  * intentionally accepts `Data` instead of exposing public overloads for those shapes. That keeps
  * call sites stable when a value is typed as a common supertype, and avoids ambiguous overload
  * resolution around Chisel aggregate types such as `NamedVec`, which are both hardware `Data` and
  * Scala collections.
  *
  * This helper is the private typed dispatcher behind that single public entry point. For each
  * supported representation it provides the AXI config, a valid signal whose Chisel direction is
  * used to infer master/slave role, and the default hdlinfo interface kind.
  */
private object Axi4HdlinfoDeclaration {
  private val requireAxi4 =
    chext.util.Require("chext.AnnotatedModule.declareAxi4Interface")

  private sealed trait Axi4InterfaceDeclaration[-T <: Data] {
    def cfg(interface: T): chext.amba.axi4.Config
    def valid(interface: T)(implicit si: SourceInfo): Bool
    def defaultKind: String
  }

  case class Result(
      cfg: chext.amba.axi4.Config,
      valid: Bool,
      defaultKind: String
  )

  private implicit object Axi4HdlinfoDeclaration_Raw
      extends Axi4InterfaceDeclaration[chext.amba.axi4.RawInterface] {
    def cfg(interface: chext.amba.axi4.RawInterface): chext.amba.axi4.Config =
      interface.cfg

    def valid(interface: chext.amba.axi4.RawInterface)(implicit si: SourceInfo): Bool =
      interface.ARVALID.getOrElse(
        interface.AWVALID.getOrElse(
          requireAxi4.failHere(
            "cannot determine AXI4 role",
            Seq("The interface has neither ARVALID nor AWVALID because read=false and write=false.")
          )
        )
      )

    val defaultKind: String = "axi4"
  }

  private implicit object Axi4HdlinfoDeclaration_Full
      extends Axi4InterfaceDeclaration[chext.amba.axi4.full.Interface] {
    def cfg(interface: chext.amba.axi4.full.Interface): chext.amba.axi4.Config =
      interface.cfg

    def valid(interface: chext.amba.axi4.full.Interface)(implicit si: SourceInfo): Bool =
      if (interface.cfg.read) interface.ar.$valid
      else if (interface.cfg.write) interface.aw.$valid
      else
        requireAxi4.failHere(
          "cannot determine AXI4 full role",
          Seq("The interface config has read=false and write=false.")
        )

    val defaultKind: String = "axi4_rtl_hier"
  }

  private implicit object Axi4HdlinfoDeclaration_Lite
      extends Axi4InterfaceDeclaration[chext.amba.axi4.lite.Interface] {
    def cfg(interface: chext.amba.axi4.lite.Interface): chext.amba.axi4.Config =
      interface.cfg

    def valid(interface: chext.amba.axi4.lite.Interface)(implicit si: SourceInfo): Bool =
      if (interface.cfg.read) interface.ar.$valid
      else if (interface.cfg.write) interface.aw.$valid
      else
        requireAxi4.failHere(
          "cannot determine AXI4 lite role",
          Seq("The interface config has read=false and write=false.")
        )

    val defaultKind: String = "axi4_rtl_hier"
  }

  def apply(interface: Data)(implicit si: SourceInfo): Result =
    interface match {
      case raw: chext.amba.axi4.RawInterface =>
        applyTyped(raw)
      case full: chext.amba.axi4.full.Interface =>
        applyTyped(full)
      case lite: chext.amba.axi4.lite.Interface =>
        applyTyped(lite)
      case other => failUnsupportedInterface(other)
    }

  private def applyTyped[T <: Data](
      interface: T
  )(implicit declaration: Axi4InterfaceDeclaration[T], si: SourceInfo): Result =
    Result(declaration.cfg(interface), declaration.valid(interface), declaration.defaultKind)

  private def failUnsupportedInterface(other: Data)(implicit si: SourceInfo): Nothing =
    requireAxi4.failHere(
      "unsupported AXI4 interface type",
      Seq(
        s"Actual type: ${other.getClass.getName}",
        "Expected chext.amba.axi4.RawInterface, chext.amba.axi4.full.Interface, or chext.amba.axi4.lite.Interface."
      )
    )
}

/** Mixin for modules that describe their ports and interfaces for hdlinfo emission.
  *
  * An `AnnotatedModule` is still an ordinary Chisel `RawModule`; the mixin only adds declaration
  * helpers such as `declareClock`, `declareReset`, `declareData`, `declareElasticInterface`, and
  * `declareAxi4Interface`. `chext.TestBench.emit` consumes the collected annotations and writes
  * the corresponding hdlinfo sidecar.
  */
trait AnnotatedModule extends HasHdlinfoModule {
  private val require_ = chext.util.Require.inferred()

  private val encodedDataBuilder = new chext.util.EncodedDataBuilder()
  private val ports = scala.collection.mutable.ArrayBuffer.empty[hdlinfo.Port]
  private val interfaces = scala.collection.mutable.ArrayBuffer.empty[hdlinfo.Interface]
  private val args = scala.collection.mutable.ArrayBuffer.empty[(String, hdlinfo.TypedObject)]

  private val argNames = scala.collection.mutable.Set.empty[String]
  private val encodedDataNames = scala.collection.mutable.Set.empty[String]
  private val portNames = scala.collection.mutable.Set.empty[String]
  private val interfaceNames = scala.collection.mutable.Set.empty[String]

  private def hasArgNamed(x: String) = argNames(x)
  private def hasPortNamed(x: String) = portNames(x)
  private def hasInterfaceNamed(x: String) = interfaceNames(x)
  private def hasEncodedDataNamed(x: String) = encodedDataNames(x)

  private def nameFor(x: Data, defaultName: Option[String]) = {
    defaultName.getOrElse(
      chisel3.reflect.DataMirror
        .queryNameGuess(x)
        .replace('.', '_')
        .replaceAll("\\[([^\\]]+)\\]", "_$1")
    )
  }

  private def getValidDirection(x: Data)(implicit si: SourceInfo): hdlinfo.PortDirection = {
    import chisel3.reflect.DataMirror

    @scala.annotation.nowarn
    val result = DataMirror.directionOf(x) match {
      case ActualDirection.Output => hdlinfo.PortDirection.output
      case ActualDirection.Input  => hdlinfo.PortDirection.input

      case ActualDirection.Empty              => hdlinfo.PortDirection.none
      case ActualDirection.Unspecified        => hdlinfo.PortDirection.none
      case ActualDirection.Bidirectional(dir) => hdlinfo.PortDirection.none
    }
    require_.here(
      result.str != "none",
      "port direction must be input or output",
      Seq(s"Chisel direction: ${DataMirror.directionOf(x)}")
    )

    result
  }

  final def declareArg[T: io.circe.Encoder](
      name: String,
      typedObject: hdlinfo.TypedObject
  )(implicit si: SourceInfo): Unit = {
    require_.here(
      !hasArgNamed(name),
      "duplicate hdlinfo argument",
      Seq(s"Argument name: $name")
    )
    args.addOne(name -> typedObject)
    argNames.addOne(name)
  }

  final def declareClock(
      clock: Element,
      sensitivity: hdlinfo.PortSensitivity = hdlinfo.PortSensitivity.clockRising,
      frequencyMHz: Float = 100,
      associatedReset: String = "reset",
      portName: Option[String] = None
  )(implicit si: SourceInfo): Unit = {
    val name = nameFor(clock, portName)
    require_.here(
      !hasPortNamed(name),
      "duplicate hdlinfo port",
      Seq(s"Port name: $name")
    )

    require_.here(
      sensitivity.str.startsWith("clock"),
      "invalid clock sensitivity",
      Seq(
        s"Port name: $name",
        s"Sensitivity: ${sensitivity.str}",
        "Expected a sensitivity whose name starts with 'clock'."
      )
    )

    ports.addOne(
      hdlinfo.Port(
        name,
        getValidDirection(clock),
        hdlinfo.PortKind.clock,
        sensitivity,
        false,
        (0, 0),
        frequencyMHz,
        "",
        associatedReset
      )
    )
    portNames.addOne(name)
  }

  final def declareReset(
      reset: Element,
      sensitivity: hdlinfo.PortSensitivity = hdlinfo.PortSensitivity.resetActiveHigh,
      associatedClock: String = "clock",
      portName: Option[String] = None
  )(implicit si: SourceInfo): Unit = {
    val name = nameFor(reset, portName)
    require_.here(
      !hasPortNamed(name),
      "duplicate hdlinfo port",
      Seq(s"Port name: $name")
    )

    require_.here(
      sensitivity.str.startsWith("reset"),
      "invalid reset sensitivity",
      Seq(
        s"Port name: $name",
        s"Sensitivity: ${sensitivity.str}",
        "Expected a sensitivity whose name starts with 'reset'."
      )
    )

    ports.addOne(
      hdlinfo.Port(
        name,
        getValidDirection(reset),
        hdlinfo.PortKind.reset,
        sensitivity,
        false,
        (0, 0),
        100,
        associatedClock,
        ""
      )
    )
    portNames.addOne(name)
  }

  /** Declares a scalar or integer data port.
    *
    * `Bool`, `UInt`, and `SInt` ports are supported. Integer signedness is intentionally not
    * represented by hdlinfo: `UInt` and `SInt` are both described as raw bit vectors using their
    * elaborated width.
    */
  final def declareData(
      data: Data,
      associatedClock: String = "clock",
      associatedReset: String = "reset",
      portName: Option[String] = None,
      args: Map[String, hdlinfo.TypedObject] = Map.empty
  )(implicit si: SourceInfo): Unit = {
    val name = nameFor(data, portName)
    require_.here(
      !hasPortNamed(name),
      "duplicate hdlinfo port",
      Seq(s"Port name: $name")
    )

    val width = data match {
      case _: Bool => 1
      case integer: UInt =>
        integer.widthOption.getOrElse(
          require_.failHere(
            "data port width must be known",
            Seq(s"Port name: $name", s"Actual type: ${data.getClass.getName}")
          )
        )
      case integer: SInt =>
        integer.widthOption.getOrElse(
          require_.failHere(
            "data port width must be known",
            Seq(s"Port name: $name", s"Actual type: ${data.getClass.getName}")
          )
        )
      case other =>
        require_.failHere(
          "unsupported data port type",
          Seq(
            s"Port name: $name",
            s"Actual type: ${other.getClass.getName}",
            "Expected Bool, UInt, or SInt."
          )
        )
    }

    require_.here(
      width > 0,
      "data port width must be positive",
      Seq(s"Port name: $name", s"Width: $width")
    )

    val isBus = width > 1
    ports.addOne(
      hdlinfo.Port(
        name,
        getValidDirection(data),
        hdlinfo.PortKind.data,
        hdlinfo.PortSensitivity.none,
        isBus,
        if (isBus) (width - 1, 0) else (0, 0),
        100,
        associatedClock,
        associatedReset,
        args
      )
    )
    portNames.addOne(name)
  }

  /** Declares a single interrupt pin.
    *
    * @todo
    *   Figure out a way to represent a vector of interrupts in hdlinfo.
    *
    * @param interrupt
    * @param sensitivity
    * @param associatedClock
    * @param associatedReset
    */
  final def declareInterrupt(
      interrupt: Bool,
      sensitivity: hdlinfo.PortSensitivity,
      associatedClock: String = "clock",
      associatedReset: String = "reset",
      portName: Option[String] = None
  )(implicit si: SourceInfo): Unit = {
    val name = nameFor(interrupt, portName)
    require_.here(
      !hasPortNamed(name),
      "duplicate hdlinfo port",
      Seq(s"Port name: $name")
    )

    require_.here(
      sensitivity.str.startsWith("interrupt"),
      "invalid interrupt sensitivity",
      Seq(
        s"Port name: $name",
        s"Sensitivity: ${sensitivity.str}",
        "Expected a sensitivity whose name starts with 'interrupt'."
      )
    )

    ports.addOne(
      hdlinfo.Port(
        name,
        getValidDirection(interrupt),
        hdlinfo.PortKind.interrupt,
        sensitivity,
        false,
        (0, 0),
        100,
        associatedClock,
        associatedReset
      )
    )
    portNames.addOne(name)
  }

  final def declareAxi4Interface(
      interface: Data,
      associatedClock: String = "clock",
      associatedReset: String = "reset",
      interfaceName: Option[String] = None,
      kind: String = "",
      args: Map[String, hdlinfo.TypedObject] = Map.empty
  )(implicit si: SourceInfo): Unit = {
    interface match {
      case interfaces: chext.util.NamedVec[_] =>
        require_.here(
          interfaceName.isEmpty,
          "cannot use one explicit name for multiple AXI4 interfaces",
          Seq("Declare the interfaces individually when explicit names are needed.")
        )

        interfaces.foreach { interface =>
          declareAxi4InterfaceOne(
            interface.asInstanceOf[Data],
            associatedClock,
            associatedReset,
            None,
            kind,
            args
          )
        }

      case _ =>
        declareAxi4InterfaceOne(
          interface,
          associatedClock,
          associatedReset,
          interfaceName,
          kind,
          args
        )
    }
  }

  private def declareAxi4InterfaceOne(
      interface: Data,
      associatedClock: String,
      associatedReset: String,
      interfaceName: Option[String],
      kind: String,
      args: Map[String, hdlinfo.TypedObject]
  )(implicit si: SourceInfo): Unit = {
    interface match {
      case tracked: chext.amba.axi4.full.Interface =>
        chext.amba.axi4.tracking.registerInterface(tracked)
      case tracked: chext.amba.axi4.lite.Interface =>
        chext.amba.axi4.tracking.registerInterface(tracked)
      case _ => ()
    }

    val name = nameFor(interface, interfaceName)
    require_.here(
      !hasInterfaceNamed(name),
      "duplicate hdlinfo interface",
      Seq(s"Interface name: $name")
    )

    val declaration = Axi4HdlinfoDeclaration(interface)

    declareAxi4InterfaceImpl(
      name,
      axi4RoleFromValid(declaration.valid),
      declaration.cfg,
      associatedClock,
      associatedReset,
      if (kind.isEmpty) declaration.defaultKind else kind,
      args
    )
  }

  private def axi4RoleFromValid(valid: Bool)(implicit si: SourceInfo): hdlinfo.InterfaceRole = {
    import chisel3.reflect.DataMirror

    DataMirror.directionOf(valid) match {
      case ActualDirection.Input  => hdlinfo.InterfaceRole.slave
      case ActualDirection.Output => hdlinfo.InterfaceRole.master
      case _ =>
        require_.failHere(
          "cannot determine AXI4 interface role",
          Seq(
            s"Valid signal direction: ${DataMirror.directionOf(valid)}",
            "Expected an input valid signal for a slave role or an output valid signal for a master role."
          )
        )
    }
  }

  private def declareAxi4InterfaceImpl(
      name: String,
      role: hdlinfo.InterfaceRole,
      cfg: chext.amba.axi4.Config,
      associatedClock: String,
      associatedReset: String,
      kind: String,
      args: Map[String, hdlinfo.TypedObject]
  ): Unit = {
    import io.circe.generic.auto._

    interfaces.addOne(
      hdlinfo.Interface(
        name,
        role,
        hdlinfo.InterfaceKind(kind),
        associatedClock,
        associatedReset,
        Map("config" -> hdlinfo.TypedObject(cfg)) ++ args
      )
    )
    interfaceNames.addOne(name)
  }

  private def axi4sRoleFromData(data: Data, name: String)(implicit
      si: SourceInfo
  ): hdlinfo.InterfaceRole = {
    import chisel3.reflect.DataMirror

    DataMirror.directionOf(data) match {
      case ActualDirection.Input  => hdlinfo.InterfaceRole.slave
      case ActualDirection.Output => hdlinfo.InterfaceRole.master
      case _ =>
        require_.failHere(
          "cannot determine AXI4-stream interface role",
          Seq(
            s"Interface name: $name",
            s"Data direction: ${DataMirror.directionOf(data)}"
          )
        )
    }
  }

  private def declareAxi4sInterfaceImpl(
      name: String,
      role: hdlinfo.InterfaceRole,
      cfg: chext.amba.axi4s.Config,
      associatedClock: String,
      associatedReset: String,
      kind: String,
      args: Map[String, hdlinfo.TypedObject]
  ): Unit = {
    import io.circe.generic.auto._

    interfaces.addOne(
      hdlinfo.Interface(
        name,
        role,
        hdlinfo.InterfaceKind(kind),
        associatedClock,
        associatedReset,
        Map("config" -> hdlinfo.TypedObject(cfg)) ++ args
      )
    )
    interfaceNames.addOne(name)
  }

  final def declareAxi4sInterface(
      interface: chext.amba.axi4s.Interface,
      associatedClock: String = "clock",
      associatedReset: String = "reset",
      interfaceName: Option[String] = None,
      kind: String = "axi4s",
      args: Map[String, hdlinfo.TypedObject] = Map.empty
  )(implicit si: SourceInfo): Unit = {
    val name = nameFor(interface, interfaceName)
    require_.here(
      !hasInterfaceNamed(name),
      "duplicate hdlinfo interface",
      Seq(s"Interface name: $name")
    )

    declareAxi4sInterfaceImpl(
      name,
      axi4sRoleFromData(interface.TDATA, name),
      interface.cfg,
      associatedClock,
      associatedReset,
      kind,
      args
    )
  }

  final def declareAxi4sInterface(
      interface: chext.elastic.Interface[chext.amba.axi4s.FullChannel],
      cfg: chext.amba.axi4s.Config,
      associatedClock: String,
      associatedReset: String,
      interfaceName: Option[String],
      kind: String,
      args: Map[String, hdlinfo.TypedObject]
  )(implicit si: SourceInfo): Unit = {
    val name = nameFor(interface, interfaceName)
    require_.here(
      !hasInterfaceNamed(name),
      "duplicate hdlinfo interface",
      Seq(s"Interface name: $name")
    )

    declareAxi4sInterfaceImpl(
      name,
      axi4sRoleFromData(interface.$bits.data, name),
      cfg,
      associatedClock,
      associatedReset,
      kind,
      args
    )
  }

  final def declareAxi4sInterface(
      interface: chext.elastic.Interface[chext.amba.axi4s.FullChannel],
      cfg: chext.amba.axi4s.Config
  )(implicit si: SourceInfo): Unit = {
    declareAxi4sInterface(
      interface = interface,
      cfg = cfg,
      associatedClock = "clock",
      associatedReset = "reset",
      interfaceName = None,
      kind = "axi4s_rtl_hier",
      args = Map.empty
    )
  }

  final def declareElasticInterface[T <: Data](
      interfaces: Seq[chext.elastic.Interface[T]],
      dataTypeName: String
  )(implicit si: SourceInfo): Unit =
    declareElasticInterface(
      interfaces,
      dataTypeName,
      "clock",
      "reset",
      "readyValid[$]",
      Map.empty[String, hdlinfo.TypedObject]
    )

  final def declareElasticInterface[T <: Data](
      interfaces: Seq[chext.elastic.Interface[T]],
      dataTypeName: String,
      associatedClock: String,
      associatedReset: String,
      kind: String,
      args: Map[String, hdlinfo.TypedObject]
  )(implicit si: SourceInfo): Unit =
    interfaces.foreach { interface =>
      declareElasticInterface(
        interface,
        dataTypeName,
        associatedClock,
        associatedReset,
        None,
        kind,
        args
      )
    }

  final def declareElasticInterface[T <: Data](
      interface: chext.elastic.Interface[T],
      dataTypeName: String,
      associatedClock: String = "clock",
      associatedReset: String = "reset",
      interfaceName: Option[String] = None,
      kind: String = "readyValid[$]",
      args: Map[String, hdlinfo.TypedObject] = Map.empty
  )(implicit si: SourceInfo): Unit = {
    val name = nameFor(interface, interfaceName)
    require_.here(
      !hasInterfaceNamed(name),
      "duplicate hdlinfo interface",
      Seq(s"Interface name: $name")
    )

    val role = {
      import chisel3.reflect.DataMirror

      DataMirror.directionOf(interface.$bits) match {
        case ActualDirection.Input  => hdlinfo.InterfaceRole("source")
        case ActualDirection.Output => hdlinfo.InterfaceRole("sink")
        case _ =>
          require_.failHere(
            "cannot determine elastic interface role",
            Seq(
              s"Interface name: $name",
              s"bits direction: ${DataMirror.directionOf(interface.$bits)}"
            )
          )
      }
    }

    if (!encodedDataNames(dataTypeName)) {
      encodedDataBuilder.add(dataTypeName, chiselTypeOf(interface.$bits))
      encodedDataNames.addOne(dataTypeName)
    }

    interfaces.addOne(
      hdlinfo.Interface(
        name,
        role,
        hdlinfo.InterfaceKind(kind.replace("$", dataTypeName)),
        associatedClock,
        associatedReset,
        args
      )
    )
    interfaceNames.addOne(name)
  }

  final def hdlinfoModule: hdlinfo.Module = {
    val portsSeq = ports.toSeq
    val interfacesSeq = interfaces.toSeq

    portsSeq.foreach { case port =>
      if (port.kind.str == "clock") {
        require_(
          hasPortNamed(port.associatedReset),
          "clock port references an unknown reset",
          Seq(
            s"Clock port: ${port.name}",
            s"Associated reset: ${port.associatedReset}",
            s"Known ports: ${portNames.toSeq.sorted.mkString(", ")}"
          )
        )
      } else if (port.kind.str == "reset") {
        require_(
          hasPortNamed(port.associatedClock),
          "reset port references an unknown clock",
          Seq(
            s"Reset port: ${port.name}",
            s"Associated clock: ${port.associatedClock}",
            s"Known ports: ${portNames.toSeq.sorted.mkString(", ")}"
          )
        )
      } else {
        require_(
          hasPortNamed(port.associatedClock) && hasPortNamed(port.associatedReset),
          "port references an unknown clock or reset",
          Seq(
            s"Port: ${port.name}",
            s"Associated clock: ${port.associatedClock}",
            s"Associated reset: ${port.associatedReset}",
            s"Known ports: ${portNames.toSeq.sorted.mkString(", ")}"
          )
        )
      }
    }

    interfacesSeq.foreach { case interface =>
      require_(
        hasPortNamed(interface.associatedClock) && hasPortNamed(interface.associatedReset),
        "interface references an unknown clock or reset",
        Seq(
          s"Interface: ${interface.name}",
          s"Associated clock: ${interface.associatedClock}",
          s"Associated reset: ${interface.associatedReset}",
          s"Known ports: ${portNames.toSeq.sorted.mkString(", ")}"
        )
      )
    }

    hdlinfo.Module(
      desiredName,
      portsSeq,
      interfacesSeq,
      Map.from(args.toSeq ++ Seq(encodedDataBuilder.build()))
    )
  }
}

@deprecated("Use AnnotatedModule; it describes modules annotated for hdlinfo emission.", "0.2.4")
trait TestBenchTop extends AnnotatedModule
