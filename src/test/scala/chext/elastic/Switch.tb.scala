package chext.elastic

import chisel3._
import chisel3.util._

import chext.elastic

class Switch_Tbtop extends Module with chext.AnnotatedModule {
  val source = IO(elastic.Source(new Bundle {
    val tokenType = UInt(2.W)
    val data = UInt(32.W)
    val repeat = UInt(32.W)
  }))

  val sink = IO(elastic.Sink(new Bundle {
    val data = UInt(32.W)
  }))

  private val wire0 = EWire(new Bundle {
    val data = UInt(32.W)
    val last = Bool()
  })

  private val switch0 = new Switch(source, wire0) {
    last { _.last }

    namedBranch("x0") { _.tokenType === 0.U } { //
      (source, sink) =>
        val transform0 = new elastic.Transform(source, sink) {
          out.data := 0x0f0f0f0f.U
          out.last := true.B
        }

    }

    namedBranch("x1") { _.tokenType === 1.U } { //
      (source, sink) =>
        val transform0 = new elastic.Transform(source, sink) {
          out.data := in.data
          out.last := true.B
        }
    }

    namedBranch("x2") { _.tokenType === 2.U } { //
      (source, sink) =>
        val repeat0 = new elastic.Repeat(source, sink, 32) {
          len { in => in.repeat +& 1.U }

          outExplicit {
            case (in, _, _, last, out) => {

              out.data := in.data
              out.last := last
            }
          }
        }
    }
  }

  val transform0 = new elastic.Transform(wire0, sink) {
    out.data := in.data
  }

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(source, "Task")
  declareElasticInterface(sink, "Result")
}

object Switch_Tb extends chext.TestBench {
  emit(new Switch_Tbtop)
}
