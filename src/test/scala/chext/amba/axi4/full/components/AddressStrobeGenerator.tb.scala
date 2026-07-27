package chext.amba.axi4.full.components

import chisel3._

import chext.elastic
import elastic.ConnectOp._

class AddressStrobeGenerator_Tbtop(
    val wAddr: Int,
    val wData: Int,
    override val desiredName: String
) extends Module
    with chext.AnnotatedModule {

  private val dut = Module(new AddressStrobeGenerator(wAddr, wData))

  val source = IO(elastic.Source.like(dut.source))
  val sink = IO(elastic.Sink.like(dut.sink))

  source :=> dut.source
  dut.sink :=> sink

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(source, "Task")
  declareElasticInterface(sink, "Result")
}

object AddressStrobeGenerator_Tb extends App with chext.TestBench {
  emit(new AddressStrobeGenerator_Tbtop(32, 128, "AddressStrobeGenerator_Tbtop_1"))
}
