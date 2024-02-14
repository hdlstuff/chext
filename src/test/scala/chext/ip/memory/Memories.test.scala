package chext.ip.memory

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import chiseltest._

import org.scalatest.freespec.AnyFreeSpec

class SimpleDualPortRAMSpec extends AnyFreeSpec with ChiselScalatestTester {
  Target.setCurrent(chisel.Target)

  val rand = scala.util.Random
  val genWriteRequest = new WriteRequest(32, 32)
  def writeRequest(addr: BigInt, data: BigInt) =
    genWriteRequest.Lit(_.addr -> addr.U, _.data -> data.U, _.wstrb -> 15.U)

  "chext.ip.memory.ReadWriteMem.Basic1" in {
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

  "chext.ip.memory.ReadWriteMem.Basic2" in {
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

  "chext.ip.memory.ReadWriteMem.LongLatency" in {
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
