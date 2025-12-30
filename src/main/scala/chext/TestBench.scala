package chext

import chisel3._

object exportIO {
  def module(parentModule: Module, childModule: Module) = {
    import chisel3.reflect.DataMirror
    val ports = DataMirror.modulePorts(childModule)

    ports.foreach {
      case (portName, portData) => {
        if (!(portData.isInstanceOf[Clock] || portData.isInstanceOf[Reset])) {
          IO(chiselTypeOf(portData)).suggestName(portName) <> portData
        }
      }
    }
  }

  def rawModule(parentModule: RawModule, childModule: RawModule) = {
    import chisel3.reflect.DataMirror
    val ports = DataMirror.modulePorts(childModule)

    ports.foreach {
      case (portName, portData) => {
        IO(chiselTypeOf(portData)).suggestName(portName) <> portData
      }
    }
  }
}

private object emitHdlinfo {
  def apply(module: hdlinfo.Module, targetDir: String): Unit = {
    import io.circe.syntax._
    import io.circe.generic.auto._
    import java.io.PrintWriter

    val pw = new PrintWriter(f"${targetDir}/${module.name}.hdlinfo.json")
    pw.write(module.asJson.toString())
    pw.close()
  }
}

private object emitModuleGraph {
  def apply(moduleGraph: tracking.Graph.Module, targetDir: String): Unit = {
    import io.circe.syntax._
    import io.circe.generic.auto._
    import java.io.PrintWriter

    val pw = new PrintWriter(f"${targetDir}/${moduleGraph.name}.moduleGraph.json")
    pw.write(moduleGraph.flatten.asJson.toString())
    pw.close()
  }
}

trait HasHdlinfoModule extends RawModule {
  def hdlinfoModule: hdlinfo.Module
}

/** Utility trait that allows for easier registration of the hdlinfo object.
  */
