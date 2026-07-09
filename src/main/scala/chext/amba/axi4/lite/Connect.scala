package chext.amba.axi4.lite

import chisel3._
import chisel3.experimental.prefix
import chisel3.experimental.SourceInfo

import chext.amba.axi4
import axi4.{Diagnostic, DiagnosticReporter}
import chext.elastic
import chext.tracking
import chext.tracking.uniquePrefix
import chext.util.SimulationCheck

final class Connect(
    master: Interface,
    slave: Interface,
    cfgOption: Option[ConnectConfig]
)(implicit si_ : SourceInfo)
    extends tracking.Container {
  val sourceInfo: SourceInfo = si_
  def tpe: String = "Axi4l_Connect"
  def namePrefix: String = "axi4lConnect"

  private def error(message: String, lines: Seq[String] = Seq()): Diagnostic =
    Diagnostic.error(message, lines)

  private def warning(message: String, lines: Seq[String] = Seq()): Diagnostic =
    Diagnostic.warning(message, lines)

  private def emitDiagnostics(diagnostics: Seq[Diagnostic]): Unit = {
    DiagnosticReporter.emit(
      "axi4.lite.Connect",
      sourceInfo,
      diagnostics,
      configLines ++ interfaceSourceLines,
      "axi4.lite.Connect failed"
    )
  }

  private def sourceInfoString(interface: Interface): String =
    chext.util.sourceInfoToString(interface.sourceInfo)

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

  private def tieOffReadMaster(master: Interface): Unit = {
    val nullSinkAr = new elastic.NullSink(master.ar)
    val nullSourceR = new elastic.NullSource(master.r)
  }

  private def tieOffReadSlave(slave: Interface): Unit = {
    val nullSourceAr = new elastic.NullSource(slave.ar)
    val nullSinkR = new elastic.NullSink(slave.r)
  }

  private def tieOffWriteMaster(master: Interface): Unit = {
    val nullSinkAw = new elastic.NullSink(master.aw)
    val nullSinkW = new elastic.NullSink(master.w)
    val nullSourceB = new elastic.NullSource(master.b)
  }

  private def tieOffWriteSlave(slave: Interface): Unit = {
    val nullSourceAw = new elastic.NullSource(slave.aw)
    val nullSourceW = new elastic.NullSource(slave.w)
    val nullSinkB = new elastic.NullSink(slave.b)
  }

  private def connectRead(master: Interface, slave: Interface): Unit = {
    val connectAr = new elastic.Connect(master.ar, slave.ar)
    val connectR = new elastic.Connect(slave.r, master.r)
  }

  private def connectWrite(master: Interface, slave: Interface): Unit = {
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

  private def implStrict(): Unit = {
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

  private def implConfigured(cfg: ConnectConfig): Unit = {
    val readCommon = master.cfg.read && slave.cfg.read
    val writeCommon = master.cfg.write && slave.cfg.write

    val diagnostics =
      reportConfigDifferences(master, slave, cfg) ++
        commonChannelDiagnostics(master, slave, cfg.tieOffMaster, cfg.tieOffSlave) ++
        readWriteMismatchWarnings(cfg)

    emitDiagnostics(diagnostics)
    insertSimulationChecks(master, slave, cfg)

    if (readCommon) connectRead(master, slave)
    else {
      if (master.cfg.read && cfg.tieOffMaster) tieOffReadMaster(master)
      if (slave.cfg.read && cfg.tieOffSlave) tieOffReadSlave(slave)
    }

    if (writeCommon) connectWrite(master, slave)
    else {
      if (master.cfg.write && cfg.tieOffMaster) tieOffWriteMaster(master)
      if (slave.cfg.write && cfg.tieOffSlave) tieOffWriteSlave(slave)
    }
  }

  private def impl(): Unit =
    tracking.withContainer(this) {
      cfgOption match {
        case Some(cfg) => implConfigured(cfg)
        case None      => implStrict()
      }
    }

  impl()
}

/** AXI4-Lite connection options.
  *
  * Address widths may differ; optional simulation checks can validate that truncated upper address
  * bits are zero.
  */
case class ConnectConfig(
    tieOffMaster: Boolean = true,
    tieOffSlave: Boolean = true,
    warnReadWriteMismatch: Boolean = true,
    warnAddrWidth: Boolean = true,
    simCheckAddrWidth: SimulationCheck = SimulationCheck.Default
)

trait ConnectOp {
  private val require_ = chext.util.Require.inferred()

  /* implicit class names should be different, otherwise shadowed */
  implicit class axi4_lite_connect_op(master: Interface) {
    def :=>(slave: Interface)(implicit si: SourceInfo): Unit = {
      uniquePrefix("axi4lConnect") {
        new Connect(master, slave, None)
      }
    }

    def connect(slave: Interface, cfg: ConnectConfig = ConnectConfig())(implicit
        si: SourceInfo
    ): Unit = {
      uniquePrefix("axi4lConnect") {
        new Connect(master, slave, Some(cfg))
      }
    }
  }

  implicit class axi4_lite_connect_seq_op(masters: Seq[Interface]) {
    def :=>(slaves: Seq[Interface])(implicit si: SourceInfo): Unit = {
      require_(
        masters.length == slaves.length,
        f"master/slave sequence length mismatch: ${masters.length} != ${slaves.length}"
      )

      uniquePrefix("axi4lConnectMany") {
        masters.zip(slaves).zipWithIndex.foreach { case ((master, slave), index) =>
          prefix(index.toString) {
            new Connect(master, slave, None)
          }
        }
      }
    }

    def connect(slaves: Seq[Interface], cfg: ConnectConfig)(implicit si: SourceInfo): Unit = {
      require_(
        masters.length == slaves.length,
        f"master/slave sequence length mismatch: ${masters.length} != ${slaves.length}"
      )

      uniquePrefix("axi4lConnectMany") {
        masters.zip(slaves).zipWithIndex.foreach { case ((master, slave), index) =>
          prefix(index.toString) {
            new Connect(master, slave, Some(cfg))
          }
        }
      }
    }
  }
}

object ConnectOp extends ConnectOp
