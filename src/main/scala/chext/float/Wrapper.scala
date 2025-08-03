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
  require(moduleDelay >= 0)
  require(queueLength >= 1)

  val source = IO(elastic.Source(genInput))
  val sink = IO(elastic.Sink(genOutput))

  val moduleIn = IO(Output(genInput))
  val moduleOut = IO(Input(genOutput))

  if (moduleDelay == 0) {
    // this is a combinational module
    moduleIn := source.bits
    sink.bits := moduleOut

    source.ready := sink.ready
    sink.valid := source.valid

    source.markSource()
    sink.markSink()

  } else {
    source.markSource()

    val ctr = Module(new chext.util.Counter(queueLength + 1))
    ctr.noInc()
    ctr.noDec()

    val qOutput = elastic.Queue(genOutput, queueLength)

    qOutput.source.noenq()
    source.nodeq()

    qOutput.source.markSink()

    moduleIn := source.bits
    qOutput.source.bits := moduleOut

    source.ready := ctr.notFull && source.valid

    when(source.fire) {
      ctr.inc()
    }

    qOutput.source.valid := ShiftRegister(source.fire, moduleDelay)
    qOutput.sink :=> sink

    when(qOutput.sink.fire) {
      ctr.dec()
    }
  }
}
