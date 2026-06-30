package chext.amba.axi4.full

import chisel3._
import chext.elastic
import chisel3.experimental.SourceInfo

import chext.amba.axi4.full.components
import chext.tracking
import chext.util.SimulationCheck

private final class ConnectImpl(
    master: Interface,
    slave: Interface,
    cfgOption: Option[ConnectConfig]
)(si_ : SourceInfo)
    extends tracking.Container {
  private val require_ = new chext.util.Require("axi4.full.connect")

  val sourceInfo: SourceInfo = si_
  def tpe: String = "Connect"
  def namePrefix: String = cfgOption.map(_.name).getOrElse("connect")

  private def warn(message: String, lines: Seq[String] = Seq())(
      implicit si: SourceInfo
  ): Unit = {
    val source = si.makeMessage((x) => x)
    println(f"axi4.full.connect : $message $source")
    (lines ++ interfaceSourceLines).foreach { line => println(f"                  : $line") }
  }

  private def sourceInfoString(interface: Interface): String =
    chext.tracking.util.sourceInfoToString(interface.sourceInfo)

  private def interfaceSourceLines: Seq[String] =
    Seq(
      f"master.sourceInfo = ${sourceInfoString(master)}",
      f"slave.sourceInfo = ${sourceInfoString(slave)}"
    )

  private def withInterfaceSourceInfo(message: String): String =
    (Seq(message) ++ interfaceSourceLines).mkString("\n")

  private def tieOffReadMaster(master: Interface)(implicit si: SourceInfo): Unit = {
    val nullSinkAr = new elastic.NullSink(master.ar)
    val nullSourceR = new elastic.NullSource(master.r)
  }

  private def tieOffReadSlave(slave: Interface)(implicit si: SourceInfo): Unit = {
    val nullSourceAr = new elastic.NullSource(slave.ar)
    val nullSinkR = new elastic.NullSink(slave.r)
  }

  private def tieOffWriteMaster(master: Interface)(implicit si: SourceInfo): Unit = {
    val nullSinkAw = new elastic.NullSink(master.aw)
    val nullSinkW = new elastic.NullSink(master.w)
    val nullSourceB = new elastic.NullSource(master.b)
  }

  private def tieOffWriteSlave(slave: Interface)(implicit si: SourceInfo): Unit = {
    val nullSourceAw = new elastic.NullSource(slave.aw)
    val nullSourceW = new elastic.NullSource(slave.w)
    val nullSinkB = new elastic.NullSink(slave.b)
  }

  private def connectRead(master: Interface, slave: Interface)(implicit si: SourceInfo): Unit = {
    val connectAr = new elastic.Connect(master.ar, slave.ar)
    val connectR = new elastic.Connect(slave.r, master.r)
  }

  private def connectWrite(master: Interface, slave: Interface)(implicit si: SourceInfo): Unit = {
    val connectAw = new elastic.Connect(master.aw, slave.aw)
    val connectW = new elastic.Connect(master.w, slave.w)
    val connectB = new elastic.Connect(slave.b, master.b)
  }

  private def requireStrictCompatibility(
      master: Interface,
      slave: Interface
  )(implicit si: SourceInfo): Unit = {
    require_(
      master.cfg.wId <= slave.cfg.wId,
      "master interface should have a narrower ID field than the slave interface",
      Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
    )

    require_(
      master.cfg.wAddr >= slave.cfg.wAddr,
      "master interface should have a wider address field than the slave interface",
      Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
    )

    require_(
      !master.cfg.axi3Compat && !slave.cfg.axi3Compat || master.cfg.axi3Compat,
      "master interface that is not AXI3-compatible cannot drive an AXI3-compatible interface",
      Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
    )

    val masterCfg =
      master.cfg.copy(wId = 0, wAddr = 0, read = false, write = false, axi3Compat = false)
    val slaveCfg =
      slave.cfg.copy(wId = 0, wAddr = 0, read = false, write = false, axi3Compat = false)

    require_(
      masterCfg == slaveCfg,
      "configurations do not match after normalization",
      Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
    )
  }

  private def implStrict()(implicit si: SourceInfo): Unit = {
    requireStrictCompatibility(master, slave)

    require_(
      master.cfg.read == slave.cfg.read || master.cfg.write == slave.cfg.write,
      "master and slave share no channel group",
      Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
    )

    if (master.cfg.read && slave.cfg.read) connectRead(master, slave)
    else if (master.cfg.read) tieOffReadMaster(master)
    else if (slave.cfg.read) tieOffReadSlave(slave)

    if (master.cfg.write && slave.cfg.write) connectWrite(master, slave)
    else if (master.cfg.write) tieOffWriteMaster(master)
    else if (slave.cfg.write) tieOffWriteSlave(slave)
  }

  private def checkUpperZero(
      value: UInt,
      lowWidth: Int,
      enable: Bool,
      policy: SimulationCheck,
      message: String
  ): Unit = {
    if (value.getWidth > lowWidth) {
      val upper = value(value.getWidth - 1, lowWidth)
      policy(!enable || upper === 0.U, withInterfaceSourceInfo(message))
    }
  }

  private def insertSimulationChecks(
      master: Interface,
      slave: Interface,
      cfg: ConnectConfig
  ): Unit = {
    if (master.cfg.read && slave.cfg.read) {
      checkUpperZero(
        master.ar.$bits.id,
        slave.cfg.wId,
        master.ar.$valid,
        cfg.simCheckIdWidth,
        "axi4.full.connect: ARID upper bits are not zero"
      )
      checkUpperZero(
        master.ar.$bits.addr,
        slave.cfg.wAddr,
        master.ar.$valid,
        cfg.simCheckAddrWidth,
        "axi4.full.connect: ARADDR upper bits are not zero"
      )

      if (!master.cfg.axi3Compat && slave.cfg.axi3Compat)
        cfg.simCheckAxi3Compat(
          !master.ar.$valid || master.ar.$bits.len < 16.U,
          withInterfaceSourceInfo("axi4.full.connect: ARLEN is not AXI3-compatible")
        )

      checkUpperZero(
        slave.r.$bits.id,
        master.cfg.wId,
        slave.r.$valid,
        cfg.simCheckIdWidth,
        "axi4.full.connect: RID upper bits are not zero"
      )
    }

    if (master.cfg.write && slave.cfg.write) {
      checkUpperZero(
        master.aw.$bits.id,
        slave.cfg.wId,
        master.aw.$valid,
        cfg.simCheckIdWidth,
        "axi4.full.connect: AWID upper bits are not zero"
      )
      checkUpperZero(
        master.aw.$bits.addr,
        slave.cfg.wAddr,
        master.aw.$valid,
        cfg.simCheckAddrWidth,
        "axi4.full.connect: AWADDR upper bits are not zero"
      )

      if (!master.cfg.axi3Compat && slave.cfg.axi3Compat)
        cfg.simCheckAxi3Compat(
          !master.aw.$valid || master.aw.$bits.len < 16.U,
          withInterfaceSourceInfo("axi4.full.connect: AWLEN is not AXI3-compatible")
        )

      checkUpperZero(
        slave.b.$bits.id,
        master.cfg.wId,
        slave.b.$valid,
        cfg.simCheckIdWidth,
        "axi4.full.connect: BID upper bits are not zero"
      )
    }
  }

  private def reportConfigDifferences(
      master: Interface,
      slave: Interface,
      cfg: ConnectConfig
  )(implicit si: SourceInfo): Unit = {
    if (cfg.warnIdWidth && master.cfg.wId > slave.cfg.wId)
      warn(
        "master ID width is wider than slave ID width",
        Seq(f"master.cfg.wId = ${master.cfg.wId}", f"slave.cfg.wId = ${slave.cfg.wId}")
      )

    if (cfg.warnAddrWidth && master.cfg.wAddr > slave.cfg.wAddr)
      warn(
        "master address width is wider than slave address width",
        Seq(
          f"master.cfg.wAddr = ${master.cfg.wAddr}",
          f"slave.cfg.wAddr = ${slave.cfg.wAddr}"
        )
      )

    if (
      cfg.warnAxi3Compat &&
      !(!master.cfg.axi3Compat && !slave.cfg.axi3Compat || master.cfg.axi3Compat)
    )
      warn(
        "AXI3 compatibility differs",
        Seq(
          f"master.cfg.axi3Compat = ${master.cfg.axi3Compat}",
          f"slave.cfg.axi3Compat = ${slave.cfg.axi3Compat}"
        )
      )
  }

  private def connectReadBuffered(
      master: Interface,
      slave: Interface,
      cfg: ConnectConfig
  )(implicit si: SourceInfo): Unit = {
    val afterAr = if (cfg.arBuffer > 0) {
      val wire = Wire(Master(master.cfg))
      val connectAr =
        new elastic.Connect(elastic.SourceBuffer(master.ar, cfg.arBuffer, name = "arBuffer"), wire.ar)
      wire
    } else master

    if (cfg.rBuffer > 0) {
      val responseBuffer =
        Module(
          new components.ReadResponseBuffer(
            master.cfg,
            components.ReadResponseBufferConfig(cfg.rBuffer)
          )
      )
      val connectArIn = new elastic.Connect(
        afterAr.ar.asInstanceOf[elastic.Interface[ReadAddressChannel]],
        responseBuffer.s_ar
      )
      val connectArOut = new elastic.Connect(
        responseBuffer.m_ar,
        slave.ar.asInstanceOf[elastic.Interface[ReadAddressChannel]]
      )
      val connectROut = new elastic.Connect(slave.r, responseBuffer.m_r)
      val connectRIn = new elastic.Connect(responseBuffer.s_r, afterAr.r)
    } else {
      connectRead(afterAr, slave)
    }
  }

  private def connectWriteBuffered(
      master: Interface,
      slave: Interface,
      cfg: ConnectConfig
  )(implicit si: SourceInfo): Unit = {
    val afterPayload = if (cfg.wBuffer > 0) {
      val wire = Wire(Master(master.cfg))
      val payloadBuffer =
        Module(
          new components.WritePayloadBuffer(
            master.cfg,
            components.WritePayloadBufferConfig(
              bufLengthW = cfg.wBuffer,
              bufLengthAW = math.max(cfg.awBuffer, 1)
            )
          )
      )
      val connectAwIn = new elastic.Connect(
        master.aw.asInstanceOf[elastic.Interface[WriteAddressChannel]],
        payloadBuffer.s_aw
      )
      val connectWIn = new elastic.Connect(master.w, payloadBuffer.s_w)
      val connectAwOut = new elastic.Connect(
        payloadBuffer.m_aw,
        wire.aw.asInstanceOf[elastic.Interface[WriteAddressChannel]]
      )
      val connectWOut = new elastic.Connect(payloadBuffer.m_w, wire.w)
      wire
    } else if (cfg.awBuffer > 0) {
      val wire = Wire(Master(master.cfg))
      val connectAw = new elastic.Connect(
        elastic.SourceBuffer(master.aw, cfg.awBuffer, name = "awBuffer"),
        wire.aw
      )
      val connectW = new elastic.Connect(master.w, wire.w)
      wire
    } else master

    if (cfg.bBuffer > 0) {
      val responseBuffer =
        Module(
          new components.WriteResponseBuffer(
            master.cfg,
            components.WriteResponseBufferConfig(cfg.bBuffer)
          )
      )
      val connectAwIn = new elastic.Connect(
        afterPayload.aw.asInstanceOf[elastic.Interface[WriteAddressChannel]],
        responseBuffer.s_aw
      )
      val connectAwOut = new elastic.Connect(
        responseBuffer.m_aw,
        slave.aw.asInstanceOf[elastic.Interface[WriteAddressChannel]]
      )
      val connectW = new elastic.Connect(afterPayload.w, slave.w)
      val connectBOut = new elastic.Connect(slave.b, responseBuffer.m_b)
      val connectBIn = new elastic.Connect(responseBuffer.s_b, afterPayload.b)
    } else {
      connectWrite(afterPayload, slave)
    }
  }

  private def implConfigured(cfg: ConnectConfig)(implicit si: SourceInfo): Unit = {
    val masterCfg =
      master.cfg.copy(wId = 0, wAddr = 0, read = false, write = false, axi3Compat = false)
    val slaveCfg =
      slave.cfg.copy(wId = 0, wAddr = 0, read = false, write = false, axi3Compat = false)

    require_(
      masterCfg == slaveCfg,
      "configurations do not match after normalization",
      Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
    )

    reportConfigDifferences(master, slave, cfg)
    insertSimulationChecks(master, slave, cfg)

    val readCommon = master.cfg.read && slave.cfg.read
    val writeCommon = master.cfg.write && slave.cfg.write
    val masterNeedsTieOff =
      (master.cfg.read && !slave.cfg.read) || (master.cfg.write && !slave.cfg.write)
    val slaveNeedsTieOff =
      (!master.cfg.read && slave.cfg.read) || (!master.cfg.write && slave.cfg.write)
    val masterWillTieOff = cfg.tieOffMaster && masterNeedsTieOff
    val slaveWillTieOff = cfg.tieOffSlave && slaveNeedsTieOff

    require_(
      readCommon || writeCommon || masterWillTieOff || slaveWillTieOff,
      "master and slave share no channel group",
      Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
    )

    if (cfg.warnReadWriteMismatch && masterWillTieOff && slaveWillTieOff)
      warn(
        "both master and slave have channel groups tied off",
        Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
      )
    else if (cfg.warnReadWriteMismatch && (masterWillTieOff || slaveWillTieOff))
      warn(
        "read/write channel groups differ",
        Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
      )

    if (readCommon) connectReadBuffered(master, slave, cfg)
    else {
      if (master.cfg.read && cfg.tieOffMaster) tieOffReadMaster(master)
      if (slave.cfg.read && cfg.tieOffSlave) tieOffReadSlave(slave)
    }

    if (writeCommon) connectWriteBuffered(master, slave, cfg)
    else {
      if (master.cfg.write && cfg.tieOffMaster) tieOffWriteMaster(master)
      if (slave.cfg.write && cfg.tieOffSlave) tieOffWriteSlave(slave)
    }
  }

  private def impl()(implicit si: SourceInfo): Unit =
    tracking.withContainer(this) {
      cfgOption match {
        case Some(cfg) => implConfigured(cfg)
        case None      => implStrict()
      }
    }

  impl()
}

