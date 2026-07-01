package chext.amba.axi4.full.components

import chisel3._
import chisel3.experimental.SourceInfo

import chext.amba.axi4
import chext.amba.axi4.full
import chext.elastic

/** AXI4 full credit buffer configuration.
  *
  * Credit buffering reserves local response or payload capacity before forwarding the matching
  * address channel. Plain per-channel skid buffers remain in `axi4.full.Buffer`.
  */
case class CreditBufferConfig(
    val axiCfg: axi4.Config,
    val rBuffer: Int = 0,
    val awBuffer: Int = 0,
    val wBuffer: Int = 0,
    val bBuffer: Int = 0
) {
  require(!axiCfg.lite)
  require(axiCfg.read || axiCfg.write)
  require(rBuffer >= 0)
  require(awBuffer >= 0)
  require(wBuffer >= 0)
  require(bBuffer >= 0)

  val axiMasterCfg: axi4.Config = axiCfg
}

/** AXI4 full credit buffer.
  *
  * AR/AW may be delayed until the local R/B/W buffering has enough credit to absorb the associated
  * response or payload traffic. A depth of zero leaves that channel directly connected.
  */
class CreditBuffer(val cfg: CreditBufferConfig) extends Module {
  import cfg._
  private implicit val _axiCfg: axi4.Config = axiCfg

  val s_axi = IO(full.Slave(axiCfg))
  val m_axi = IO(full.Master(axiMasterCfg))

  private def connectRead()(implicit si: SourceInfo): Unit = {
    if (rBuffer > 0) {
      val responseBuffer = Module(new ReadResponseBuffer(ReadResponseBufferConfig(axiCfg, rBuffer)))
      val connectArIn = new elastic.Connect(s_axi.ar, responseBuffer.s_ar)
      val connectArOut = new elastic.Connect(responseBuffer.m_ar, m_axi.ar)
      val connectROut = new elastic.Connect(m_axi.r, responseBuffer.m_r)
      val connectRIn = new elastic.Connect(responseBuffer.s_r, s_axi.r)
    } else {
      val connectAr = new elastic.Connect(s_axi.ar, m_axi.ar)
      val connectR = new elastic.Connect(m_axi.r, s_axi.r)
    }
  }

  private def connectWrite()(implicit si: SourceInfo): Unit = {
    def connectWriteResponse(
        awSource: elastic.Interface[full.AddressChannel],
        wSource: elastic.Interface[full.WriteDataChannel]
    ): Unit = {
      if (bBuffer > 0) {
        val responseBuffer =
          Module(new WriteResponseBuffer(WriteResponseBufferConfig(axiCfg, bBuffer)))
        val connectAwRespIn =
          new elastic.Connect(
            awSource.asInstanceOf[elastic.Interface[full.WriteAddressChannel]],
            responseBuffer.s_aw
          )
        val connectAwRespOut = new elastic.Connect(responseBuffer.m_aw, m_axi.aw)
        val connectW = new elastic.Connect(wSource, m_axi.w)
        val connectBOut = new elastic.Connect(m_axi.b, responseBuffer.m_b)
        val connectBIn = new elastic.Connect(responseBuffer.s_b, s_axi.b)
      } else {
        val connectAw = new elastic.Connect(awSource, m_axi.aw)
        val connectW = new elastic.Connect(wSource, m_axi.w)
        val connectB = new elastic.Connect(m_axi.b, s_axi.b)
      }
    }

    if (wBuffer > 0) {
      val payloadBuffer =
        Module(
          new WritePayloadBuffer(
            WritePayloadBufferConfig(
              axiCfg = axiCfg,
              bufLengthW = wBuffer,
              bufLengthAW = math.max(awBuffer, 1)
            )
          )
        )
      val connectAwIn =
        new elastic.Connect(
          s_axi.aw.asInstanceOf[elastic.Interface[full.WriteAddressChannel]],
          payloadBuffer.s_aw
        )
      val connectWIn = new elastic.Connect(s_axi.w, payloadBuffer.s_w)
      connectWriteResponse(
        payloadBuffer.m_aw.asInstanceOf[elastic.Interface[full.AddressChannel]],
        payloadBuffer.m_w
      )
    } else if (awBuffer > 0) {
      connectWriteResponse(
        elastic.SourceBuffer(s_axi.aw, awBuffer, name = "awBuffer"),
        s_axi.w
      )
    } else {
      connectWriteResponse(s_axi.aw, s_axi.w)
    }
  }

