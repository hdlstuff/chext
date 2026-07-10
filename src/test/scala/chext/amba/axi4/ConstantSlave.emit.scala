package chext.amba.axi4

import chisel3._
import circt.stage.ChiselStage

object ConstantSlave_Emit extends App {
  private val fullCfg = Config(wId = 4, wAddr = 32, wData = 64)
  private val liteCfg = Config(wAddr = 32, wData = 32, lite = true)

  ChiselStage.emitSystemVerilog(
    new full.components.ConstantSlave(fullCfg, "h1234".U, ResponseFlag.SLVERR)
  )
  ChiselStage.emitSystemVerilog(new full.components.ZeroSlave(fullCfg))
  ChiselStage.emitSystemVerilog(new full.components.ErrorSlave(fullCfg))
  ChiselStage.emitSystemVerilog(
    new full.components.ErrorSlave(fullCfg, ResponseFlag.SLVERR)
  )
  ChiselStage.emitSystemVerilog(new full.components.StallSlave(fullCfg))
  ChiselStage.emitSystemVerilog(new full.components.IdleMaster(fullCfg))
  ChiselStage.emitSystemVerilog(new full.components.ZeroSlave(fullCfg.copy(write = false)))
  ChiselStage.emitSystemVerilog(new full.components.ZeroSlave(fullCfg.copy(read = false)))
  ChiselStage.emitSystemVerilog(
    new lite.components.ConstantSlave(liteCfg, "h1234".U, ResponseFlag.SLVERR)
  )
  ChiselStage.emitSystemVerilog(new lite.components.ZeroSlave(liteCfg))
  ChiselStage.emitSystemVerilog(new lite.components.ErrorSlave(liteCfg))
  ChiselStage.emitSystemVerilog(
    new lite.components.ErrorSlave(liteCfg, ResponseFlag.SLVERR)
  )
  ChiselStage.emitSystemVerilog(new lite.components.StallSlave(liteCfg))
  ChiselStage.emitSystemVerilog(new lite.components.IdleMaster(liteCfg))
  ChiselStage.emitSystemVerilog(new lite.components.ZeroSlave(liteCfg.copy(write = false)))
  ChiselStage.emitSystemVerilog(new lite.components.ZeroSlave(liteCfg.copy(read = false)))
}
