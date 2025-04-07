package chext.elastic2

import chisel3._
import chisel3.experimental.{prefix, AffectsChiselPrefix}

class MyConnect extends AffectsChiselPrefix {
  private val q = RegInit(0.U(0.W))
  dontTouch(q)

  println(chisel3.hacks.PrefixManager.current)
  // println(chisel3.reflect.DataMirror.queryNameGuess(q).stripSuffix("q"))
}

class MyModule extends Module {
  val source = IO(Source(UInt(64.W)))
  val sinkA = IO(Sink(UInt(32.W)))
  val sinkB = IO(Sink(UInt(32.W)))

  val sourceA = IO(Source(UInt(32.W)))
  val sourceB = IO(Source(UInt(32.W)))
  val sink = IO(Sink(UInt(64.W)))

  val sourceX = IO(Source(UInt(64.W)))
  val sinkX = IO(Sink(UInt(64.W)))

  val outData = IO(UInt(64.W))

  import chext.util.BitOps._
  import chext.elastic.ConnectOp._

  prefix("myPrefix") {
    val buffered = SourceBuffer(source)

    val fork1 = new Fork(buffered) {
      fork(in.lsbN(32)) :=> sinkA
      fork(in.msbN(32)) :=> sinkB
    }

    val reg = RegInit(0.U)
    outData := reg
    reg := reg + 1.U
  }

  val join1 = new Join(sink) {
    out := join(sourceA) ## join(sourceB)
  }

  val fork2 = new Fork(sourceX) {
    val join2 = new Join(sinkX) {
      out := join(fork()) + join(fork())
    }
  }
}

class MyModule2 extends Module {
  val source = IO(Source(Bool()))
  val sink = IO(Sink(Bool()))

  Queue(source, sink, 18)
}

class MyModule3 extends Module {
  val in = IO(Input(UInt(4.W)))
  val out = IO(Output(UInt(4.W)))

  def f[T](t: T) = t

  val x = new MyConnect {
    val reg = RegInit(0.U(32.W))
    out := in + reg
    reg := reg + 1.U

    { val y = f(new MyConnect {}) }
    { val y = f(new MyConnect {}) }
  }
}

object EmitMyModule extends App {
  emitVerilog(new MyModule)
  emitVerilog(new MyModule2)
  emitVerilog(new MyModule3)
}
