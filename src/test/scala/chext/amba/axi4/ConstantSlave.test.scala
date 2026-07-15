package chext.amba.axi4

import chisel3._
import chext.util.ElaborationTest

import java.nio.file.Path

object ConstantSlave_Test extends App with ElaborationTest {
  private val fullCfg = Config(wId = 4, wAddr = 32, wData = 64)
  private val liteCfg = Config(wAddr = 32, wData = 32, lite = true)

  suite(
    name = "constant-slave",
    outputDir = Path.of("output", "constant_slave")
  )

  test(
    name = "full_constant_slave",
    gen = () => new full.components.ConstantSlave(fullCfg, "h1234".U, ResponseFlag.SLVERR)
  )
  test(name = "full_zero_slave", gen = () => new full.components.ZeroSlave(fullCfg))
  test(name = "full_error_slave", gen = () => new full.components.ErrorSlave(fullCfg))
  test(
    name = "full_error_slave_slverr",
    gen = () => new full.components.ErrorSlave(fullCfg, ResponseFlag.SLVERR)
  )
  test(name = "full_stall_slave", gen = () => new full.components.StallSlave(fullCfg))
  test(name = "full_idle_master", gen = () => new full.components.IdleMaster(fullCfg))
  test(
    name = "full_zero_slave_read_only",
    gen = () => new full.components.ZeroSlave(fullCfg.copy(write = false))
  )
  test(
    name = "full_zero_slave_write_only",
    gen = () => new full.components.ZeroSlave(fullCfg.copy(read = false))
  )

  test(
    name = "lite_constant_slave",
    gen = () => new lite.components.ConstantSlave(liteCfg, "h1234".U, ResponseFlag.SLVERR)
  )
  test(name = "lite_zero_slave", gen = () => new lite.components.ZeroSlave(liteCfg))
  test(name = "lite_error_slave", gen = () => new lite.components.ErrorSlave(liteCfg))
  test(
    name = "lite_error_slave_slverr",
    gen = () => new lite.components.ErrorSlave(liteCfg, ResponseFlag.SLVERR)
  )
  test(name = "lite_stall_slave", gen = () => new lite.components.StallSlave(liteCfg))
  test(name = "lite_idle_master", gen = () => new lite.components.IdleMaster(liteCfg))
  test(
    name = "lite_zero_slave_read_only",
    gen = () => new lite.components.ZeroSlave(liteCfg.copy(write = false))
  )
  test(
    name = "lite_zero_slave_write_only",
    gen = () => new lite.components.ZeroSlave(liteCfg.copy(read = false))
  )

  runTests()
}
