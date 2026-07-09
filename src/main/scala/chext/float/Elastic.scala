package chext.float

import chisel3._
import chisel3.util._

import chext.elastic
import elastic.ConnectOp._

import chext.bundles.Bundle2

class ElasticAdd(genFp: FloatingPoint, val combinational: Boolean = false)
    extends Module
    with chext.AnnotatedModule {
  private val require_ = chext.util.Require.inferred()

  val sourceInA = IO(elastic.Source(genFp))
  val sourceInB = IO(elastic.Source(genFp))
  val sinkOut = IO(elastic.Sink(genFp))

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(sourceInA, "Fp")
  declareElasticInterface(sourceInB, "Fp")
  declareElasticInterface(sinkOut, "Fp")

  private val add = Module(new OpAdd(genFp, combinational))

  // HARDCODED: 8, the queue length
  private val wrapper = Module(new Wrapper(new Bundle2(genFp, genFp), genFp, add.delay, 8))
  require_(add.delay < 8)

  private val join0 = new elastic.Join(wrapper.source) {
    out._1 := join(sourceInA)
    out._2 := join(sourceInB)
  }

  wrapper.sink :=> sinkOut

  add.inA := wrapper.moduleIn._1
  add.inB := wrapper.moduleIn._2
  wrapper.moduleOut := add.out
}

class ElasticMultiply(genFp: FloatingPoint, val combinational: Boolean = false)
    extends Module
    with chext.AnnotatedModule {
  val sourceInA = IO(elastic.Source(genFp))
  val sourceInB = IO(elastic.Source(genFp))
  val sinkOut = IO(elastic.Sink(genFp))

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(sourceInA, "Fp")
  declareElasticInterface(sourceInB, "Fp")
  declareElasticInterface(sinkOut, "Fp")

  private val multiply = Module(new OpMultiply(genFp, combinational))

  // HARDCODED: 8, the queue length
  private val wrapper = Module(new Wrapper(new Bundle2(genFp, genFp), genFp, multiply.delay, 8))

  private val join0 = new elastic.Join(wrapper.source) {
    out._1 := join(sourceInA)
    out._2 := join(sourceInB)
  }

  wrapper.sink :=> sinkOut

  multiply.inA := wrapper.moduleIn._1
  multiply.inB := wrapper.moduleIn._2
  wrapper.moduleOut := multiply.out
}
