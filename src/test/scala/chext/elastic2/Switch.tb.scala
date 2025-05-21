package chext.elastic2

import chisel3._

class Switch_Tbtop1 extends Module with chext.TestBenchTop {
  class Task extends Bundle {
    val target = UInt(2.W)
    val operand = UInt(32.W)
    val delay = UInt(8.W)
  }

  val sourceTask = IO(Source(new Task))
  val sinkResult = IO(Sink(UInt(32.W)))

  // 0 -> pass through
  // 1 -> multiply by two
  // 2 -> multiply by two and delay
  // 3 -> multiply by four

  new Switch(sourceTask, sinkResult, 16) {
    new Case(in.target === 0.U) {
      new Transform(source, SinkBuffer(sink)) {
        out := in.operand
      }
    }

    new Case(in.target === 1.U) {
      new Transform(source, SinkBuffer(sink)) {
        out := in.operand * 2.U
      }
    }

    new Case(in.target === 2.U) {
      new Arrival(source, sink) {
        val processed = RegInit(false.B)
        val leftCycles = RegInit(0.U(8.W))

        when(arrived) {
          out := in.operand * 2.U

          when(processed) {
            when(leftCycles === 0.U) {
              accept()
              processed := false.B
            }.otherwise {
              noAccept()
              leftCycles := leftCycles - 1.U
            }
          }.otherwise {
            leftCycles := in.delay
            processed := true.B
          }
        }
      }
    }

    new Case(in.target === 3.U) {
      new Transform(source, sink) {
        out := in.operand * 4.U
      }
    }

    done()
  }

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(sourceTask, "Task")
  declareElasticInterface(sinkResult, "Result")
}

object Switch_Tb extends chext.TestBench {
  emit(new Switch_Tbtop1)
}
