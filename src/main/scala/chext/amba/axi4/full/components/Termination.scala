package chext.amba.axi4.full.components

import chisel3._
import chisel3.experimental.prefix

import chext.amba.axi4
import chext.elastic

/** AXI4-Full slave that returns a constant value and response.
  *
  * A transducer expands each AR packet into the requested number of read response beats. A second
  * transducer consumes write data through WLAST; its completion token is joined with AW to create
  * one write response. Requests remain at the elastic boundary until their transaction completes.
  */
class ConstantSlave(
    val axiCfg: axi4.Config,
    val readData: UInt,
    val response: UInt
) extends Module
    with chext.AnnotatedModule {
  private val require_ = chext.util.Require.inferred()

  require_(!axiCfg.lite)
  require_(axiCfg.read || axiCfg.write)
  private val readDataValue = readData.litValue
  require_(readDataValue.bitLength <= axiCfg.wData)
  private val responseValue = response.litValue
  require_(responseValue >= 0 && responseValue < 4)

  val s_axi = IO(axi4.full.Slave(axiCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)

  private val s_axi_ = axi4.full.SlaveBuffered(s_axi)

  if (axiCfg.read) prefix("read") {
    val transducer0 = new elastic.Transducer(s_axi_.ar, s_axi_.r) {
      val generating = RegInit(false.B)
      val beatsLeft = Reg(UInt(axiCfg.wLen.W))

      out.id := in.id
      out.data := readDataValue.U(axiCfg.wData.W)
      out.resp := responseValue.U(2.W)
      out.last := Mux(generating, beatsLeft === 0.U, in.len === 0.U)
      out.user := 0.U

      packet {
        when(generating) {
          when(beatsLeft === 0.U) {
            accept {
              generating := false.B
            }
          }.otherwise {
            produce {
              beatsLeft := beatsLeft - 1.U
            }
          }
        }.otherwise {
          when(in.len === 0.U) {
            accept {}
          }.otherwise {
            produce {
              generating := true.B
              beatsLeft := in.len - 1.U
            }
          }
        }
      }
    }
  }

  if (axiCfg.write) prefix("write") {
    val writeComplete = elastic.EWire(Bool())
    val transducer0 = new elastic.Transducer(s_axi_.w, writeComplete) {
      out := true.B

      packet {
        when(in.last) {
          accept {}
        }.otherwise {
          consume {}
        }
      }
    }

    val join0 = new elastic.Join(s_axi_.b) {
      val aw = join(s_axi_.aw)
      join(writeComplete)

      out.id := aw.id
      out.resp := responseValue.U(2.W)
      out.user := 0.U
    }
  }
}

/** AXI4-Full slave that returns zero data and OKAY responses. */
class ZeroSlave(axiCfg: axi4.Config)
    extends ConstantSlave(axiCfg, 0.U, axi4.ResponseFlag.OKAY)

/** AXI4-Full slave that returns zero data and SLVERR or DECERR responses. */
class ErrorSlave(
    axiCfg: axi4.Config,
    val errorResponse: UInt = axi4.ResponseFlag.DECERR
) extends ConstantSlave(axiCfg, 0.U, errorResponse) {
  private val require_ = chext.util.Require.inferred()
  private val errorResponseValue = errorResponse.litValue

  require_(
    errorResponseValue == axi4.ResponseFlag.SLVERR.litValue ||
      errorResponseValue == axi4.ResponseFlag.DECERR.litValue,
    "errorResponse must be axi4.ResponseFlag.SLVERR or axi4.ResponseFlag.DECERR"
  )
}


/** AXI4-Full slave that permanently backpressures requests and produces no responses. */
class StallSlave(val axiCfg: axi4.Config) extends Module with chext.AnnotatedModule {
  private val require_ = chext.util.Require.inferred()

  require_(!axiCfg.lite)
  require_(axiCfg.read || axiCfg.write)

  val s_axi = IO(axi4.full.Slave(axiCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)

  if (axiCfg.read) prefix("read") {
    val stallSinkAr = new elastic.StallSink(s_axi.ar)
    val nullSourceR = new elastic.NullSource(s_axi.r)
  }

  if (axiCfg.write) prefix("write") {
    val stallSinkAw = new elastic.StallSink(s_axi.aw)
    val stallSinkW = new elastic.StallSink(s_axi.w)
    val nullSourceB = new elastic.NullSource(s_axi.b)
  }
}

/** AXI4-Full master that issues no requests and consumes any responses. */
class IdleMaster(val axiCfg: axi4.Config) extends Module with chext.AnnotatedModule {
  private val require_ = chext.util.Require.inferred()

  require_(!axiCfg.lite)
  require_(axiCfg.read || axiCfg.write)

  val m_axi = IO(axi4.full.Master(axiCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(m_axi)

  if (axiCfg.read) prefix("read") {
    val nullSourceAr = new elastic.NullSource(m_axi.ar)
    val nullSinkR = new elastic.NullSink(m_axi.r)
  }

  if (axiCfg.write) prefix("write") {
    val nullSourceAw = new elastic.NullSource(m_axi.aw)
    val nullSourceW = new elastic.NullSource(m_axi.w)
    val nullSinkB = new elastic.NullSink(m_axi.b)
  }
}
