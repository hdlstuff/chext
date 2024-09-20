package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

import chext.amba.axi4
import chext.elastic
import elastic.ConnectOp._
import axi4.Ops._

import axi4.full.components.addrgen

case class UpscaleConfig(
    val axiCfgSlave: axi4.Config,
    val wDataMaster: Int = 64,
    val readAddressStrobeQueueLength: Int = 16,
    val writeAddressStrobeQueueLength: Int = 16
) {
  assert(axiCfgSlave.wId == 0, "axiCfgSlave.wId must be zero!")
  assert(!axiCfgSlave.lite, "axiCfgSlave.lite must be false!")
  assert(wDataMaster > axiCfgSlave.wData, "wDataMaster must be >= axiCfgSlave.wData")

  val wAddr = axiCfgSlave.wAddr
  val wDataSlave = axiCfgSlave.wData
  val axiCfgMaster = axiCfgSlave.copy(wData = wDataMaster)
}

class Upscale(val cfg: UpscaleConfig) extends Module {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiCfgSlave))
  val m_axi = IO(axi4.full.Master(axiCfgMaster))

  private def implRead(): Unit = prefix("read") {
    val addressStrobeGenerator =
      Module(
        new addrgen.AddressStrobeGenerator(wAddr, wDataMaster)
      )

    val addressStrobeQueue =
      Module(
        new Queue(
          addressStrobeGenerator.genOutput,
          readAddressStrobeQueueLength
        )
      )

    addressStrobeGenerator.sink :=> addressStrobeQueue.io.enq

    def implAR(): Unit = prefix("ar") {
      new elastic.Fork(s_axi.ar) {
        override protected def onFork: Unit = {
          new elastic.Transform(fork(), addressStrobeGenerator.source) {
            override protected def onTransform: Unit = {
              out.addr := in.addr
              out.len := in.len
              out.size := in.size
              out.burst := in.burst
            }
          }

          fork() :=> m_axi.ar
        }
      }
    }

    def implR(): Unit = prefix("r") {
      new elastic.Join(s_axi.r) {
        override protected def onJoin: Unit = {
          val beat = join(m_axi.r)
          val addressStrobe = join(addressStrobeQueue.io.deq)

          //
          // This is called "lane steering" by the following guys:
          // https://github.com/pulp-platform/axi/blob/master/src/axi_dw_upsizer.sv
          //
          // My implementation is definitely not the best.
          // Depending on the data widths, addressStrobe.lowerByteIndex is constrained.
          // TODO: create a new module for doing this more optimally.
          //
          out.data := beat.data >> (addressStrobe.lowerByteIndex << 3)

          out.id := beat.id // must be zero
          out.resp := beat.resp
          out.user := beat.user
          out.last := beat.last
        }
      }
    }

    implAR()
    implR()
  }

  private def implWrite(): Unit = prefix("write") {
    val addressStrobeGenerator =
      Module(
        new addrgen.AddressStrobeGenerator(wAddr, wDataMaster)
      )

    val addressStrobeQueue =
      Module(
        new Queue(
          addressStrobeGenerator.genOutput,
          writeAddressStrobeQueueLength
        )
      )

    addressStrobeGenerator.sink :=> addressStrobeQueue.io.enq

    def implAW(): Unit = prefix("aw") {
      new elastic.Fork(s_axi.aw) {
        override protected def onFork: Unit = {
          new elastic.Transform(fork(), addressStrobeGenerator.source) {
            override protected def onTransform: Unit = {
              out.addr := in.addr
              out.len := in.len
              out.size := in.size
              out.burst := in.burst
            }
          }

          fork() :=> m_axi.aw
        }
      }
    }

    def implW(): Unit = prefix("w") {
      new elastic.Join(m_axi.w) {
        override protected def onJoin: Unit = {
          val beat = join(s_axi.w)
          val addressStrobe = join(addressStrobeQueue.io.deq)

          // TODO: the same concern as above
          out.data := (beat.data << (addressStrobe.lowerByteIndex << 3))

          out.strb := (beat.strb << addressStrobe.lowerByteIndex) & addressStrobe.strb
          out.last := beat.last
          out.user := beat.user
        }
      }
    }

    def implB(): Unit = prefix("b") {
      m_axi.b :=> s_axi.b
    }

    implAW()
    implW()
    implB()
  }

  if (axiCfgSlave.read) implRead()
  if (axiCfgSlave.write) implWrite()
}
