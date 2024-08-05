package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chiseltest._

import chext.amba.axi4

import axi4.Ops._
import axi4.full.test.PacketUtils._
import chext.test.{FreeSpec, TestMixin}

class BurstSplitterSpec extends FreeSpec with TestMixin {

  useVerilator()
  enableVcd()

  "chext.amba.axi4.full.component.BurstSplitterZeroId.BasicR1" in test(
    new BurstSplitterZeroId(axiCfg = axi4.Config(), 2)
  ) { dut =>
    {
      import axi4.full.test._

      val s_axi = dut.s_axi
      s_axi.initSlave()

      val m_axi = dut.m_axi
      m_axi.initMaster()

      fork {
        s_axi.sendReadAddress(AddressPacket(0, 0x0000, 3, 0, 1))
      }.fork {
        val rd = s_axi.receiveReadDataBurst()
        assert(rd(0) == ReadDataPacket(0, 0x00a0, false))
        assert(rd(1) == ReadDataPacket(0, 0x00a1, false))
        assert(rd(2) == ReadDataPacket(0, 0x00a2, false))
        assert(rd(3) == ReadDataPacket(0, 0x00a3, true))
      }.fork {
        assert(m_axi.receiveReadAddress() == AddressPacket(0, 0x0000, 0, 0, 0))
        assert(m_axi.receiveReadAddress() == AddressPacket(0, 0x0001, 0, 0, 0))
        assert(m_axi.receiveReadAddress() == AddressPacket(0, 0x0002, 0, 0, 0))
        assert(m_axi.receiveReadAddress() == AddressPacket(0, 0x0003, 0, 0, 0))
      }.fork {
        m_axi.sendReadData(0, 0x00a0)
        m_axi.sendReadData(0, 0x00a1)
        m_axi.sendReadData(0, 0x00a2)
        m_axi.sendReadData(0, 0x00a3)
      }.join()
    }
  }

  "chext.amba.axi4.full.component.BurstSplitterZeroId.BasicW1" in test(
    new BurstSplitterZeroId(axiCfg = axi4.Config(), 2)
  ) { dut =>
    {
      import axi4.full.test._

      val s_axi = dut.s_axi
      s_axi.initSlave()

      val m_axi = dut.m_axi
      m_axi.initMaster()

      fork {
        s_axi.sendWriteAddress(AddressPacket(0, 0x0000, 3, 0, 1))
      }.fork {
        s_axi.sendWriteData(WriteDataPacket(0xa0, 0xf, false))
        s_axi.sendWriteData(WriteDataPacket(0xa1, 0xf, false))
        s_axi.sendWriteData(WriteDataPacket(0xa2, 0xf, false))
        s_axi.sendWriteData(WriteDataPacket(0xa3, 0xf, true))
      }.fork {
        s_axi.receiveWriteResponse()
      }.fork {
        assert(m_axi.receiveWriteAddress() == AddressPacket(0, 0x0000, 0, 0, 0))
        assert(m_axi.receiveWriteAddress() == AddressPacket(0, 0x0001, 0, 0, 0))
        assert(m_axi.receiveWriteAddress() == AddressPacket(0, 0x0002, 0, 0, 0))
        assert(m_axi.receiveWriteAddress() == AddressPacket(0, 0x0003, 0, 0, 0))
      }.fork {
        assert(m_axi.receiveWriteData() == WriteDataPacket(0xa0, 0xf, true))
        assert(m_axi.receiveWriteData() == WriteDataPacket(0xa1, 0xf, true))
        assert(m_axi.receiveWriteData() == WriteDataPacket(0xa2, 0xf, true))
        assert(m_axi.receiveWriteData() == WriteDataPacket(0xa3, 0xf, true))

        m_axi.sendWriteResponse(0)
        m_axi.sendWriteResponse(0)
        m_axi.sendWriteResponse(0)
        m_axi.sendWriteResponse(0)
      }.join()
    }
  }

