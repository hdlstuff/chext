package chext.ip.memory

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

case class ReadToRawBridgeConfig(
    val wAddr: Int,
    val wData: Int,
    val latency: Int,
    val numOutstanding: Int
)

class ReadToRawBridge(val cfg: ReadToRawBridgeConfig) extends Module {
  val read = IO(new ReadInterface(cfg.wAddr, cfg.wData))
  val raw = IO(Flipped(new RawInterface(cfg.wAddr, cfg.wData, true, false)))

  private val ctr = Module(new chext.util.Counter(cfg.numOutstanding + 1))
  ctr.noInc()
  ctr.noDec()

  private val dataQueue = Module(
    new Queue(
      read.resp.bits.cloneType,
      cfg.numOutstanding,
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

  read.req.nodeq()
  read.resp.noenq()

  when(ctr.notFull && read.req.valid) {
    raw.addr := read.req.bits
    read.req.deq()
  }

  when(read.req.fire) {
    ctr.inc()
  }

  when(ShiftRegister(read.req.fire, cfg.latency)) {
    dataQueueEnq.enq(raw.dOut)
  }

  when(read.resp.ready && dataQueueDeq.valid) {
    read.resp.enq(dataQueueDeq.deq())
    ctr.dec()
  }
}

case class WriteToRawBridgeConfig(
    val wAddr: Int,
    val wData: Int,
    val latency: Int,
    val numOutstanding: Int
)

class WriteToRawBridge(val cfg: WriteToRawBridgeConfig) extends Module {
  val write = IO(new WriteInterface(cfg.wAddr, cfg.wData))
  val raw = IO(Flipped(new RawInterface(cfg.wAddr, cfg.wData, true, false)))

  private val ctr = Module(new chext.util.Counter(cfg.numOutstanding + 1))
  ctr.noInc()
  ctr.noDec()

  private val ctrResp = Module(new chext.util.Counter(cfg.numOutstanding + 1))
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
    raw.wstrb := write.req.bits.strb

    write.req.deq()
  }

  when(write.req.fire) {
    ctr.inc()
  }

  when(ShiftRegister(write.req.fire, cfg.latency)) {
    ctrResp.inc()
  }

  when(write.resp.ready && ctrResp.notZero) {
    write.resp.enq(0.U)
    ctr.dec()
    ctrResp.dec()
  }
}

case class ReadWriteToRawBridgeConfig(
    val wAddr: Int,
    val wData: Int,
    val latencyRead: Int,
    val latencyWrite: Int,
    val numOutstandingRead: Int,
    val numOutstandingWrite: Int
)

class ReadWriteToRawBridge(val cfg: ReadWriteToRawBridgeConfig) extends Module {
  val read = IO(new ReadInterface(cfg.wAddr, cfg.wData))
  val write = IO(new WriteInterface(cfg.wAddr, cfg.wData))
  val raw = IO(Flipped(new RawInterface(cfg.wAddr, cfg.wData, true, true)))

  private val ctrRead = Module(new chext.util.Counter(cfg.numOutstandingRead + 1))
  ctrRead.noInc()
  ctrRead.noDec()

  read.req.nodeq()
  read.resp.noenq()

  private val ctrWrite = Module(new chext.util.Counter(cfg.numOutstandingWrite + 1))
  ctrWrite.noInc()
  ctrWrite.noDec()

  private val ctrWriteResp = Module(
    new chext.util.Counter(cfg.numOutstandingWrite + 1)
  )
  ctrWriteResp.noInc()
  ctrWriteResp.noDec()

  raw.addr := DontCare
  raw.dIn := DontCare
  raw.wstrb := 0.U

  write.req.nodeq()
  write.resp.noenq()

  prefix("arbiter") {
    val canAcceptRead = ctrRead.notFull && read.req.valid
    val canAcceptWrite = ctrWrite.notFull && write.req.valid

    val chooser =
      chext.elastic.Chooser.rr(VecInit(canAcceptRead, canAcceptWrite))

    when(canAcceptRead && chooser.choice === 0.U) {
      raw.addr := read.req.bits

      read.req.deq()
      chooser.updateState
    }.elsewhen(canAcceptWrite && chooser.choice === 1.U) {
      raw.addr := write.req.bits.addr
      raw.dIn := write.req.bits.data
      raw.wstrb := write.req.bits.strb

      write.req.deq()
      chooser.updateState
    }
  }

  prefix("read") {
    val dataQueue = Module(
      new Queue(
        read.resp.bits.cloneType,
        cfg.numOutstandingRead,
        flow = true,
        pipe = true
      )
    )

    val dataQueueEnq = dataQueue.io.enq
    val dataQueueDeq = dataQueue.io.deq

    dataQueueEnq.noenq()
    dataQueueDeq.nodeq()

    when(read.req.fire) {
      ctrRead.inc()
    }

    when(ShiftRegister(read.req.fire, cfg.latencyRead)) {
      dataQueueEnq.enq(raw.dOut)
    }

    when(read.resp.ready && dataQueueDeq.valid) {
      read.resp.enq(dataQueueDeq.deq())
      ctrRead.dec()
    }
  }

  prefix("write") {
    when(write.req.fire) {
      ctrWrite.inc()
    }

    when(ShiftRegister(write.req.fire, cfg.latencyWrite)) {
      ctrWriteResp.inc()
    }
    when(write.resp.ready && ctrWriteResp.notZero) {
      write.resp.enq(0.U)
      ctrWrite.dec()
      ctrWriteResp.dec()
    }
  }
}
