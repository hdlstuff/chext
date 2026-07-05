package chext.elastic

import chisel3._

object ErrorMessagesExample extends App {
  private val targetDir = "/tmp/chext-require-examples"

  private def show(name: String)(fn: => Unit): Unit = {
    println(s"--- $name ---")

    try {
      fn
      println("no error")
    } catch {
      case e: IllegalArgumentException =>
        println(e.getMessage)
    }

    println()
  }

  show("AXI4 config bad data width") {
    chext.amba.axi4.Config(wData = 7)
  }

  show("AXI4 buffer config negative channel depth") {
    chext.amba.axi4.BufferConfig(aw = -1)
  }

  show("Stream read config with AXI4-Lite") {
    chext.stream.ReadConfig(
      chext.amba.axi4.Config(lite = true)
    )
  }

  show("Fold missing last") {
    emitVerilog(
      new FoldMissingLastExample,
      Array("--target-dir", targetDir)
    )
  }

  show("Fork missing sink") {
    emitVerilog(
      new ForkMissingSinkExample,
      Array("--target-dir", targetDir)
    )
  }

  show("Transducer missing packet") {
    emitVerilog(
      new TransducerMissingPacketExample,
      Array("--target-dir", targetDir)
    )
  }

  show("Transducer action outside packet") {
    emitVerilog(
      new TransducerActionOutsidePacketExample,
      Array("--target-dir", targetDir)
    )
  }

  show("Transducer packet without action") {
    emitVerilog(
      new TransducerPacketWithoutActionExample,
      Array("--target-dir", targetDir)
    )
  }
}

private class FoldMissingLastExample extends Module {
  val source = IO(Source(UInt(8.W)))
  val sink = IO(Sink(UInt(8.W)))

  new Fold(source, chext.elastic.Const(0.U(8.W)), sink) {
    operand { in => in }
  }
}

private class ForkMissingSinkExample extends Module {
  val source = IO(Source(UInt(8.W)))

  new Fork(source) {}
}

private class TransducerMissingPacketExample extends Module {
  val source = IO(Source(UInt(8.W)))
  val sink = IO(Sink(UInt(8.W)))

  new Transducer(source, sink) {}
}

private class TransducerActionOutsidePacketExample extends Module {
  val source = IO(Source(UInt(8.W)))
  val sink = IO(Sink(UInt(8.W)))

  new Transducer(source, sink) {
    accept {
      out := in
    }
  }
}

private class TransducerPacketWithoutActionExample extends Module {
  val source = IO(Source(UInt(8.W)))
  val sink = IO(Sink(UInt(8.W)))

  new Transducer(source, sink) {
    packet {
      out := in
    }
  }
}
