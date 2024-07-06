package chext.amba.axi4.full.components

import chext.amba.axi4
import chext.elastic

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
            len := in.len +& 1.U
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

  override def desiredName: String = "axi4FullIdSerializer"

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
          elastic.Demux(
            fork(),
            idSerializerZeros.map { _.s_axi.ar },
            fork(in.id(wIdSelect - 1, 0))
          )
        }
      }

      elastic.Arbiter(
        idSerializerZeros
          .map { _.m_axi.ar }
          .zipWithIndex
          .map {
            case (arSource, idx) => {
              val arSink = Wire(chiselTypeOf(m_axi.ar))

              arSource :=> arSink
              arSink.bits.id := idx.U

              arSink
            }
          },
        m_axi.ar,
        Chooser.rr
      )
    }

    def implR(): Unit = prefix("r") {
      new Fork(m_axi.r) {
        protected def onFork: Unit = {
          elastic.Demux(
            fork(),
            idSerializerZeros.map { _.m_axi.r },
            fork(in.id)
          )
        }
      }

      elastic.Arbiter(
        idSerializerZeros.map { _.s_axi.r },
        s_axi.r,
        Chooser.rr
      )
    }

    implAr()
    implR()
  }

  private def implWrite(): Unit = prefix("write") {
    def implAw(): Unit = prefix("aw") {
      val selectQueue = Module(
        new Queue(genIdSelect, cfg.capacityIdQueueW /* TODO: define a different var */, true, false)
      )

      new Fork(s_axi.aw) {
        protected def onFork: Unit = {
          val demuxSelect = Wire(Decoupled(genIdSelect))

          elastic.Demux(
            fork(),
            idSerializerZeros.map { _.s_axi.aw },
            demuxSelect
          )

          fork(in.id(wIdSelect - 1, 0)) :=> demuxSelect
          fork(in.id(wIdSelect - 1, 0)) :=> selectQueue.io.enq
        }
      }

      elastic.Mux(
        idSerializerZeros
          .map { _.m_axi.aw }
          .zipWithIndex
          .map {
            case (arSource, idx) => {
              val arSink = Wire(chiselTypeOf(m_axi.aw))

              arSource :=> arSink
              arSink.bits.id := idx.U

              arSink
            }
          },
        m_axi.aw,
        selectQueue.io.deq
      )
    }

    def implW(): Unit = prefix("w") {
      import axi4.full.WriteDataChannel

      idSerializerZeros.foreach { (x) =>
        {
          x.s_axi.w.noenq()
          x.m_axi.w.nodeq()
        }
      }

      s_axi.w :=> m_axi.w
    }

    def implB(): Unit = prefix("b") {
      new Fork(m_axi.b) {
        protected def onFork: Unit = {
          elastic.Demux(
            fork(),
            idSerializerZeros.map { _.m_axi.b },
            fork(in.id)
          )
        }
      }

      elastic.Arbiter(
        idSerializerZeros.map { _.s_axi.b },
        s_axi.b,
        Chooser.rr
      )
    }

    implAw()
    implW()
    implB()
  }

  if (axiSlaveCfg.read) implRead()
  if (axiSlaveCfg.write) implWrite()
}
