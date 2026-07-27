package chext.amba.axi4

import chisel3._
import chext.amba.axi4.tracking.properties.{Master, Slave}
import chext.amba.axi4.tracking.values.{BurstShape, MemoryMap, ThreadMode}
import chext.util.ElaborationTest

import java.nio.file.Path

private class FullSlaveTestTop(cfg: Config, endpointGen: => full.Interface)
    extends Module
    with chext.AnnotatedModule {
  import full.ConnectOp._

  val s_axi = IO(full.Slave(cfg))
  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)

  s_axi :=> endpointGen
  if (cfg.read) {
    s_axi.masterProps(Master.ReadBurstShape) = BurstShape()
    s_axi.masterProps(Master.ReadThreadMode) = ThreadMode.SingleTransaction
  }
  if (cfg.write) {
    s_axi.masterProps(Master.WriteBurstShape) = BurstShape()
    s_axi.masterProps(Master.WriteThreadMode) = ThreadMode.SingleTransaction
  }
}

private class FullMasterTestTop(cfg: Config, endpointGen: => full.Interface)
    extends Module
    with chext.AnnotatedModule {
  import full.ConnectOp._

  val m_axi = IO(full.Master(cfg))
  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(m_axi)

  endpointGen :=> m_axi
  if (cfg.read) {
    m_axi.slaveProps(Slave.ReadBurstShape) = BurstShape()
    m_axi.slaveProps(Slave.ReadThreadMode) = ThreadMode.Unconstrained
  }
  if (cfg.write) {
    m_axi.slaveProps(Slave.WriteBurstShape) = BurstShape()
    m_axi.slaveProps(Slave.WriteThreadMode) = ThreadMode.Unconstrained
  }
  m_axi.slaveProps(Slave.MemoryMap) = MemoryMap(size = 0)
}

private class LiteSlaveTestTop(cfg: Config, endpointGen: => lite.Interface)
    extends Module
    with chext.AnnotatedModule {
  import lite.ConnectOp._

  val s_axil = IO(lite.Slave(cfg))
  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axil)

  s_axil :=> endpointGen
  if (cfg.read) {
    s_axil.masterProps(Master.ReadThreadMode) = ThreadMode.SingleTransaction
  }
  if (cfg.write) {
    s_axil.masterProps(Master.WriteThreadMode) = ThreadMode.SingleTransaction
  }
}

private class LiteMasterTestTop(cfg: Config, endpointGen: => lite.Interface)
    extends Module
    with chext.AnnotatedModule {
  import lite.ConnectOp._

  val m_axil = IO(lite.Master(cfg))
  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(m_axil)

  endpointGen :=> m_axil
  if (cfg.read) {
    m_axil.slaveProps(Slave.ReadThreadMode) = ThreadMode.SingleThread
  }
  if (cfg.write) {
    m_axil.slaveProps(Slave.WriteThreadMode) = ThreadMode.SingleThread
  }
  m_axil.slaveProps(Slave.MemoryMap) = MemoryMap(size = 0)
}

object ConstantSlave_Test extends App with ElaborationTest {
  private val fullCfg = Config(wId = 4, wAddr = 32, wData = 64)
  private val liteCfg = Config(wAddr = 32, wData = 32, lite = true)

  suite(
    name = "constant-slave",
    outputDir = Path.of("output", "constant_slave")
  )

  test(
    name = "full_constant_slave",
    gen = () =>
      new FullSlaveTestTop(
        fullCfg,
        Module(new full.components.ConstantSlave(fullCfg, "h1234".U, ResponseFlag.SLVERR)).s_axi
      )
  )
  test(
    name = "full_zero_slave",
    gen = () =>
      new FullSlaveTestTop(fullCfg, Module(new full.components.ZeroSlave(fullCfg)).s_axi)
  )
  test(
    name = "full_error_slave",
    gen = () =>
      new FullSlaveTestTop(fullCfg, Module(new full.components.ErrorSlave(fullCfg)).s_axi)
  )
  test(
    name = "full_error_slave_slverr",
    gen = () =>
      new FullSlaveTestTop(
        fullCfg,
        Module(new full.components.ErrorSlave(fullCfg, ResponseFlag.SLVERR)).s_axi
      )
  )
  test(
    name = "full_stall_slave",
    gen = () =>
      new FullSlaveTestTop(fullCfg, Module(new full.components.StallSlave(fullCfg)).s_axi)
  )
  test(
    name = "full_idle_master",
    gen = () =>
      new FullMasterTestTop(fullCfg, Module(new full.components.IdleMaster(fullCfg)).m_axi)
  )
  test(
    name = "full_zero_slave_read_only",
    gen = () => {
      val cfg = fullCfg.copy(write = false)
      new FullSlaveTestTop(cfg, Module(new full.components.ZeroSlave(cfg)).s_axi)
    }
  )
  test(
    name = "full_zero_slave_write_only",
    gen = () => {
      val cfg = fullCfg.copy(read = false)
      new FullSlaveTestTop(cfg, Module(new full.components.ZeroSlave(cfg)).s_axi)
    }
  )

  test(
    name = "lite_constant_slave",
    gen = () =>
      new LiteSlaveTestTop(
        liteCfg,
        Module(new lite.components.ConstantSlave(liteCfg, "h1234".U, ResponseFlag.SLVERR)).s_axil
      )
  )
  test(
    name = "lite_zero_slave",
    gen = () =>
      new LiteSlaveTestTop(liteCfg, Module(new lite.components.ZeroSlave(liteCfg)).s_axil)
  )
  test(
    name = "lite_error_slave",
    gen = () =>
      new LiteSlaveTestTop(liteCfg, Module(new lite.components.ErrorSlave(liteCfg)).s_axil)
  )
  test(
    name = "lite_error_slave_slverr",
    gen = () =>
      new LiteSlaveTestTop(
        liteCfg,
        Module(new lite.components.ErrorSlave(liteCfg, ResponseFlag.SLVERR)).s_axil
      )
  )
  test(
    name = "lite_stall_slave",
    gen = () =>
      new LiteSlaveTestTop(liteCfg, Module(new lite.components.StallSlave(liteCfg)).s_axil)
  )
  test(
    name = "lite_idle_master",
    gen = () =>
      new LiteMasterTestTop(liteCfg, Module(new lite.components.IdleMaster(liteCfg)).m_axil)
  )
  test(
    name = "lite_zero_slave_read_only",
    gen = () => {
      val cfg = liteCfg.copy(write = false)
      new LiteSlaveTestTop(cfg, Module(new lite.components.ZeroSlave(cfg)).s_axil)
    }
  )
  test(
    name = "lite_zero_slave_write_only",
    gen = () => {
      val cfg = liteCfg.copy(read = false)
      new LiteSlaveTestTop(cfg, Module(new lite.components.ZeroSlave(cfg)).s_axil)
    }
  )

  runTests()
}
