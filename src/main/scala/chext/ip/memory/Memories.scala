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
    Target.current.createSimpleDualPortRawRAM(
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

object Emitter extends App {
  Target.setCurrent(xilinx.Target)

  emitVerilog(
    new SimpleDualPortRAM(256, 15, 8, 1, 12, 12),
    Array("--target-dir", "output/")
  )
}
