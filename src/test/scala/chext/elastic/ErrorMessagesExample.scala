package chext.elastic

import chisel3._

object ErrorMessagesExample extends App {
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

  show("Fold missing last") {
    emitVerilog(
      new FoldMissingLastExample,
      Array("--target-dir", "/tmp/chext-error-examples")
    )
  }

  show("Transducer action outside packet") {
    emitVerilog(
      new TransducerActionOutsidePacketExample,
      Array("--target-dir", "/tmp/chext-error-examples")
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

private class TransducerActionOutsidePacketExample extends Module {
  val source = IO(Source(UInt(8.W)))
  val sink = IO(Sink(UInt(8.W)))

  new Transducer(source, sink) {
    accept {
      out := in
    }
  }
}
