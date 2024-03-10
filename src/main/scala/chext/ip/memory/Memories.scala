package chext.ip.memory

import chisel3._
import chisel3.util._

class MemConfig(
    override val wAddr: Int,
    override val wData: Int,
    override val latencyRead: Int,
    override val latencyWrite: Int,
    val numOutstandingRead: Int,
    val numOutstandingWrite: Int,
    val arbiterFn: () => ReadWriteArbiter = () => new BasicReadWriteArbiter(8)
) extends RawMemConfig(wAddr, wData, latencyRead, latencyWrite) {
  assert(numOutstandingRead >= 1)
  assert(numOutstandingWrite >= 1)
}

object MemConfig {
  def apply(
      wAddr: Int = 6,
      wData: Int = 32,
      latencyRead: Int = 2,
      latencyWrite: Int = 1,
      numOutstandingRead: Int = 1,
      numOutstandingWrite: Int = 1
  ) =
    new MemConfig(
      wAddr = wAddr,
      wData = wData,
      latencyRead = latencyRead,
      latencyWrite = latencyWrite,
      numOutstandingRead = numOutstandingRead,
      numOutstandingWrite = numOutstandingWrite
    )
}

class SinglePortRAM(val cfg: MemConfig) extends Module {
  override val desiredName = f"${Target.current.name}SinglePortRAM"

  val read = IO(new ReadInterface(cfg.wAddr, cfg.wData))
  val write = IO(new WriteInterface(cfg.wAddr, cfg.wData))

  private val rawMem = Module(Target.current.createSinglePortRawRAM(cfg))

  private val raw = rawMem.getPorts(0)

  private val bridge = Module(new ReadWriteToRawBridge(cfg))

  // TODO: change <> with a better operator
  read <> bridge.read
  write <> bridge.write
  raw <> bridge.raw
}

class SimpleDualPortRAM(val cfg: MemConfig) extends Module {
  override val desiredName = f"${Target.current.name}SimpleDualPortRAM"

  val read = IO(new ReadInterface(cfg.wAddr, cfg.wData))
  val write = IO(new WriteInterface(cfg.wAddr, cfg.wData))

  require(cfg.latencyRead >= 1)
  require(cfg.latencyWrite >= 1)

  private val rawMem = Module(Target.current.createSimpleDualPortRawRAM(cfg))

  private val rawRead =
    rawMem.getPorts.filter((x) => x.supportsRead && !x.supportsWrite)(0)
  private val rawWrite =
    rawMem.getPorts.filter((x) => !x.supportsRead && x.supportsWrite)(0)

  private val readBridge = Module(new ReadToRawBridge(cfg))
  private val writeBridge = Module(new WriteToRawBridge(cfg))

  // TODO: change <> with a better operator
  read <> readBridge.read
  write <> writeBridge.write
  rawRead <> readBridge.raw
  rawWrite <> writeBridge.raw
}

class TrueDualPortRAM(val cfg: MemConfig) extends Module {
  override val desiredName = f"${Target.current.name}TrueDualPortRAM"

  val read1 = IO(new ReadInterface(cfg.wAddr, cfg.wData))
  val read2 = IO(new ReadInterface(cfg.wAddr, cfg.wData))
  val write1 = IO(new WriteInterface(cfg.wAddr, cfg.wData))
  val write2 = IO(new WriteInterface(cfg.wAddr, cfg.wData))

  private val rawMem = Module(Target.current.createTrueDualPortRawRAM(cfg))

  private val raw1 = rawMem.getPorts(0)
  private val raw2 = rawMem.getPorts(1)

  private val bridge1 = Module(new ReadWriteToRawBridge(cfg))
  private val bridge2 = Module(new ReadWriteToRawBridge(cfg))

  // TODO: change <> with a better operator

  // first port
  read1 <> bridge1.read
  write1 <> bridge1.write
  raw1 <> bridge1.raw

  // second port
  read2 <> bridge2.read
  write2 <> bridge2.write
  raw2 <> bridge2.raw
}

object Emitter extends App {
  Target.setCurrent(xilinx.Target)

  emitVerilog(
    new SimpleDualPortRAM(MemConfig(256, 15, 8, 1, 12, 12)),
    Array("--target-dir", "output/")
  )

  emitVerilog(
    new SinglePortRAM(MemConfig(256, 15, 8, 1, 12, 12)),
    Array("--target-dir", "output/")
  )

  emitVerilog(
    new TrueDualPortRAM(MemConfig(256, 15, 8, 1, 12, 12)),
    Array("--target-dir", "output/")
  )

}
