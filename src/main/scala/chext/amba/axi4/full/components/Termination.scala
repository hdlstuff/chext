package chext.amba.axi4.full.components

import chisel3._
import chisel3.experimental.{prefix, SourceInfo}
import chisel3.util.log2Ceil

import chext.amba.axi4
import chext.amba.axi4.tracking.{
  ResolveRequest,
  ResolveResult,
  Resolver
}
import chext.amba.axi4.tracking.properties.{Master, Slave}
import chext.amba.axi4.util.MemoryMap
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
  private[components] val responseValue = response.litValue
  require_(responseValue >= 0 && responseValue < 4)

  val s_axi = IO(axi4.full.Slave(axiCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)

  private val resolver = new ConstantSlave_Resolver(this)

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

  private val resolver = new StallSlave_Resolver(this)

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

  private val resolver = new IdleMaster_Resolver(this)

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

private final class ConstantSlave_Resolver(owner: ConstantSlave)(implicit sourceInfo: SourceInfo)
    extends Resolver(owner) {
  private val SlaveRequests = bindSlave(owner.s_axi)

  override def kind: String = "constant-slave"
  override def resolver: String = "ConstantSlaveResolver"

  private val fullSize = log2Ceil(owner.axiCfg.wData / 8)
  private val burstBeats = if (owner.axiCfg.axi3Compat) 16 else 256

  if (owner.axiCfg.read) {
    owner.s_axi.slaveProps(Slave.ReadOutstandingTransactions) = 1
    owner.s_axi.slaveProps(Slave.ReadThreads) = 1
    owner.s_axi.slaveProps(Slave.ReadBurstBeats) = burstBeats
    owner.s_axi.slaveProps(Slave.ReadBurstNarrow) = true
    owner.s_axi.slaveProps(Slave.ReadBurstTypes) = Set(0, 1, 2)
    owner.s_axi.slaveProps(Slave.ReadBurstSizes) = (0 to fullSize).toSet
  }
  if (owner.axiCfg.write) {
    owner.s_axi.slaveProps(Slave.WriteOutstandingTransactions) = 1
    owner.s_axi.slaveProps(Slave.WriteThreads) = 1
    owner.s_axi.slaveProps(Slave.WriteBurstBeats) = burstBeats
    owner.s_axi.slaveProps(Slave.WriteBurstNarrow) = true
    owner.s_axi.slaveProps(Slave.WriteBurstTypes) = Set(0, 1, 2)
    owner.s_axi.slaveProps(Slave.WriteBurstSizes) = (0 to fullSize).toSet
  }
  if (owner.responseValue <= axi4.ResponseFlag.EXOKAY.litValue)
    owner.s_axi.slaveProps(Slave.MemoryMap) =
      MemoryMap(size = BigInt(1) << owner.axiCfg.wAddr)
  else
    owner.s_axi.slaveProps.markUndefined(Slave.MemoryMap)

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case SlaveRequests(_) =>
        request.undefined()
      case _ =>
        request.failure(
          s"ConstantSlave cannot resolve '${request.qualifiedName}' from this endpoint"
        )
    }
}

private final class StallSlave_Resolver(owner: StallSlave)(implicit sourceInfo: SourceInfo)
    extends Resolver(owner) {
  private val SlaveRequests = bindSlave(owner.s_axi)

  override def kind: String = "stall-slave"
  override def resolver: String = "StallSlaveResolver"

  if (owner.axiCfg.read) {
    owner.s_axi.slaveProps(Slave.ReadOutstandingTransactions) = 0
    owner.s_axi.slaveProps(Slave.ReadThreads) = 0
    owner.s_axi.slaveProps(Slave.ReadBurstBeats) = 0
    owner.s_axi.slaveProps(Slave.ReadBurstNarrow) = false
    owner.s_axi.slaveProps(Slave.ReadBurstTypes) = Set.empty[Int]
    owner.s_axi.slaveProps(Slave.ReadBurstSizes) = Set.empty[Int]
  }
  if (owner.axiCfg.write) {
    owner.s_axi.slaveProps(Slave.WriteOutstandingTransactions) = 0
    owner.s_axi.slaveProps(Slave.WriteThreads) = 0
    owner.s_axi.slaveProps(Slave.WriteBurstBeats) = 0
    owner.s_axi.slaveProps(Slave.WriteBurstNarrow) = false
    owner.s_axi.slaveProps(Slave.WriteBurstTypes) = Set.empty[Int]
    owner.s_axi.slaveProps(Slave.WriteBurstSizes) = Set.empty[Int]
  }
  owner.s_axi.slaveProps.markUndefined(Slave.MemoryMap)

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case SlaveRequests(_) => request.undefined()
      case _ =>
        request.failure(
          s"StallSlave cannot resolve '${request.qualifiedName}' from this endpoint"
        )
    }
}

private final class IdleMaster_Resolver(owner: IdleMaster)(implicit sourceInfo: SourceInfo)
    extends Resolver(owner) {
  private val MasterRequests = bindMaster(owner.m_axi)

  override def kind: String = "idle-master"
  override def resolver: String = "IdleMasterResolver"

  if (owner.axiCfg.read) {
    owner.m_axi.masterProps(Master.ReadOutstandingTransactions) = 0
    owner.m_axi.masterProps(Master.ReadThreads) = 0
    owner.m_axi.masterProps(Master.ReadBurstBeats) = 0
    owner.m_axi.masterProps(Master.ReadBurstNarrow) = false
    owner.m_axi.masterProps(Master.ReadBurstTypes) = Set.empty[Int]
    owner.m_axi.masterProps(Master.ReadBurstSizes) = Set.empty[Int]
  }
  if (owner.axiCfg.write) {
    owner.m_axi.masterProps(Master.WriteOutstandingTransactions) = 0
    owner.m_axi.masterProps(Master.WriteThreads) = 0
    owner.m_axi.masterProps(Master.WriteBurstBeats) = 0
    owner.m_axi.masterProps(Master.WriteBurstNarrow) = false
    owner.m_axi.masterProps(Master.WriteBurstTypes) = Set.empty[Int]
    owner.m_axi.masterProps(Master.WriteBurstSizes) = Set.empty[Int]
  }

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case MasterRequests(_) => request.undefined()
      case _ =>
        request.failure(
          s"IdleMaster cannot resolve '${request.qualifiedName}' from this endpoint"
        )
    }
}
