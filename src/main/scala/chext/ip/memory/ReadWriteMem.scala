package chext.ip.memory

import chisel3._
import chisel3.util._

case class ReadWriteMemConfig(
    wData: Int,
    wAddr: Int,
    readLatency: Int,
    writeLatency: Int,
    numReadOutstanding: Int,
    numWriteOutstanding: Int
) {
  require(readLatency >= 1)
  require(writeLatency >= 1)
  require(isPow2(wData))
}

class ReadWriteMemController(
    cfg: ReadWriteMemConfig
) extends Module {
  val read = IO(new ReadInterface(cfg.wAddr, cfg.wData))
  val write = IO(new WriteInterface(cfg.wAddr, cfg.wData))
  val rawRead = IO(Flipped(new RawInterface(cfg.wAddr, cfg.wData)))
  val rawWrite = IO(Flipped(new RawInterface(cfg.wAddr, cfg.wData)))

  private val readBridge = Module(
    new ReadToRawBridge(
      cfg.wAddr,
      cfg.wData,
      cfg.readLatency,
      cfg.numReadOutstanding
    )
  )

  private val writeBridge = Module(
    new WriteToRawBridge(
      cfg.wAddr,
      cfg.wData,
      cfg.writeLatency,
      cfg.numWriteOutstanding
    )
  )

  read <> readBridge.read
  rawRead <> readBridge.raw

  write <> writeBridge.write
  rawWrite <> writeBridge.raw
}

class ReadWriteMem(
    cfg: ReadWriteMemConfig
) extends Module {
  override val desiredName = f"${Target.current.name}ReadWriteMem"

  val read = IO(new ReadInterface(cfg.wAddr, cfg.wData))
  val write = IO(new WriteInterface(cfg.wAddr, cfg.wData))

  private val rawMem = Module(
    Target.current.createSimpleDualPortRawMem(
      RawMemConfig(
        wAddr = cfg.wAddr,
        wData = cfg.wData,
        readLatency = cfg.readLatency,
        writeLatency = cfg.writeLatency
      )
    )
  )

  private val controller = Module(new ReadWriteMemController(cfg))

  private val internalRead =
    rawMem.getPorts.filter((x) => x.supportsRead && !x.supportsWrite)(0)
  private val internalWrite =
    rawMem.getPorts.filter((x) => !x.supportsRead && x.supportsWrite)(0)

  // TODO: change <> with a better operator
  read <> controller.read
  write <> controller.write
  internalRead <> controller.rawRead
  internalWrite <> controller.rawWrite
}

object Emitter extends App {
  Target.setCurrent(xilinx.Target)

  emitVerilog(
    new ReadWriteMem(ReadWriteMemConfig(256, 15, 8, 1, 12, 12)),
    Array("--target-dir", "output/")
  )
}
