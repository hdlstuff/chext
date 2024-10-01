package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.amba.axi4
import chext.elastic
import chext.util.BitOps._

import elastic.{Source, Sink, SinkBuffer}
import elastic.ConnectOp._

case class ParallelizeConfig(
    val axiSlaveCfg: axi4.Config = axi4.Config(wId = 0, wAddr = 12, wData = 64),
    val wIdMaster: Int = 3
) {
  require(axiSlaveCfg.wId == 0)

  val axiMasterCfg = axiSlaveCfg.copy(wId = wIdMaster)
}

class IdFreeList(wId: Int) extends Module {
  override val desiredName = f"IdFreeList_$wId"

  val genId = UInt(wId.W)

  val source = IO(Source(Irrevocable(genId)))
  val sink = IO(Sink(Irrevocable(genId)))

  private val index = RegInit(0.U((wId + 1).W))
  private val queue = Module(new Queue(genId, 1 << wId))

  when(index.dropLsbN(wId) === 1.U) {
    source :=> queue.io.enq
    queue.io.deq :=> sink
  }.otherwise {
    queue.io.enq.bits := index
    queue.io.enq.valid := true.B

    when(queue.io.enq.fire) {
      index := index + 1.U
    }

    queue.io.deq.ready := false.B

    source.ready := false.B

    sink.bits := DontCare
    sink.valid := false.B
  }
}

class IdQueue(wId: Int) extends Module {
  override val desiredName = f"IdQueue_$wId"

  val genId = UInt(wId.W)

  val source = IO(Source(Irrevocable(genId)))
  val sink = IO(Sink(Irrevocable(genId)))

  private val queue = Module(new Queue(genId, 1 << wId))

  source :=> queue.io.enq
  queue.io.deq :=> sink
}

class Parallelize(val cfg: ParallelizeConfig = ParallelizeConfig()) extends Module {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.full.Master(axiMasterCfg))

  // NOTE: I use SinkBuffer(...) to avoid combinational loops

  def implRead(): Unit = {
    val wBufferIdx = 3
    val buffer = Mem(1 << wBufferIdx, chiselTypeOf(s_axi.r.bits))
    val xIdxFill = Mem(1 << wIdMaster, UInt(wBufferIdx.W))
    val xIdxDrain = Mem(1 << wIdMaster, UInt(wBufferIdx.W))
    val xComplete = Mem(1 << wIdMaster, Bool())

    val idFreeList = Module(new IdFreeList(wIdMaster))
    val idQueue = Module(new IdQueue(wIdMaster))

    val bufferFree = RegInit((1L << wBufferIdx).U((wBufferIdx + 1).W))
    val bufferIdxNext = RegInit(0.U(wBufferIdx.W))

    val s_ar = s_axi.ar
    val m_ar = SinkBuffer(m_axi.ar)

    val s_r = SinkBuffer(s_axi.r)
    val m_r = m_axi.r

    val xLength = s_ar.bits.len + 1.U

    s_ar.ready :=
      m_ar.ready &&
        idFreeList.sink.valid &&
        idQueue.source.ready &&
        (bufferFree >= xLength)

    m_ar.bits := s_ar.bits
    m_ar.bits.id := idFreeList.sink.bits
    m_ar.valid := s_ar.fire

    m_r.ready := s_r.ready && idFreeList.source.ready

    s_r.valid := idQueue.sink.valid && xComplete(idQueue.sink.bits)
    s_r.bits := buffer(xIdxDrain(idQueue.sink.bits))

    idFreeList.source.bits := m_r.bits.id
    idFreeList.source.valid := m_r.fire && m_r.bits.last

    idFreeList.sink.ready := s_ar.fire

    idQueue.source.bits := idFreeList.sink.bits
    idQueue.source.valid := s_ar.fire

    idQueue.sink.ready := s_r.fire && s_r.bits.last

    when(s_ar.fire) {
      val idNext = idFreeList.sink.bits

      bufferFree := bufferFree - xLength
      bufferIdxNext := bufferIdxNext + xLength

      xIdxFill(idNext) := bufferIdxNext
      xIdxDrain(idNext) := bufferIdxNext
      xComplete(idNext) := false.B
    }

    when(m_r.fire) {
      val offset = xIdxFill(m_r.bits.id)

      xIdxFill(m_r.bits.id) := offset + 1.U
      buffer(offset) := m_r.bits

      when(m_r.bits.last) {
        xComplete(m_r.bits.id) := true.B
      }
    }

    when(s_r.fire) {
      xIdxDrain(idQueue.sink.bits) := xIdxDrain(idQueue.sink.bits) + 1.U
    }
  }

  def implWrite(): Unit = {
    val s_aw = s_axi.aw
    val m_aw = SinkBuffer(m_axi.aw)

    val s_w = s_axi.w
    val m_w = SinkBuffer(m_axi.w)

    val s_b = SinkBuffer(s_axi.b)
    val m_b = m_axi.b

    s_aw :=> m_aw
    s_w :=> m_w
    m_b :=> s_b
  }

  if (axiSlaveCfg.read) implRead()
  if (axiSlaveCfg.write) implWrite()
}

object EmitParallelize extends App {
  emitVerilog(new Parallelize, Array("--target-dir", "output/"))
}
