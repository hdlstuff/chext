package chext.tracking

import chisel3.RawModule
import chisel3.experimental.BaseModule

import chisel3.hacks.ModuleInternals
import chisel3.hacks.DataInternals
import chisel3.hacks.PrefixManager

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.Stack

import hdlinfo.TypedObject
import chext.tracking.util.sourceInfoToString

private[tracking] class ModuleInfo(
    val module: BaseModule,
    val parent: Option[ModuleInfo]
) {
  private var atModuleBodyEndCalled_ = false
  private val logger = new Logger("tracking")

  private val childIO_ = HashMap.empty[BaseModule, Seq[Tracked]]
  private val children_ = HashMap.empty[BaseModule, ModuleInfo]

  private val baseComponents_ = ArrayBuffer.empty[BaseComponent]

  private val containerStack_ = Stack.empty[Container]

  private val uniquePrefix_ = HashMap.empty[String, Int]

  private var moduleGraph_ = Option.empty[Graph.Module]

  private var suggestedInstanceName_ = Option.empty[String]

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

  /** Suggests a name for this instance.
    *
    * @param name
    */
  def suggestInstanceName(name: String): Unit = {
    val fullName = (name :: PrefixManager.current).reverse.mkString("_")
    suggestedInstanceName_ = Some(fullName)
  }

  /** @return
    *   Instance name, if used as an instance.
    */
  lazy val instanceName = suggestedInstanceName_ match {
    case Some(value) => value
    case None =>
      try {
        module.instanceName
      } catch {
        case _: NoSuchElementException => {
          logger.error(
            "instanceName",
            f"Instance name of $this could not be determined.",
            f"You instantiated a module in a deferred block, check 'suggestInstanceName'."
          )

          "???"
        }
      }
  }

  private def childInstanceName(childModule: BaseModule) =
    children_(childModule).instanceName

  /** Registers a new component.
    *
    * @param component
    */
  def addComponent(component: BaseComponent): Unit = {
    baseComponents_.addOne(component)
  }

  def pushContainer(container: Container): Unit = {
    containerStack_.push(container)
  }

  def popContainer(): Unit = {
    containerStack_.pop()
  }

  def lastContainerOption: Option[Container] =
    if (containerStack_.length > 0)
      Some(containerStack_.top)
    else
      None

  def uniquePrefix(x: String): String = {
    val fullPrefixStr = (x :: PrefixManager.current).reverse.mkString("_")
    val n = uniquePrefix_.getOrElseUpdate(fullPrefixStr, 0)
    uniquePrefix_.update(fullPrefixStr, n + 1)
    f"$x$n"
  }

  def addArgument(name: String, arg: TypedObject): Unit = {
    args_.addOne(name -> arg)
  }

  def args = args_.toSeq

  /** Adds an onComplete handler.
    *
    * @param f
    * @return
    */
  def onComplete(f: => Unit) = onComplete_.addOne(() => f)

  parent.foreach { _.children_.addOne(module -> this) }

  /** Called when the module body completes.
    */
  def atModuleBodyEnd(): Unit = {
    require(!atModuleBodyEndCalled_, "atModuleBodyEnd must be called once!")
    atModuleBodyEndCalled_ = true

    val wires = ModuleInternals
      .getWires(module)
      .map { data => DataInternals.getChildrenOfType[Tracked](data) }
      .flatten

    val ports = ModuleInternals
      .getPorts(module)
      .map { case (data, si) =>
        DataInternals.getChildrenOfType[Tracked](data).map { (_, si) }
      }
      .flatten

    /** Path checks.
      */
    def pathChecks() = {
      val usedPaths = baseComponents_.groupBy(_.pathStr)

      usedPaths.foreach {
        case (pathStr, users) => {
          def warn(msg: String): Unit =
            logger.warn(
              "pathChecks",
              // format: off
              Seq(
                msg,
                f"Module: ${module.toString()} @[${util.sourceInfoToString(ModuleInternals.getSourceInfo(module))}]",
                f"Prefix: $pathStr"
              ) ++ users.map { util.baseComponentToString(_) }: _*
              // format: on
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

      if (module.isInstanceOf[RawModule]) {
        val sourceInfos = ModuleInternals.getChildrenSourceInfo(module.asInstanceOf[RawModule])

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
          value.childIO_.addOne(module -> ports.map { _._1 })
      }
    }

    sanityChecks()

    /** Constructs the module graph.
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

            if (owningModule == module)
              (interface, Graph.InterfaceRef(path = f"/$earlyName", desc = "IO"))
            else {
              val instanceName = childInstanceName(owningModule)

              (
                interface,
                Graph.InterfaceRef(path = f"/$instanceName/$earlyName", desc = "ChildIO")
              )
            }
          } else {
            logger.error(
              "constructModuleGraph",
              "Encountered an interface which is neither an IO or Wire.",
              "Maybe you used a view? Please do not use views.",
              f"${interface} @[${sourceInfoToString(interface.sourceInfo)}]"
            )

            (
              interface,
              Graph.InterfaceRef(path = f"/???/$earlyName", desc = "Unknown")
            )
          }
        }
      }.toMap

      moduleGraph_ = Some(
        Graph.Module(
          name = module.desiredName,
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
                moduleInfo.moduleGraph.copy(
                  path = f"/${moduleInfo.instanceName}"
                )
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

    /** Unregisters the children, and if there is no parent, this module.
      */
    def unregister() = {
      children.foreach { //
        case (module, _) => Manager.unregisterModule(module)
      }

      if (parent.isEmpty) {
        Manager.unregisterModule(module)
      }
    }

    unregister()

  }
}
