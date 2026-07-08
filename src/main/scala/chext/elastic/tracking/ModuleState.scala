package chext.elastic.tracking

import chisel3.RawModule
import chisel3.experimental.{BaseModule, SourceInfo}
import chisel3.hacks.{DataInternals, ModuleInternals}

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import chext.tracking
import chext.tracking.Logger
import chext.tracking.util.sourceInfoToString

final class ModuleState private[tracking] (moduleInfo: tracking.ModuleInfo)
    extends tracking.ModuleState {
  private val logger = new Logger("tracking")

  private var moduleGraph_ = Option.empty[Graph.Module]

  def moduleGraphOption: Option[Graph.Module] =
    moduleGraph_

  def moduleGraph: Graph.Module =
    moduleGraph_.get

  def newComponentState(component: tracking.Component): ComponentState =
    new ComponentState(component)

  private[chext] case class ViewSource(
      source: chisel3.Data,
      suffix: Option[String],
      sourceInfo: Option[SourceInfo]
  )

  private val viewSources_ = HashMap.empty[Tracked, ViewSource]
  private val childIO_ = HashMap.empty[BaseModule, Seq[Tracked]]

  private[chext] def registerView(
      view: chisel3.Data,
      source: chisel3.Data,
      suffix: Option[String],
      sourceInfo: Option[SourceInfo]
  ): Unit = {
    DataInternals
      .getChildrenOfType[Tracked](view)
      .foreach { interface => viewSources_.addOne(interface -> ViewSource(source, suffix, sourceInfo)) }
  }

  private[chext] def viewSource(interface: Tracked): Option[ViewSource] =
    viewSources_.get(interface)

  private[chext] def viewSources: Iterable[Tracked] =
    viewSources_.keys

  private def wires: Seq[Tracked] =
    ModuleInternals
      .getWires(moduleInfo.module)
      .map { data => DataInternals.getChildrenOfType[Tracked](data) }
      .flatten

  private def ports: Seq[(Tracked, SourceInfo)] =
    ModuleInternals
      .getPorts(moduleInfo.module)
      .map { case (data, si) =>
        DataInternals.getChildrenOfType[Tracked](data).map { (_, si) }
      }
      .flatten

  private def viewSuffix(interface: Tracked): String =
    DataInternals
      .earlyName(interface)
      .split('.')
      .lastOption
      .map { channel => f"$$view.$channel" }
      .getOrElse("$view")

  /** Converts a Chisel early name into the spelling used by module-graph paths.
    *
    * `Data.earlyName` exposes Chisel aggregate syntax. For `Record`-backed data this can include
    * dot-separated field names, e.g. `sources.0`; for `Vec`-like data it can include index syntax,
    * e.g. `sources[0]`. The rest of the tracking graph is based on Chisel prefixes via
    * `PrefixManager.currentStr`, and those prefixes are flattened with underscores. Firtool also
    * emits public ports using underscore-separated names such as `sources_0_bits`.
    *
    * Normalizing at this boundary keeps `NamedVec` free to remain a `Record` while ensuring all
    * module-graph interface paths use the same underscore-separated naming convention as component
    * paths and generated HDL ports.
    */
  private def graphName(data: chisel3.Data): String =
    DataInternals
      .earlyName(data)
      .replace('.', '_')
      .replaceAll("\\[([^\\]]+)\\]", "_$1")

  /** Resolves Elastic interfaces that are real hardware objects.
    *
    * Wires are local to this module. IO can either belong to this module or to a child module,
    * where it is displayed under the child instance path. DataView-created interfaces are not
    * wires or IO and intentionally fall through to `None`; `viewInterfacePath` handles those using
    * the explicit `.asLite`/`.asFull` registration.
    */
  private def directInterfaceRef(interface: chisel3.Data): Option[Graph.InterfaceRef] = {
    val name = graphName(interface)

    if (DataInternals.isWire(interface))
      Some(Graph.InterfaceRef(path = f"/$name", desc = "Wire"))
    else if (DataInternals.isIO(interface)) {
      val owningModule = DataInternals.getOwningModule(interface)

      if (owningModule == moduleInfo.module)
        Some(Graph.InterfaceRef(path = f"/$name", desc = "IO"))
      else {
        val instanceName = moduleInfo.childInstanceName(owningModule)

        Some(Graph.InterfaceRef(path = f"/$instanceName/$name", desc = "ChildIO"))
      }
    } else
      None
  }

  private def viewInterfaceRef(interface: Tracked): Option[Graph.InterfaceRef] =
    viewSource(interface).flatMap { viewSource =>
      directInterfaceRef(viewSource.source).map { sourceRef =>
        Graph.InterfaceRef(
          path = f"${sourceRef.path}${viewSource.suffix.getOrElse(viewSuffix(interface))}",
          desc = f"View(${sourceRef.desc})"
        )
      }
    }

  private def interfaceRef(interface: Tracked): Option[Graph.InterfaceRef] =
    directInterfaceRef(interface)
      .orElse(viewInterfaceRef(interface))

  private def interfaceDisplayOwner(interface: Tracked): Option[BaseModule] =
    viewSource(interface)
      .flatMap(viewSource => DataInternals.getOwningModuleOption(viewSource.source))
      .orElse(DataInternals.getOwningModuleOption(interface))

  private def interfaceDisplaySourceInfo(interface: Tracked): Option[SourceInfo] =
    viewSource(interface).flatMap(_.sourceInfo)

  private def addChildIO(module: BaseModule, io: Seq[Tracked]): Unit =
    childIO_.addOne(module -> io)

  private[chext] def sanityChecks(): Unit = {
    def sanityCheck(interface: Tracked, isRootIO: Boolean, instanceSourceInfo: Option[SourceInfo]) =
      interface.sanityCheck(
        logger,
        isRootIO,
        instanceSourceInfo,
        interfaceRef(interface).map(_.path),
        interfaceDisplayOwner(interface),
        interfaceDisplaySourceInfo(interface)
      )

    wires.foreach { sanityCheck(_, false, None) }
    viewSources.foreach { sanityCheck(_, true, None) }

    if (moduleInfo.module.isInstanceOf[RawModule]) {
      val sourceInfos = ModuleInternals.getChildrenSourceInfo(moduleInfo.module.asInstanceOf[RawModule])

      childIO_
        .map { case (module, io) =>
          (module, io, sourceInfos.get(module))
        }
        .foreach {
          case (_, io, sourceLocation) => {
            io.foreach { sanityCheck(_, false, sourceLocation) }
          }
        }
    }

    moduleInfo.parent match {
      case None =>
        ports.foreach { case (interface, _) => sanityCheck(interface, true, None) }

      case Some(parent) =>
        parent.trackingState(Tag).addChildIO(moduleInfo.module, ports.map { _._1 })
    }
  }

  private def constructModuleGraph(): Unit = {
    val components = moduleInfo.baseComponents
      .filter { _.isComponent }
      .map { _.asComponent }

    val containers = moduleInfo.baseComponents
      .filter { _.isContainer }
      .map { _.asContainer }

    val interfaces = HashSet
      .from(
        components
          .map { component =>
            val elasticState = component.trackingState(Tag)
            elasticState.sourcePorts.map { _._2 } ++ elasticState.sinkPorts.map { _._2 }
          }
          .flatten
      )
      .toSeq
      .sortBy(graphName)

    val interfaceRefs = interfaces.map { interface =>
      val name = graphName(interface)

      val ref = interfaceRef(interface)
        .getOrElse {
          logger.error(
            "constructModuleGraph",
            "Encountered an Elastic interface which is neither an IO or Wire.",
            "Maybe you used an unregistered view?",
            f"${interface} @[${sourceInfoToString(interface.sourceInfo)}]"
          )

          Graph.InterfaceRef(path = f"/???/$name", desc = "Unknown")
        }

      interface -> ref
    }.toMap

    moduleGraph_ = Some(
      Graph.Module(
        name = moduleInfo.module.desiredName,
        path = "/",
        sources = {
          ports
            .map { _._1 }
            .filter { _.declaredRole == DeclaredRole.Source }
            .sortBy(graphName)
            .map { interface =>
              Graph.Interface(
                path = f"/${graphName(interface)}",
                tpe = f"${interface.tpe}",
                args = Map.empty
              )
            }
        },
        sinks = {
          ports
            .map { _._1 }
            .filter { _.declaredRole == DeclaredRole.Sink }
            .sortBy(graphName)
            .map { interface =>
              Graph.Interface(
                path = f"/${graphName(interface)}",
                tpe = f"${interface.tpe}",
                args = Map.empty
              )
            }
        },
        wires = {
          interfaces
            .filter { DataInternals.isWire }
            .map { interface =>
              Graph.Interface(
                path = f"/${graphName(interface)}",
                tpe = f"${interface.tpe}",
                args = Map.empty
              )
            }
        },
        components = {
          components.map { component =>
            val elasticState = component.trackingState(Tag)

            Graph.Component(
              path = f"/${component.pathStr}",
              tpe = component.tpe,
              sources = elasticState.sourcePorts.map {
                case (name, interface) => (name, interfaceRefs(interface))
              },
              sinks = elasticState.sinkPorts.map {
                case (name, interface) => (name, interfaceRefs(interface))
              },
              parent = {
                component.parentOption.map { parent => f"/${parent.pathStr}" }.getOrElse("")
              },
              args = component.args.toMap
            )
          }.toSeq
        },
        containers = {
          containers.map { container =>
            Graph.Container(
              path = f"/${container.pathStr}",
              tpe = container.tpe,
              children = container.children.map { child => f"/${child.pathStr}" },
              parent = {
                container.parentOption.map { parent => f"/${parent.pathStr}" }.getOrElse("")
              },
              args = container.args.toMap
            )
          }.toSeq
        },
        children = {
          moduleInfo.children
            .map { _._2 }
            .map { childInfo =>
              childInfo.trackingState(Tag).moduleGraph.copy(
                path = f"/${childInfo.instanceName}"
              )
            }
            .toSeq
            .sortBy(_.name)
        },
        args = moduleInfo.args.toMap
      )
    )
  }

  def onComplete(): Unit = {
    sanityChecks()
    constructModuleGraph()
  }
}
