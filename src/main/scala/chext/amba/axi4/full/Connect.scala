package chext.amba.axi4.full

import chisel3._
import chext.elastic
import chisel3.experimental.prefix
import chisel3.experimental.SourceInfo

import chext.amba.axi4

import chext.util.SimulationCheck

import chext.tracking
import tracking.uniquePrefix

final class Connect(
    master: Interface,
    slave: Interface,
    cfgOption: Option[ConnectConfig]
)(si_ : SourceInfo)
    extends tracking.Container {
  val sourceInfo: SourceInfo = si_
  def tpe: String = "Axi4f_Connect"
  def namePrefix: String = "connect"

  private case class Diagnostic(
      isError: Boolean,
      message: String,
      lines: Seq[String] = Seq()
  ) {
    def kind: String = if (isError) "error" else "warning"
  }

  private def error(message: String, lines: Seq[String] = Seq()): Diagnostic =
    Diagnostic(isError = true, message, lines)

  private def warning(message: String, lines: Seq[String] = Seq()): Diagnostic =
    Diagnostic(isError = false, message, lines)

  private def emitDiagnostics(diagnostics: Seq[Diagnostic]): Unit = {
    if (diagnostics.nonEmpty) {
      val prefix = "axi4.full.Connect"
      val source = sourceInfo.makeMessage((x) => x)
      println(f"$prefix : diagnostics $source")

      diagnostics.foreach { diagnostic =>
        println(f"$prefix : ${diagnostic.kind}: ${diagnostic.message}")
        diagnostic.lines.foreach { line => println(f"$prefix :   $line") }
      }

      configLines.foreach { line => println(f"$prefix : $line") }
      interfaceSourceLines.foreach { line => println(f"$prefix : $line") }

      if (diagnostics.exists(_.isError))
        throw new IllegalArgumentException("axi4.full.Connect failed")
    }
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

  private def configLines: Seq[String] =
    Seq(
      f"master.cfg = ${master.cfg.prettyString}",
      f"slave.cfg = ${slave.cfg.prettyString}"
    )

  private def fieldMismatch[T](field: String, masterValue: T, slaveValue: T): Option[String] =
    Option.when(masterValue != slaveValue)(f"$field: master=$masterValue, slave=$slaveValue")

  private def cfgMismatches(
      masterCfg: axi4.Config,
      slaveCfg: axi4.Config,
      includeWidthExceptions: Boolean,
      includeReadWrite: Boolean,
      includeAxi3Compat: Boolean
  ): Seq[String] = {
    val widthMismatches =
      if (includeWidthExceptions)
        Seq(
          fieldMismatch("wId", masterCfg.wId, slaveCfg.wId),
          fieldMismatch("wAddr", masterCfg.wAddr, slaveCfg.wAddr)
        )
      else Seq.empty

    val channelMismatches =
      if (includeReadWrite)
        Seq(
          fieldMismatch("read", masterCfg.read, slaveCfg.read),
          fieldMismatch("write", masterCfg.write, slaveCfg.write)
        )
      else Seq.empty

    val axi3Mismatch =
      if (includeAxi3Compat)
        Seq(fieldMismatch("axi3Compat", masterCfg.axi3Compat, slaveCfg.axi3Compat))
      else Seq.empty

    (
      widthMismatches ++
        Seq(
          fieldMismatch("wData", masterCfg.wData, slaveCfg.wData),
          fieldMismatch("lite", masterCfg.lite, slaveCfg.lite)
        ) ++
        channelMismatches ++
        Seq(
          fieldMismatch("hasLock", masterCfg.hasLock, slaveCfg.hasLock),
          fieldMismatch("hasCache", masterCfg.hasCache, slaveCfg.hasCache),
          fieldMismatch("hasProt", masterCfg.hasProt, slaveCfg.hasProt),
          fieldMismatch("hasQos", masterCfg.hasQos, slaveCfg.hasQos),
          fieldMismatch("hasRegion", masterCfg.hasRegion, slaveCfg.hasRegion)
        ) ++
        axi3Mismatch ++
        Seq(
          fieldMismatch("wUserAR", masterCfg.wUserAR, slaveCfg.wUserAR),
          fieldMismatch("wUserR", masterCfg.wUserR, slaveCfg.wUserR),
          fieldMismatch("wUserAW", masterCfg.wUserAW, slaveCfg.wUserAW),
          fieldMismatch("wUserW", masterCfg.wUserW, slaveCfg.wUserW),
          fieldMismatch("wUserB", masterCfg.wUserB, slaveCfg.wUserB)
        )
    ).flatten
  }

  private def sidebandMismatches(masterCfg: axi4.Config, slaveCfg: axi4.Config): Seq[String] =
    Seq(
      fieldMismatch("wLock", masterCfg.wLock, slaveCfg.wLock),
      fieldMismatch("wCache", masterCfg.wCache, slaveCfg.wCache),
      fieldMismatch("wProt", masterCfg.wProt, slaveCfg.wProt),
      fieldMismatch("wQos", masterCfg.wQos, slaveCfg.wQos),
      fieldMismatch("wRegion", masterCfg.wRegion, slaveCfg.wRegion),
      fieldMismatch("wUserAR", masterCfg.wUserAR, slaveCfg.wUserAR),
      fieldMismatch("wUserR", masterCfg.wUserR, slaveCfg.wUserR),
      fieldMismatch("wUserAW", masterCfg.wUserAW, slaveCfg.wUserAW),
      fieldMismatch("wUserW", masterCfg.wUserW, slaveCfg.wUserW),
      fieldMismatch("wUserB", masterCfg.wUserB, slaveCfg.wUserB)
    ).flatten

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
    val diagnostics =
      strictDiagnostics(master, slave) ++
        commonChannelDiagnostics(master, slave, tieOffMaster = true, tieOffSlave = true)
    emitDiagnostics(diagnostics)
  }

  private def strictDiagnostics(master: Interface, slave: Interface): Seq[Diagnostic] = {
    val configMismatch = cfgMismatches(
      master.cfg,
      slave.cfg,
      includeWidthExceptions = false,
      includeReadWrite = false,
      includeAxi3Compat = false
    )

    Seq(
      Option.when(master.cfg.wId > slave.cfg.wId)(
        error(
          "master ID width is wider than slave ID width",
          Seq(f"wId: master=${master.cfg.wId}, slave=${slave.cfg.wId}")
        )
      ),
      Option.when(!(!master.cfg.axi3Compat && !slave.cfg.axi3Compat || master.cfg.axi3Compat))(
        error(
          "AXI3 compatibility is not strict-connect compatible",
          Seq(
            f"axi3Compat: master=${master.cfg.axi3Compat}, slave=${slave.cfg.axi3Compat}"
          )
        )
      ),
      Option.when(configMismatch.nonEmpty)(
        error("configurations differ in strict fields", configMismatch)
      )
    ).flatten
  }

  private def commonChannelDiagnostics(
      master: Interface,
      slave: Interface,
      tieOffMaster: Boolean,
      tieOffSlave: Boolean
  ): Seq[Diagnostic] = {
    val readCommon = master.cfg.read && slave.cfg.read
    val writeCommon = master.cfg.write && slave.cfg.write
    val masterNeedsTieOff =
      (master.cfg.read && !slave.cfg.read) || (master.cfg.write && !slave.cfg.write)
    val slaveNeedsTieOff =
      (!master.cfg.read && slave.cfg.read) || (!master.cfg.write && slave.cfg.write)
    val masterWillTieOff = tieOffMaster && masterNeedsTieOff
    val slaveWillTieOff = tieOffSlave && slaveNeedsTieOff

    Option
      .when(!(readCommon || writeCommon || masterWillTieOff || slaveWillTieOff))(
        error(
          "master and slave share no connected or tied-off channel group",
          Seq(
            f"master.read=${master.cfg.read}, master.write=${master.cfg.write}",
            f"slave.read=${slave.cfg.read}, slave.write=${slave.cfg.write}",
            f"tieOffMaster=$tieOffMaster, tieOffSlave=$tieOffSlave"
          )
        )
      )
      .toSeq
  }

  private def readWriteMismatchWarnings(cfg: ConnectConfig): Seq[Diagnostic] = {
    val masterNeedsTieOff =
      (master.cfg.read && !slave.cfg.read) || (master.cfg.write && !slave.cfg.write)
    val slaveNeedsTieOff =
      (!master.cfg.read && slave.cfg.read) || (!master.cfg.write && slave.cfg.write)
    val masterWillTieOff = cfg.tieOffMaster && masterNeedsTieOff
    val slaveWillTieOff = cfg.tieOffSlave && slaveNeedsTieOff

    if (cfg.warnReadWriteMismatch && masterWillTieOff && slaveWillTieOff)
      Seq(warning("both master and slave have channel groups tied off"))
    else if (cfg.warnReadWriteMismatch && (masterWillTieOff || slaveWillTieOff))
      Seq(warning("read/write channel groups differ"))
    else Seq.empty
  }

  private def implStrict()(implicit si: SourceInfo): Unit = {
    requireStrictCompatibility(master, slave)

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
        "axi4.full.Connect: ARID upper bits are not zero"
      )
      checkUpperZero(
        master.ar.$bits.addr,
        slave.cfg.wAddr,
        master.ar.$valid,
        cfg.simCheckAddrWidth,
        "axi4.full.Connect: ARADDR upper bits are not zero"
      )

      if (!master.cfg.axi3Compat && slave.cfg.axi3Compat)
        cfg.simCheckAxi3Compat(
          !master.ar.$valid || master.ar.$bits.len < 16.U,
          withInterfaceSourceInfo("axi4.full.Connect: ARLEN is not AXI3-compatible")
        )

      checkUpperZero(
        slave.r.$bits.id,
        master.cfg.wId,
        slave.r.$valid,
        cfg.simCheckIdWidth,
        "axi4.full.Connect: RID upper bits are not zero"
      )
    }

    if (master.cfg.write && slave.cfg.write) {
      checkUpperZero(
        master.aw.$bits.id,
        slave.cfg.wId,
        master.aw.$valid,
        cfg.simCheckIdWidth,
        "axi4.full.Connect: AWID upper bits are not zero"
      )
      checkUpperZero(
        master.aw.$bits.addr,
        slave.cfg.wAddr,
        master.aw.$valid,
        cfg.simCheckAddrWidth,
        "axi4.full.Connect: AWADDR upper bits are not zero"
      )

      if (!master.cfg.axi3Compat && slave.cfg.axi3Compat)
        cfg.simCheckAxi3Compat(
          !master.aw.$valid || master.aw.$bits.len < 16.U,
          withInterfaceSourceInfo("axi4.full.Connect: AWLEN is not AXI3-compatible")
        )

      checkUpperZero(
        slave.b.$bits.id,
        master.cfg.wId,
        slave.b.$valid,
        cfg.simCheckIdWidth,
        "axi4.full.Connect: BID upper bits are not zero"
      )
    }
  }

  private def reportConfigDifferences(
      master: Interface,
      slave: Interface,
      cfg: ConnectConfig
  ): Seq[Diagnostic] = {
    val configMismatch = cfgMismatches(
      master.cfg,
      slave.cfg,
      includeWidthExceptions = false,
      includeReadWrite = false,
      includeAxi3Compat = false
    )
    val sideband = sidebandMismatches(master.cfg, slave.cfg)

    Option
      .when(configMismatch.nonEmpty)(
        error("configurations differ in unsupported fields", configMismatch)
      )
      .toSeq ++
      Option
        .when(cfg.warnSideband && sideband.nonEmpty)(
          warning("sideband widths differ", sideband)
        )
        .toSeq ++
      Seq(
        Option.when(cfg.warnIdWidth && master.cfg.wId > slave.cfg.wId)(
          warning(
            "master ID width is wider than slave ID width",
            Seq(f"wId: master=${master.cfg.wId}, slave=${slave.cfg.wId}")
          )
        ),
        Option.when(cfg.warnAddrWidth && master.cfg.wAddr > slave.cfg.wAddr)(
          warning(
            "master address width is wider than slave address width",
            Seq(f"wAddr: master=${master.cfg.wAddr}, slave=${slave.cfg.wAddr}")
          )
        ),
        Option.when(
          cfg.warnAxi3Compat &&
            !(!master.cfg.axi3Compat && !slave.cfg.axi3Compat || master.cfg.axi3Compat)
        )(
          warning(
            "AXI3 compatibility differs",
            Seq(
              f"axi3Compat: master=${master.cfg.axi3Compat}, slave=${slave.cfg.axi3Compat}"
            )
          )
        )
      ).flatten
  }

  private def connectReadBuffered(
      master: Interface,
      slave: Interface,
      cfg: ConnectConfig
  )(implicit si: SourceInfo): Unit = {
    if (cfg.rBuffer > 0) {
      val responseBuffer =
        Module(
          new ReadResponseBuffer(
            master.cfg,
            ReadResponseBufferConfig(cfg.rBuffer)
          )
        )
      val arSource =
        if (cfg.arBuffer > 0) elastic.SourceBuffer(master.ar, cfg.arBuffer, name = "arBuffer")
        else master.ar
      val connectArIn =
        new elastic.Connect(
          arSource.asInstanceOf[elastic.Interface[ReadAddressChannel]],
          responseBuffer.s_ar
        )
      val connectArOut = new elastic.Connect(
        responseBuffer.m_ar,
        slave.ar.asInstanceOf[elastic.Interface[ReadAddressChannel]]
      )
      val connectROut = new elastic.Connect(slave.r, responseBuffer.m_r)
      val connectRIn = new elastic.Connect(responseBuffer.s_r, master.r)
    } else {
      val arSource =
        if (cfg.arBuffer > 0) elastic.SourceBuffer(master.ar, cfg.arBuffer, name = "arBuffer")
        else master.ar
      val connectAr = new elastic.Connect(arSource, slave.ar)
      val connectR = new elastic.Connect(slave.r, master.r)
    }
  }

  private def connectWriteBuffered(
      master: Interface,
      slave: Interface,
      cfg: ConnectConfig
  )(implicit si: SourceInfo): Unit = {
    def connectWriteResponse(
        awSource: elastic.Interface[AddressChannel],
        wSource: elastic.Interface[WriteDataChannel]
    ): Unit = {
      if (cfg.bBuffer > 0) {
        val responseBuffer =
          Module(
            new WriteResponseBuffer(
              master.cfg,
              WriteResponseBufferConfig(cfg.bBuffer)
            )
          )
        val connectAwRespIn = new elastic.Connect(
          awSource.asInstanceOf[elastic.Interface[WriteAddressChannel]],
          responseBuffer.s_aw
        )
        val connectAwRespOut = new elastic.Connect(
          responseBuffer.m_aw,
          slave.aw.asInstanceOf[elastic.Interface[WriteAddressChannel]]
        )
        val connectW = new elastic.Connect(wSource, slave.w)
        val connectBOut = new elastic.Connect(slave.b, responseBuffer.m_b)
        val connectBIn = new elastic.Connect(responseBuffer.s_b, master.b)
      } else {
        val connectAw = new elastic.Connect(awSource, slave.aw)
        val connectW = new elastic.Connect(wSource, slave.w)
        val connectB = new elastic.Connect(slave.b, master.b)
      }
    }

    if (cfg.wBuffer > 0) {
      val payloadBuffer =
        Module(
          new WritePayloadBuffer(
            master.cfg,
            WritePayloadBufferConfig(
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
      connectWriteResponse(payloadBuffer.m_aw, payloadBuffer.m_w)
    } else if (cfg.awBuffer > 0) {
      connectWriteResponse(
        elastic.SourceBuffer(master.aw, cfg.awBuffer, name = "awBuffer"),
        master.w
      )
    } else {
      connectWriteResponse(master.aw, master.w)
    }
  }

  private def implConfigured(cfg: ConnectConfig)(implicit si: SourceInfo): Unit = {
    val readCommon = master.cfg.read && slave.cfg.read
    val writeCommon = master.cfg.write && slave.cfg.write

    val diagnostics =
      reportConfigDifferences(master, slave, cfg) ++
        commonChannelDiagnostics(master, slave, cfg.tieOffMaster, cfg.tieOffSlave) ++
        readWriteMismatchWarnings(cfg)

    emitDiagnostics(diagnostics)
    insertSimulationChecks(master, slave, cfg)

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

  private def impl(): Unit =
    tracking.withContainer(this) {
      implicit val si: SourceInfo = sourceInfo
      cfgOption match {
        case Some(cfg) => implConfigured(cfg)
        case None      => implStrict()
      }
    }

  impl()
}

/** AXI full connection options.
  *
  * Buffers are per AXI channel. W buffering uses a payload buffer and consumes AW buffering as its
  * address capacity. Simulation checks only emit hardware when the checked condition is relevant.
  */
case class ConnectConfig(
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
    warnSideband: Boolean = true,
    simCheckAxi3Compat: SimulationCheck = SimulationCheck.Default,
    simCheckIdWidth: SimulationCheck = SimulationCheck.Default,
    simCheckAddrWidth: SimulationCheck = SimulationCheck.Default
) {
  require(arBuffer >= 0)
  require(rBuffer >= 0)
  require(awBuffer >= 0)
  require(wBuffer >= 0)
  require(bBuffer >= 0)
}

trait ConnectOp {
  /* implicit class names should be different, otherwise shadowed */
  implicit class axi4_full_connect_op(master: Interface) {
    def :=>(slave: Interface)(implicit si: SourceInfo) = {
      uniquePrefix("axi4f_connect") {
        new Connect(master, slave, None)(si)
      }
    }

    def connect(slave: Interface, cfg: ConnectConfig = ConnectConfig())(implicit
        si: SourceInfo
    ) = {
      uniquePrefix("axi4f_connect") {
        new Connect(master, slave, Some(cfg))(si)
      }
    }
  }

  implicit class axi4_full_connect_seq_op(masters: Seq[Interface]) {
    def :=>(slaves: Seq[Interface])(implicit si: SourceInfo): Unit = {
      require(
        masters.length == slaves.length,
        f"master/slave sequence length mismatch: ${masters.length} != ${slaves.length}"
      )

      masters.zip(slaves).zipWithIndex.foreach { case ((master, slave), index) =>
        prefix(index.toString) {
          master :=> slave
        }
      }
    }

    def connect(slaves: Seq[Interface], cfg: ConnectConfig)(implicit si: SourceInfo): Unit = {
      require(
        masters.length == slaves.length,
        f"master/slave sequence length mismatch: ${masters.length} != ${slaves.length}"
      )

      masters.zip(slaves).zipWithIndex.foreach { case ((master, slave), index) =>
        prefix(index.toString) {
          master.connect(slave, cfg)
        }
      }
    }

    def connect(slaves: Seq[Interface])(implicit si: SourceInfo): Unit =
      connect(slaves, ConnectConfig())
  }
}

object ConnectOp extends ConnectOp
