package chext.amba.axi4.full.components

import chext.amba.axi4
import chext.elastic

import chisel3._
import chisel3.util._
import chisel3.experimental._

import elastic._
import elastic.ConnectOp._

import axi4.Casts._
import axi4.full.{SlaveBuffer, MasterBuffer, WriteDataChannel}

case class MuxConfig(
    val slaveBuffers: axi4.BufferConfig = axi4.BufferConfig.all(0),
    val masterBuffers: axi4.BufferConfig = axi4.BufferConfig.all(2),
    val arbiterPolicy: Chooser.ChooserFn = Chooser.rr
)

class Mux(
    val axiCfgSlave: axi4.Config,
    val numSlaves: Int = 4,
    val muxCfg: MuxConfig = MuxConfig()
) extends Module {
  require(!axiCfgSlave.lite)
  require(axiCfgSlave.read || axiCfgSlave.write)
  require(numSlaves > 0)

  override def desiredName: String = "axi4FullMux"

  private val wPort = log2Ceil(numSlaves)
  private val genPort = UInt(wPort.W)
  val axiCfgMaster = axiCfgSlave.copy(wId = axiCfgSlave.wId + wPort)

  val s_axi = IO(Vec(numSlaves, axi4.full.Slave(axiCfgSlave)))
  val m_axi = IO(axi4.full.Master(axiCfgMaster))

  private val s_axi_ = {
    val result = Wire(Vec(numSlaves, axi4.full.Interface(axiCfgMaster)))

    val buffered = s_axi.map { (x) =>
      SlaveBuffer(x, muxCfg.slaveBuffers)
    }

    if (axiCfgSlave.read)
      helpers.IdExtend.read(buffered, result)

    if (axiCfgSlave.write)
      helpers.IdExtend.write(buffered, result)

    result
  }
  private val m_axi_ = MasterBuffer(m_axi, muxCfg.masterBuffers)

  private def implRead(): Unit = prefix("read") {
    def arLogic: Unit = {
      elastic.Arbiter(s_axi_.map { _.ar }, m_axi_.ar, muxCfg.arbiterPolicy)
    }

    def rLogic: Unit = {
      val demuxInput = Wire(Irrevocable(m_axi_.r.bits.cloneType))
      val demuxSelect = Wire(Irrevocable(UInt(wPort.W)))

      new Fork(m_axi_.r) {
        protected def onFork: Unit = {
          fork { in } :=> demuxInput
          fork { in.id >> axiCfgSlave.wId } :=> demuxSelect
        }
      }

      // R channel supports burst interleaving, so no isLastFn
      elastic.Demux(demuxInput, s_axi_.map { _.r }, demuxSelect)
    }

    arLogic
    rLogic
  }

  private def implWrite(): Unit = prefix("write") {
    val portQueue = Module(new Queue(genPort, 32, flow = true, pipe = true))

    def awLogic: Unit = {
      elastic.Arbiter(
        s_axi_.map { _.aw },
        m_axi_.aw,
        muxCfg.arbiterPolicy,
        Some(portQueue.io.enq)
      )
    }

    def wLogic: Unit = {
      // W channel does not support burst interleaving due to the selection logic
      // so isLastFn
      elastic.Mux(
        s_axi_.map { _.w },
        m_axi_.w,
        portQueue.io.deq,
        isLastFn = (x: WriteDataChannel) => x.last
      )
    }

    def bLogic: Unit = {
      val demuxInput = Wire(Irrevocable(m_axi_.b.bits.cloneType))
      val demuxSelect = Wire(Irrevocable(UInt(wPort.W)))

      new Fork(m_axi_.b) {
        protected def onFork: Unit = {
          fork { in } :=> demuxInput
          fork { in.id >> axiCfgSlave.wId } :=> demuxSelect
        }
      }

      elastic.Demux(demuxInput, s_axi_.map { _.b }, demuxSelect)
    }

    awLogic
    wLogic
    bLogic
  }

  if (axiCfgSlave.read) implRead()
  if (axiCfgSlave.write) implWrite()
}

object MuxEmitter extends App {
  def muxModule = new Mux(
    axi4.Config(
      wId = 4,
      wAddr = 32,
      wData = 256,
      read = true,
      write = true,
      lite = false
    ),
    numSlaves = 8
  )

  emitVerilog(muxModule, Array("--target-dir", "output/"))
}
