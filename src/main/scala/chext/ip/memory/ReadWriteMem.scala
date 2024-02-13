package chext.ip.memory

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

case class ReadWriteMemConfig(
    wData: Int,
    wAddr: Int,
    readLatency: Int,
    writeLatency: Int,
    numRdOutstanding: Int,
    numWrOutstanding: Int
) {
  require(readLatency >= 1)
  require(writeLatency >= 1)
  require(isPow2(wData))
}

class ReadWriteMemController(
    cfg: ReadWriteMemConfig
) extends Module {
  val read = IO(new RdInterface(cfg.wAddr, cfg.wData))
  val write = IO(new WrInterface(cfg.wAddr, cfg.wData))
  val rawRead = IO(Flipped(new RawInterface(cfg.wAddr, cfg.wData)))
  val rawWrite = IO(Flipped(new RawInterface(cfg.wAddr, cfg.wData)))

  assert(rawRead.supportsRead)
  assert(rawWrite.supportsWrite)

  prefix("read") {
    val counter = Module(new chext.util.Counter(cfg.numRdOutstanding + 1))
    counter.noInc()
    counter.noDec()

    val dataQueue = Module(
      new Queue(
        read.data.bits.cloneType,
        cfg.numRdOutstanding,
        flow = true,
        pipe = true
      )
    )
    val dataQueueEnq = dataQueue.io.enq
    val dataQueueDeq = dataQueue.io.deq
    dataQueueEnq.noenq()
    dataQueueDeq.nodeq()

    rawRead.addr := DontCare
    rawRead.dataIn := DontCare
    rawRead.writeStrobe := 0.U

    read.addr.nodeq()
    read.data.noenq()

    when(counter.notFull && read.addr.valid) {
      rawRead.addr := read.addr.bits
      read.addr.deq()
    }

    when(read.addr.fire) {
      counter.inc()
    }

    when(ShiftRegister(read.addr.fire, cfg.readLatency)) {
      dataQueueEnq.enq(rawRead.dataOut)
    }

    when(read.data.ready && dataQueueDeq.valid) {
      read.data.enq(dataQueueDeq.deq())
      counter.dec()
    }
  }

  prefix("write") {
    val counter1 = Module(new chext.util.Counter(cfg.numWrOutstanding + 1))
    counter1.noInc()
    counter1.noDec()

    val counter2 = Module(new chext.util.Counter(cfg.numWrOutstanding + 1))
    counter2.noInc()
    counter2.noDec()

    rawWrite.addr := DontCare
    rawWrite.dataIn := DontCare
    rawWrite.writeStrobe := 0.U

    write.payload.nodeq()
    write.response.noenq()

    when(counter1.notFull && write.payload.valid) {
      rawWrite.addr := write.payload.bits.addr
      rawWrite.dataIn := write.payload.bits.data
      rawWrite.writeStrobe := write.payload.bits.writeStrobe

      write.payload.deq()
    }

    when(write.payload.fire) {
      counter1.inc()
    }

    when(ShiftRegister(write.payload.fire, cfg.writeLatency)) {
      counter2.inc()
    }

    when(write.response.ready && counter2.notZero) {
      write.response.enq(true.B)
      counter1.dec()
      counter2.dec()
    }
  }
}

class ReadWriteMem(
    cfg: ReadWriteMemConfig
) extends Module {
  override val desiredName = f"${Target.current.name}ReadWriteMem"

  val read = IO(new RdInterface(cfg.wAddr, cfg.wData))
  val write = IO(new WrInterface(cfg.wAddr, cfg.wData))

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
