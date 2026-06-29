package chext.tracking

import chisel3.experimental.SourceInfo
import chisel3.hacks.PrefixManager

trait HasPath extends chisel3.experimental.AffectsChiselPrefix {
  def tpe: String
  def namePrefix: String

  private[tracking] final val path = PrefixManager.current
  private[tracking] final val pathStr = PrefixManager.currentStr

  /** This variable shall be provided by derived classes. It is advisable for a class to have a
    * private implicit `SourceInfo` because if that class contains other modules needing source
    * infos, source locator is broken.
    *
    * @return
    */
  val sourceInfo: SourceInfo
}

private object Prefix_Emit extends App {
  import chisel3._
  import chext.{elastic => e}
  import e.ConnectOp._

  class MyModule0 extends Module {
    val source = IO(e.Source(UInt(32.W)))
    val sink0 = IO(e.Sink(UInt(32.W)))
    val sink1 = IO(e.Sink(UInt(32.W)))

    val wire0 = e.EWire(UInt(0.W))
    val wire1 = e.EWire(UInt(0.W))

    new e.Fork(source) {
      fork() :=> sink0
      fork() :=> sink1

      new e.Fork(wire0) {
        fork() :=> wire1
      }
    }

    val nullSource0 = new e.NullSource(wire0)
    val nullSink0 = new e.NullSink(wire1)
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

      val join0 = new e.Join(e.SinkBuffer(sinkB)) {
        val result = WireInit(join(wire0) + join(sourceB))
        dontTouch(result)
        out := result
      }
    }

    val m = Module(new MyModule0)

    sourceC :=> m.source
    m.sink0 :=> sinkC
    m.sink1 :=> sinkD

    chext.tracking.onComplete(this) {
      val moduleGraph = chext.tracking.moduleGraphOption(this).get

      import io.circe.syntax._
      import io.circe.generic.auto._

      // println(moduleGraph.flatten.asJson.toString())
    }
  }

  emitVerilog(new MyModule, Array("--target-dir", "output/"))
}
