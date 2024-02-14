package chext.ip.memory

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import chiseltest._

import TestOps._
import chext.test.Expect

class SinglePortRAMSpec extends chext.test.FreeSpec with chext.test.TestMixin {
  Target.setCurrent(chisel.Target)

  useVerilator()
  enableVcd()

  val genWriteRequest = new WriteRequest(32, 32)
  def writeRequest(addr: BigInt, data: BigInt) =
    genWriteRequest.Lit(_.addr -> addr.U, _.data -> data.U, _.wstrb -> 15.U)

  "chext.ip.memory.SinglePortRAMSpec.Basic1" in
    test(new SinglePortRAM(32, 10, 4, 4, 8, 8)) { dut =>
      {
        val testSize = 128;
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
              stepRandom(16)
            }
          }

        }.fork {
          data.foreach { (x) => dut.write.receiveResp() }
        }.join()

        fork {
          addr.foreach { (a) =>
            {
              dut.read.sendReq(a)
              stepRandom(16)
            }
          }
        }.fork {
          data.foreach((d) => {
            dut.read.expectResp(d)
          })
        }.join()
      }
    }

  "chext.ip.memory.SinglePortRAMSpec.Basic2" in
    test(new SinglePortRAM(32, 10, 4, 4, 8, 8)) { dut =>
      {
        val testSize = 128;
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
            stepRandom(16)
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
            stepRandom(16)
          })
        }.join()
      }
    }

  "chext.ip.memory.SinglePortRAMSpec.LongLatency1" in
    test(new SinglePortRAM(32, 10, 32, 32, 4, 4)) { dut =>
      {
        val testSize = 128;
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
          })
        }.join()
      }
    }

  "chext.ip.memory.SinglePortRAMSpec.LongLatency2" in
    test(new SinglePortRAM(32, 10, 16, 8, 1, 1)) { dut =>
      {
        val testSize = 128;
        val addr = Seq.range(0, testSize)
        val data = addr.map { (x) => ((x + 0xa0) + ((x + 0xb0) << 16)) }

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
            {
              dut.write.receiveResp()
            }
          }
        }.join()

        fork {
          addr.foreach { (a) =>
            {
              dut.read.sendReq(a)
            }
          }
        }.fork {
          data.foreach { (d) =>
            {
              dut.read.expectResp(d)
            }
          }
        }.join()
      }
    }

  "chext.ip.memory.SinglePortRAMSpec.Interleaved" in
    test(new SinglePortRAM(32, 10, 1, 1, 8, 8)) { dut =>
      {
        val testSize = 128;
        val addr = Seq.range(0, testSize)
        val data = addr.map { (x) => ((x + 0xa0) + ((x + 0xb0) << 16)) }

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
          data.foreach { (d) =>
            {
              dut.write.receiveResp()
            }
          }
        }.fork {
          addr.zip(data).foreach {
            case (a, d) => {
              dut.read.sendReq(a)
            }
          }
        }.fork {
          data.foreach { (d) =>
            {
              dut.read.expectResp(d)
            }
          }
        }.join()
      }
    }

}

class SimpleDualPortRAMSpec
    extends chext.test.FreeSpec
    with chext.test.TestMixin {
  Target.setCurrent(chisel.Target)

  useVerilator()
  enableVcd()

  val genWriteRequest = new WriteRequest(32, 32)
  def writeRequest(addr: BigInt, data: BigInt) =
    genWriteRequest.Lit(_.addr -> addr.U, _.data -> data.U, _.wstrb -> 15.U)

  "chext.ip.memory.SimpleDualPortRAMSpec.Basic1" in
    test(new SimpleDualPortRAM(32, 10, 4, 4, 8, 8)) { dut =>
      {
        val testSize = 128;
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
              stepRandom(16)
            }
          }

        }.fork {
          data.foreach { (x) => dut.write.receiveResp() }
        }.join()

        fork {
          addr.foreach { (a) =>
            {
              dut.read.sendReq(a)
              stepRandom(16)
            }
          }
        }.fork {
          data.foreach((d) => {
            dut.read.expectResp(d)
          })
        }.join()
      }
    }

  "chext.ip.memory.SimpleDualPortRAMSpec.Basic2" in
    test(new SimpleDualPortRAM(32, 10, 4, 4, 8, 8)) { dut =>
      {
        val testSize = 128;
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
            stepRandom(16)
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
            stepRandom(16)
          })
        }.join()
      }
    }

  "chext.ip.memory.SimpleDualPortRAMSpec.LongLatency1" in
    test(new SimpleDualPortRAM(32, 10, 32, 32, 4, 4)) { dut =>
      {
        val testSize = 128;
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
          })
        }.join()
      }
    }

  "chext.ip.memory.SimpleDualPortRAMSpec.LongLatency2" in
    test(new SimpleDualPortRAM(32, 10, 16, 8, 1, 1)) { dut =>
      {
        val testSize = 128;
        val addr = Seq.range(0, testSize)
        val data = addr.map { (x) => ((x + 0xa0) + ((x + 0xb0) << 16)) }

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
            {
              dut.write.receiveResp()
            }
          }
        }.join()

        fork {
          addr.foreach { (a) =>
            {
              dut.read.sendReq(a)
            }
          }
        }.fork {
          data.foreach { (d) =>
            {
              dut.read.expectResp(d)
            }
          }
        }.join()
      }
    }

  "chext.ip.memory.SimpleDualPortRAMSpec.Interleaved" in
    test(new SimpleDualPortRAM(32, 10, 1, 1, 8, 8)) { dut =>
      {
        val testSize = 128;
        val addr = Seq.range(0, testSize)
        val data = addr.map { (x) => ((x + 0xa0) + ((x + 0xb0) << 16)) }

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
          data.foreach { (d) =>
            {
              dut.write.receiveResp()
            }
          }
        }.fork {
          addr.zip(data).foreach {
            case (a, d) => {
              dut.read.sendReq(a)
            }
          }
        }.fork {
          data.foreach { (d) =>
            {
              dut.read.expectResp(d)
            }
          }
        }.join()
      }
    }

}
