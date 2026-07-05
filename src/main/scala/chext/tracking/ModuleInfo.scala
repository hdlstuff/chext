package chext.tracking

import chisel3.{Data, RawModule}
import chisel3.experimental.{BaseModule, SourceInfo}

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
  private val require_ = chext.util.Require.inferred()

  private var atModuleBodyEndCalled_ = false
  private val logger = new Logger("tracking")

  private val childIO_ = HashMap.empty[BaseModule, Seq[Tracked]]
  private val children_ = HashMap.empty[BaseModule, ModuleInfo]

  private val baseComponents_ = ArrayBuffer.empty[BaseComponent]
  private case class ViewSource(
      source: Data,
      suffix: Option[String],
      sourceInfo: Option[SourceInfo]
  )

  private val viewSources_ = HashMap.empty[Tracked, ViewSource]

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

  private[tracking] def registerView(
      view: Data,
      source: Data,
      suffix: Option[String],
      sourceInfo: Option[SourceInfo]
  ): Unit = {
    DataInternals
      .getChildrenOfType[Tracked](view)
      .foreach { interface => viewSources_.addOne(interface -> ViewSource(source, suffix, sourceInfo)) }
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

  /** Resolves interfaces that are real hardware objects.
    *
    * Wires are local to this module. IO can either belong to this module or to a child module,
    * where it is displayed under the child instance path. DataView-created interfaces are not
    * wires or IO and intentionally fall through to `None`; `viewInterfaceRef` handles those using
    * the explicit `.asLite`/`.asFull` registration.
    */
  private def directInterfaceRef(interface: Data): Option[Graph.InterfaceRef] = {
    val earlyName = DataInternals.earlyName(interface)

    if (DataInternals.isWire(interface))
      Some(Graph.InterfaceRef(path = f"/$earlyName", desc = "Wire"))
    else if (DataInternals.isIO(interface)) {
      val owningModule = DataInternals.getOwningModule(interface)

      if (owningModule == module)
        Some(Graph.InterfaceRef(path = f"/$earlyName", desc = "IO"))
      else {
        val instanceName = childInstanceName(owningModule)

        Some(Graph.InterfaceRef(path = f"/$instanceName/$earlyName", desc = "ChildIO"))
      }
    } else
      None
  }

  /** Returns the raw interface metadata for a tracked child of a registered view.
    *
    * `.asLite`/`.asFull` register every tracked child of their elastic view. This lookup is the
    * bridge from a Scala-side view object back to the raw AXI/AXIS hardware interface that the
    * user named.
    */
  private def viewSource(interface: Tracked): Option[ViewSource] =
    viewSources_.get(interface)

  /** Derives a display suffix for view children that did not register one explicitly.
    *
    * AXI4 views expose named elastic channel children such as `ar`, `r`, and `b`; their Chisel
    * early names therefore contain a useful last segment. That segment becomes `$view.<channel>`.
    * Protocols whose whole raw interface maps to one elastic endpoint, such as AXI4 Stream, pass
    * an explicit suffix at registration time and do not use this fallback.
    */
  private def viewSuffix(interface: Tracked): String =
    DataInternals
      .earlyName(interface)
      .split('.')
      .lastOption
      .map { channel => f"$$view.$channel" }
      .getOrElse("$view")

  /** Resolves a registered view child to the graph/display path of its raw source.
    *
    * The raw source is first resolved like ordinary hardware, then the registered or inferred view
    * suffix is appended. Examples are `/s_axi$view.ar` for AXI4 and `/axis$view` for AXI4 Stream.
    */
  private def viewInterfaceRef(interface: Tracked): Option[Graph.InterfaceRef] =
    viewSource(interface).flatMap { viewSource =>
      directInterfaceRef(viewSource.source).map { sourceRef =>
        Graph.InterfaceRef(
          path = f"${sourceRef.path}${viewSource.suffix.getOrElse(viewSuffix(interface))}",
          desc = f"View(${sourceRef.desc})"
        )
      }
    }

  /** Resolves the path used by both module graph generation and tracking diagnostics.
    *
    * Direct hardware paths are preferred. If the interface is not itself hardware, the registered
    * view map is used to recover a path from the raw source interface.
    */
  private def interfaceRef(interface: Tracked): Option[Graph.InterfaceRef] =
    directInterfaceRef(interface).orElse(viewInterfaceRef(interface))

  /** Chooses the module shown as the owner in tracking diagnostics.
    *
    * For views, the useful owner is the raw source interface's module, not the internal Chisel
    * view object. Ordinary interfaces fall back to their actual owning module.
    */
  private def interfaceDisplayOwner(interface: Tracked): Option[BaseModule] =
    viewSource(interface)
      .flatMap(viewSource => DataInternals.getOwningModuleOption(viewSource.source))
      .orElse(DataInternals.getOwningModuleOption(interface))

  /** Chooses the source location shown in tracking diagnostics.
    *
    * Registered views carry the `.asLite` or `.asFull` call-site SourceInfo so warnings point at
    * the code that introduced the viewed endpoint. Ordinary interfaces keep their own source
    * information.
    */
  private def interfaceDisplaySourceInfo(interface: Tracked): Option[SourceInfo] =
    viewSource(interface).flatMap(_.sourceInfo)

  /** Called when the module body completes.
    */
  def atModuleBodyEnd(): Unit = {
    require_(!atModuleBodyEndCalled_, "atModuleBodyEnd must be called once!")
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
      viewSources_.keySet.foreach { sanityCheck(_, true, None) }

      if (module.isInstanceOf[RawModule]) {
        val sourceInfos = ModuleInternals.getChildrenSourceInfo(module.asInstanceOf[RawModule])

        childIO
          .map { case (module, io) =>
            // this one maps the source infos of the module instantiations
            (module, io, sourceInfos.get(module))
          }
          .foreach {
            case (module, io, sourceLocation) => {
              io.foreach { sanityCheck(_, false, sourceLocation) }
            }
          }
      }

      parent match {
        case None =>
          // this is the root module, we cannot defer checking IO ports later
          ports.foreach { case (interface, _) => sanityCheck(interface, true, None) }

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

          val ref = interfaceRef(interface)
            .getOrElse {
              logger.error(
                "constructModuleGraph",
                "Encountered an interface which is neither an IO or Wire.",
                "Maybe you used a view? Please do not use views.",
                f"${interface} @[${sourceInfoToString(interface.sourceInfo)}]"
              )

              Graph.InterfaceRef(path = f"/???/$earlyName", desc = "Unknown")
            }

          (interface, ref)
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
