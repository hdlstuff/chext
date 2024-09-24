package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

import chext.amba.axi4
import chext.elastic

import elastic.ConnectOp._

import axi4.full.{
    AddressChannel,
    WriteDataChannel,
    ReadDataChannel,
    WriteResponseChannel,
    SlaveBuffer,
    MasterBuffer
}

case class IdDemuxConfig(
    val axiCfg: chext.amba.axi4.Config,
    val idSelBits: Seq[Int],
    val demuxCfg: DemuxConfig = DemuxConfig(),
    val numIdsTrackedRead: Int = 4,
    val numIdsTrackedWrite: Int = 4,
    val numOutstandingRead: Int = 16,
    val numOutstandingWrite: Int = 16,
    val capacityPortQueueW: Int = 8,
    val slaveBuffers: axi4.BufferConfig = axi4.BufferConfig.all(2),
    val masterBuffers: axi4.BufferConfig = axi4.BufferConfig.all(0),
    val arbiterPolicy: elastic.Chooser.ChooserFn = elastic.Chooser.rr
) {
  require(!axiCfg.lite)
  require(axiCfg.read || axiCfg.write)
  
  require(numIdsTrackedRead > 0)
  require(numIdsTrackedWrite > 0)
  require(numOutstandingRead > 0)
  require(numOutstandingWrite > 0)

  val numMasters = 1 << idSelBits.length

  val wIdTrackedRead: Int = log2Ceil(numIdsTrackedRead + 1)
  val wIdTrackedWrite: Int = log2Ceil(numIdsTrackedWrite + 1)
  val wOutstandingRead: Int = log2Ceil(numOutstandingRead + 1)
  val wOutstandingWrite: Int = log2Ceil(numOutstandingWrite + 1)
}

class IdDemux(val cfg: IdDemuxConfig) extends Module {
  import cfg._

  override def desiredName: String = "axi4FullDemux"

  val s_axi = IO(axi4.full.Slave(axiCfg))
  val m_axi = IO(Vec(numMasters, axi4.full.Master(axiCfg)))

  private val s_axi_ = SlaveBuffer(s_axi, demuxCfg.slaveBuffers)
  private val m_axi_ = m_axi.map { (x) =>
    MasterBuffer(x, demuxCfg.masterBuffers)
  }

  private val genSelect = UInt(idSelBits.length.W)

  private def implRead(): Unit = prefix("read") {
    def arLogic: Unit = {
      val demuxInput = Wire(Irrevocable(s_axi_.ar.bits.cloneType))
      val demuxSelect = Wire(Irrevocable(genSelect))

      new elastic.Fork(s_axi_.ar) {
        override protected def onFork = {
          // TODO
          fork(in) :=> demuxInput
          fork(in.id) :=> demuxSelect
        }
      }

      chext.elastic.Demux(
        demuxInput,
        m_axi_.map { _.ar },
        demuxSelect
      )
    }

    // TODO: construct the complete ID using the port number
    def rLogic: Unit = {
      // R channel supports burst interleaving, so no isLastFn
      chext.elastic.Arbiter(
        m_axi_.map { _.r },
        s_axi_.r,
        demuxCfg.arbiterPolicy
      )
    }

    arLogic
    rLogic
  }

  private def implWrite(): Unit = prefix("write") {
    val portQueue = Module(
      new Queue(
        genSelect,
        demuxCfg.capacityPortQueueW,
        flow = true,
        pipe = true
      )
    )

    def awLogic: Unit = {
      val genAwPort = new chext.bundles.Bundle2(s_axi_.aw.bits.cloneType, genSelect)
      val awPort = Wire(Irrevocable(genAwPort))

      val demuxInput = Wire(Irrevocable(s_axi_.aw.bits.cloneType))
      val demuxSelect = Wire(Irrevocable(genSelect))

      new elastic.Fork(s_axi_.aw) {
        override protected def onFork = {
          fork(in) :=> demuxInput

          // TODO: extract the bits/modify
          fork(in.id) :=> demuxSelect
          fork(in.id) :=> portQueue.io.enq
        }
      }

      chext.elastic.Demux(demuxInput, m_axi_.map { _.aw }, demuxSelect)
    }

    def wLogic: Unit = {
      // W channel does not support burst interleaving due to the selection logic
      // so isLastFn
      chext.elastic.Demux(
        s_axi_.w,
        m_axi_.map { _.w },
        portQueue.io.deq,
        isLastFn = (x: WriteDataChannel) => x.last
      )
    }

    // TODO: construct the complete ID using the port number
    def bLogic: Unit = {
      chext.elastic.Arbiter(
        m_axi_.map { _.b },
        s_axi_.b,
        demuxCfg.arbiterPolicy
      )
    }

    awLogic
    wLogic
    bLogic
  }

  if (axiCfg.read) implRead()
  if (axiCfg.write) implWrite()
}
