package chext.amba.axi4.full

import chisel3._
import java.nio.file.Path

import chext.amba.axi4
import chext.amba.axi4.full.components.CreditBufferConfig
import chext.amba.axi4.full.ConnectOp._
import chext.amba.axi4.tracking.{properties => p}
import chext.amba.axi4.tracking.values.{BurstShape, MemoryMap, ThreadMode}
import chext.util.{ElaborationTest, SimulationCheck}

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

private class BufferedConnectTop extends Module with chext.AnnotatedModule {
  val cfg = axi4.Config()
  val master = IO(Slave(cfg))
  val slave = IO(Master(cfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(master)
  declareAxi4Interface(slave)

  val creditBuffer = Module(
    new components.CreditBuffer(
      CreditBufferConfig(
        axiCfg = cfg,
        rBuffer = 2,
        wBuffer = 4,
        bBuffer = 2
      )
    )
  )

  master :=> creditBuffer.s_axi
  creditBuffer.m_axi :=> slave

  master.properties(p.MasterReadBurstShape) = BurstShape()
  master.properties(p.MasterWriteBurstShape) = BurstShape()
  master.properties(p.MasterReadThreadMode) = ThreadMode.SingleTransaction
  master.properties(p.MasterWriteThreadMode) = ThreadMode.SingleTransaction
  slave.properties(p.SlaveReadBurstShape) = BurstShape()
  slave.properties(p.SlaveWriteBurstShape) = BurstShape()
  slave.properties(p.SlaveReadThreadMode) = ThreadMode.Unconstrained
  slave.properties(p.SlaveWriteThreadMode) = ThreadMode.Unconstrained
  slave.properties(p.SlaveMemoryMap) = MemoryMap(size = 0)
}

private class TieOffWarningTop extends Module {
  val master = IO(Slave(axi4.Config(read = true, write = false)))
  val slave = IO(Master(axi4.Config(read = false, write = true)))

  master.connect(slave, ConnectConfig())
}

object Connect_Test extends App with ElaborationTest {
  suite(
    name = "axi4-full-connect",
    outputDir = Path.of("output", "connect_diag")
  )

  test(name = "strict_mismatch_diagnostics", expected = Failure, gen = () => new StrictMismatchTop)
  test(
    name = "configured_sideband_warning",
    gen = () => new ConfiguredSidebandWarningTop,
    checks = Seq(SystemVerilog contains "ConfiguredSidebandWarningTop")
  )
  test(
    name = "id_simulation_check",
    gen = () => new IdSimulationCheckTop,
    checks = Seq(
      SystemVerilog contains "ARID upper bits are not zero",
      SystemVerilog contains "AWID upper bits are not zero"
    )
  )
  test(
    name = "axi3_simulation_check",
    gen = () => new Axi3SimulationCheckTop,
    checks = Seq(
      SystemVerilog contains "ARLEN is not AXI3-compatible",
      SystemVerilog contains "AWLEN is not AXI3-compatible"
    )
  )
  test(
    name = "buffered_connect_generation",
    gen = () => new BufferedConnectTop,
    checks = Seq(
      SystemVerilog contains "ReadResponseBuffer",
      SystemVerilog contains "WriteResponseBuffer",
      SystemVerilog contains "WritePayloadBuffer"
    )
  )
  test(
    name = "read_write_tie_off_diagnostics",
    gen = () => new TieOffWarningTop,
    checks = Seq(SystemVerilog contains "TieOffWarningTop")
  )

  runTests()
}
