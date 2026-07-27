package chext.float

import chisel3._

import chext.elastic
import elastic.ConnectOp._

class Elastic_Tbtop extends Module with chext.AnnotatedModule {
  val genFp32 = FloatingPoint.ieeeFp32
  val genFp64 = FloatingPoint.ieeeFp64

  val fp32_inA = IO(elastic.Source(UInt(32.W)))
  val fp32_inB = IO(elastic.Source(UInt(32.W)))
  val fp32_addOut = IO(elastic.Sink(UInt(32.W)))
  val fp32_multiplyOut = IO(elastic.Sink(UInt(32.W)))

  val fp64_inA = IO(elastic.Source(UInt(64.W)))
  val fp64_inB = IO(elastic.Source(UInt(64.W)))
  val fp64_addOut = IO(elastic.Sink(UInt(64.W)))
  val fp64_multiplyOut = IO(elastic.Sink(UInt(64.W)))

  {
    val fp32_add = Module(new ElasticAdd(genFp32))
    val fp64_add = Module(new ElasticAdd(genFp64))

    val fp32_multiply = Module(new ElasticMultiply(genFp32))
    val fp64_multiply = Module(new ElasticMultiply(genFp64))

    val transform0 = new elastic.Transform(fp32_add.sinkOut, elastic.SinkBuffer(fp32_addOut, 32)) {
      out := in.asUInt
    }

    val transform1 = new elastic.Transform(fp64_add.sinkOut, elastic.SinkBuffer(fp64_addOut, 32)) {
      out := in.asUInt
    }

    val transform2 =
      new elastic.Transform(fp32_multiply.sinkOut, elastic.SinkBuffer(fp32_multiplyOut, 32)) {
        out := in.asUInt
      }

    val transform3 =
      new elastic.Transform(fp64_multiply.sinkOut, elastic.SinkBuffer(fp64_multiplyOut, 32)) {
        out := in.asUInt
      }

    val fork0 = new elastic.Fork(elastic.SourceBuffer(fp32_inA, 32)) {
      fork { in.asTypeOf(genFp32) } :=> fp32_add.sourceInA
      fork { in.asTypeOf(genFp32) } :=> fp32_multiply.sourceInA
    }

    val fork1 = new elastic.Fork(elastic.SourceBuffer(fp32_inB, 32)) {
      fork { in.asTypeOf(genFp32) } :=> fp32_add.sourceInB
      fork { in.asTypeOf(genFp32) } :=> fp32_multiply.sourceInB
    }

    val fork2 = new elastic.Fork(elastic.SourceBuffer(fp64_inA, 32)) {
      fork { in.asTypeOf(genFp64) } :=> fp64_add.sourceInA
      fork { in.asTypeOf(genFp64) } :=> fp64_multiply.sourceInA
    }

    val fork3 = new elastic.Fork(elastic.SourceBuffer(fp64_inB, 32)) {
      fork { in.asTypeOf(genFp64) } :=> fp64_add.sourceInB
      fork { in.asTypeOf(genFp64) } :=> fp64_multiply.sourceInB
    }

  }

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(fp32_inA, "Fp32")
  declareElasticInterface(fp32_inB, "Fp32")
  declareElasticInterface(fp32_addOut, "Fp32")
  declareElasticInterface(fp32_multiplyOut, "Fp32")
  declareElasticInterface(fp64_inA, "Fp64")
  declareElasticInterface(fp64_inB, "Fp64")
  declareElasticInterface(fp64_addOut, "Fp64")
  declareElasticInterface(fp64_multiplyOut, "Fp64")
}

object Elastic_Tb extends chext.TestBench {
  emit(new Elastic_Tbtop)
}
