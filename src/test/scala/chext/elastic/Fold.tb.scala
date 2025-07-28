package chext.elastic

import chisel3._

import chext.elastic

class Fold_Tbtop extends Module with chext.TestBenchTop {
  val source = IO(Source(new Bundle {
    val zero = Bool()
    val last = Bool()
    val data = SInt(32.W)
  }))

  val sink = IO(Sink(new Bundle {
    val result = SInt(32.W)
  }))

  private val wire0 = Wire(Interface(SInt(64.W)))

  private val fold0 = new Fold(
    source,
    elastic.Const(0.S),
    wire0
  ) {
    operand { (in) => in.data }
    zero { (in) => in.zero }
    last { (in) => in.last }

    // new elastic.Join(elastic.SinkBuffer(sourceResult)) {
    //   out := join(sinkOpA) + join(sinkOpB)
    // }

    dontTouch(sinkA)
    dontTouch(sinkB)

    new elastic.Join((sourceResult)) {
      out := join(sinkA) + join(sinkB)
    }

    val count = RegInit(0.U(32.W))
    fire {
      printf("reduction complete. count = %d\n", count)
      count := count + 1.U
    }
  }

  private val transform0 = new Transform(wire0, sink) {
    out.result := in
  }

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(source, "Task")
  declareElasticInterface(sink, "Result")
}

object Fold_Tb extends chext.TestBench {
  emit(new Fold_Tbtop)
}
