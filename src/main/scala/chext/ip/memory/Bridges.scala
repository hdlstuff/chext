package chext.ip.memory

import chisel3._
import chisel3.util._

class ReadToRawBridge(
    val wAddr: Int,
    val wData: Int,
    val latency: Int,
    val numOutstanding: Int
) extends Module {
  val read = IO(new ReadInterface(wAddr, wData))
  val raw = IO(Flipped(new RawInterface(wAddr, wData, true, false)))

  private val ctr = Module(new chext.util.Counter(numOutstanding + 1))
  ctr.noInc()
  ctr.noDec()

  private val dataQueue = Module(
    new Queue(
      read.data.bits.cloneType,
      numOutstanding,
      flow = true,
      pipe = true
    )
  )
  private val dataQueueEnq = dataQueue.io.enq
  private val dataQueueDeq = dataQueue.io.deq
  dataQueueEnq.noenq()
  dataQueueDeq.nodeq()

  raw.addr := DontCare
  raw.dIn := DontCare
  raw.wstrb := 0.U

  read.addr.nodeq()
  read.data.noenq()

  when(ctr.notFull && read.addr.valid) {
    raw.addr := read.addr.bits
    read.addr.deq()
  }

  when(read.addr.fire) {
    ctr.inc()
  }

  when(ShiftRegister(read.addr.fire, latency)) {
    dataQueueEnq.enq(raw.dOut)
  }

  when(read.data.ready && dataQueueDeq.valid) {
    read.data.enq(dataQueueDeq.deq())
    ctr.dec()
  }
}

class WriteToRawBridge(
    val wAddr: Int,
    val wData: Int,
    val latency: Int,
    val numOutstanding: Int
) extends Module {
  val write = IO(new WriteInterface(wAddr, wData))
  val raw = IO(Flipped(new RawInterface(wAddr, wData, true, false)))

  private val ctr = Module(new chext.util.Counter(numOutstanding + 1))
  ctr.noInc()
  ctr.noDec()

  private val ctrResp = Module(new chext.util.Counter(numOutstanding + 1))
  ctrResp.noInc()
  ctrResp.noDec()

  raw.addr := DontCare
  raw.dIn := DontCare
  raw.wstrb := 0.U

  write.req.nodeq()
  write.resp.noenq()

  when(ctr.notFull && write.req.valid) {
    raw.addr := write.req.bits.addr
    raw.dIn := write.req.bits.data
    raw.wstrb := write.req.bits.wstrb

    write.req.deq()
  }

  when(write.req.fire) {
    ctr.inc()
  }

  when(ShiftRegister(write.req.fire, latency)) {
    ctrResp.inc()
  }

  when(write.resp.ready && ctrResp.notZero) {
    write.resp.enq(0.U)
    ctr.dec()
    ctrResp.dec()
  }
}
