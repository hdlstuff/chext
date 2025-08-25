package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.elastic
import elastic.ConnectOp._

import chext.amba.axi4
import axi4.Casts._

import chext.Prefix.prefix

case class IdSerializeConfig(
    val axiSlaveCfg: axi4.Config,
    val numOutstandingRead: Int = 4,
    val numOutstandingWrite: Int = 4,
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
    val wire0 = elastic.EWire(genId)

    val fork0 = new elastic.Fork(s_axi.ar) {
      val repeat0 = new elastic.Repeat(
        elastic.SourceBuffer(fork(), numOutstandingRead, pipe = true, flow = true),
        wire0,
        9
      ) {
        len { (in) => in.len +& 1.U }
        out { (in, _, _, _) => in.id }
      }

      fork() :=> m_axi.ar
    }

    val join0 = new elastic.Join(s_axi.r) {
      out := join(m_axi.r)
      out.id := join(wire0)
    }
  }

  private def implWrite(): Unit = prefix("write") {
    val wire0 = elastic.EWire(genId)

    val fork0 = new elastic.Fork(s_axi.aw) {
      fork { in.id } :=> wire0
      fork() :=> m_axi.aw
    }

    s_axi.w :=> m_axi.w

    val join0 = new elastic.Join(s_axi.b) {
      out := join(m_axi.b)
      out.id := join(wire0)
    }
  }

  if (axiSlaveCfg.read) implRead()
  if (axiMasterCfg.write) implWrite()
}
