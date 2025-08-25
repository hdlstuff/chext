package chext.elastic.tracking

import chisel3.{ChiselException, Data, Module, RawModule}
import chisel3.experimental.{SourceInfo, BaseModule, requireIsHardware}
import chisel3.hacks.{DataInternals, ModuleInternals}

import chext.elastic.Interface

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

sealed abstract case class DeclaredRole(str: String)

object DeclaredRole {
  object Source extends DeclaredRole("Source")
  object Sink extends DeclaredRole("Sink")
  object None extends DeclaredRole("None")
}

object Tracked {
  private class ModuleInfo(
      val target: BaseModule,
      val parent: Option[ModuleInfo]
  ) {
    private var atModuleBodyEndCalled_ = false
    private val childIO_ = HashMap.empty[BaseModule, Seq[Interface[_]]]
    private val children_ = HashMap.empty[BaseModule, ModuleInfo]
    private val components_ = ArrayBuffer.empty[Component]
    private var graphModule_ = Option.empty[Graph.Module]

    def childIO = childIO_.toMap
    def children = children_.toMap
    def components = components_.toSeq

    def graphModuleOption = graphModule_
    def graphModule = graphModule_.get

    def instanceName = target.instanceName

    parent.foreach { _.children_.addOne(target -> this) }

    def addComponent(component: Component): Unit = {
      println(component)
      components_.addOne(component)
    }

    def atModuleBodyEnd(): Unit = {
      require(!atModuleBodyEndCalled_, "atModuleBodyEnd must be called once!")
      atModuleBodyEndCalled_ = true

      val wires = ModuleInternals
        .getWires(target)
        .map { data => DataInternals.getChildrenOfType[Interface[Data]](data) }
        .flatten

      val ports = ModuleInternals
        .getPorts(target)
        .map { case (data, si) =>
          DataInternals.getChildrenOfType[Interface[Data]](data).map { (_, si) }
        }
        .flatten

      def sanityChecks() = {
        wires.foreach { _.sanityCheck() }

        if (target.isInstanceOf[RawModule]) {
          val sourceInfos = ModuleInternals.getChildrenSourceInfo(target.asInstanceOf[RawModule])
          childIO
            .map { case (module, io) =>
              // this one maps the source infos of the module instantiations
              (module, io, sourceInfos.get(module))
            }
            .foreach {
              case (module, io, sourceLocation) => {
                io.foreach { _.sanityCheck(false, sourceLocation) }
              }
            }
        }

        parent match {
          case None =>
            // this is the root module, we cannot defer checking IO ports later
            ports.foreach { _._1.sanityCheck(true) }

          case Some(value) =>
            // this is a child module, we can perform the checks later
            value.childIO_.addOne(target -> ports.map { _._1 })
        }
      }

      sanityChecks()

      def constructGraphModule() = {
        val interfaces = HashSet
          .from(
            components_.map { x => x.sources.map { _._2 } ++ x.sinks.map { _._2 } }.flatten
          )
          .toSeq
          .sortBy(DataInternals.earlyName)

        val interfaceRefs = interfaces.map { interface =>
          {
            val earlyName = DataInternals.earlyName(interface)

            if (DataInternals.isWire(interface))
              (interface, Graph.InterfaceRef(path = f"/$earlyName", desc = "Wire"))
            else {
              assert(DataInternals.isIO(interface))
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
            }
          }
        }.toMap

        graphModule_ = Some(
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
                    tpe = "TODO",
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
                    tpe = "TODO",
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
                    tpe = "TODO",
                    args = Map.empty // TODO
                  )
                }
            },
            components = {
              components_.map { component =>
                {
                  Graph.Component(
                    path = f"/${component.name}",
                    tpe = component.tpe,
                    sources = component.sources.map { case (name, interface) =>
                      (name, interfaceRefs(interface))
                    },
                    sinks = component.sinks.map { case (name, interface) =>
                      (name, interfaceRefs(interface))
                    },
                    args = component.args
                  )
                }
              }.toSeq
            },
            children = {
              children
                .map { _._2 }
                .map { moduleInfo =>
                  moduleInfo.graphModule.copy(path = f"/${moduleInfo.instanceName}")
                }
                .toSeq
                .sortBy(_.name)
            },
            args = {
              // TODO
              Map.empty
            }
          )
        )
      }

      constructGraphModule()

      import io.circe.syntax._
      import io.circe.generic.auto._
      println(graphModule_.get.flatten.asJson.toString())
    }
  }

  private val moduleInfos_ = HashMap.empty[BaseModule, ModuleInfo]

  private def trackModule(module: BaseModule): ModuleInfo = {
    val parentModule = ModuleInternals.getParent(module)
    parentModule.foreach { trackModule(_) }

    val parentModuleInfo = parentModule.map { moduleInfos_(_) }

    moduleInfos_.getOrElseUpdate(
      module, {
        import chisel3.hacks.deferred

        val moduleInfo = new ModuleInfo(module, parentModuleInfo)

        println("Created a module info for: ", module)

        deferred.add(-1, Some(module.asInstanceOf[RawModule])) {
          moduleInfo.atModuleBodyEnd()
          moduleInfos_.remove(module)
        }

        moduleInfo
      }
    )
  }

  private def trackCurrentModule_(): ModuleInfo = {
    val currentModule = Module.currentModule.getOrElse(
      throw new ChiselException(
        "chext.naming: track must be called from a module!"
      )
    )

    trackModule(currentModule)
  }

  private[tracking] final def trackCurrentModule(): Unit =
    trackCurrentModule_()

  private[tracking] final def registerComponent(component: Component): Unit =
    trackCurrentModule_().addComponent(component)

}

