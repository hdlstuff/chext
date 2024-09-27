package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

import chext.amba.axi4
import chext.elastic

import elastic.ConnectOp._
import axi4.Ops._
import chext.util.BitOps._

import axi4.full.components.addrgen

case class UpscaleConfig(
    val axiSlaveCfg: axi4.Config,
    val wDataMaster: Int,
    val readAddressStrobeQueueLength: Int = 16,
    val writeAddressStrobeQueueLength: Int = 16
) {
  require(axiSlaveCfg.wId == 0, "axiSlaveCfg.wId must be zero!")
  require(!axiSlaveCfg.lite, "axiSlaveCfg.lite must be false!")
  require(wDataMaster > axiSlaveCfg.wData, "wDataMaster must be > axiSlaveCfg.wData")

  require(wDataMaster >= 8)
  require(isPow2(wDataMaster))

  val wDataSlave = axiSlaveCfg.wData
  val wAddr = axiSlaveCfg.wAddr
  val axiMasterCfg = axiSlaveCfg.copy(wData = wDataMaster)
}

class Upscale(val cfg: UpscaleConfig) extends Module {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.full.Master(axiMasterCfg))

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

      addressStrobeGenerator.sink :=> addressStrobeQueue.io.enq
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
          val shiftBytes = addressStrobe.lowerByteIndex.resetLsbN(log2Ceil(wDataSlave / 8))
          out.data := beat.data >> (shiftBytes << 3)

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

      addressStrobeGenerator.sink :=> addressStrobeQueue.io.enq
    }

    def implW(): Unit = prefix("w") {
      new elastic.Join(m_axi.w) {
        override protected def onJoin: Unit = {
          val beat = join(s_axi.w)
          val addressStrobe = join(addressStrobeQueue.io.deq)
          val shiftBytes = addressStrobe.lowerByteIndex.resetLsbN(log2Ceil(wDataSlave / 8))

          // TODO: the same concern as above
          out.data := beat.data << (shiftBytes << 3)
          out.strb := (beat.strb << shiftBytes) & addressStrobe.strb

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

  if (axiSlaveCfg.read) implRead()
  if (axiSlaveCfg.write) implWrite()
}
