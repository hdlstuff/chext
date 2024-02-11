package chext.axi4.full.components

import chext.{axi4, elastic}

import chisel3._
import chisel3.util._
import chisel3.experimental._

import axi4.Casts._

import elastic._
import elastic.TransformOp._
import elastic.ConnectOp._

case class IdSerializerConfig(
    val capacityIdQueueR: Int = 4,
    val capacityIdQueueW: Int = 4
)

/** Serializes the AXI transactions to a single ID, which is 0.
  *
  * @param axiCfg
  *   AXI configuration of the slave interface.
  */
class IdSerializerZero(
    val axiCfg: axi4.Config,
    val idSerializerCfg: IdSerializerConfig = IdSerializerConfig()
) extends Module {
  require(!axiCfg.lite)
  require(axiCfg.read || axiCfg.write)

  override def desiredName: String = "axi4FullIdSerializerBasic"

  val axiSlaveCfg = axiCfg
  val axiMasterCfg = axiCfg.copy(wId = 0)

  val S_AXI = IO(axi4.Slave(axiSlaveCfg))
  val M_AXI = IO(axi4.Master(axiMasterCfg))

  private val genId = UInt(axiSlaveCfg.wId.W)

  private def implRead(): Unit = prefix("read") {
    val s_axi = S_AXI.asFull
    val m_axi = M_AXI.asFull

    val idQueue = Module(
      new Queue(
        genId,
        idSerializerCfg.capacityIdQueueR,
        flow = true,
        pipe = true
      )
    )

    new Fork(s_axi.ar) {
      protected def onFork: Unit = {
        fork(in.id) :=> idQueue.io.enq
        fork(in) :=> m_axi.ar
      }
    }

    new Join(s_axi.r) {
      protected def onJoin: Unit = {
        val id = join(idQueue.io.deq)

        out := join(m_axi.r)
        out.id := id
      }
    }
  }

  private def implWrite(): Unit = prefix("write") {
    val s_axi = S_AXI.asFull
    val m_axi = M_AXI.asFull

    val idQueue = Module(
      new Queue(
        genId,
        idSerializerCfg.capacityIdQueueR,
        flow = true,
        pipe = true
      )
    )

    new Fork(s_axi.aw) {
      protected def onFork: Unit = {
        fork(in.id) :=> idQueue.io.enq
        fork(in) :=> m_axi.aw
      }
    }

    s_axi.w :=> m_axi.w

    new Join(s_axi.b) {
      protected def onJoin: Unit = {
        val id = join(idQueue.io.deq)

        out := join(m_axi.b)
        out.id := id
      }
    }
  }

  if (axiSlaveCfg.read) implRead()
  if (axiSlaveCfg.write) implWrite()
}
