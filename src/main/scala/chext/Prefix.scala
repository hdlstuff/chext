package chext

import chisel3._
import chisel3.util._

import chisel3.experimental.BaseModule
import chisel3.experimental.SourceInfo
import chisel3.hacks._

import scala.collection.mutable.HashMap
import chisel3.experimental.AffectsChiselPrefix

object Prefix {
  private class ModuleInfo(val module: BaseModule) {
    private var usedPrefixes_ = HashMap.empty[String, (String, SourceInfo)]
    private var checks_ = true

    def checkPrefix(name: String, startsWith: String)(implicit si: SourceInfo): Unit = {
      if (!checks_)
        return

      val currentPrefix = PrefixManager.currentStr

      if (currentPrefix.isEmpty) {
        val pos0 = si.makeMessage(x => x)

        println(
          // format: off
          f"chext.naming: Warning: '$name' needs a unique prefix to avoid confusion. Current prefix is empty.\n" +
          f"              Module '${module.name}'\n" +
          f"              Attempt to create '$name' $pos0"
          // format: on
        )
      } else if (usedPrefixes_.contains(currentPrefix)) {
        val old = usedPrefixes_(currentPrefix)

        val pos0 = si.makeMessage(x => x)
        val pos1 = old._2.makeMessage(x => x)

        println(
          // format: off
          f"chext.naming: Warning: '$name' needs a unique prefix to avoid confusion. Current prefix '$currentPrefix' is used already by '${old._1}'.\n" +
          f"              Module '${module.name}', prefix '$currentPrefix'\n" +
          f"              Used already by '${old._1}' $pos1\n" +
          f"              Attempt to create '$name' $pos0"
          // format: on
        )
      } else {
        if (startsWith.nonEmpty) {
          val pos0 = si.makeMessage(x => x)

          if (!PrefixManager.current.head.startsWith(startsWith))
            println(
              // format: off
              f"chext.naming: Warning: '$name' needs a unique prefix starting with '$startsWith', but the current prefix is '$currentPrefix'.\n" +
              f"              Module '${module.name}', prefix '$currentPrefix'\n" +
              f"              Attempt to create '$name' $pos0"
              // format: on
            )
        }

        usedPrefixes_.addOne(currentPrefix -> (name, si))
      }
    }

    def prefix[T](p: String)(f: => T)(implicit si: SourceInfo): T = {
      PrefixManager.withRelative(p) {
        usedPrefixes_.addOne(PrefixManager.currentStr -> ("naming.prefix", si))
        f
      }
    }

    def weakPrefix[T](p: String)(f: => T)(implicit si: SourceInfo): T = {
      PrefixManager.withRelative(p) {
        f
      }
    }

    def noPrefixChecks[T](f: => T): T = {
      val oldChecks_ = checks_

      checks_ = false
      val t = f
      checks_ = oldChecks_

      t
    }

    def currentPrefix: String = {
      PrefixManager.currentStr
    }

  }

  private val moduleInfos = HashMap.empty[BaseModule, ModuleInfo]

  private def getModuleInfo(): ModuleInfo = {
    val currentModule = Module.currentModule.getOrElse(
      throw new ChiselException(
        "chext.naming: needsUniquePrefix must be called from a module!"
      )
    )
    moduleInfos.getOrElseUpdate(
      currentModule, {
        val moduleInfo = new ModuleInfo(currentModule)

        deferred.add(-1) {
          moduleInfos.remove(currentModule)
        }

        moduleInfo
      }
    )

  }

  def needsPrefix(name: String, startsWith: String = "")(implicit si: SourceInfo): Unit =
    getModuleInfo().checkPrefix(name, startsWith)

  def prefix[T](p: String)(f: => T)(implicit si: SourceInfo): T =
    getModuleInfo().prefix(p) { f }

  /** Introduces a prefix without owning it (unlike `prefix`).
    */
  def weakPrefix[T](p: String)(f: => T)(implicit si: SourceInfo): T =
    getModuleInfo().weakPrefix(p) { f }

  def noPrefixChecks[T](f: => T): T =
    getModuleInfo().noPrefixChecks { f }

  def currentPrefix: String = getModuleInfo().currentPrefix

}

private object Prefix_Emit extends App {
  import _root_.chext.{elastic => e}
  import e.ConnectOp._

  class MyModule0 extends Module {
    val source = IO(e.Source(UInt(32.W)))
    val sink0 = IO(e.Sink(UInt(32.W)))
    val sink1 = IO(e.Sink(UInt(32.W)))

    val wire0 = e.EWire(UInt(0.W))
    val wire1 = e.EWire(UInt(0.W))

    val fork0 = new e.Fork(source) {
      fork() :=> sink0
      fork() :=> sink1

      new e.Fork(wire0) {
        fork() :=> wire1
      }
    }

    wire0.noenq()
    wire1.nodeq()

    wire0.markSink()
    wire1.markSource()
  }

  class MyModule extends Module {
    val sourceA = IO(e.Source(UInt(32.W)))
    val sourceB = IO(e.Source(UInt(32.W)))
    val sourceC = IO(e.Source(UInt(32.W)))
    val sinkA = IO(e.Sink(UInt(32.W)))
    val sinkB = IO(e.Sink(UInt(32.W)))
    val sinkC = IO(e.Sink(UInt(32.W)))
    val sinkD = IO(e.Sink(UInt(32.W)))

    val wire0 = e.EWire.like(sourceA)

    val fork0 = new e.Fork(sourceA) {
      fork() :=> e.SinkBuffer(sinkA)
      fork() :=> wire0

      new e.Join(e.SinkBuffer(sinkB)) {
        val result = WireInit(join(wire0) + join(sourceB))
        dontTouch(result)
        out := result
      }
    }

    val m = Module(new MyModule0)

    sourceC :=> m.source
    m.sink0 :=> sinkC
    m.sink1 :=> sinkD
  }

  emitVerilog(new MyModule, Array("--target-dir", "output/"))
}
