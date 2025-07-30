package chext.util

import chisel3._
import chisel3.util._

import chisel3.experimental.BaseModule
import chisel3.experimental.SourceInfo
import chisel3.hacks._

import scala.collection.mutable.HashMap

object Naming {
  private class ModuleInfo(val module: BaseModule) {
    private var usedPrefixes = HashMap.empty[String, (String, SourceInfo)]

    def needsUniquePrefix(name: String)(implicit si: SourceInfo): Unit = {
      val currentPrefix = PrefixManager.currentStr

      if (currentPrefix.isEmpty) {
        val pos0 = si.makeMessage(x => x)

        println(
        // format: off
        f"chext.util.Naming: Warning: '$name' needs a unique prefix to avoid confusion. Current prefix is empty.\n" +
        f"                   Module '${module.name}'\n" +
        f"                   Attempt to create '$name' $pos0"
        // format: on
        )
      } else if (usedPrefixes.contains(currentPrefix)) {
        val old = usedPrefixes(currentPrefix)

        val pos0 = si.makeMessage(x => x)
        val pos1 = old._2.makeMessage(x => x)

        println(
        // format: off
        f"chext.util.Naming: Warning: '$name' needs a unique prefix to avoid confusion. Current prefix '$currentPrefix' is used already by '${old._1}'.\n" +
        f"                   Module '${module.name}', prefix '$currentPrefix'\n" +
        f"                   Used already by '${old._1}' $pos1\n" +
        f"                   Attempt to create '$name' $pos0"
        // format: on
        )
      } else {
        usedPrefixes.addOne(currentPrefix -> (name, si))
      }
    }
  }

  private val moduleInfos = HashMap.empty[BaseModule, ModuleInfo]

  private def getModuleInfo(): ModuleInfo = {
    val currentModule = Module.currentModule.getOrElse(
      throw new ChiselException(
        "chext.util.Naming: needsUniquePrefix must be called from a module!"
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

  def needsUniquePrefix(name: String)(implicit si: SourceInfo): Unit =
    getModuleInfo().needsUniquePrefix(name)

}

object NamingApp extends App {
  import chext.elastic
  import elastic.ConnectOp._

  class MyModule0 extends Module {
    val source = IO(elastic.Source(UInt(32.W)))
    val sink0 = IO(elastic.Sink(UInt(32.W)))
    val sink1 = IO(elastic.Sink(UInt(32.W)))

    val wire0 = elastic.EWire(UInt(0.W))
    val wire1 = elastic.EWire(UInt(0.W))

    val fork0 = new elastic.Fork(source) {
      fork() :=> sink0
      fork() :=> sink1

      new elastic.Fork(wire0) {
        fork() :=> wire1
      }
    }

    wire0.noenq()
    wire1.nodeq()
  }

  class MyModule extends Module {
    val sourceA = IO(elastic.Source(UInt(32.W)))
    val sourceB = IO(elastic.Source(UInt(32.W)))
    val sourceC = IO(elastic.Source(UInt(32.W)))
    val sinkA = IO(elastic.Sink(UInt(32.W)))
    val sinkB = IO(elastic.Sink(UInt(32.W)))
    val sinkC = IO(elastic.Sink(UInt(32.W)))
    val sinkD = IO(elastic.Sink(UInt(32.W)))

    val wire0 = elastic.EWire.like(sourceA)

    val fork0 = new elastic.Fork(sourceA) {
      fork() :=> elastic.SinkBuffer(sinkA)
      fork() :=> wire0

      new elastic.Join(elastic.SinkBuffer(sinkB)) {
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

  emitVerilog(new MyModule)
}
