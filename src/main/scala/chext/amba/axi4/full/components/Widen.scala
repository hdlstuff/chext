package chext.amba.axi4.full.components

import chisel3._
import chisel3.util.log2Ceil
import chisel3.util.{Fill, Cat}

import chext.elastic
import elastic.ConnectOp._

import chext.amba.axi4

import chisel3.experimental.{prefix, AffectsChiselPrefix}

case class WidenConfig(
    val axiCfg: axi4.Config,
    val numOutstandingRead: Int = 32,
    val numOutstandingWrite: Int = 32
) {
  private val require_ = chext.util.Require.inferred()

  require_(!axiCfg.lite, "Widen requires an AXI4-Full interface.")
}

/** Converts narrow transfers to full-sized transfers.
  *
  * @note
  *   Does not work for FIXED bursts.
  */
class Widen(val cfg: WidenConfig) extends Module with chext.AnnotatedModule {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiCfg))
  val m_axi = IO(axi4.full.Master(axiCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)
  declareAxi4Interface(m_axi)

  private class Control extends Bundle {
    val beatFirst = Bool()
    val beatLast = Bool()

    val transferFirst = Bool()
    val transferLast = Bool()
  }

  private val genControl = new Control

  /*
  private def alignDown(x: UInt, size: UInt, width: Int) = {
    x & ~((size - 1.U).pad(width))
  }

  private def alignUp(x: UInt, size: UInt, width: Int) = {
    alignDown(x + (size - 1.U), size, width)
  }
   */

  /** Transforms the address packet to match the target AxSIZE.
    */
  private def transformAx(
      source: elastic.Interface[axi4.full.AddressChannel],
      sink: elastic.Interface[axi4.full.AddressChannel],
      size: Int
  ): AffectsChiselPrefix = {
    // calculates the size of the master-side AXI transaction
    // Please check Transaction.hpp, simpleRead to understand the logic
    new elastic.Transform(source, sink) {
      out := in

      val mask0 = WireInit((1.U << in.size) - 1.U)
      val mask1 = WireInit(((1 << size) - 1).U)

      // .pad is necessary, otherwise the negated signal is extended with zeros,
      // corrupting the address
      // HARDCODED: addr(7, 0) and .pad(8)
      val addr0 = WireInit(in.addr(7, 0) & ~(mask0.pad(8)))
      val addr1 = WireInit(in.addr(7, 0) & ~(mask1.pad(8)))

      // aligned transfer size
      // pad(9) and pad(17) to avoid overflows/underflows
      val dtsize = ((in.len.pad(9) + 1.U) << in.size).pad(17) +% addr0 -% addr1

      val len0 = WireInit(dtsize >> size.U)
      val len1 = WireInit(Mux((dtsize & mask1) > 0.U, len0 +% 1.U, len0))

      out.size := size.U
      out.len := len1 -% 1.U

      // left here for debugging purposes
      dontTouch(mask0)
      dontTouch(mask1)
      dontTouch(addr0)
      dontTouch(addr1)
      dontTouch(dtsize)
      dontTouch(len0)
      dontTouch(len1)
    }
  }

  private def generateControl(
      source: elastic.Interface[axi4.full.AddressChannel],
      sink: elastic.Interface[Control],
      size: Int
  ): AffectsChiselPrefix = new AffectsChiselPrefix {
    val gen0 = new Bundle {
      val index = UInt(7.W)
      val size = UInt(3.W)

      val first = Bool()
      val last = Bool()
    }

    val wire0 = elastic.EWire(gen0)

    val transducer0 = new elastic.Transducer(source, wire0) {
      val state = RegInit(false.B)

      val mask = WireInit((1.U << (size.U - in.size)) -% 1.U)

      val index = RegInit(0.U(7.W))
      val nextIndex = Wire(chiselTypeOf(index))

      val counter = RegInit(0.U(8.W))
      val nextCounter = Wire(chiselTypeOf(counter))

      val first = Wire(Bool())
      val last = Wire(Bool())

      nextIndex := Mux(
        state,
        Mux(
          (index & mask) === mask,
          0.U,
          index + 1.U
        ),
        (in.addr >> in.size) & mask
      )

      nextCounter := Mux(
        last,
        0.U,
        counter + 1.U
      )

      first := !state
      last := Mux(
        state,
        counter === in.len,
        in.len === 0.U
      )

      out.index := nextIndex
      out.size := in.size

      out.first := first
      out.last := last

      packet {
        when(state) {
          when(last) {
            accept {
              state := false.B

              index := nextIndex
              counter := nextCounter
            }
          }.otherwise {
            produce {
              index := nextIndex
              counter := nextCounter
            }
          }
        }.otherwise {
          when(last) {
            accept {
              index := nextIndex
              counter := nextCounter
            }
          }.otherwise {
            produce {
              state := true.B

              index := nextIndex
              counter := nextCounter
            }
          }
        }
      }
    }

    dontTouch(wire0)

    val transform0 = new elastic.Transform(wire0, sink) {
      val mask = WireInit((1.U << (size.U -% in.size)) - 1.U)

      out.beatFirst := in.first || ((in.index & mask) === 0.U)
      out.beatLast := in.last || ((in.index & mask) === mask)

      out.transferFirst := in.first
      out.transferLast := in.last
    }
  }

  private def implRead() = prefix("read") {
    val ewireControl = elastic.EWire(genControl)
    val ewireTransferLast = elastic.EWire(Bool())
    val ewireBeatFirst = elastic.EWire(Bool())
    val ewireBeatLast = elastic.EWire(Bool())

    dontTouch(ewireControl)
    dontTouch(ewireTransferLast)
    dontTouch(ewireBeatFirst)
    dontTouch(ewireBeatLast)

    val fork0 = new elastic.Fork(s_axi.ar) {
      val transform0 = transformAx(
        fork(),
        m_axi.ar,
        log2Ceil(axiCfg.wData) - 3
      )

      val control0 = generateControl(
        elastic.SourceBuffer(fork(), numOutstandingRead),
        ewireControl,
        log2Ceil(axiCfg.wData) - 3
      )
    }

    val fork1 = new elastic.Fork(ewireControl) {
      fork { in.transferLast } :=> ewireTransferLast
      fork { in.beatFirst } :=> ewireBeatFirst
      fork { in.beatLast } :=> ewireBeatLast
    }

    val genData = chiselTypeOf(m_axi.r.$bits)

    val ewireMuxSink = elastic.EWire(genData)
    val ewireMuxSink0 = elastic.EWire(genData)
    val ewireDemuxSource = elastic.EWire(genData)
    val ewireDemuxSink = elastic.EWire(genData)

    val queue0 = elastic.Queue(genData, 2)

    val mux0 = new elastic.Mux(
      Seq(queue0.sink, m_axi.r),
      ewireMuxSink,
      ewireBeatFirst
    )

    val demux0 = new elastic.Demux(
      ewireDemuxSource,
      Seq(queue0.source, ewireDemuxSink),
      ewireBeatLast
    )

    val fork2 = new elastic.Fork(ewireMuxSink) {
      fork() :=> ewireDemuxSource
      fork() :=> ewireMuxSink0
    }

    val join0 = new elastic.Join(s_axi.r) {
      out := join(ewireMuxSink0)
      out.last := join(ewireTransferLast)
    }

    ewireDemuxSink.deq() // disposed
    ewireDemuxSink.markSource()
  }

  private def implWrite() = prefix("write") {
    val ewireControl = elastic.EWire(genControl)
    val ewireTransferLast = elastic.EWire(Bool())
    val ewireBeatFirst = elastic.EWire(Bool())
    val ewireBeatLast = elastic.EWire(Bool())

    dontTouch(ewireControl)
    dontTouch(ewireTransferLast)
    dontTouch(ewireBeatFirst)
    dontTouch(ewireBeatLast)

    val fork0 = new elastic.Fork(s_axi.aw) {
      val transform0 = transformAx(
        fork(),
        m_axi.aw,
        log2Ceil(axiCfg.wData) - 3
      )

      val control0 = generateControl(
        elastic.SourceBuffer(fork(), numOutstandingWrite),
        ewireControl,
        log2Ceil(axiCfg.wData) - 3
      )
    }

    val fork1 = new elastic.Fork(ewireControl) {
      fork { in.transferLast } :=> ewireTransferLast
      fork { in.beatFirst } :=> ewireBeatFirst
      fork { in.beatLast } :=> ewireBeatLast
    }

    val genData = chiselTypeOf(m_axi.w.$bits)

    val ewireW0 = elastic.EWire(genData)
    val ewireMuxSink = elastic.EWire(genData)
    val ewireDemuxSource = elastic.EWire(genData)

    val queue0 = elastic.Queue(genData, 2)

    val const0 = new elastic.Const(ewireW0) {
      out.data := 0.U
      out.strb := 0.U
      out.last := false.B
      out.user := 0.U
    }

    val mux0 = new elastic.Mux(
      Seq(queue0.sink, ewireW0),
      ewireMuxSink,
      ewireBeatFirst
    )

    val demux0 = new elastic.Demux(
      ewireDemuxSource,
      Seq(queue0.source, m_axi.w),
      ewireBeatLast
    )

    val join0 = new elastic.Join(ewireDemuxSource) {
      val opA = join(ewireMuxSink)
      val opB = join(s_axi.w)
      val transferLast = join(ewireTransferLast)

      val mask = Cat(opB.strb.asBools.reverse.map { x => Fill(8, x) })
      dontTouch(mask)

      out.data := opA.data | (opB.data & mask)
      out.strb := opA.strb | opB.strb

      // or, equivalently, we can also use opB.last ?
      // TODO: assert that they are the same thing
      out.last := transferLast

      out.user := 0.U
    }

    m_axi.b :=> s_axi.b

  }

  if (axiCfg.read)
    implRead()

  if (axiCfg.write)
    implWrite()

}
