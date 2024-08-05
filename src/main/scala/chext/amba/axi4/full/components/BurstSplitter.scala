package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext._
import chext.elastic._

import chext.amba._
import chext.amba.axi4._
import chext.amba.axi4.full.AddressChannel
import chext.elastic.ConnectOp._
import chext.bundles.Bundle2

/** Splits the burst transactions into single-beat transactions. Assumes only zero bit id is used.
  *
  * @param axiCfg
  *   AXI configuration of the slave interface.
  */
class BurstSplitterZeroId(
    val axiCfg: Config,
    val onFlight: Int = 2
) extends Module {

  // IO of the module
  val s_axi = IO(axi4.full.Slave(axiCfg))
  val m_axi = IO(axi4.full.Master(axiCfg))

  override def desiredName: String = "axi4FullBurstSplitterZeroId"

  // Internal stuff

  private class AddressSplitter[T <: AddressChannel](
      gen: AddressChannel,
      genLen: UInt
  ) extends Module {
    // IO
    val s_axi_ax = IO(Flipped(new IrrevocableIO(gen)))
    val m_axi_ax = IO(new IrrevocableIO(gen))
    val resp_guard = IO(new IrrevocableIO(Bool()))

    // Internal
    private val ag = Module(new addrgen.AddressGenerator(axiCfg.wAddr))
    private val arQueue = Module(new Queue(chiselTypeOf(s_axi.ar.bits), onFlight))
    private val lenQueue = Module(new Queue(genLen, onFlight))

    new Fork(s_axi_ax) {
      protected def onFork: Unit = {
        fork({
          val dat = Wire(new addrgen.AddrLenSizeBurstBundle(axiCfg.wAddr))
          dat.addr := in.addr
          dat.len := in.len
          dat.size := in.size
          dat.burst := in.burst
          dat
        }) :=> ag.source

        fork(in.len) :=> lenQueue.io.enq
        fork(in) :=> arQueue.io.enq
      }
    }

    // Replicate the arQueue data to get the tags for the `ar` packages
    private val replAr = Wire(chiselTypeOf(s_axi_ax))

    new Replicate(arQueue.io.deq, replAr) {
      protected def onReplicate: Unit = {
        len := in.len +& 1.U
        out := in
      }
    }

    new Join(m_axi_ax) {
      protected def onJoin: Unit = {
        out := join(replAr)
        val newAr = join(ag.sink)
        out.addr := newAr.addr
        out.size := newAr.size
        out.burst := BurstType.FIXED
        out.len := 0.U
      }
    }

    // Take the r channel and add set the last bit correctly
    new Replicate(lenQueue.io.deq, resp_guard) {
      protected def onReplicate: Unit = {
        len := in +& 1.U
        out := last
      }
    }

  }

  private def generateRead = {
    val genLen = chiselTypeOf(s_axi.ar.bits.len)
    val addrSpl = Module(new AddressSplitter(chiselTypeOf(s_axi.ar.bits), genLen))

    // Connect `ar` to `ar`
    s_axi.ar :=> addrSpl.s_axi_ax 
    addrSpl.m_axi_ax :=> m_axi.ar

    // Join the `r` channel with the last bit
    new Join(s_axi.r) {
      protected def onJoin: Unit = {
        out := join(m_axi.r)
        out.last := join(addrSpl.resp_guard)
      }
    }
  }

  private def generateWrite = {
    val genLen = chiselTypeOf(s_axi.aw.bits.len)
    val addrSpl = Module(new AddressSplitter(chiselTypeOf(s_axi.aw.bits), genLen))

    // Connect `aw` to `aw`
    s_axi.aw :=> addrSpl.s_axi_ax
    addrSpl.m_axi_ax :=> m_axi.aw

    // Connect `w` to `w`
    new Transform(s_axi.w, m_axi.w) {
      protected def onTransform: Unit = {
        out := in
        out.last := true.B
      }
    }

    // Connect `b`
    // {{ writeResponse | isLast }}
    val bWire = Wire(new IrrevocableIO(new Bundle2(chiselTypeOf(m_axi.b.bits), Bool())))

    new Join(bWire) {
      protected def onJoin: Unit = {
        out._1 := join(m_axi.b)
        out._2 := join(addrSpl.resp_guard)
      }
    }

    val bReg = RegInit(0.U(2.W))

    new Arrival(bWire, s_axi.b) {
      protected def onArrival: Unit = {
        val newResp = chisel3.Mux(bReg === 0.U, in._1.resp, bReg)

        when(in._2) { // if the last `b`
          out := in._1
          out.resp := newResp
          bReg := 0.U
          accept()
        }.otherwise {
          drop()
        }
      }
    }
  }

  if (axiCfg.read) {
    generateRead
  }

  if (axiCfg.write) {
    generateWrite
  }
}

object BurstSplitterEmitter extends App {
  import _root_.circt.stage.ChiselStage

  ChiselStage.emitSystemVerilogFile(
    new BurstSplitterZeroId(Config(
      wAddr = 32
    ), 2),
    Array("--target-dir", "output/"),
    Array("--disable-all-randomization")
  )
}
