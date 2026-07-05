package chext.memory

import chisel3._
import chisel3.util._

import ConnectOp._

case class PortConfig(
    val numOutstandingRead: Int = 4,
    val numOutstandingWrite: Int = 4,
    val arbiterFunc: ReadWriteArbiter.Func = ReadWriteArbiter.defaultFunc
) {
  assert(numOutstandingRead >= 1)
  assert(numOutstandingWrite >= 1)
}

class SinglePortRAM(
    val rawMemCfg: RawMemConfig,
    val portCfg: PortConfig = PortConfig()
) extends Module {
  private val require_ = chext.util.Require.inferred()

  import rawMemCfg._

  override val desiredName = f"${Target.current.name}SinglePortRAM"

  val read = IO(new ReadInterface(wAddr, wData))
  val write = IO(new WriteInterface(wAddr, wData))

  require_(latencyRead >= 1)
  require_(latencyWrite >= 1)

  private val rawMem = Module(Target.current.createSinglePortRawRAM(rawMemCfg))

  private val raw = rawMem.getPorts(0)

  private val bridge = Module(new ReadWriteToRawBridge(rawMemCfg, portCfg))

  read :=> bridge.read
  write :=> bridge.write

  // TODO: change <> with a better operator
  raw <> bridge.raw
}

class SimpleDualPortRAM(val rawMemCfg: RawMemConfig, val portCfg: PortConfig = PortConfig())
    extends Module {
  private val require_ = chext.util.Require.inferred()

  import rawMemCfg._

  override val desiredName = f"${Target.current.name}SimpleDualPortRAM"

  val read = IO(new ReadInterface(wAddr, wData))
  val write = IO(new WriteInterface(wAddr, wData))

  require_(latencyRead >= 1)
  require_(latencyWrite >= 1)

  private val rawMem = Module(Target.current.createSimpleDualPortRawRAM(rawMemCfg))

  private val rawRead =
    rawMem.getPorts.filter((x) => x.supportsRead && !x.supportsWrite)(0)
  private val rawWrite =
    rawMem.getPorts.filter((x) => !x.supportsRead && x.supportsWrite)(0)

  private val readBridge = Module(new ReadToRawBridge(rawMemCfg, portCfg))
  private val writeBridge = Module(new WriteToRawBridge(rawMemCfg, portCfg))

  read :=> readBridge.read
  write :=> writeBridge.write

  // TODO: change <> with a better operator
  rawRead <> readBridge.raw
  rawWrite <> writeBridge.raw
}

class TrueDualPortRAM(
    val rawMemCfg: RawMemConfig,
    val portCfg1: PortConfig = PortConfig(),
    val portCfg2: PortConfig = PortConfig()
) extends Module {
  import rawMemCfg._

  override val desiredName = f"${Target.current.name}TrueDualPortRAM"

  val read1 = IO(new ReadInterface(wAddr, wData))
  val read2 = IO(new ReadInterface(wAddr, wData))
  val write1 = IO(new WriteInterface(wAddr, wData))
  val write2 = IO(new WriteInterface(wAddr, wData))

  private val rawMem = Module(Target.current.createTrueDualPortRawRAM(rawMemCfg))

  private val raw1 = rawMem.getPorts(0)
  private val raw2 = rawMem.getPorts(1)

  private val bridge1 = Module(new ReadWriteToRawBridge(rawMemCfg, portCfg1))
  private val bridge2 = Module(new ReadWriteToRawBridge(rawMemCfg, portCfg2))

  // first port
  read1 :=> bridge1.read
  write1 :=> bridge1.write

  // TODO: change <> with a better operator
  raw1 <> bridge1.raw

  // second port
  read2 :=> bridge2.read
  write2 :=> bridge2.write

  // TODO: change <> with a better operator
  raw2 <> bridge2.raw
}

private object RAM_Emit extends App {
  Target.setCurrent(xilinx.Target)

  emitVerilog(
    new SimpleDualPortRAM(RawMemConfig(256, 15, 8, 1), PortConfig(12, 12)),
    Array("--target-dir", "output/")
  )

  emitVerilog(
    new SinglePortRAM(RawMemConfig(256, 15, 8, 1), PortConfig(12, 12)),
    Array("--target-dir", "output/")
  )

  emitVerilog(
    new TrueDualPortRAM(RawMemConfig(256, 15, 8, 1), PortConfig(12, 12)),
    Array("--target-dir", "output/")
  )
}
