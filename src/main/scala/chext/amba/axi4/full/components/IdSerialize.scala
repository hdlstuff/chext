package chext.amba.axi4.full.components

import chext.amba.axi4

import chext.{elastic2 => elastic}
import elastic.ConnectOp._

import chisel3._
import chisel3.util._
import chisel3.experimental._

import axi4.Casts._

case class IdSerializeConfig(
    val axiSlaveCfg: axi4.Config,
    val capacityIdQueueR: Int = 4,
    val capacityIdQueueW: Int = 4,
    val wIdSelect: Int = 0
) {
  require(!axiSlaveCfg.lite)
  require(axiSlaveCfg.read || axiSlaveCfg.write)

  val axiMasterCfg = axiSlaveCfg.copy(wId = 0)
}

/** Serializes the AXI transactions to a single ID, which is 0.
  *
  * @param axiCfg
  *   AXI configuration of the slave interface.
  */
class IdSerialize(val cfg: IdSerializeConfig) extends Module {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.full.Master(axiMasterCfg))

  private val genId = UInt(axiSlaveCfg.wId.W)

  private def implRead(): Unit = prefix("read") {
    val idQueue = elastic.Queue(
      genId,
      cfg.capacityIdQueueR,
      flow = true,
      pipe = true
    )

    idQueue.sink.nodeq()

    new elastic.Fork(s_axi.ar) {
      new elastic.Replicate(fork(in), idQueue.source) {
        len := in.len +& 1.U
        out := in.id
      }

      fork(in) :=> m_axi.ar
    }

    new elastic.Transform(m_axi.r, s_axi.r) {
      out := in
      out.id := idQueue.sink.bits
    }

    when(s_axi.r.fire) {
      idQueue.sink.deq()
    }
  }

  private def implWrite(): Unit = prefix("write") {
    val idQueue = elastic.Queue(
      genId,
      cfg.capacityIdQueueR,
      flow = true,
      pipe = true
    )

    new elastic.Fork(s_axi.aw) {
      fork(in.id) :=> idQueue.source
      fork(in) :=> m_axi.aw
    }

    s_axi.w :=> m_axi.w

    new elastic.Join(s_axi.b) {
      val id = join(idQueue.sink)

      out := join(m_axi.b)
      out.id := id
    }
  }

  if (axiSlaveCfg.read) implRead()
  if (axiMasterCfg.write) implWrite()
}
