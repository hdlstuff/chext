package chext.elastic2

import chisel3._
import chisel3.experimental.{prefix, AffectsChiselPrefix}
import chext.HasHdlinfoModule

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

class MyBundle extends Bundle {
  val a = UInt(8.W)
  val b = UInt(16.W)
}

class MyModule2 extends Module {
  val sourceA = IO(Source(UInt(32.W)))
  val sinkA = IO(Sink(UInt(32.W)))

  val sourceB = IO(Source(new MyBundle))
  val sinkB = IO(Sink(new MyBundle))

  Queue.between(sourceA, sinkA, 18, flow = true, pipe = true, useVerilog = false)

  val buffer1 = LeftBuffer(sourceB)
  val buffer2 = RightBuffer(sinkB)
  Queue.between(
    buffer1,
    buffer2,
    18,
    flow = true,
    pipe = true,
    useVerilog = true
  )
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

class MyModule4 extends Module {
  val in = IO(Source(UInt(4.W)))
  val out = IO(Sink(UInt(4.W)))

  val count = IO(Output(UInt(32.W)))

  private val count_ = RegInit(0.U(32.W))
  count := count_

  val arrival0 = new Arrival(in, out) {
    when(arrived) {
      count_ := count_ + 1.U
      accept()
    }
  }
}

class QueueTestTop1 extends Module with HasHdlinfoModule {
  val source = IO(Source(UInt(32.W)))
  val sink = IO(Sink(UInt(32.W)))

  Queue.between(source, sink, 18, flow = true, pipe = true, useVerilog = true)

  def hdlinfoModule: hdlinfo.Module = {
    import hdlinfo._
    import io.circe.generic.auto._
    import scala.collection.mutable.ArrayBuffer

    val ports = ArrayBuffer.empty[Port]
    val interfaces = ArrayBuffer.empty[Interface]

    ports.append(
      Port(
        "clock",
        PortDirection.input,
        PortKind.clock,
        PortSensitivity.clockRising,
        associatedReset = "reset"
      )
    )
    ports.append(
      Port(
        "reset",
        PortDirection.input,
        PortKind.reset,
        PortSensitivity.resetActiveHigh,
        associatedClock = "clock"
      )
    )

    interfaces.append(
      Interface(
        "source",
        InterfaceRole("source"),
        InterfaceKind("readyValid[chext.elastic.Data]"),
        associatedClock = "clock",
        associatedReset = "reset",
        args = Map("width" -> TypedObject(32))
      )
    )

    interfaces.append(
      Interface(
        "sink",
        InterfaceRole("sink"),
        InterfaceKind("readyValid[chext.elastic.Data]"),
        associatedClock = "clock",
        associatedReset = "reset",
        args = Map("width" -> TypedObject(32))
      )
    )

    Module(
      "QueueTestTop1",
      ports.toSeq,
      interfaces.toSeq
    )
  }
}

class MyModule5 extends Module {
  val sel = IO(Input(UInt(3.W)))

  val in = IO(Input(Vec(8, UInt(32.W))))
  val out = IO(Output(UInt(32.W)))

  out := in(sel)
}

object Queue_TB extends App with chext.TestBench {
  emit(new QueueTestTop1)
}

object EmitMyModule extends App {
  // emitVerilog(new MyModule)
  // emitVerilog(new MyModule2)
  // emitVerilog(new MyModule3)
  // emitVerilog(new MyModule4)
  // emitVerilog(new QueueTestTop1)
  emitVerilog(new MyModule5)
}
