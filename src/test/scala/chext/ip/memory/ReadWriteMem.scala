package chext.ip.memory

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import chiseltest._

import org.scalatest.freespec.AnyFreeSpec

class ReadWriteMemSpec extends AnyFreeSpec with ChiselScalatestTester {
  Target.setCurrent(chisel.Target)

  val rand = scala.util.Random
  val genWrBundle = new WrBundle(32, 32)
  def writeBundle(addr: BigInt, data: BigInt) =
    genWrBundle.Lit(_.addr -> addr.U, _.data -> data.U, _.writeStrobe -> 15.U)

  "chext.ip.memory.ReadWriteMem.Basic" in {
    test(
      new ReadWriteMem(ReadWriteMemConfig(32, 10, 1, 1, 8, 8))
    ).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
      {
        val TEST_SIZE = 100;
        val indices = Seq.range(0, TEST_SIZE).map(_.U)
        val data = indices.map(x => (x.litValue + 0xa0).U)
        val writeData =
          indices.zip(data).map(x => writeBundle(x._1.litValue, x._2.litValue))

        dut.write.payload.initSource()
        dut.write.response.initSink()
        dut.read.addr.initSource()
        dut.read.data.initSink()

        fork {
          dut.write.payload.enqueueSeq(writeData)
        }.fork {
          for (i <- 0 until TEST_SIZE) {
            dut.write.response.expectDequeue(0.U)
            dut.clock.step(1 + rand.nextInt(16))
          }
        }.join()

        fork {
          dut.read.addr.enqueueSeq(indices)
        }.fork {
          data.foreach(x => {
            dut.read.data.expectDequeue(x)
            dut.clock.step(1 + rand.nextInt(16))
          })
        }.join()
      }
    }
  }
}
