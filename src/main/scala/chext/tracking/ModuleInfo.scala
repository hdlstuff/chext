package chext.tracking

import chisel3.RawModule
import chisel3.experimental.BaseModule

import chisel3.hacks.ModuleInternals
import chisel3.hacks.DataInternals

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.Stack

import hdlinfo.TypedObject

private[tracking] class ModuleInfo(
    val target: BaseModule,
    val parent: Option[ModuleInfo]
) {
  private var atModuleBodyEndCalled_ = false
  private val logger = new Logger("tracking")

  private val childIO_ = HashMap.empty[BaseModule, Seq[Tracked]]
  private val children_ = HashMap.empty[BaseModule, ModuleInfo]

  private val baseComponents_ = ArrayBuffer.empty[BaseComponent]

  private val containerStack_ = Stack.empty[Container]

  private var moduleGraph_ = Option.empty[Graph.Module]

  private val args_ = ArrayBuffer.empty[(String, TypedObject)]

  private var onComplete_ = ArrayBuffer.empty[() => Unit]

  /** IO of the children. Sanity checking for child IO after the child is constructed would trigger
    * false positives because they can be fully marked only by the parent.
    *
    * @return
    *   IO of the children to be checked by the parent.
    */
  def childIO = childIO_.toMap

  /** @return
    *   Children `ModuleInfo`s.
    */
  def children = children_.toMap

  /** @return
    *   Components used by the module.
    */
  def baseComponents = baseComponents_.toSeq

  /** @return
    *   Optional of generated module graph.
    */
  def moduleGraphOption = moduleGraph_

  /** @return
    *   Generated module graph.
    */
  def moduleGraph = moduleGraph_.get

  /** @return
    *   Instance name, if used as an instance.
    */
  def instanceName = target.instanceName

  /** Registers a new component.
    *
    * @param component
    */
  def addComponent(component: BaseComponent): Unit = {
    baseComponents_.addOne(component)
  }

  final def pushContainer(container: Container): Unit = {
    containerStack_.push(container)
  }

  final def popContainer(): Unit = {
    containerStack_.pop()
  }

  final def lastContainerOption: Option[Container] =
    if (containerStack_.length > 0)
      Some(containerStack_.top)
    else
      None

  final def addArgument(name: String, arg: TypedObject): Unit = {
    args_.addOne(name -> arg)
  }

  final def args = args_.toSeq

  /** Adds an onComplete handler.
    *
    * @param f
    * @return
    */
  def onComplete(f: => Unit) = onComplete_.addOne(() => f)

  parent.foreach { _.children_.addOne(target -> this) }

  /** Called when the module body completes.
    */
  def atModuleBodyEnd(): Unit = {
    require(!atModuleBodyEndCalled_, "atModuleBodyEnd must be called once!")
    atModuleBodyEndCalled_ = true

    val wires = ModuleInternals
      .getWires(target)
      .map { data => DataInternals.getChildrenOfType[Tracked](data) }
      .flatten

    val ports = ModuleInternals
      .getPorts(target)
      .map { case (data, si) =>
        DataInternals.getChildrenOfType[Tracked](data).map { (_, si) }
      }
      .flatten

    def pathChecks() = {
      val usedPaths = baseComponents_.groupBy(_.pathStr)

      usedPaths.foreach {
        case (pathStr, users) => {
          def warn(msg: String): Unit =
            logger.warn(
              "pathChecks",
              Seq(
                msg,
                f"Module: ${target.toString()} @[${util
                    .sourceInfoToString(ModuleInternals.getSourceInfo(target))}]",
                f"Prefix: $pathStr"
              ) ++ users.map { util.baseComponentToString(_) }: _*
            )

          if (pathStr.isEmpty) {
            warn("Multiple components or containers use an empty path, which should be avoided")
          } else {
            if (users.length == 1) {
              // this is OK, the prefix has a single user
            } else {
              warn(
                "Multiple components or containers use the same path, which should be avoided"
              )
            }
          }
        }
      }

    }

    pathChecks()

    /** Sanity checks.
      */
    def sanityChecks(): Unit = {
      wires.foreach { _.sanityCheck(logger) }

      if (target.isInstanceOf[RawModule]) {
        val sourceInfos = ModuleInternals.getChildrenSourceInfo(target.asInstanceOf[RawModule])
        childIO
          .map { case (module, io) =>
            // this one maps the source infos of the module instantiations
            (module, io, sourceInfos.get(module))
          }
          .foreach {
            case (module, io, sourceLocation) => {
              io.foreach { _.sanityCheck(logger, false, sourceLocation) }
            }
          }
      }

      parent match {
        case None =>
          // this is the root module, we cannot defer checking IO ports later
          ports.foreach { _._1.sanityCheck(logger, true) }

        case Some(value) =>
          // this is a child module, we can perform the checks later
          value.childIO_.addOne(target -> ports.map { _._1 })
      }
    }

    sanityChecks()

    /** Constructs the module graph in the background.
      */
    def constructmoduleGraph() = {
      val components = baseComponents_
        .filter { _.isComponent }
        .map { x => x.asComponent }

      val containers = baseComponents_
        .filter { _.isContainer }
        .map { x => x.asContainer }

      val interfaces = HashSet
        .from(
          components.map { x => x.sourcePorts.map { _._2 } ++ x.sinkPorts.map { _._2 } }.flatten
        )
        .toSeq
        .sortBy(DataInternals.earlyName)

      val interfaceRefs = interfaces.map { interface =>
        {
          val earlyName = DataInternals.earlyName(interface)

          if (DataInternals.isWire(interface))
            (interface, Graph.InterfaceRef(path = f"/$earlyName", desc = "Wire"))
          else if (DataInternals.isIO(interface)) {
            val owningModule = DataInternals.getOwningModule(interface)

            if (owningModule == target)
              (interface, Graph.InterfaceRef(path = f"/$earlyName", desc = "IO"))
            else {
              val instanceName = owningModule.instanceName

              (
                interface,
                Graph.InterfaceRef(path = f"/$instanceName/$earlyName", desc = "ChildIO")
              )
            }
          } else {
            (
              interface,
              Graph.InterfaceRef(path = f"/???/$earlyName", desc = "Unknown")
            )
          }
        }
      }.toMap

      moduleGraph_ = Some(
        Graph.Module(
          name = target.desiredName,
          path = "/",
          sources = {
            ports
              .map { _._1 }
              .filter { interface =>
                interface.declaredRole == DeclaredRole.Source
              }
              .map { interface =>
                Graph.Interface(
                  path = f"/${DataInternals.earlyName(interface)}",
                  tpe = f"${interface.tpe}",
                  args = Map.empty // TODO
                )
              }
          },
          sinks = {
            ports
              .map { _._1 }
              .filter { interface =>
                interface.declaredRole == DeclaredRole.Sink
              }
              .map { interface =>
                Graph.Interface(
                  path = f"/${DataInternals.earlyName(interface)}",
                  tpe = f"${interface.tpe}",
                  args = Map.empty // TODO
                )
              }
          },
          wires = {
            interfaces
              .filter { interface =>
                DataInternals.isWire(interface)
              }
              .map { interface =>
                Graph.Interface(
                  path = f"/${DataInternals.earlyName(interface)}",
                  tpe = f"${interface.tpe}",
                  args = Map.empty // TODO
                )
              }
          },
          components = {
            components.map { component =>
              {
                Graph.Component(
                  path = f"/${component.pathStr}",
                  tpe = component.tpe,
                  sources = component.sourcePorts.map { //
                    case (name, interface) => (name, interfaceRefs(interface))
                  },
                  sinks = component.sinkPorts.map { //
                    case (name, interface) => (name, interfaceRefs(interface))
                  },
                  parent = {
                    component.parentOption.map { parent => f"/${parent.pathStr}" }.getOrElse("")
                  },
                  args = component.args.toMap
                )
              }
            }.toSeq
          },
          containers = {
            containers.map { container =>
              {
                Graph.Container(
                  path = f"/${container.pathStr}",
                  tpe = container.tpe,
                  children = container.children.map { //
                    child => f"/${child.pathStr}"
                  },
                  parent = {
                    container.parentOption.map { parent => f"/${parent.pathStr}" }.getOrElse("")
                  },
                  args = container.args.toMap
                )
              }
            }.toSeq
          },
          children = {
            children
              .map { _._2 }
              .map { moduleInfo =>
                moduleInfo.moduleGraph.copy(path = f"/${moduleInfo.instanceName}")
              }
              .toSeq
              .sortBy(_.name)
          },
          args = args.toMap
        )
      )
    }

    constructmoduleGraph()

    onComplete_.foreach { _() }

  }
}
