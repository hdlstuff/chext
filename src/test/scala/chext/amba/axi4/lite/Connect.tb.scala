package chext.amba.axi4.lite

import chisel3._
import circt.stage.ChiselStage
import java.io.PrintWriter
import java.nio.file.{Files, Path}

import chext.amba.axi4
import chext.amba.axi4.lite.ConnectOp._
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
  val master = IO(Slave(axi4.Config(wAddr = 24, wData = 32, lite = true)))
  val slave = IO(Master(axi4.Config(wAddr = 24, wData = 64, lite = true)))

  master :=> slave
}

private class ConfiguredAddressWarningTop extends Module {
  val master = IO(Slave(axi4.Config(wAddr = 32, lite = true)))
  val slave = IO(Master(axi4.Config(wAddr = 16, lite = true)))

  master.connect(slave, ConnectConfig())
}

private class AddressSimulationCheckTop extends Module {
  val master = IO(Slave(axi4.Config(wAddr = 32, lite = true)))
  val slave = IO(Master(axi4.Config(wAddr = 16, lite = true)))

  master.connect(
    slave,
    ConnectConfig(
      simCheckAddrWidth = SimulationCheck.Printf
    )
  )
}

private class BufferedConnectTop extends Module {
  val cfg = axi4.Config(lite = true)
  val master = IO(Slave(cfg))
  val slave = IO(Master(cfg))

  master.connect(
    slave,
    ConnectConfig(
      arBuffer = 2,
      rBuffer = 2,
      awBuffer = 2,
      wBuffer = 2,
      bBuffer = 2
    )
  )
}

private class TieOffWarningTop extends Module {
  val master = IO(Slave(axi4.Config(read = true, write = false, lite = true)))
  val slave = IO(Master(axi4.Config(read = false, write = true, lite = true)))

  master.connect(slave, ConnectConfig())
}

object ConnectDiag_Tb extends App {
  private val outputDir = Path.of("output", "connect_diag_lite")
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
      contains = Seq("wData")
    ),
    TestCase(
      "configured address warning",
      "configured_address_warning.sv",
      shouldPass = true,
      () => new ConfiguredAddressWarningTop,
      contains = Seq("ConfiguredAddressWarningTop")
    ),
    TestCase(
      "address simulation check",
      "address_simulation_check.sv",
      shouldPass = true,
      () => new AddressSimulationCheckTop,
      contains = Seq("ARADDR upper bits are not zero", "AWADDR upper bits are not zero")
    ),
    TestCase(
      "buffered connect generation",
      "buffered_connect_generation.sv",
      shouldPass = true,
      () => new BufferedConnectTop,
      contains = Seq(
        "ReadResponseBuffer",
        "WritePayloadBuffer",
        "WriteResponseBuffer",
        "BufferedConnectTop"
      )
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
          line.contains("upper bits")
      )
      .take(10)
      .mkString("\n")

  cases.foreach { testCase =>
    println(s"[connect-diag-lite] running: ${testCase.name}")
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

      println(s"[connect-diag-lite] pass: ${testCase.name}")
      println(s"[connect-diag-lite] wrote: ${outputPath}")
      val svPreview = preview(sv)
      if (svPreview.nonEmpty)
        println(svPreview)
    } catch {
      case e: Throwable =>
        if (testCase.shouldPass)
          throw e

        val message = Option(e.getMessage).getOrElse(e.getClass.getName)
        println(s"[connect-diag-lite] expected failure: ${testCase.name}: $message")
    }
  }
}