  "chext.amba.axi4.full.component.BurstSplitterZeroId.FullR1" in test(
    new BurstSplitterZeroId(axiCfg = axi4.Config(axi3Compat = false), 2)
  ) { dut =>
    {
      import axi4.full.test._
      val rnd = new scala.util.Random

      val TEST_SIZE = 100
      val LOG = false

      val bursts = Array.fill(TEST_SIZE)(rnd.nextInt(256))
      val ars = Array.fill(TEST_SIZE)(rnd.nextInt(0x10000))
      val rs = bursts.map(n => Array.fill(n + 1)(rnd.nextInt(0x10000)))

      val s_axi = dut.s_axi
      s_axi.initSlave()

      val m_axi = dut.m_axi
      m_axi.initMaster()

      fork {
        ars.zip(bursts).foreach { case (ar, n) =>
          val p = AddressPacket(0, ar, n, 0, 1)
          if (LOG) println(f"Send AR: ${p}")
          s_axi.sendReadAddress(p)
        }
      }.fork {
        rs.foreach(pkt => {
          val p = s_axi.receiveReadDataBurst()
          if (LOG) println(f"Recv R: ${p}")
          p.zip(pkt).foreach(x => assert(x._1.data == x._2))
        })
      }.fork {
        ars.zip(bursts).foreach { case (ar, n) =>
          for (i <- 0 to n) {
            val p = m_axi.receiveReadAddress()
            if (LOG) println(f"Recv AR: ${p}")
            assert(p == AddressPacket(0, ar + i, 0, 0, 0))
          }
        }
      }.fork {
        rs.foreach(_.foreach(r => {
          if (LOG) println(f"Send R: ${r}")
          m_axi.sendReadData(0, r)
        }))
      }.join()
    }
  }

  "chext.amba.axi4.full.component.BurstSplitterZeroId.FullW1" in test(
    new BurstSplitterZeroId(axiCfg = axi4.Config(axi3Compat = false), 2)
  ) { dut =>
    {
      import axi4.full.test._
      val rnd = new scala.util.Random

      val TEST_SIZE = 100
      val LOG = false

      val bursts = Array.fill(TEST_SIZE)(rnd.nextInt(256))
      val aws = Array.fill(TEST_SIZE)(rnd.nextInt(0x10000))
      val ws = bursts.map(n => Array.fill(n + 1)(rnd.nextInt(0x10000)))

      val s_axi = dut.s_axi
      s_axi.initSlave()

      val m_axi = dut.m_axi
      m_axi.initMaster()

      fork {
        aws.zip(bursts).foreach { case (aw, n) =>
          val p = AddressPacket(0, aw, n, 0, 1)
          if (LOG) println(f"Send AW: ${p}")
          s_axi.sendWriteAddress(p)
        }
      }.fork {
        ws.foreach(pkt =>
          pkt.zipWithIndex.foreach {
            case (x, idx) => {
              val p = WriteDataPacket(x, 0xf, idx == pkt.length - 1)
              if (LOG) println(f"Send W: ${p}")
              s_axi.sendWriteData(p)
            }
          }
        )
      }.fork {
        bursts.foreach(_ => {
          s_axi.receiveWriteResponse()
          if (LOG) println("Recv B")
        })
      }.fork {
        aws.zip(bursts).foreach { case (aw, n) =>
          for (i <- 0 to n) {
            val p = m_axi.receiveWriteAddress()
            if (LOG) println(f"Recv AW: ${p}")
            assert(p == AddressPacket(0, aw + i, 0, 0, 0))
          }
        }
      }.fork {
        ws.foreach(pkt => {
          pkt.foreach(x => {
            val p = m_axi.receiveWriteData()
            if (LOG) println(f"Recv W: ${p}")
            assert(p == WriteDataPacket(x, 0xf, true))
            m_axi.sendWriteResponse(0)
            if (LOG) println(f"Send B")
          })
        })
      }.join()
    }
  }

}