private object connect {
  def apply(
      master: Interface,
      slave: Interface
  )(implicit si: SourceInfo): Unit = {
    val connect0 = new ConnectImpl(master, slave, None)(si)
  }

  def apply(
      master: Interface,
      slave: Interface,
      cfg: ConnectConfig
  )(implicit si: SourceInfo): Unit = {
    val connect0 = new ConnectImpl(master, slave, Some(cfg))(si)
  }
}

/** AXI full connection options.
  *
  * Buffers are per AXI channel. W buffering uses a payload buffer and consumes AW buffering as its
  * address capacity. Simulation checks only emit hardware when the checked condition is relevant.
  */
case class ConnectConfig(
    name: String = "connect",
    arBuffer: Int = 0,
    rBuffer: Int = 0,
    awBuffer: Int = 0,
    wBuffer: Int = 0,
    bBuffer: Int = 0,
    tieOffMaster: Boolean = true,
    tieOffSlave: Boolean = true,
    warnReadWriteMismatch: Boolean = true,
    warnAxi3Compat: Boolean = true,
    warnIdWidth: Boolean = true,
    warnAddrWidth: Boolean = true,
    simCheckAxi3Compat: SimulationCheck = SimulationCheck.Default,
    simCheckIdWidth: SimulationCheck = SimulationCheck.Default,
    simCheckAddrWidth: SimulationCheck = SimulationCheck.Default
) {
  require(arBuffer >= 0)
  require(rBuffer >= 0)
  require(awBuffer >= 0)
  require(wBuffer >= 0)
  require(bBuffer >= 0)
  require(rBuffer == 0 || rBuffer >= 2)
  require(bBuffer == 0 || bBuffer >= 2)
}

trait ConnectOp {
  /* implicit class names should be different, otherwise shadowed */
  implicit class axi4_full_connect_op(master: Interface)(implicit si: SourceInfo) {
    def :=>(slave: Interface) = {
      chext.amba.axi4.full.connect(master, slave)
    }

    def connect(slave: Interface, cfg: ConnectConfig = ConnectConfig()) = {
      chext.amba.axi4.full.connect(master, slave, cfg)
    }
  }
}

object ConnectOp extends ConnectOp