  if (axiCfg.read) connectRead()
  if (axiCfg.write) connectWrite()
}

/** Config for [[ReadResponseBuffer]].
  *
  * @param axiCfg
  *   AXI configuration for this component.
  * @param bufLengthR
  *   R-channel buffer capacity, in beats.
  */
case class ReadResponseBufferConfig(
    val axiCfg: axi4.Config,
    val bufLengthR: Int = 2
) {
  require(axiCfg.read && !axiCfg.lite)
  require(bufLengthR >= 1)
}

/** AXI4 read response buffer.
  *
  * `s_*` are slave-side ports of this component; `m_*` are master-side ports. AR is forwarded only
  * when the local R buffer has room for the whole burst (`ar.len + 1` beats).
  */
class ReadResponseBuffer(val cfg: ReadResponseBufferConfig) extends Module {
  import cfg._
  private implicit val _axiCfg: axi4.Config = axiCfg

  val s_ar = IO(elastic.Source(new full.ReadAddressChannel))
  val s_r = IO(elastic.Sink(new full.ReadDataChannel))
  val m_ar = IO(elastic.Sink(new full.ReadAddressChannel))
  val m_r = IO(elastic.Source(new full.ReadDataChannel))

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
  * @param axiCfg
  *   AXI configuration for this component.
  * @param bufLengthB
  *   B-channel buffer capacity, in responses.
  */
case class WriteResponseBufferConfig(
    val axiCfg: axi4.Config,
    val bufLengthB: Int = 2
) {
  require(axiCfg.write && !axiCfg.lite)
  require(bufLengthB >= 1)
}

/** AXI4 write response buffer.
  *
  * `s_*` are slave-side ports of this component; `m_*` are master-side ports. AW is forwarded only
  * when one B-buffer entry can be reserved. The W channel is intentionally not included.
  */
class WriteResponseBuffer(val cfg: WriteResponseBufferConfig) extends Module {
  import cfg._
  private implicit val _axiCfg: axi4.Config = axiCfg

  val s_aw = IO(elastic.Source(new full.WriteAddressChannel))
  val s_b = IO(elastic.Sink(new full.WriteResponseChannel))
  val m_aw = IO(elastic.Sink(new full.WriteAddressChannel))
  val m_b = IO(elastic.Source(new full.WriteResponseChannel))

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
  * @param axiCfg
  *   AXI configuration for this component.
  * @param bufLengthW
  *   W-channel buffer capacity, in beats.
  * @param bufLengthAW
  *   AW-channel buffer capacity, in addresses.
  */
case class WritePayloadBufferConfig(
    val axiCfg: axi4.Config,
    val bufLengthW: Int = 64,
    val bufLengthAW: Int = 2
) {
  require(axiCfg.write && !axiCfg.lite)
  require(bufLengthW >= 1)
  require(bufLengthAW >= 1)
}

/** AXI4 write payload buffer.
  *
  * `s_*` are slave-side ports of this component; `m_*` are master-side ports. W is buffered, and AW
  * is released only after the matching W burst is locally accepted. B is intentionally not included.
  */
class WritePayloadBuffer(val cfg: WritePayloadBufferConfig) extends Module {
  import cfg._
  private implicit val _axiCfg: axi4.Config = axiCfg

  val s_aw = IO(elastic.Source(new full.WriteAddressChannel))
  val s_w = IO(elastic.Source(new full.WriteDataChannel))
  val m_aw = IO(elastic.Sink(new full.WriteAddressChannel))
  val m_w = IO(elastic.Sink(new full.WriteDataChannel))

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
