package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.elastic

import chext.amba.axi4
import axi4.Ops._

import chext.util.NamedVec

class Interconnect_Tbtop(
    override val desiredName: String
) extends Module
    with chext.TestBenchTop {
  val axiCfg = axi4.Config(wId = 2, wAddr = 32, wData = 32)

  val S_AXI = IO(axi4.Slave.many(16, axiCfg, NamedVec.ZeroExtended()))
  val M_AXI = IO(axi4.Master.many(16, axiCfg.copy(wId = 6), NamedVec.ZeroExtended()))

  {
    val demux_N = Seq.tabulate(16) {
      case (n) => {
        val demuxCfg = DemuxConfig(axiCfg, 16, _ >> 12, arbiterPolicy = elastic.Chooser.priority)
        Module(new Demux(demuxCfg))
      }
    }

    val mux_N = Seq.tabulate(16) {
      case (n) => {
        val muxCfg = MuxConfig(axiCfg, 16, arbiterPolicy = elastic.Chooser.priority)
        Module(new Mux(muxCfg))
      }
    }

    S_AXI.map { _.asFull } :=> demux_N.map { _.s_axi }

    mux_N.map { _.m_axi } :=> M_AXI.map { _.asFull }

    for (i <- (0 until 16))
      for (j <- (0 until 16)) {
        demux_N(i).m_axi(j) :=> mux_N(j).s_axi(i)
      }
  }

  declareClock(clock)
  declareReset(reset)
  S_AXI.foreach { declareAxi4Interface(_) }
  M_AXI.foreach { declareAxi4Interface(_) }
}

object Interconnect_Tb extends chext.TestBench {
  emit(new Interconnect_Tbtop("Interconnect_Tbtop_1"))
}
