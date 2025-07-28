package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.elastic
import elastic.ConnectOp._

class AddressGenerator_Tbtop(val wAddr: Int, override val desiredName: String)
    extends Module
    with chext.TestBenchTop {

  private val dut = Module(new AddressGenerator(wAddr))

  val source = IO(elastic.Source.like(dut.source))
  val sink = IO(elastic.Sink.like(dut.sink))

  source :=> dut.source
  dut.sink :=> sink

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(source, "Task")
  declareElasticInterface(sink, "Result")
}

object AddressGenerator_Tb extends App with chext.TestBench {
  emit(new AddressGenerator_Tbtop(32, "AddressGenerator_Tbtop_1"))
}
