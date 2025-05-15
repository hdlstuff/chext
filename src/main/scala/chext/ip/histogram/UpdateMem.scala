package chext.ip.histogram

import chisel3._
import chisel3.util._

import chext.{elastic2 => elastic}
import elastic._

import chext.amba.axi4
import axi4.Ops._

class Task extends Bundle {
  val zero = Bool()
  val last = Bool()

  val bucket = UInt(64.W)
  val value = UInt(64.W)
}

case class UpdateMemConfig() {
  val axiCfg = axi4.Config(wId = 0, wAddr = 64, wData = 64)

  val genTask = new Task
  val genResult = UInt(0.W)
}

class UpdateMem(cfg: UpdateMemConfig) extends Module {
  import cfg._

  val m_axi = IO(axi4.Master(axiCfg))

  val sourceTask = IO(Source(genTask))
  val sinkResult = IO(Sink(genResult))

  // here is the idea
  // let's not even use Reduce in the beginning
  // if there is a need to reduce, we can do it later
  // this module is just a simple load/store unit

  object State extends ChiselEnum {
    val stIdle = Value
    val stLoad = Value
    val stStore = Value
  }

  import State._

  private val state = RegInit(stIdle)

  // m_axi_ is buffered to make the FSM simpler
  // m_axi_.ar.enq(...) never stalls, so no need to wait for the backpressure
  private val m_axi_ = axi4.full.MasterBuffer(m_axi.asFull, axi4.BufferConfig.all(2))

  m_axi_.ar.noenq()
  m_axi_.r.nodeq()

  m_axi_.aw.noenq()
  m_axi_.w.noenq()
  m_axi_.b.nodeq()

  new Arrival(sourceTask, sinkResult) {
    out := 0.U

    when(arrived) {
      switch(state) {
        is(stIdle) {
          when(in.zero) {
            when(in.last) {
              accept()
            }.otherwise {
              drop()
            }
          }.otherwise {
            state := stLoad

            {
              val arPacket = Wire(chiselTypeOf(m_axi_.ar.bits))

              arPacket := 0.U.asTypeOf(arPacket)
              arPacket.addr := (in.bucket << 3)
              arPacket.len := 0.U

              m_axi_.ar.enq(arPacket)
            }
          }
        }

        is(stLoad) {
          when(m_axi_.r.valid) {
            m_axi_.r.deq()

            {
              val awPacket = Wire(chiselTypeOf(m_axi_.aw.bits))

              awPacket := 0.U.asTypeOf(awPacket)
              awPacket.addr := (in.bucket << 3)
              awPacket.len := 0.U

              m_axi_.aw.enq(awPacket)
            }

            {
              val wPacket = Wire(chiselTypeOf(m_axi_.w.bits))

              wPacket := 0.U.asTypeOf(wPacket)
              wPacket.data := m_axi_.r.bits.data + in.value
              wPacket.strb := 0xff.U // HARDCODED
              wPacket.last := true.B

              m_axi_.w.enq(wPacket)
            }

            state := stStore
          }
        }

        is(stStore) {
          when(m_axi_.b.valid) {
            m_axi_.b.deq()

            state := stIdle

            when(in.last) {
              accept()
            }.otherwise {
              drop()
            }
          }
        }
      }
    }
  }
}

object MyApp extends App {
  emitVerilog(new UpdateMem(UpdateMemConfig()))
}
