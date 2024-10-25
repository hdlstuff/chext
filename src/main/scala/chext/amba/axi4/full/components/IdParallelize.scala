package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

import chext.util.BitOps._
import chext.bundles.BundleN

import chext.amba.axi4
import chext.elastic

import elastic.{Source, Sink, SinkBuffer}
import elastic.ConnectOp._

import chext.ip.memory

case class IdParallelizeConfig(
    val axiSlaveCfg: axi4.Config = axi4.Config(wId = 0, wAddr = 12, wData = 64),
    val wIdMaster: Int = 3,
    val wBufferIdx: Int = 10,
    val useSyncMem: Boolean = true
) {
  require(axiSlaveCfg.wId == 0)
  val axiMasterCfg = axiSlaveCfg.copy(wId = wIdMaster)

  // pedantic, to avoid overflows in the calculations
  assert(wIdMaster <= 30)
  assert(wBufferIdx <= 30)
}

/** Creates a memory that supports synchronous writes, elastic reads. If `numOutstandingRead` is
  * zero, uses `Mem` primitive with combinational reads. Otherwise uses the `SRAM` primitive, which
  * has 1-cycle read latency.
  *
  * @param wAddr
  * @param gen
  * @param numOutstandingRead
  */
private class SyncWriteElasticReadMemory[T <: Data](
    wAddr: Int,
    gen: T,
    numOutstandingRead: Int = 4
) extends Module {
  require(wAddr >= 0)
  require(wAddr <= 30)
  require(numOutstandingRead >= 0)

  private val genAddr = UInt(wAddr.W)
  private val rdLatency = 1

  val io = IO(new Bundle {
    val wrEn = Input(Bool())
    val wrAddr = Input(genAddr)
    val wrData = Input(gen)

    val rdReq = Flipped(Decoupled(genAddr))
    val rdResp = Decoupled(gen)
  })

  if (numOutstandingRead > 0) {
    val sram = SRAM(1 << wAddr, gen, 1, 1, 0)

    sram.writePorts(0).enable := io.wrEn
    sram.writePorts(0).address := io.wrAddr
    sram.writePorts(0).data := io.wrData

    val rdCounter = Module(new chext.util.Counter(numOutstandingRead + 1))
    rdCounter.noInc()
    rdCounter.noDec()

    val rdQueue = Module(new Queue(gen, numOutstandingRead))
    rdQueue.io.enq.noenq()

    io.rdReq.ready := rdCounter.notFull
    rdQueue.io.deq :=> io.rdResp

    sram.readPorts(0).address := DontCare
    sram.readPorts(0).enable := true.B // TODO check this

    when(io.rdReq.fire) {
      sram.readPorts(0).address := io.rdReq.bits
      rdCounter.inc()
    }

    when(ShiftRegister(io.rdReq.fire, rdLatency)) {
      rdQueue.io.enq.enq(sram.readPorts(0).data)
    }

    when(io.rdResp.fire) {
      rdCounter.dec()
    }
  }
  else {
    val mem = Mem(1 << wAddr, gen)

    when (io.wrEn) {
      mem.write(io.wrAddr, io.wrData)
    }

    io.rdReq.ready := io.rdResp.ready
    io.rdResp.valid := io.rdReq.valid
    io.rdResp.bits := mem.read(io.rdReq.bits)
  }

}

