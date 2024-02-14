package chext.ip.memory

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import chiseltest._

import TestOps._
import chext.test.Expect

class SinglePortRAMSpec extends chext.test.FreeSpec {
  Target.setCurrent(chisel.Target)

  val rand = scala.util.Random
  val genWriteRequest = new WriteRequest(32, 32)
  def writeRequest(addr: BigInt, data: BigInt) =
    genWriteRequest.Lit(_.addr -> addr.U, _.data -> data.U, _.wstrb -> 15.U)

  "chext.ip.memory.SinglePortRAMSpec.Basic1" in {
    test(
      new SinglePortRAM(32, 10, 4, 4, 8, 8)
    ).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) {
      dut =>
        {
          val testSize = 100;
          val addr = Seq.range(0, testSize)
          val data = addr.map { (x) => (x + 0xa0) + ((x + 0xb0) << 16) }

          dut.write.req.initSource()
          dut.write.resp.initSink()
          dut.read.req.initSource()
          dut.read.resp.initSink()

          fork {
            addr.zip(data).foreach {
              case (a, d) => {
                dut.write.sendReq(a, d, 0xf)
                dut.clock.step(1 + rand.nextInt(16))
              }
            }

          }.fork {
            data.foreach { (x) => dut.write.receiveResp() }
          }.join()

          fork {
            addr.foreach { (a) =>
              {
                dut.read.sendReq(a)
                dut.clock.step(1 + rand.nextInt(16))
              }
            }
          }.fork {
            data.foreach((d) => {
              dut.read.expectResp(d)
            })
          }.join()
        }
    }
  }

  "chext.ip.memory.SinglePortRAMSpec.Basic2" in {
    test(
      new SimpleDualPortRAM(32, 10, 4, 4, 8, 8)
    ).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) {
      dut =>
        {
          val testSize = 100;
          val addr = Seq.range(0, testSize)
          val data = addr.map { (x) => (x + 0xa0) + ((x + 0xb0) << 16) }

          dut.write.req.initSource()
          dut.write.resp.initSink()
          dut.read.req.initSource()
          dut.read.resp.initSink()

          fork {
            addr.zip(data).foreach {
              case (a, d) => {
                dut.write.sendReq(a, d, 0xf)
              }
            }

          }.fork {
            data.foreach { (x) =>
              dut.write.receiveResp()
              dut.clock.step(1 + rand.nextInt(16))
            }
          }.join()

          fork {
            addr.foreach { (a) =>
              {
                dut.read.sendReq(a)
              }
            }
          }.fork {
            data.foreach((d) => {
              dut.read.expectResp(d)
              dut.clock.step(1 + rand.nextInt(16))
            })
          }.join()
        }
    }
  }

  "chext.ip.memory.SinglePortRAMSpec.LongLatency" in {
    test(
      new SimpleDualPortRAM(32, 10, 32, 32, 4, 4)
    ).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) {
      dut =>
        {
          val TEST_SIZE = 100;
          val indices = Seq.range(0, TEST_SIZE).map(_.U)
          val data = indices.map(x => (x.litValue + 0xa0).U)
          val writeData =
            indices
              .zip(data)
              .map(x => writeRequest(x._1.litValue, x._2.litValue))

          dut.write.req.initSource()
          dut.write.resp.initSink()
          dut.read.req.initSource()
          dut.read.resp.initSink()

          fork {
            writeData.foreach { (x) =>
              {
                dut.write.req.enqueue(x)
              }
            }

          }.fork {
            for (i <- 0 until TEST_SIZE) {
              dut.write.resp.expectDequeue(0.U)
            }
          }.join()

          fork {
            indices.foreach { (x) =>
              {
                dut.read.req.enqueue(x)
              }
            }
          }.fork {
            data.foreach(x => {
              dut.read.resp.expectDequeue(x)
            })
          }.join()
        }
    }
  }

  "chext.ip.memory.SinglePortRAMSpec.InterleavedReadsWrites" in {
    test(
      new SimpleDualPortRAM(32, 10, 16, 8, 1, 1)
    ).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) {
      dut =>
        {
          val TEST_SIZE = 100;
          val indices = Seq.range(0, TEST_SIZE).map(_.U)
          val data = indices.map(x => (x.litValue + 0xa0).U)
          val writeData =
            indices
              .zip(data)
              .map(x => writeRequest(x._1.litValue, x._2.litValue))

          dut.write.req.initSource()
          dut.write.resp.initSink()
          dut.read.req.initSource()
          dut.read.resp.initSink()

          fork {
            writeData.foreach { (x) =>
              {
                dut.write.req.enqueue(x)
              }
            }

          }.fork {
            for (i <- 0 until TEST_SIZE) {
              dut.write.resp.expectDequeue(0.U)
            }
          }.join()

          fork {
            indices.foreach { (x) =>
              {
                dut.read.req.enqueue(x)
              }
            }
          }.fork {
            data.foreach(x => {
              dut.read.resp.expectDequeue(x)
            })
          }.join()
        }
    }
  }
}
class SimpleDualPortRAMSpec extends chext.test.FreeSpec {
  Target.setCurrent(chisel.Target)