/** This trait brings `markSource()`, `markSink()`, `sanityCheck()` capabilities to `Interface`.
  */
private[elastic] trait Tracked extends Data {
  protected[elastic] def sourceInfo: SourceInfo
  protected[elastic] def declaredRole: DeclaredRole

  private def module_ = DataInternals.getOwningModuleOption(this)

  private val markSource_ = ArrayBuffer.empty[(BaseModule, SourceInfo)]
  private val markSink_ = ArrayBuffer.empty[(BaseModule, SourceInfo)]

  private def mark_(role: DeclaredRole, array: ArrayBuffer[(BaseModule, SourceInfo)])(implicit
      sourceInfo: SourceInfo
  ): Unit = {
    requireIsHardware(
      this,
      f"chext.elastic.Interface: mark${role.str} must be called on a hardware!"
    )

    val currentModule = Module.currentModule.getOrElse(
      throw new ChiselException(
        "chext.elastic.Interface: mark${role.str} must be called from a module!"
      )
    )
    array.addOne((currentModule, sourceInfo))

    // chisel can catch this mistake too, but the error message is not great
    // check 5 below.
    assert(module_.nonEmpty)

    if (currentModule == module_.get) {
      if (declaredRole != DeclaredRole.None && declaredRole != role) {
        val pos = sourceInfo.makeMessage(x => x)

        println(
          f"chext.elastic.Interface.markSource : Interface '$this' is declared as a ${declaredRole.str}, but marked as ${role.str}. $pos"
        )
      }
    }

    // ensure that the current module is tracked
    Tracked.trackCurrentModule()
  }

  final def markSource()(implicit sourceInfo: SourceInfo): Unit =
    mark_(DeclaredRole.Source, markSource_)

  final def markSink()(implicit sourceInfo: SourceInfo): Unit =
    mark_(DeclaredRole.Sink, markSink_)

  /** Sanity checks on the interface.
    *
    * @param isRootIO
    *   True if the interface is an IO port of the root module. Such interfaces cannot be marked
    *   both sink and source.
    *
    * @param instanceSourceInfo
    *   If an interface belongs to child, describes where it is instantiated.
    */
  private[Tracked] final def sanityCheck(
      isRootIO: Boolean = false,
      instanceSourceInfo: Option[SourceInfo] = Option.empty
  ): Unit = {
    val currentModule = Module.currentModule.getOrElse(
      throw new ChiselException(
        "chext.elastic.Interface: sanityCheck must be called from a module!"
      )
    )

    def log(msg: String): Unit = {
      val label = "chext.elastic.Interface.sanityCheck :"
      val indent = label.map(_ => ' ')
      val pos = sourceInfo.makeMessage(x => x)
      val moduleName = module_.map(_.toString()).getOrElse("(null)")

      println(f"$label $msg")

      if (instanceSourceInfo.nonEmpty) {
        val pos = instanceSourceInfo.get.makeMessage(x => x)
        println(
          f"$indent Module '$moduleName' with instance name '${module_.get.instanceName}' is instantiated $pos"
        )
      }

      println(f"$indent Interface '$this' is defined by '$moduleName' $pos")

      markSource_.foreach {
        case (module, si) => {
          val pos = si.makeMessage(x => x)
          println(f"$indent Marked as source by '$module' $pos")
        }
      }

      markSink_.foreach {
        case (module, si) => {
          val pos = si.makeMessage(x => x)
          println(f"$indent Marked as sink by '$module' $pos")
        }
      }

    }

    // log("debug")

    // Check (1)
    if (!isRootIO) {
      if (markSource_.length >= 1 && markSink_.length == 0) {
        log("Interface is marked as a source but never as a sink!")
      }

      if (markSink_.length >= 1 && markSource_.length == 0) {
        log("Interface is marked as a sink but never as a source!")
      }
    }

    // Check (2)
    if (markSource_.isEmpty && markSink_.isEmpty) {
      log("Interface never marked!")
    }

    // Check (3)
    if (markSource_.length > 1) {
      log("Interface marked as source more than 1 times!")
    }

    // Check (4)
    if (markSink_.length > 1) {
      log("Interface marked as sink more than 1 times!")
    }
  }
}
