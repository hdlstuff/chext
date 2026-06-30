package chext.amba.axi4.lite

import chisel3._
import chisel3.util._
import chisel3.experimental.SourceInfo

import chext.amba.axi4
import chext.amba.axi4.BufferConfig

import chext.elastic
import elastic.{SinkBuffer, SourceBuffer}
import elastic.ConnectOp._

import chext.tracking.uniquePrefix

object buffer {
  val require_ = new chext.util.Require("axi4.lite.buffer")

  private[lite] def insertBufferR(
      master: Interface,
      slave: Interface,
      cfg: BufferConfig
  )(implicit si: SourceInfo): Unit = {
    SourceBuffer(master.ar, cfg.ar, name = "arBuffer") :=> slave.ar
    slave.r :=> SinkBuffer(master.r, cfg.r, name = "rBuffer")
  }

  private[lite] def insertBufferW(
      master: Interface,
      slave: Interface,
      cfg: BufferConfig
  )(implicit si: SourceInfo): Unit = {
    SourceBuffer(master.aw, cfg.aw, name = "awBuffer") :=> slave.aw
    SourceBuffer(master.w, cfg.w, name = "wBuffer") :=> slave.w
    slave.b :=> SinkBuffer(master.b, cfg.b, name = "bBuffer")
  }

  /** Inserts a buffer between a master and a slave interface.
    *
    * @param master
    * @param slave
    * @param cfg
    */
  def apply(
      master: Interface,
      slave: Interface,
      cfg: BufferConfig
  )(implicit si: SourceInfo): Unit = {
    require_(
      master.cfg == slave.cfg,
      "master and slave configurations do not match!",
      Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
    )

    if (master.cfg.read)
      insertBufferR(master, slave, cfg)

    if (master.cfg.write)
      insertBufferW(master, slave, cfg)
  }
}

object SlaveBuffer {
  def apply(
      interface: Interface,
      cfg: BufferConfig = BufferConfig.all(2),
      name: String = "slaveBuffer"
  )(implicit si: SourceInfo): Interface = uniquePrefix(name) {
    val result = Wire(Slave(interface.cfg))
    buffer(interface, result, cfg)
    result
  }
}

object LeftBuffer {
  def apply(
      interface: Interface,
      cfg: BufferConfig = BufferConfig.all(2),
      name: String = "leftBuffer"
  )(implicit si: SourceInfo): Interface = uniquePrefix(name) {
    val result = Wire(Slave(interface.cfg))
    buffer(interface, result, cfg)
    result
  }
}

object MasterBuffer {
  def apply(
      interface: Interface,
      cfg: BufferConfig = BufferConfig.all(2),
      name: String = "masterBuffer"
  )(implicit si: SourceInfo): Interface = uniquePrefix(name) {
    val result = Wire(Master(interface.cfg))
    buffer(result, interface, cfg)
    result
  }
}

object RightBuffer {
  def apply(
      interface: Interface,
      cfg: BufferConfig = BufferConfig.all(2),
      name: String = "rightBuffer"
  )(implicit si: SourceInfo): Interface = uniquePrefix(name) {
    val result = Wire(Master(interface.cfg))
    buffer(result, interface, cfg)
    result
  }
}

/** Config for [[ReadResponseBuffer]].
  *
  * @param bufLengthR
  *   R-channel buffer capacity, in responses.
  */
case class ReadResponseBufferConfig(
    val bufLengthR: Int = 2
) {
  require(bufLengthR >= 1)
}

/** AXI4-Lite read response buffer.
  *
  * `s_*` are slave-side ports of this component; `m_*` are master-side ports. AR is forwarded only
  * when one R-buffer entry can be reserved.
  */
