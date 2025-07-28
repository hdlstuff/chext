package chext.elastic

import chisel3._

class Transducer_Tbtop extends Module with chext.TestBenchTop {
  val source = IO(Source(new Bundle {
    val skip = UInt(8.W)
    val data = UInt(32.W)
    val bad = Bool()
    val stallForever = Bool()
  }))
  val sink = IO(Sink(UInt(32.W)))

  private val source_ = EWire.like(source)
  private val sink_ = EWire.like(sink)

  val stall0 = new RandomStall(source, source_)
  val stall1 = new RandomStall(sink_, sink)

  new Transducer(source_, sink_) {
    val state = RegInit(0.U(8.W))

    packet {
      out := in.data

      when(in.stallForever) {
        // do nothing
      }.otherwise {
        when(state === 0.U) {
          accept { state := in.skip }

          // the following is not allowed
          when(in.bad) {
            consume { state := 0.U }
          }
        }.otherwise {
          consume { state := state - 1.U }

          // the following is not allowed
          when(in.bad) {
            consume { state := 0.U }
          }
        }
      }
    }
  }

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(source, "In")
  declareElasticInterface(sink, "Out")
}

object Transducer_Tb extends chext.TestBench {
  emit(new Transducer_Tbtop)
}
