package chext.elastic2

import chext.{elastic2 => elastic}

import chisel3._
import chisel3.experimental.AffectsChiselPrefix
import chisel3.experimental.prefix
import chisel3.hacks.deferred

abstract class Reduce[T1 <: Data, T2 <: Data](
    sourceElem: Interface[T1],
    sourceInit: Interface[T2],
    sinkRes: Interface[T2],
    val explicitZeros: Boolean = false,
    val noFirstBit: Boolean = true
) extends AffectsChiselPrefix {
  protected val elem = sourceElem.bits
  protected val gen = chiselTypeOf(sinkRes.bits)

  protected val op_sinkA = dontTouch { Wire(Interface(gen)) }
  protected val op_sinkB = dontTouch { Wire(Interface(gen)) }
  protected val op_sourceRes = { Wire(Interface(gen)) }

  protected var first = dontTouch { Wire(Bool()) }
  protected var last = dontTouch { Wire(Bool()) }
  protected var zero = dontTouch { Wire(Bool()) }

  protected var data = dontTouch { Wire(gen) }

  if (!explicitZeros)
    zero := false.B

  if (noFirstBit)
    first := false.B

  import ConnectOp._

  protected val genStage0 = new Bundle {
    val bits = gen.cloneType

    val first = Bool()
    val last = Bool()
    val zero = Bool()
  }

  protected val genStage1 = new Bundle {
    val bits = gen.cloneType

    val zero = Bool()
  }

  protected val stage0_elem = dontTouch { Wire(Interface(genStage0)) }
  protected val stage0_init = dontTouch { Wire(Interface(gen)) }
  protected val stage0_res = dontTouch { Wire(Interface(gen)) }

  protected val stage1_opA = dontTouch { Wire(Interface(genStage1)) }
  protected val stage1_opB = dontTouch { Wire(Interface(gen)) }
  protected val stage1_res = dontTouch { Wire(Interface(gen)) }

  def stage0(): Unit = prefix("stage0") {
    // stage0 implements the first logic

    if (noFirstBit) {
      val arrival0 = new Arrival(sourceElem, stage0_elem) {
        val rState = RegInit(true.B)

        out.bits := data

        out.first := rState
        out.last := last
        out.zero := zero

        when(arrived) {
          when(rState) {
            when(out.last) {
              accept()
            }.otherwise {
              accept()

              rState := false.B
            }
          }.otherwise {
            rState := out.last
            accept()
          }
        }
      }
    } else {
      val transform0 = new Transform(sourceElem, stage0_elem) {
        out.bits := data

        out.first := first
        out.last := last
        out.zero := zero
      }
    }

    sourceInit :=> stage0_init
    stage0_res :=> sinkRes
  }

  def stage1(): Unit = prefix("stage1") {
    // stage1 implements the reduce logic

    val fork0 = new Fork(stage0_elem) {
      new Transform(fork(), stage1_opA) {
        out.bits := in.bits
        out.zero := in.zero
      }

      val temp = dontTouch { Wire(Interface(gen)) }

      val mux0 = elastic.Mux(
        Seq(temp, stage0_init),
        stage1_opB,
        SourceBuffer(fork { in.first }, flow = true)
      )
      val demux0 =
        Demux(stage1_res, Seq(temp, stage0_res), SourceBuffer(fork { in.last }, flow = true))
    }
  }

  def stage2(): Unit = prefix("stage2") {
    // stage2 implements the zero logic

    if (explicitZeros) {
      val fork0 = new Fork(stage1_opA) {
        val disposed = Wire(Interface(gen))
        val temp = Wire(Interface(gen))

        val demux0 = Demux(fork { in.bits }, Seq(op_sinkA, disposed), fork { in.zero })
        val demux1 = Demux(stage1_opB, Seq(op_sinkB, temp), fork { in.zero })
        val mux0 =
          elastic.Mux(Seq(op_sourceRes, temp), elastic.SinkBuffer(stage1_res), fork { in.zero })

        disposed.deq()
      }
    } else {
      val transform0 = new Transform(stage1_opA, op_sinkA) {
        out := in.bits
      }

      stage1_opB :=> op_sinkB
      op_sourceRes :=> stage1_res
    }
  }

  stage0()
  stage1()
  stage2()
}

class ReduceTestTop1 extends Module with chext.HasHdlinfoModule {
  val sourceElem = IO(Source(UInt(32.W)))
  val sinkRes = IO(Sink(UInt(32.W)))

  private val reduce = new Reduce(sourceElem, Constant(0.U), sinkRes) {
    import chext.util.BitOps._

    last := elem.msbN(1)
    data := elem.dropMsbN(1)

    val join0 = new Join(SinkBuffer(op_sourceRes)) {
      out := join(op_sinkA) + join(op_sinkB)
    }
  }

  def hdlinfoModule: hdlinfo.Module = {
    import hdlinfo._
    import io.circe.generic.auto._
    import scala.collection.mutable.ArrayBuffer

    val ports = ArrayBuffer.empty[Port]
    val interfaces = ArrayBuffer.empty[Interface]

    ports.append(
      Port(
        "clock",
        PortDirection.input,
        PortKind.clock,
        PortSensitivity.clockRising,
        associatedReset = "reset"
      )
    )
    ports.append(
      Port(
        "reset",
        PortDirection.input,
        PortKind.reset,
        PortSensitivity.resetActiveHigh,
        associatedClock = "clock"
      )
    )

    interfaces.append(
      Interface(
        "sourceElem",
        InterfaceRole("source"),
        InterfaceKind("readyValid[chext.elastic.Data]"),
        associatedClock = "clock",
        associatedReset = "reset",
        args = Map("width" -> TypedObject(32))
      )
    )

    interfaces.append(
      Interface(
        "sinkRes",
        InterfaceRole("sink"),
        InterfaceKind("readyValid[chext.elastic.Data]"),
        associatedClock = "clock",
        associatedReset = "reset",
        args = Map("width" -> TypedObject(32))
      )
    )

    Module(
      "ReduceTestTop1",
      ports.toSeq,
      interfaces.toSeq
    )
  }
}

object Reduce_TB extends App with chext.TestBench {
  emit(new ReduceTestTop1)
}

class ReduceTest1_Tbtop extends Module with chext.TestBenchTop {
  val source = IO(elastic.Source(new Bundle {
    val zero = Bool()
    val last = Bool()
    val data = SInt(32.W)
  }))

  val sink = IO(elastic.Sink(new Bundle {
    val result = SInt(32.W)
  }))

  private val wire0 = Wire(elastic.Interface(SInt(64.W)))

  private val reduce0 = new elastic.Reduce(
    source,
    elastic.Constant(0.S),
    wire0,
    true
  ) {
    zero := elem.zero
    last := elem.last
    data := elem.data

    new elastic.Join(elastic.SinkBuffer(op_sourceRes)) {
      out := join(op_sinkA) + join(op_sinkB)
    }
  }

  private val transform0 = new elastic.Transform(wire0, sink) {
    out.result := in
  }

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(source, "Task")
  declareElasticInterface(sink, "Result")
}

object ReduceTest1_Tb extends chext.TestBench {
  emit(new ReduceTest1_Tbtop)
}
