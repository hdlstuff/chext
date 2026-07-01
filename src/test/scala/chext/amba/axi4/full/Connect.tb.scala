package chext.amba.axi4.full

import chisel3._
import circt.stage.ChiselStage
import java.io.PrintWriter
import java.nio.file.{Files, Path}

import chext.amba.axi4
import chext.amba.axi4.full.components.CreditBufferConfig
import chext.amba.axi4.full.ConnectOp._
import chext.util.SimulationCheck

private object ConnectDiagEmit {
  def apply(gen: => RawModule): String =
    ChiselStage.emitSystemVerilog(
      gen,
      args = Array("--no-source-info"),
      firtoolOpts = Array(
        "-disable-all-randomization",
        "-strip-debug-info",
        "-default-layer-specialization=enable"
      )
    )
}

private class StrictMismatchTop extends Module {
  val master = IO(Slave(axi4.Config(wId = 4, wAddr = 24, wData = 32, wUserAR = 1)))
  val slave = IO(
    Master(
      axi4.Config(
        wId = 2,
        wAddr = 32,
        wData = 64,
        hasCache = false,
        wUserAR = 3
      )
    )
  )

  master :=> slave
}

private class ConfiguredSidebandWarningTop extends Module {
  val master = IO(Slave(axi4.Config(axi3Compat = true)))
  val slave = IO(Master(axi4.Config()))

  master.connect(slave, ConnectConfig())
}

private class IdSimulationCheckTop extends Module {
  val master = IO(Slave(axi4.Config(wId = 4)))
  val slave = IO(Master(axi4.Config(wId = 2)))

  master.connect(
    slave,
    ConnectConfig(
      simCheckIdWidth = SimulationCheck.Printf
    )
  )
}

private class Axi3SimulationCheckTop extends Module {
  val master = IO(Slave(axi4.Config()))
  val slave = IO(Master(axi4.Config(axi3Compat = true)))

  master.connect(
    slave,
    ConnectConfig(
      simCheckAxi3Compat = SimulationCheck.Printf
    )
  )
}

private class BufferedConnectTop extends Module {
  val cfg = axi4.Config()
  val master = IO(Slave(cfg))
  val slave = IO(Master(cfg))

  val creditBuffer = Module(
    new components.CreditBuffer(
      CreditBufferConfig(
        axiCfg = cfg,
        rBuffer = 2,
        awBuffer = 2,
        wBuffer = 4,
        bBuffer = 2
      )
    )
  )

  master :=> creditBuffer.s_axi
  creditBuffer.m_axi :=> slave
}

private class TieOffWarningTop extends Module {
  val master = IO(Slave(axi4.Config(read = true, write = false)))
  val slave = IO(Master(axi4.Config(read = false, write = true)))

  master.connect(slave, ConnectConfig())
}

object ConnectDiag_Tb extends App {
  private val outputDir = Path.of("output", "connect_diag")
  Files.createDirectories(outputDir)

  private case class TestCase(
      name: String,
      fileName: String,
      shouldPass: Boolean,
      gen: () => RawModule,
      contains: Seq[String] = Seq()
  )

  private val cases = Seq(
    TestCase(
      "strict mismatch diagnostics",
      "strict_mismatch_diagnostics.sv",
      shouldPass = false,
      () => new StrictMismatchTop,
      contains = Seq("master ID width is wider", "wData", "wUserAR")
    ),
    TestCase(
      "configured sideband warning",
      "configured_sideband_warning.sv",
      shouldPass = true,
      () => new ConfiguredSidebandWarningTop,
      contains = Seq("ConfiguredSidebandWarningTop")
    ),
    TestCase(
      "ID simulation check",
      "id_simulation_check.sv",
      shouldPass = true,
      () => new IdSimulationCheckTop,
      contains = Seq("ARID upper bits are not zero", "AWID upper bits are not zero")
    ),
    TestCase(
      "AXI3 simulation check",
      "axi3_simulation_check.sv",
      shouldPass = true,
      () => new Axi3SimulationCheckTop,
      contains = Seq("ARLEN is not AXI3-compatible", "AWLEN is not AXI3-compatible")
    ),
    TestCase(
      "buffered connect generation",
      "buffered_connect_generation.sv",
      shouldPass = true,
      () => new BufferedConnectTop,
      contains = Seq("ReadResponseBuffer", "WriteResponseBuffer", "WritePayloadBuffer")
    ),
    TestCase(
      "read/write tie-off diagnostics",
      "read_write_tie_off_diagnostics.sv",
      shouldPass = true,
      () => new TieOffWarningTop,
      contains = Seq("TieOffWarningTop")
    )
  )

  private def preview(sv: String): String =
    sv
      .linesIterator
      .filter(line =>
        line.contains("module ") ||
          line.contains("upper bits") ||
          line.contains("AXI3-compatible")
      )
      .take(10)
      .mkString("\n")

  cases.foreach { testCase =>
    println(s"[connect-diag] running: ${testCase.name}")
    try {
      val sv = ConnectDiagEmit(testCase.gen())

      if (!testCase.shouldPass)
        throw new RuntimeException(s"case was expected to fail: ${testCase.name}")

      testCase.contains.foreach { needle =>
        require(
          sv.contains(needle),
          s"case '${testCase.name}' did not generate expected text: $needle"
        )
      }

      val outputPath = outputDir.resolve(testCase.fileName)
      val pw = new PrintWriter(outputPath.toFile)
      try pw.write(sv)
      finally pw.close()

      println(s"[connect-diag] pass: ${testCase.name}")
      println(s"[connect-diag] wrote: ${outputPath}")
      val svPreview = preview(sv)
      if (svPreview.nonEmpty)
        println(svPreview)
    } catch {
      case e: Throwable =>
        if (testCase.shouldPass)
          throw e

        val message = Option(e.getMessage).getOrElse(e.getClass.getName)
        println(s"[connect-diag] expected failure: ${testCase.name}: $message")
    }
  }
}
