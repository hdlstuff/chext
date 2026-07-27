package chext.amba.axi4.lite

import chisel3._
import java.nio.file.Path

import chext.amba.axi4
import chext.amba.axi4.lite.components.CreditBufferConfig
import chext.amba.axi4.lite.ConnectOp._
import chext.amba.axi4.tracking.properties.{Master => MasterProps, Slave => SlaveProps}
import chext.amba.axi4.tracking.values.{MemoryMap, ThreadMode}
import chext.util.{ElaborationTest, SimulationCheck}

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

private class BufferedConnectTop extends Module with chext.AnnotatedModule {
  val cfg = axi4.Config(lite = true)
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
        wBuffer = 2,
        bBuffer = 2
      )
    )
  )

  master :=> creditBuffer.s_axi
  creditBuffer.m_axi :=> slave

  master.masterProps(MasterProps.ReadThreadMode) = ThreadMode.SingleTransaction
  master.masterProps(MasterProps.WriteThreadMode) = ThreadMode.SingleTransaction
  slave.slaveProps(SlaveProps.ReadThreadMode) = ThreadMode.SingleThread
  slave.slaveProps(SlaveProps.WriteThreadMode) = ThreadMode.SingleThread
  slave.slaveProps(SlaveProps.MemoryMap) = MemoryMap(size = 0)
}

private class TieOffWarningTop extends Module {
  val master = IO(Slave(axi4.Config(read = true, write = false, lite = true)))
  val slave = IO(Master(axi4.Config(read = false, write = true, lite = true)))

  master.connect(slave, ConnectConfig())
}

object Connect_Test extends App with ElaborationTest {
  suite(
    name = "axi4-lite-connect",
    outputDir = Path.of("output", "connect_diag_lite")
  )

  test(name = "strict_mismatch_diagnostics", expected = Failure, gen = () => new StrictMismatchTop)
  test(
    name = "configured_address_warning",
    gen = () => new ConfiguredAddressWarningTop,
    checks = Seq(SystemVerilog contains "ConfiguredAddressWarningTop")
  )
  test(
    name = "address_simulation_check",
    gen = () => new AddressSimulationCheckTop,
    checks = Seq(
      SystemVerilog contains "ARADDR upper bits are not zero",
      SystemVerilog contains "AWADDR upper bits are not zero"
    )
  )
  test(
    name = "buffered_connect_generation",
    gen = () => new BufferedConnectTop,
    checks = Seq(
      SystemVerilog contains "ReadResponseBuffer",
      SystemVerilog contains "WritePayloadBuffer",
      SystemVerilog contains "WriteResponseBuffer",
      SystemVerilog contains "BufferedConnectTop"
    )
  )
  test(
    name = "read_write_tie_off_diagnostics",
    gen = () => new TieOffWarningTop,
    checks = Seq(SystemVerilog contains "TieOffWarningTop")
  )

  runTests()
}
