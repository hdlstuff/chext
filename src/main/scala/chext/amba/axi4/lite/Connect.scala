package chext.amba.axi4.lite

import chisel3._
import chisel3.experimental.SourceInfo

import chext.amba.axi4
import chext.elastic
import chext.tracking
import chext.tracking.uniquePrefix
import chext.util.SimulationCheck

final class Connect(
    master: Interface,
    slave: Interface,
    cfgOption: Option[ConnectConfig]
)(si_ : SourceInfo)
    extends tracking.Container {
  val sourceInfo: SourceInfo = si_
  def tpe: String = "Axi4l_Connect"
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
      val prefix = "axi4.lite.Connect"
      val source = sourceInfo.makeMessage((x) => x)
      println(f"$prefix : diagnostics $source")

      diagnostics.foreach { diagnostic =>
        println(f"$prefix : ${diagnostic.kind}: ${diagnostic.message}")
        diagnostic.lines.foreach { line => println(f"$prefix :   $line") }
      }

      configLines.foreach { line => println(f"$prefix : $line") }
      interfaceSourceLines.foreach { line => println(f"$prefix : $line") }

      if (diagnostics.exists(_.isError))
        throw new IllegalArgumentException("axi4.lite.Connect failed")
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
      includeAddress: Boolean,
      includeReadWrite: Boolean
  ): Seq[String] = {
    val addressMismatch =
      if (includeAddress)
        Seq(fieldMismatch("wAddr", masterCfg.wAddr, slaveCfg.wAddr))
      else Seq.empty

    val channelMismatches =
      if (includeReadWrite)
        Seq(
          fieldMismatch("read", masterCfg.read, slaveCfg.read),
          fieldMismatch("write", masterCfg.write, slaveCfg.write)
        )
      else Seq.empty

    (
      addressMismatch ++
        Seq(
          fieldMismatch("wData", masterCfg.wData, slaveCfg.wData),
          fieldMismatch("lite", masterCfg.lite, slaveCfg.lite),
          fieldMismatch("hasProt", masterCfg.hasProt, slaveCfg.hasProt)
        ) ++
        channelMismatches
    ).flatten
  }

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

  private def strictDiagnostics(master: Interface, slave: Interface): Seq[Diagnostic] = {
    val configMismatch = cfgMismatches(
      master.cfg,
      slave.cfg,
      includeAddress = false,
      includeReadWrite = false
    )

    Option
      .when(configMismatch.nonEmpty)(
        error("configurations differ in strict fields", configMismatch)
      )
      .toSeq
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
    val diagnostics =
      strictDiagnostics(master, slave) ++
        commonChannelDiagnostics(master, slave, tieOffMaster = true, tieOffSlave = true)
    emitDiagnostics(diagnostics)

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
    if (master.cfg.read && slave.cfg.read)
      checkUpperZero(
        master.ar.$bits.addr,
        slave.cfg.wAddr,
        master.ar.$valid,
        cfg.simCheckAddrWidth,
        "axi4.lite.Connect: ARADDR upper bits are not zero"
      )

    if (master.cfg.write && slave.cfg.write)
      checkUpperZero(
        master.aw.$bits.addr,
        slave.cfg.wAddr,
        master.aw.$valid,
        cfg.simCheckAddrWidth,
        "axi4.lite.Connect: AWADDR upper bits are not zero"
      )
  }

  private def reportConfigDifferences(
      master: Interface,
      slave: Interface,
      cfg: ConnectConfig
  ): Seq[Diagnostic] = {
    val configMismatch = cfgMismatches(
      master.cfg,
      slave.cfg,
      includeAddress = false,
      includeReadWrite = false
    )

    Option
      .when(configMismatch.nonEmpty)(
        error("configurations differ in unsupported fields", configMismatch)
      )
      .toSeq ++
      Seq(
        Option.when(cfg.warnAddrWidth && master.cfg.wAddr > slave.cfg.wAddr)(
          warning(
            "master address width is wider than slave address width",
            Seq(f"wAddr: master=${master.cfg.wAddr}, slave=${slave.cfg.wAddr}")
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
      val connectArIn = new elastic.Connect(arSource, responseBuffer.s_ar)
      val connectArOut = new elastic.Connect(responseBuffer.m_ar, slave.ar)
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
        val connectAwRespIn = new elastic.Connect(awSource, responseBuffer.s_aw)
        val connectAwRespOut = new elastic.Connect(responseBuffer.m_aw, slave.aw)
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
      val connectAwIn = new elastic.Connect(master.aw, payloadBuffer.s_aw)
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

/** AXI4-Lite connection options.
  *
  * Buffers are per AXI-Lite channel. Address widths may differ; optional simulation checks can
  * validate that truncated upper address bits are zero.
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
    warnAddrWidth: Boolean = true,
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
  implicit class axi4_lite_connect_op(master: Interface) {
    def :=>(slave: Interface)(implicit si: SourceInfo): Unit = {
      uniquePrefix("axi4l_connect") {
        new Connect(master, slave, None)(si)
      }
    }

    def connect(slave: Interface, cfg: ConnectConfig = ConnectConfig())(implicit
        si: SourceInfo
    ): Unit = {
      uniquePrefix("axi4l_connect") {
        new Connect(master, slave, Some(cfg))(si)
      }
    }
  }
}

object ConnectOp extends ConnectOp
