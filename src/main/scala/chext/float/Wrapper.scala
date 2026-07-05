package chext.float

import chisel3._
import chisel3.util._
import chisel3.experimental.AffectsChiselPrefix

import chext.elastic
import elastic.ConnectOp._

class Wrapper[
    InputType <: Data,
    OutputType <: Data,
    ModuleType <: Module
](
    genInput: InputType,
    genOutput: OutputType,
    val moduleDelay: Int,
    val queueLength: Int = 2
) extends Module {
  private val require_ = chext.util.Require.inferred()

  require_(moduleDelay >= 0)
  require_(queueLength >= 1)

  val source = IO(elastic.Source(genInput))
  val sink = IO(elastic.Sink(genOutput))

  val moduleIn = IO(Output(genInput))
  val moduleOut = IO(Input(genOutput))

  if (moduleDelay == 0) {
    // this is a combinational module
    moduleIn := source.$bits
    sink.$bits := moduleOut

    source.$ready := sink.$ready
    sink.$valid := source.$valid

    source.markSource()
    sink.markSink()

  } else {
    source.markSource()

    val ctr = new chext.util.Counter(queueLength + 1)
    ctr.noInc()
    ctr.noDec()

    val queueOutput = elastic.Queue(genOutput, queueLength)

    queueOutput.source.noenq()
    source.nodeq()

    queueOutput.source.markSink()

    moduleIn := source.$bits
    queueOutput.source.$bits := moduleOut

    source.$ready := ctr.notFull && source.$valid

    when(source.fire) {
      ctr.inc()
    }

    queueOutput.source.$valid := ShiftRegister(source.fire, moduleDelay)
    queueOutput.sink :=> sink

    when(queueOutput.sink.fire) {
      ctr.dec()
    }
  }
}
