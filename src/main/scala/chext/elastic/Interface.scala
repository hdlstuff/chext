package chext.elastic

import chisel3._

import chisel3.experimental.BaseModule
import chisel3.experimental.SourceInfo
import chisel3.experimental.requireIsChiselType
import chisel3.experimental.requireIsHardware

import chisel3.reflect.DataMirror

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

object track {
  private class ModuleInfo(val module: BaseModule) {
    val childIO = HashMap.empty[BaseModule, Seq[Interface[_]]]
  }

  private val moduleInfos_ = HashMap.empty[BaseModule, ModuleInfo]

  private def trackModule(module: BaseModule): Unit = {
    val parentModule = ModuleInternals.getParent(module)
    parentModule.foreach { trackModule(_) }

    val parentModuleInfo = parentModule match {
      case None        => None // this is the root module
      case Some(value) => Some(moduleInfos_(value))
    }

    if (!moduleInfos_.contains(module)) {
      import chisel3.hacks._

      val moduleInfo = new ModuleInfo(module)

      deferred.add(-1, Some(module.asInstanceOf[RawModule])) {
        val wires = ModuleInternals
          .getWires(module)
          .map { data => DataInternals.getChildrenOfType[Interface[Data]](data) }
          .flatten

        val ports = ModuleInternals
          .getPorts(module)
          .map { case (data, si) =>
            DataInternals.getChildrenOfType[Interface[Data]](data).map { (_, si) }
          }
          .flatten

        wires.foreach { _.sanityCheck() }

        if (module.isInstanceOf[RawModule]) {
          val sourceInfos = ModuleInternals.getChildrenSourceInfo(module.asInstanceOf[RawModule])
          moduleInfo.childIO
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

        parentModuleInfo match {
          case None =>
            // this is the root module, we cannot defer checking IO ports later
            ports.foreach { _._1.sanityCheck(true) }

          case Some(value) =>
            // this is a child module, we can perform the checks later
            value.childIO.addOne(module -> ports.map { _._1 })
        }

        moduleInfos_.remove(module)
      }

      moduleInfos_.addOne(module -> moduleInfo)
    }
  }

  def apply(): Unit = {
    val currentModule = Module.currentModule.getOrElse(
      throw new ChiselException(
        "chext.naming: track must be called from a module!"
      )
    )

    ModuleInternals.getChildren(currentModule)
    trackModule(currentModule)
  }

}

class Interface[+T <: Data](gen: T)(implicit sourceInfo: SourceInfo)
    extends util.ReadyValidIO[T](gen) {
  val module_ = Module.currentModule

  private var markSource_ = ArrayBuffer.empty[(BaseModule, SourceInfo)]
  private var markSink_ = ArrayBuffer.empty[(BaseModule, SourceInfo)]

  def markSource()(implicit si: SourceInfo): Unit = {
    requireIsHardware(this, "chext.elastic.Interface: markSource must be called on a hardware!")

    val currentModule = Module.currentModule.getOrElse(
      throw new ChiselException(
        "chext.elastic.Interface: markSource must be called from a module!"
      )
    )
    markSource_.addOne((currentModule, si))

    // chisel can catch this mistake too, but the error message is not great
    // check 5 below.
    assert(module_.nonEmpty)

    if (DataInternals.isParentIO(this) && currentModule == module_.get) {
      if (DataMirror.directionOf(this.valid) == ActualDirection.Output) {
        val pos = si.makeMessage(x => x)
        println(
          f"chext.elastic.Interface.markSource : Interface '$this' is declared as a Sink, but marked as Source. $pos"
        )
      }
    }

    // ensure that the current module is tracked
    track()
  }

  def markSink()(implicit si: SourceInfo): Unit = {
    requireIsHardware(this, "chext.elastic.Interface: markSink must be called on a hardware!")

    val currentModule = Module.currentModule.getOrElse(
      throw new ChiselException(
        "chext.elastic.Interface: markSink must be called from a module!"
      )
    )
    markSink_.addOne((currentModule, si))

    assert(module_.nonEmpty)

    if (DataInternals.isParentIO(this) && currentModule == module_.get) {
      if (DataMirror.directionOf(this.valid) == ActualDirection.Input) {
        val pos = si.makeMessage(x => x)
        println(
          f"chext.elastic.Interface.markSink : Interface '$this' is declared as a Source, but marked as Sink. $pos"
        )
      }
    }

    track()
  }

  private[elastic] def sanityCheck(
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

object Interface {
  def apply[T <: Data](gen: T)(implicit sourceInfo: SourceInfo): Interface[T] = {
    requireIsChiselType(
      gen,
      sourceInfo.makeMessage((x) => "elastic.Interface: expected Chisel type for gen $x")
    )
    new Interface(gen)
  }
}

object Source {
  def apply[T <: Data](gen: T)(implicit sourceInfo: SourceInfo): Interface[T] = {
    requireIsChiselType(
      gen,
      sourceInfo.makeMessage((x) => "elastic.Source: expected Chisel type for gen $x")
    )
    Flipped(new Interface(gen))
  }

  def io[T <: Data](gen: T)(implicit sourceInfo: SourceInfo): Interface[T] = IO(apply(gen))

  def like[T <: Data](hw: Interface[T])(implicit sourceInfo: SourceInfo) = {
    requireIsHardware(
      hw,
      sourceInfo.makeMessage((x) => "elastic.Source: expected hardware for hw $x")
    )
    Source(chiselTypeOf(hw.bits))
  }

  def ioLike[T <: Data](hw: Interface[T])(implicit sourceInfo: SourceInfo) = IO(like(hw))
}

object Sink {
  def apply[T <: Data](gen: T)(implicit sourceInfo: SourceInfo): Interface[T] = {
    requireIsChiselType(
      gen,
      sourceInfo.makeMessage((x) => "elastic.Sink: expected Chisel type for gen $x")
    )
    new Interface(gen)
  }

  def io[T <: Data](gen: T)(implicit sourceInfo: SourceInfo): Interface[T] = IO(apply(gen))

  def like[T <: Data](hw: Interface[T])(implicit sourceInfo: SourceInfo) = {
    requireIsHardware(
      hw,
      sourceInfo.makeMessage((x) => "elastic.Sink: expected hardware for hw $x")
    )
    Sink(chiselTypeOf(hw.bits))
  }

  def ioLike[T <: Data](hw: Interface[T])(implicit sourceInfo: SourceInfo) = IO(like(hw))
}

object EWire {
  def apply[T <: Data](gen: T)(implicit sourceInfo: SourceInfo) = {
    requireIsChiselType(
      gen,
      sourceInfo.makeMessage((x) => "elastic.EWire: expected Chisel type for gen $x")
    )
    Wire(Interface(gen))
  }

  def like[T <: Data](hw: Interface[T])(implicit sourceInfo: SourceInfo) = {
    requireIsHardware(
      hw,
      sourceInfo.makeMessage((x) => "elastic.EWire: expected hardware for hw $x")
    )
    Wire(Interface(chiselTypeOf(hw.bits)))
  }
}

private object InterfaceApp extends App {
  class Module2 extends Module {
    val source = Source.io(UInt(32.W))
    val sink = Sink.io(UInt(32.W))

    val transform0 = new Transform(source, sink) {
      out := in + 1.U
    }
  }

  class Module1 extends Module {
    val source = Source.io(UInt(32.W))
    val sink = Sink.io(UInt(32.W))

    val transform0 = new Transform(source, sink) {
      out := in + 1.U
    }

    val module = Module(new Module2)
  }

  class Module0 extends Module {
    val source = Source.io(UInt(32.W))
    val sink = Sink.io(UInt(32.W))

    val x_source = Source.io(UInt(32.W))
    val x_sink = Sink.io(UInt(32.W))

    val module = Module(new Module1)

    val transform0 = new Transform(source, sink) {
      out := in + 1.U
    }
  }

  emitVerilog(new Module0, Array("--target-dir", "output/"))
}
