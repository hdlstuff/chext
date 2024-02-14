package chext.ip.memory

import chisel3._
import chisel3.util._

class SinglePortRAM(
    wData: Int,
    wAddr: Int,
    latencyRead: Int,
    latencyWrite: Int,
    numOutstandingRead: Int,
    numOutstandingWrite: Int
) extends Module {
  override val desiredName = f"${Target.current.name}SimpleDualPortRAM"

  val read = IO(new ReadInterface(wAddr, wData))
  val write = IO(new WriteInterface(wAddr, wData))

  require(latencyRead >= 1)
  require(latencyWrite >= 1)
  require(isPow2(wData))

  private val rawMem = Module(
    Target.current.createSinglePortRawRAM(
      RawMemConfig(
        wAddr = wAddr,
        wData = wData,
        latencyRead = latencyRead,
        latencyWrite = latencyWrite
      )
    )
  )

  private val raw = rawMem.getPorts(0)

  private val bridge = Module(
    new ReadWriteToRawBridge(
      wAddr,
      wData,
      latencyRead,
      latencyWrite,
      numOutstandingRead,
      numOutstandingWrite
    )
  )

  // TODO: change <> with a better operator
  read <> bridge.read
  write <> bridge.write
  raw <> bridge.raw
}

class SimpleDualPortRAM(
    wData: Int,
    wAddr: Int,
    latencyRead: Int,
    latencyWrite: Int,
    numOutstandingRead: Int,
    numOutstandingWrite: Int
) extends Module {
  override val desiredName = f"${Target.current.name}SimpleDualPortRAM"

  val read = IO(new ReadInterface(wAddr, wData))
  val write = IO(new WriteInterface(wAddr, wData))

  require(latencyRead >= 1)
  require(latencyWrite >= 1)
  require(isPow2(wData))

  private val rawMem = Module(
    Target.current.createSimpleDualPortRawRAM(
      RawMemConfig(
        wAddr = wAddr,
        wData = wData,
        latencyRead = latencyRead,
        latencyWrite = latencyWrite
      )
    )
  )

  private val rawRead =
    rawMem.getPorts.filter((x) => x.supportsRead && !x.supportsWrite)(0)
  private val rawWrite =
    rawMem.getPorts.filter((x) => !x.supportsRead && x.supportsWrite)(0)

  private val readBridge = Module(
    new ReadToRawBridge(
      wAddr,
      wData,
      latencyRead,
      numOutstandingRead
    )
  )

  private val writeBridge = Module(
    new WriteToRawBridge(
      wAddr,
      wData,
      latencyWrite,
      numOutstandingWrite
    )
  )

  // TODO: change <> with a better operator
  read <> readBridge.read
  write <> writeBridge.write
  rawRead <> readBridge.raw
  rawWrite <> writeBridge.raw
}

class TrueDualPortRAM(
    wData: Int,
    wAddr: Int,
    latencyRead: Int,
    latencyWrite: Int,
    numOutstandingRead: Int,
    numOutstandingWrite: Int
) extends Module {
  override val desiredName = f"${Target.current.name}SimpleDualPortRAM"

  val read1 = IO(new ReadInterface(wAddr, wData))
  val read2 = IO(new ReadInterface(wAddr, wData))
  val write1 = IO(new WriteInterface(wAddr, wData))
  val write2 = IO(new WriteInterface(wAddr, wData))

  require(latencyRead >= 1)
  require(latencyWrite >= 1)
  require(isPow2(wData))

  private val rawMem = Module(
    Target.current.createTrueDualPortRawRAM(
      RawMemConfig(
        wAddr = wAddr,
        wData = wData,
        latencyRead = latencyRead,
        latencyWrite = latencyWrite
      )
    )
  )

  private val raw1 = rawMem.getPorts(1)
  private val raw2 = rawMem.getPorts(2)

  private val bridge1 = Module(
    new ReadWriteToRawBridge(
      wAddr,
      wData,
      latencyRead,
      latencyWrite,
      numOutstandingRead,
      numOutstandingWrite
    )
  )

  private val bridge2 = Module(
    new ReadWriteToRawBridge(
      wAddr,
      wData,
      latencyRead,
      latencyWrite,
      numOutstandingRead,
      numOutstandingWrite
    )
  )

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
    new SimpleDualPortRAM(256, 15, 8, 1, 12, 12),
    Array("--target-dir", "output/")
  )
}