  val rand = scala.util.Random
  val genWriteRequest = new WriteRequest(32, 32)
  def writeRequest(addr: BigInt, data: BigInt) =
    genWriteRequest.Lit(_.addr -> addr.U, _.data -> data.U, _.wstrb -> 15.U)

  "chext.ip.memory.SimpleDualPortRAMSpec.Basic1" in {
    test(
      new SimpleDualPortRAM(32, 10, 4, 4, 8, 8)
    ).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) {
      dut =>
        {
          val TEST_SIZE = 100;
          val indices = Seq.range(0, TEST_SIZE).map(_.U)
          val data = indices.map(x => (x.litValue + 0xa0).U)
          val writeData =
            indices
              .zip(data)
              .map(x => writeRequest(x._1.litValue, x._2.litValue))

          dut.write.req.initSource()
          dut.write.resp.initSink()
          dut.read.req.initSource()
          dut.read.resp.initSink()

          fork {
            writeData.foreach { (x) =>
              {
                dut.write.req.enqueue(x)
                dut.clock.step(1 + rand.nextInt(16))
              }
            }

          }.fork {
            for (i <- 0 until TEST_SIZE) {
              dut.write.resp.expectDequeue(0.U)
            }
          }.join()

          fork {
            indices.foreach { (x) =>
              {
                dut.read.req.enqueue(x)
                dut.clock.step(1 + rand.nextInt(16))
              }
            }
          }.fork {
            data.foreach(x => {
              dut.read.resp.expectDequeue(x)
            })
          }.join()
        }
    }
  }

  "chext.ip.memory.SimpleDualPortRAMSpec.Basic2" in {
    test(
      new SimpleDualPortRAM(32, 10, 4, 4, 8, 8)
    ).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) {
      dut =>
        {
          val TEST_SIZE = 100;
          val indices = Seq.range(0, TEST_SIZE).map(_.U)
          val data = indices.map(x => (x.litValue + 0xa0).U)
          val writeData =
            indices
              .zip(data)
              .map(x => writeRequest(x._1.litValue, x._2.litValue))

          dut.write.req.initSource()
          dut.write.resp.initSink()
          dut.read.req.initSource()
          dut.read.resp.initSink()

          fork {
            writeData.foreach { (x) =>
              {
                dut.write.req.enqueue(x)
              }
            }

          }.fork {
            for (i <- 0 until TEST_SIZE) {
              dut.write.resp.expectDequeue(0.U)
              dut.clock.step(1 + rand.nextInt(16))
            }
          }.join()

          fork {
            indices.foreach { (x) =>
              {
                dut.read.req.enqueue(x)
              }
            }
          }.fork {
            data.foreach(x => {
              dut.read.resp.expectDequeue(x)
              dut.clock.step(1 + rand.nextInt(16))
            })
          }.join()
        }
    }
  }

  "chext.ip.memory.SimpleDualPortRAMSpec.LongLatency" in {
    test(
      new SimpleDualPortRAM(32, 10, 32, 32, 4, 4)
    ).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) {
      dut =>
        {
          val TEST_SIZE = 100;
          val indices = Seq.range(0, TEST_SIZE).map(_.U)
          val data = indices.map(x => (x.litValue + 0xa0).U)
          val writeData =
            indices
              .zip(data)
              .map(x => writeRequest(x._1.litValue, x._2.litValue))

          dut.write.req.initSource()
          dut.write.resp.initSink()
          dut.read.req.initSource()
          dut.read.resp.initSink()

          fork {
            writeData.foreach { (x) =>
              {
                dut.write.req.enqueue(x)
              }
            }

          }.fork {
            for (i <- 0 until TEST_SIZE) {
              dut.write.resp.expectDequeue(0.U)
            }
          }.join()

          fork {
            indices.foreach { (x) =>
              {
                dut.read.req.enqueue(x)
              }
            }
          }.fork {
            data.foreach(x => {
              dut.read.resp.expectDequeue(x)
            })
          }.join()
        }
    }
  }
}
