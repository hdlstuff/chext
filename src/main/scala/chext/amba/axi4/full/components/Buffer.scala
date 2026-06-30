package chext.amba.axi4.full.components

import chisel3._

import chext.elastic
import chext.amba.axi4

/** Config for [[ReadResponseBuffer]].
  *
  * @param bufLengthR R-channel buffer capacity, in beats.
  */
case class ReadResponseBufferConfig(
    val bufLengthR: Int = 2
) {
  require(bufLengthR >= 2)
}

/** AXI4 read response buffer.
  *
  * `s_*` are slave-side ports of this component; `m_*` are master-side ports. AR is forwarded only
  * when the local R buffer has room for the whole burst (`ar.len + 1` beats).
  */
class ReadResponseBuffer(val axiCfg: axi4.Config, val cfg: ReadResponseBufferConfig)
    extends Module {
  import cfg._
  private implicit val _axiCfg: axi4.Config = axiCfg

  require(axiCfg.read && !axiCfg.lite)

  val s_ar = IO(elastic.Source(new axi4.full.ReadAddressChannel))
  val s_r = IO(elastic.Sink(new axi4.full.ReadDataChannel))
  val m_ar = IO(elastic.Sink(new axi4.full.ReadAddressChannel))
  val m_r = IO(elastic.Source(new axi4.full.ReadDataChannel))

  private def impl(): Unit = {
    val ctrR = new chext.util.CounterEx(bufLengthR + 1)

    ctrR.noUp()
    ctrR.noDown()

    val stall0 = new elastic.Stall(s_ar, m_ar) {
      out := in
      val len = in.len +& 1.U

      cond { !ctrR.canUp(len) }
      fire { ctrR.up(len) }
    }

    val connect0 =
      new elastic.Connect(elastic.SourceBuffer(m_r, bufLengthR), s_r) {
        fire { ctrR.down(1.U) }
      }
  }

  impl()
}

/** Config for [[WriteResponseBuffer]].
  *
  * @param bufLengthB B-channel buffer capacity, in responses.
  */
case class WriteResponseBufferConfig(
    val bufLengthB: Int = 2
) {
  require(bufLengthB >= 2)
}

/** AXI4 write response buffer.
  *
  * `s_*` are slave-side ports of this component; `m_*` are master-side ports. AW is forwarded only
  * when one B-buffer entry can be reserved. The W channel is intentionally not included.
  */
class WriteResponseBuffer(val axiCfg: axi4.Config, val cfg: WriteResponseBufferConfig)
    extends Module {
  import cfg._
  private implicit val _axiCfg: axi4.Config = axiCfg

  require(axiCfg.write && !axiCfg.lite)

  val s_aw = IO(elastic.Source(new axi4.full.WriteAddressChannel))
  val s_b = IO(elastic.Sink(new axi4.full.WriteResponseChannel))
  val m_aw = IO(elastic.Sink(new axi4.full.WriteAddressChannel))
  val m_b = IO(elastic.Source(new axi4.full.WriteResponseChannel))

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
  * @param bufLengthW W-channel buffer capacity, in beats.
  * @param bufLengthAW AW-channel buffer capacity, in addresses.
  */
case class WritePayloadBufferConfig(
    val bufLengthW: Int = 64,
    val bufLengthAW: Int = 2
) {
  require(bufLengthW >= 1)
  require(bufLengthAW >= 1)
}

/** AXI4 write payload buffer.
  *
  * `s_*` are slave-side ports of this component; `m_*` are master-side ports. W is buffered, and AW
  * is released only after the matching W burst is locally accepted. B is intentionally not included.
  */
class WritePayloadBuffer(val axiCfg: axi4.Config, val cfg: WritePayloadBufferConfig)
    extends Module {
  import cfg._
  private implicit val _axiCfg: axi4.Config = axiCfg

  require(axiCfg.write && !axiCfg.lite)

  val s_aw = IO(elastic.Source(new axi4.full.WriteAddressChannel))
  val s_w = IO(elastic.Source(new axi4.full.WriteDataChannel))
  val m_aw = IO(elastic.Sink(new axi4.full.WriteAddressChannel))
  val m_w = IO(elastic.Sink(new axi4.full.WriteDataChannel))

  private def impl(): Unit = {
    val ctrAddr = new chext.util.Counter(bufLengthW + 1)

    ctrAddr.noDec()
    ctrAddr.noInc()

    val stall0 = new elastic.Stall(s_w, elastic.SinkBuffer(m_w, bufLengthW)) {
      out := in

      cond { in.last && ctrAddr.full }
      fire {
        when(in.last) { ctrAddr.inc() }
      }
    }

    val stall1 = new elastic.Stall(elastic.SourceBuffer(s_aw, bufLengthAW), m_aw) {
      out := in

      cond { ctrAddr.zero }
      fire { ctrAddr.dec() }
    }
  }

  impl()
}