class ReadResponseBuffer(val axiCfg: axi4.Config, val cfg: ReadResponseBufferConfig)
    extends Module {
  import cfg._
  private implicit val _axiCfg: axi4.Config = axiCfg

  require(axiCfg.read && axiCfg.lite)

  val s_ar = IO(elastic.Source(new AddressChannel))
  val s_r = IO(elastic.Sink(new ReadDataChannel))
  val m_ar = IO(elastic.Sink(new AddressChannel))
  val m_r = IO(elastic.Source(new ReadDataChannel))

  private def impl(): Unit = {
    val ctrR = new chext.util.Counter(bufLengthR + 1)

    ctrR.noDec()
    ctrR.noInc()

    val stall0 = new elastic.Stall(s_ar, m_ar) {
      out := in

      cond { ctrR.full }
      fire { ctrR.inc() }
    }

    val connect0 =
      new elastic.Connect(elastic.SourceBuffer(m_r, bufLengthR), s_r) {
        fire { ctrR.dec() }
      }
  }

  impl()
}

/** Config for [[WriteResponseBuffer]].
  *
  * @param bufLengthB
  *   B-channel buffer capacity, in responses.
  */
case class WriteResponseBufferConfig(
    val bufLengthB: Int = 2
) {
  require(bufLengthB >= 1)
}

/** AXI4-Lite write response buffer.
  *
  * `s_*` are slave-side ports of this component; `m_*` are master-side ports. AW is forwarded only
  * when one B-buffer entry can be reserved. The W channel is intentionally not included.
  */
class WriteResponseBuffer(val axiCfg: axi4.Config, val cfg: WriteResponseBufferConfig)
    extends Module {
  import cfg._
  private implicit val _axiCfg: axi4.Config = axiCfg

  require(axiCfg.write && axiCfg.lite)

  val s_aw = IO(elastic.Source(new AddressChannel))
  val s_b = IO(elastic.Sink(new WriteResponseChannel))
  val m_aw = IO(elastic.Sink(new AddressChannel))
  val m_b = IO(elastic.Source(new WriteResponseChannel))

  private def impl(): Unit = {
    val ctrB = new chext.util.Counter(bufLengthB + 1)

    ctrB.noDec()
    ctrB.noInc()

    val stall0 = new elastic.Stall(s_aw, m_aw) {
      out := in

      cond { ctrB.full }
      fire { ctrB.inc() }
    }

    val connect0 =
      new elastic.Connect(elastic.SourceBuffer(m_b, bufLengthB), s_b) {
        fire { ctrB.dec() }
      }
  }

  impl()
}

/** Config for [[WritePayloadBuffer]].
  *
  * @param bufLengthW
  *   W-channel buffer capacity, in payloads.
  * @param bufLengthAW
  *   AW-channel buffer capacity, in addresses.
  */
case class WritePayloadBufferConfig(
    val bufLengthW: Int = 64,
    val bufLengthAW: Int = 2
) {
  require(bufLengthW >= 1)
  require(bufLengthAW >= 1)
}

/** AXI4-Lite write payload buffer.
  *
  * `s_*` are slave-side ports of this component; `m_*` are master-side ports. W is buffered, and AW
  * is released only after the matching W payload is locally accepted. B is intentionally not
  * included.
  */
class WritePayloadBuffer(val axiCfg: axi4.Config, val cfg: WritePayloadBufferConfig)
    extends Module {
  import cfg._
  private implicit val _axiCfg: axi4.Config = axiCfg

  require(axiCfg.write && axiCfg.lite)

  val s_aw = IO(elastic.Source(new AddressChannel))
  val s_w = IO(elastic.Source(new WriteDataChannel))
  val m_aw = IO(elastic.Sink(new AddressChannel))
  val m_w = IO(elastic.Sink(new WriteDataChannel))

  private def impl(): Unit = {
    val ctrAddr = new chext.util.Counter(bufLengthW + 1)

    ctrAddr.noDec()
    ctrAddr.noInc()

    val stall0 = new elastic.Stall(s_w, elastic.SinkBuffer(m_w, bufLengthW)) {
      out := in

      cond { ctrAddr.full }
      fire { ctrAddr.inc() }
    }

    val stall1 = new elastic.Stall(elastic.SourceBuffer(s_aw, bufLengthAW), m_aw) {
      out := in

      cond { ctrAddr.zero }
      fire { ctrAddr.dec() }
    }
  }

  impl()
}
