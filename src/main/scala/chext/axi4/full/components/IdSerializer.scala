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
    val capacityIdQueueW: Int = 4,
    val wIdSelect: Int = 0
)

/** Serializes the AXI transactions to a single ID, which is 0.
  *
  * @param axiCfg
  *   AXI configuration of the slave interface.
  */
class IdSerializerZero(
    axiCfg: axi4.Config,
    val cfg: IdSerializerConfig = IdSerializerConfig()
) extends Module {
  require(!axiCfg.lite)
  require(axiCfg.read || axiCfg.write)

  override def desiredName: String = "axi4FullIdSerializerZero"

  val axiSlaveCfg = axiCfg
  val axiMasterCfg = axiCfg.copy(wId = 0)

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.full.Master(axiMasterCfg))

  private val genId = UInt(axiSlaveCfg.wId.W)

  private def implRead(): Unit = prefix("read") {
    val idQueue = Module(
      new Queue(
        genId,
        cfg.capacityIdQueueR,
        flow = true,
        pipe = true
      )
    )

    idQueue.io.deq.nodeq()

    new Fork(s_axi.ar) {
      protected def onFork: Unit = {
        new Replicate(fork(in), idQueue.io.enq) {
          protected def onReplicate: Unit = {
            len := in.len + 1.U
            out := in.id
          }
        }

        fork(in) :=> m_axi.ar
      }
    }

    new Transform(m_axi.r, s_axi.r) {
      protected def onTransform: Unit = {
        out := in
        out.id := idQueue.io.deq.bits
      }
    }

    when(s_axi.r.fire) {
      idQueue.io.deq.deq()
    }
  }

  private def implWrite(): Unit = prefix("write") {
    val idQueue = Module(
      new Queue(
        genId,
        cfg.capacityIdQueueR,
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

class IdSerializer(
    axiCfg: axi4.Config,
    val cfg: IdSerializerConfig = IdSerializerConfig()
) extends Module {
  require(!axiCfg.lite)
  require(axiCfg.read || axiCfg.write)

  override def desiredName: String = "axi4FullIdSerializerZero"

  private val wIdSelect = cfg.wIdSelect

  private val genIdSelect = UInt(wIdSelect.W)

  val axiSlaveCfg = axiCfg
  val axiMasterCfg = axiCfg.copy(wId = wIdSelect)

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.full.Master(axiMasterCfg))

  private val idSerializerZeros = Seq.fill(1 << wIdSelect) {
    Module(new IdSerializerZero(axiSlaveCfg, cfg))
  }

  private def implRead(): Unit = prefix("read") {
    def implAr(): Unit = prefix("ar") {
      new Fork(s_axi.ar) {
        protected def onFork: Unit = {
          val demuxSelect = Wire(Decoupled(genIdSelect))

          elastic.Demux(
            fork(),
            idSerializerZeros.map { _.s_axi.ar },
            demuxSelect
          )

          new Transform(fork(), demuxSelect) {
            protected def onTransform: Unit = {
              // TODO: maybe use a different select function?
              out := in.id & ((1 << wIdSelect) - 1).U
            }
          }
        }
      }

      elastic.Arbiter(
        idSerializerZeros.map { _.m_axi.ar },
        m_axi.ar,
        Chooser.rr
      )
    }

    def implR(): Unit = prefix("r") {
      new Fork(m_axi.r) {
        protected def onFork: Unit = {
          val demuxSelect = Wire(Decoupled(genIdSelect))

          elastic.Demux(
            fork(),
            idSerializerZeros.map { _.m_axi.r },
            demuxSelect
          )

          new Transform(fork(), demuxSelect) {
            protected def onTransform: Unit = {
              // TODO: maybe use a different select function?
              out := in.id
            }
          }
        }
      }

      elastic.Arbiter(
        idSerializerZeros.map { _.s_axi.r },
        m_axi.r,
        Chooser.rr
      )
    }

    implAr()
    implR()
  }

  private def implWrite(): Unit = prefix("write") {
    //
  }

  if (axiSlaveCfg.read) implRead()
  if (axiSlaveCfg.write) implWrite()
}
