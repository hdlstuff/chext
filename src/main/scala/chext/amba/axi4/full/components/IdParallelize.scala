package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.bundles.BundleN

import chext.amba.axi4
import chext.elastic

import elastic.{Source, Sink, SinkBuffer}
import elastic.ConnectOp._
import chisel3.experimental.prefix
import chext.util.BitOps.UIntOps_impl

case class IdParallelizeConfig(
    val axiSlaveCfg: axi4.Config = axi4.Config(wId = 0, wAddr = 12, wData = 64),
    val wIdMaster: Int = 3,
    val wBufferIdx: Int = 10
) {
  require(axiSlaveCfg.wId == 0)
  val axiMasterCfg = axiSlaveCfg.copy(wId = wIdMaster)

  // pedantic, to avoid overflows in the calculations
  assert(wIdMaster <= 30)
  assert(wBufferIdx <= 30)
}

class IdParallelize(cfg: IdParallelizeConfig = IdParallelizeConfig()) extends Module {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.full.Master(axiMasterCfg))

  def implRead(): Unit = prefix("read") {
    val s_ar = s_axi.ar
    val m_ar = SinkBuffer(m_axi.ar)

    val s_r = SinkBuffer(s_axi.r)
    val m_r = m_axi.r

    val genIndex = UInt(wBufferIdx.W)
    val capacity = (1 << wBufferIdx).U((wBufferIdx + 1).W)

    val stComplete = 0.U
    val stStarted = 1.U

    val memStatus = Mem(1 << wIdMaster, UInt(2.W))
    val memIndexFill = Mem(1 << wIdMaster, genIndex)
    val memRespValid = Mem(1 << wBufferIdx, UInt(1.W))
    val memRespPayload = Mem(1 << wBufferIdx, chiselTypeOf(s_axi.r.bits))

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

    s_r.valid := memRespValid(nextIdxDrain)
    s_r.bits := memRespPayload(nextIdxDrain)

    if (true) {
      // debug messages, enable them if needed
      val mon1 = new chext.util.BackpressureMonitor(s_ar, "s_ar")
      val mon2 = new chext.util.BackpressureMonitor(s_r, "s_r")
      val mon3 = new chext.util.BackpressureMonitor(m_ar, "m_ar")
      val mon4 = new chext.util.BackpressureMonitor(m_r, "m_r")
    }

    when(s_ar.fire /* eqv to m_ar.fire */ ) {
      memStatus(nextIdFill) := stStarted
      memIndexFill(nextIdFill) := nextIdxFill

      nextIdxFill := nextIdxFill + s_ar.bits.len + 1.U
      nextIdFill := nextIdFill + 1.U
      available := available - (s_ar.bits.len + 1.U)

      transactionCount.inc()
    }

    when(m_r.fire) {
      memStatus(m_r.bits.id) := !m_r.bits.last

      memIndexFill(m_r.bits.id) := memIndexFill(m_r.bits.id) + 1.U

      memRespValid(memIndexFill(m_r.bits.id)) := true.B
      memRespPayload(memIndexFill(m_r.bits.id)) := m_r.bits
    }

    when(s_r.fire) {
      when(memRespPayload(nextIdxDrain).last) {
        transactionCount.dec()
      }

      memRespValid(nextIdxDrain) := false.B
      nextIdxDrain := nextIdxDrain + 1.U
    }

    when(transactionCount.zero && !s_ar.fire && !s_r.fire && !m_r.fire /* protect writes */ ) {
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

    val nextIdFill = Reg(UInt(wIdMaster.W))
    val nextIdDrain = Reg(UInt(wIdMaster.W))

    val transactionCount = Module(new chext.util.Counter((1 << wIdMaster) + 1))
    transactionCount.noInc()
    transactionCount.noDec()

    s_aw.ready := m_aw.ready && transactionCount.notFull

    m_aw.bits := s_aw.bits
    m_aw.bits.id := nextIdFill
    m_aw.valid := s_aw.fire // m_ar.valid := m_ar.ready (comb loop) && s_ar.valid && ...

    m_b.ready := true.B // we always have space in the buffer if the transaction goes through

    s_b.valid := transactionCount.notZero && mem(nextIdDrain)._1
    s_b.bits := mem(nextIdDrain)._2

    when(s_aw.fire) {
      assert(s_aw.bits.len === 0.U)

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

    when(transactionCount.zero) {
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
