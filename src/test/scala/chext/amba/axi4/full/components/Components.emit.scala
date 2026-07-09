package chext.amba.axi4.full.components

import chisel3._

import chext.amba.axi4

object ProtocolConverter_Emit extends App {
  val cfg1 = ProtocolConverterConfig(
    axi4.Config(wAddr = 32, wData = 32, wId = 8),
    axi4.Config(wAddr = 32, wData = 128, wId = 2)
  )
  emitVerilog(new ProtocolConverter(cfg1), Array("--target-dir", "output/"))

  val cfg2 = ProtocolConverterConfig(
    axi4.Config(wAddr = 32, wData = 128, wId = 0),
    axi4.Config(wAddr = 32, wData = 32, wId = 0)
  )
  emitVerilog(new ProtocolConverter(cfg2), Array("--target-dir", "output/"))

  val cfg3 = ProtocolConverterConfig(
    axi4.Config(wAddr = 32, wData = 128, wId = 2),
    axi4.Config(wAddr = 32, wData = 128, wId = 4)
  )
  emitVerilog(new ProtocolConverter(cfg3), Array("--target-dir", "output/"))

  val cfg4 = ProtocolConverterConfig(
    axi4.Config(wAddr = 32, wData = 128, wId = 4),
    axi4.Config(wAddr = 32, wData = 128, wId = 2)
  )
  emitVerilog(new ProtocolConverter(cfg4), Array("--target-dir", "output/"))
}

object Mux_Emit extends App {
  def muxModule = new Mux(
    MuxConfig(
      axi4.Config(
        wId = 4,
        wAddr = 32,
        wData = 256,
        read = true,
        write = true,
        lite = false
      ),
      numSlaves = 8
    )
  )

  emitVerilog(muxModule, Array("--target-dir", "output/"))
}

object Demux_Emit extends App {
  def demuxModule = new Demux(
    DemuxConfig(
      axi4.Config(
        wId = 4,
        wAddr = 32,
        wData = 256,
        read = true,
        write = true,
        lite = false
      ),
      8,
      (_ >> 8),
      masterBuffers = axi4.BufferConfig.all(8)
    )
  )

  emitVerilog(demuxModule, Array("--target-dir", "output/"))
}