class IdParallelize(cfg: IdParallelizeConfig = IdParallelizeConfig()) extends Module {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.full.Master(axiMasterCfg))

  def implRead(): Unit = prefix("read") {
    val s_ar = s_axi.ar
    val m_ar = SinkBuffer(m_axi.ar)

    val s_r = s_axi.r
    val m_r = m_axi.r

    val genIndex = UInt(wBufferIdx.W)
    val capacity = (1 << wBufferIdx).U((wBufferIdx + 1).W)

    val stComplete = 0.U
    val stStarted = 1.U

    val memStatus = Mem(1 << wIdMaster, Bool())
    val memIdxFill = Mem(1 << wIdMaster, genIndex)

    val memRespValid = Mem(1 << wBufferIdx, Bool())
    val memRespPayload = Module(
      new SyncWriteElasticReadMemory(
        wBufferIdx,
        chiselTypeOf(s_axi.r.bits),
        if (useSyncMem) (1 << wIdMaster) else 0
      )
    )

    // 1 extra bit set when no more IDs are available
    val nextIdFill = RegInit(0.U((wIdMaster + 1).W))

    val nextIdxFill = RegInit(0.U(wBufferIdx.W))
    val nextIdxDrain = RegInit(0.U(wBufferIdx.W))

    val available = RegInit(capacity)

    val transactionCount = Module(new chext.util.Counter((1 << wIdMaster) + 1))
    transactionCount.noInc()
    transactionCount.noDec()

    s_ar.ready :=
      m_ar.ready &&
        !nextIdFill.dropLsbN(wIdMaster) &&
        (available >= (s_ar.bits.len + 1.U))

    m_ar.bits := s_ar.bits
    m_ar.bits.id := nextIdFill
    m_ar.valid := s_ar.fire // m_ar.valid := m_ar.ready (comb loop) && s_ar.valid && ...

    m_r.ready := memStatus(m_r.bits.id) === stStarted

    memRespPayload.io.rdResp :=> s_r

    if (false) {
      // debug messages, enable them if needed
      val mon1 = new chext.util.BackpressureMonitor(s_ar, "s_ar")
      val mon2 = new chext.util.BackpressureMonitor(s_r, "s_r")
      val mon3 = new chext.util.BackpressureMonitor(m_ar, "m_ar")
      val mon4 = new chext.util.BackpressureMonitor(m_r, "m_r")
    }

    when(s_ar.fire /* eqv to m_ar.fire */ ) {
      memStatus(nextIdFill) := stStarted
      memIdxFill(nextIdFill) := nextIdxFill

      nextIdxFill := nextIdxFill + s_ar.bits.len + 1.U
      nextIdFill := nextIdFill + 1.U
      available := available - (s_ar.bits.len + 1.U)

      transactionCount.inc()
    }

    memRespPayload.io.wrEn := m_r.fire
    memRespPayload.io.wrAddr := memIdxFill(m_r.bits.id)
    memRespPayload.io.wrData := m_r.bits

    when(m_r.fire) {
      memStatus(m_r.bits.id) := !m_r.bits.last
      memIdxFill(m_r.bits.id) := memIdxFill(m_r.bits.id) + 1.U
      memRespValid.write(memIdxFill(m_r.bits.id), true.B)
    }

    memRespPayload.io.rdReq.bits := nextIdxDrain
    memRespPayload.io.rdReq.valid := memRespValid.read(nextIdxDrain)

    when(memRespPayload.io.rdReq.fire) {
      memRespValid.write(nextIdxDrain, false.B)
      nextIdxDrain := nextIdxDrain + 1.U
    }

    when(s_r.fire) {
      when(s_r.bits.last) {
        transactionCount.dec()
      }
    }

    when(transactionCount.zero && !s_ar.fire && !s_r.fire /* protect writes */ ) {
      nextIdFill := 0.U
      nextIdxFill := 0.U
      nextIdxDrain := 0.U

      available := capacity
    }
  }

  def implWrite(): Unit = prefix("write") {
    val s_aw = s_axi.aw
    val m_aw = SinkBuffer(m_axi.aw)

    val s_b = SinkBuffer(s_axi.b)
    val m_b = m_axi.b

    val mem = Mem(1 << wIdMaster, chext.bundles.BundleN(Bool(), chiselTypeOf(s_axi.b.bits)))

    val nextIdFill = Reg(UInt((wIdMaster + 1).W))
    val nextIdDrain = Reg(UInt(wIdMaster.W))

    val transactionCount = Module(new chext.util.Counter((1 << wIdMaster) + 1))
    transactionCount.noInc()
    transactionCount.noDec()

    s_aw.ready := m_aw.ready && !nextIdFill.dropLsbN(wIdMaster)

    m_aw.bits := s_aw.bits
    m_aw.bits.id := nextIdFill
    m_aw.valid := s_aw.fire // m_ar.valid := m_ar.ready (comb loop) && s_ar.valid && ...

    m_b.ready := true.B // we always have space in the buffer if the transaction goes through

    s_b.valid := transactionCount.notZero && mem(nextIdDrain)._1
    s_b.bits := mem(nextIdDrain)._2

    when(s_aw.fire) {
      nextIdFill := nextIdFill + 1.U
      transactionCount.inc()
    }

    when(m_b.fire) {
      mem(m_b.bits.id)._1 := true.B
      mem(m_b.bits.id)._2 := m_b.bits
    }

    when(s_b.fire) {
      mem(nextIdDrain)._1 := false.B

      nextIdDrain := nextIdDrain + 1.U
      transactionCount.dec()
    }

    when(transactionCount.zero && !s_aw.fire && !s_b.fire) {
      nextIdFill := 0.U
      nextIdDrain := 0.U
    }

    s_axi.w :=> m_axi.w
  }

  if (axiSlaveCfg.read)
    implRead()

  if (axiSlaveCfg.write)
    implWrite()
}
