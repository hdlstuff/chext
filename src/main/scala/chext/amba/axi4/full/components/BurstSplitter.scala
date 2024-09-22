package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

import chext.amba.axi4
import chext.elastic
import elastic.ConnectOp._
import axi4.Ops._

import axi4.full.components.addrgen

case class BurstSplitterConfig(
    val axiCfg: axi4.Config
) {
  assert(axiCfg.wId == 0, "axiCfg.wId must be zero!")
  assert(!axiCfg.lite, "axiCfg.lite must be false!")

  val wAddr = axiCfg.wAddr
  val wData = axiCfg.wData
}

class BurstSplitter(val cfg: BurstSplitterConfig) extends Module {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiCfg))
  val m_axi = IO(axi4.full.Master(axiCfg))

  dontTouch(s_axi)
  dontTouch(m_axi)

  private def implRead(): Unit = prefix("read") {
    val addressStrobeGenerator =
      Module(
        new addrgen.AddressStrobeGenerator(wAddr, wData)
      )

    val lenQueue = Module(
      new Queue(UInt(axiCfg.wLen.W), 16 /* TODO make reconfigurable */ )
    )

    def implAR(): Unit = prefix("ar") {
      new elastic.Fork(s_axi.ar) {
        protected def onFork: Unit = {
          new elastic.Transform(fork(), addressStrobeGenerator.source) {
            protected def onTransform: Unit = {
              out.addr := in.addr
              out.len := in.len
              out.size := in.size
              out.burst := in.burst
            }
          }

          val wire0 = Wire(chiselTypeOf(s_axi.ar))

          new elastic.Replicate(fork(), wire0) {
            protected def onReplicate: Unit = {
              len := in.len +& 1.U
              out := in
            }
          }

          new elastic.Join(m_axi.ar) {
            protected def onJoin: Unit = {
              val pkt0 = join(wire0)
              val pkt1 = join(addressStrobeGenerator.sink)

              out.id := pkt0.id
              out.addr := pkt1.addr
              out.len := 0.U
              out.size := pkt1.size
              out.burst := 1.U
              out.lock := pkt0.lock
              out.cache := pkt0.cache
              out.prot := pkt0.prot
              out.qos := pkt0.qos
              out.region := pkt0.region
              out.user := pkt0.user
            }
          }

          fork(in.len) :=> lenQueue.io.enq
        }
      }
    }

    def implR(): Unit = prefix("r") {
      val wire0 = Wire(DecoupledIO(Bool()))

      new elastic.Replicate(lenQueue.io.deq, wire0) {
        protected def onReplicate: Unit = {
          len := in +& 1.U
          out := last
        }
      }

      new elastic.Join(s_axi.r) {
        protected def onJoin: Unit = {
          val r = join(m_axi.r)
          val last = join(wire0)

          out := r
          out.last := last
        }
      }
    }

    implAR()
    implR()
  }

  private def implWrite(): Unit = prefix("write") {
    // TODO impl
    s_axi.aw :=> m_axi.aw
    s_axi.w :=> m_axi.w
    m_axi.b :=> s_axi.b
  }

  if (axiCfg.read) implRead()
  if (axiCfg.write) implWrite()
}