trait TestBenchTop extends HasHdlinfoModule {
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
      chisel3.reflect.DataMirror.queryNameGuess(x).replace('.', '_')
    )
  }

  private def getValidDirection(x: Data): hdlinfo.PortDirection = {
    import chisel3.reflect.DataMirror

    @scala.annotation.nowarn
    val result = DataMirror.directionOf(x) match {
      case ActualDirection.Output => hdlinfo.PortDirection.output
      case ActualDirection.Input  => hdlinfo.PortDirection.input

      case ActualDirection.Empty              => hdlinfo.PortDirection.none
      case ActualDirection.Unspecified        => hdlinfo.PortDirection.none
      case ActualDirection.Bidirectional(dir) => hdlinfo.PortDirection.none
    }
    require(result.str != "none")

    result
  }

  import chisel3.reflect.DataMirror

  final def declareArg[T: io.circe.Encoder](
      name: String,
      typedObject: hdlinfo.TypedObject
  ): Unit = {
    require(!hasArgNamed(name))
    args.addOne(name -> typedObject)
    argNames.addOne(name)
  }

  final def declareClock(
      clock: Element,
      sensitivity: hdlinfo.PortSensitivity = hdlinfo.PortSensitivity.clockRising,
      frequencyMHz: Float = 100,
      associatedReset: String = "reset",
      portName: Option[String] = None
  ): Unit = {
    val name = nameFor(clock, portName)
    require(!hasPortNamed(name))

    require(sensitivity.str.startsWith("clock"))

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
  ): Unit = {
    val name = nameFor(reset, portName)
    require(!hasPortNamed(name))

    require(sensitivity.str.startsWith("reset"))

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
  ): Unit = {
    val name = nameFor(interrupt, portName)
    require(!hasPortNamed(name))

    require(sensitivity.str.startsWith("interrupt"))

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
      interface: chext.amba.axi4.RawInterface,
      associatedClock: String = "clock",
      associatedReset: String = "reset",
      interfaceName: Option[String] = None,
      kind: String = "axi4",
      args: Map[String, hdlinfo.TypedObject] = Map.empty
  ): Unit = {
    import io.circe.generic.auto._

    val name = nameFor(interface, interfaceName)
    require(!hasInterfaceNamed(name))

    val role = {
      import chisel3.reflect.DataMirror

      DataMirror.directionOf(
        interface.ARVALID.getOrElse(
          interface.AWVALID.getOrElse(
            throw new RuntimeException("Cannot determine the role of the AXI4 interface!")
          )
        )
      ) match {
        case ActualDirection.Input  => hdlinfo.InterfaceRole.slave
        case ActualDirection.Output => hdlinfo.InterfaceRole.master
        case _ =>
          throw new RuntimeException("Cannot determine the role of the AXI4 interface!")
      }
    }

    interfaces.addOne(
      hdlinfo.Interface(
        name,
        role,
        hdlinfo.InterfaceKind(kind),
        associatedClock,
        associatedReset,
        Map("config" -> hdlinfo.TypedObject(interface.cfg)) ++ args
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
  ): Unit = {
    import io.circe.generic.auto._

    val name = nameFor(interface, interfaceName)
    require(!hasInterfaceNamed(name))

    val role = {
      import chisel3.reflect.DataMirror

      DataMirror.directionOf(interface.TDATA) match {
        case ActualDirection.Input  => hdlinfo.InterfaceRole.slave
        case ActualDirection.Output => hdlinfo.InterfaceRole.master
        case _ =>
          throw new RuntimeException("Cannot determine the role of the AXI4-stream interface!")
      }
    }

    interfaces.addOne(
      hdlinfo.Interface(
        name,
        role,
        hdlinfo.InterfaceKind(kind),
        associatedClock,
        associatedReset,
        Map("config" -> hdlinfo.TypedObject(interface.cfg)) ++ args
      )
    )
    interfaceNames.addOne(name)
  }

  final def declareElasticInterface[T <: Data](
      interface: chext.elastic.Interface[T],
      dataTypeName: String,
      associatedClock: String = "clock",
      associatedReset: String = "reset",
      interfaceName: Option[String] = None,
      kind: String = "readyValid[$]",
      args: Map[String, hdlinfo.TypedObject] = Map.empty
  ): Unit = {
    val name = nameFor(interface, interfaceName)
    require(!hasInterfaceNamed(name))

    val role = {
      import chisel3.reflect.DataMirror

      DataMirror.directionOf(interface.$bits) match {
        case ActualDirection.Input  => hdlinfo.InterfaceRole("source")
        case ActualDirection.Output => hdlinfo.InterfaceRole("sink")
        case _ =>
          throw new RuntimeException("Cannot determine the role of the AXI4-stream interface!")
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
    import io.circe.generic.auto._

    val portsSeq = ports.toSeq
    val interfacesSeq = interfaces.toSeq

    portsSeq.foreach { case port =>
      if (port.kind.str == "clock") {
        require(hasPortNamed(port.associatedReset))
      } else if (port.kind.str == "reset") {
        require(hasPortNamed(port.associatedClock))
      } else {
        require(hasPortNamed(port.associatedClock) && hasPortNamed(port.associatedReset))
      }
    }

    interfacesSeq.foreach { case interface =>
      require(hasPortNamed(interface.associatedClock) && hasPortNamed(interface.associatedReset))
    }

    hdlinfo.Module(
      desiredName,
      portsSeq,
      interfacesSeq,
      Map.from(args.toSeq ++ Seq(encodedDataBuilder.build()))
    )
  }
}

trait TestBench extends App {
  private val _pkgName = Option(this.getClass.getPackage).map(_.getName).getOrElse(".")

  def ns(x: String): String = f"${_pkgName}.${x}".stripPrefix(".")

  def emit[T <: HasHdlinfoModule](genModule: => T): Unit = {
    val pkgPath = _pkgName.replace('.', '/')
    val hdlPath = f"./sysc_tb/${pkgPath}/hdl/"
    var moduleName: String = null

    chext.emitSystemVerilog(
      {
        val module = genModule

        tracking.onComplete(module) {
          val hdlinfoModule = Some(module.hdlinfoModule)
          val graphModule = tracking.moduleGraphOption(module)

          hdlinfoModule.foreach { x => emitHdlinfo(x, hdlPath) }
          graphModule.foreach { x => emitModuleGraph(x, hdlPath) }

        }

        moduleName = module.name

        module
      },
      hdlPath
    )
  }
}
