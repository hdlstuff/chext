package chext.amba.axi4.lite.components

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

/** AXI4-Lite slave that returns a constant value and response.
  *
  * Read requests are transformed into constant responses. Write address and data packets are
  * joined, discarded, and transformed into one constant write response.
  */
class ConstantSlave(
    val axiCfg: axi4.Config,
    val readData: UInt,
    val response: UInt
) extends Module
    with chext.AnnotatedModule {
  private val require_ = chext.util.Require.inferred()

  require_(axiCfg.lite)
  require_(axiCfg.read || axiCfg.write)
  private val readDataValue = readData.litValue
  require_(readDataValue.bitLength <= axiCfg.wData)
  private[components] val responseValue = response.litValue
  require_(responseValue >= 0 && responseValue < 4)

  val s_axil = IO(axi4.lite.Slave(axiCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axil)

  private val resolver = new ConstantSlave_Resolver(this)

  private val s_axil_ = axi4.lite.SlaveBuffered(s_axil)

  if (axiCfg.read) prefix("read") {
    val transform0 = new elastic.Transform(s_axil_.ar, s_axil_.r) {
      out.data := readDataValue.U(axiCfg.wData.W)
      out.resp := responseValue.U(2.W)
    }
  }

  if (axiCfg.write) prefix("write") {
    val join0 = new elastic.Join(s_axil_.b) {
      join(s_axil_.aw)
      join(s_axil_.w)
      out.resp := responseValue.U(2.W)
    }
  }
}

/** AXI4-Lite slave that returns zero data and OKAY responses. */
class ZeroSlave(axiCfg: axi4.Config)
    extends ConstantSlave(axiCfg, 0.U, axi4.ResponseFlag.OKAY)

/** AXI4-Lite slave that returns zero data and SLVERR or DECERR responses. */
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


/** AXI4-Lite slave that permanently backpressures requests and produces no responses. */
class StallSlave(val axiCfg: axi4.Config) extends Module with chext.AnnotatedModule {
  private val require_ = chext.util.Require.inferred()

  require_(axiCfg.lite)
  require_(axiCfg.read || axiCfg.write)

  val s_axil = IO(axi4.lite.Slave(axiCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axil)

  private val resolver = new StallSlave_Resolver(this)

  if (axiCfg.read) prefix("read") {
    val stallSinkAr = new elastic.StallSink(s_axil.ar)
    val nullSourceR = new elastic.NullSource(s_axil.r)
  }

  if (axiCfg.write) prefix("write") {
    val stallSinkAw = new elastic.StallSink(s_axil.aw)
    val stallSinkW = new elastic.StallSink(s_axil.w)
    val nullSourceB = new elastic.NullSource(s_axil.b)
  }
}

/** AXI4-Lite master that issues no requests and consumes any responses. */
class IdleMaster(val axiCfg: axi4.Config) extends Module with chext.AnnotatedModule {
  private val require_ = chext.util.Require.inferred()

  require_(axiCfg.lite)
  require_(axiCfg.read || axiCfg.write)

  val m_axil = IO(axi4.lite.Master(axiCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(m_axil)

  private val resolver = new IdleMaster_Resolver(this)

  if (axiCfg.read) prefix("read") {
    val nullSourceAr = new elastic.NullSource(m_axil.ar)
    val nullSinkR = new elastic.NullSink(m_axil.r)
  }

  if (axiCfg.write) prefix("write") {
    val nullSourceAw = new elastic.NullSource(m_axil.aw)
    val nullSourceW = new elastic.NullSource(m_axil.w)
    val nullSinkB = new elastic.NullSink(m_axil.b)
  }
}

private final class ConstantSlave_Resolver(owner: ConstantSlave)(implicit sourceInfo: SourceInfo)
    extends Resolver(owner) {
  private val SlaveRequests = bindSlave(owner.s_axil)

  override def kind: String = "constant-slave"
  override def resolver: String = "ConstantSlaveResolver"

  private val fullSize = log2Ceil(owner.axiCfg.wData / 8)

  if (owner.axiCfg.read) {
    owner.s_axil.slaveProps(Slave.ReadOutstandingTransactions) = 1
    owner.s_axil.slaveProps(Slave.ReadThreads) = 1
    owner.s_axil.slaveProps(Slave.ReadBurstBeats) = 1
    owner.s_axil.slaveProps(Slave.ReadBurstNarrow) = false
    owner.s_axil.slaveProps(Slave.ReadBurstTypes) = Set(1)
    owner.s_axil.slaveProps(Slave.ReadBurstSizes) = Set(fullSize)
  }
  if (owner.axiCfg.write) {
    owner.s_axil.slaveProps(Slave.WriteOutstandingTransactions) = 1
    owner.s_axil.slaveProps(Slave.WriteThreads) = 1
    owner.s_axil.slaveProps(Slave.WriteBurstBeats) = 1
    owner.s_axil.slaveProps(Slave.WriteBurstNarrow) = false
    owner.s_axil.slaveProps(Slave.WriteBurstTypes) = Set(1)
    owner.s_axil.slaveProps(Slave.WriteBurstSizes) = Set(fullSize)
  }
  if (owner.responseValue <= axi4.ResponseFlag.EXOKAY.litValue)
    owner.s_axil.slaveProps(Slave.MemoryMap) =
      MemoryMap(size = BigInt(1) << owner.axiCfg.wAddr)
  else
    owner.s_axil.slaveProps.markUndefined(Slave.MemoryMap)

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
  private val SlaveRequests = bindSlave(owner.s_axil)

  override def kind: String = "stall-slave"
  override def resolver: String = "StallSlaveResolver"

  if (owner.axiCfg.read) {
    owner.s_axil.slaveProps(Slave.ReadOutstandingTransactions) = 0
    owner.s_axil.slaveProps(Slave.ReadThreads) = 0
    owner.s_axil.slaveProps(Slave.ReadBurstBeats) = 0
    owner.s_axil.slaveProps(Slave.ReadBurstNarrow) = false
    owner.s_axil.slaveProps(Slave.ReadBurstTypes) = Set.empty[Int]
    owner.s_axil.slaveProps(Slave.ReadBurstSizes) = Set.empty[Int]
  }
  if (owner.axiCfg.write) {
    owner.s_axil.slaveProps(Slave.WriteOutstandingTransactions) = 0
    owner.s_axil.slaveProps(Slave.WriteThreads) = 0
    owner.s_axil.slaveProps(Slave.WriteBurstBeats) = 0
    owner.s_axil.slaveProps(Slave.WriteBurstNarrow) = false
    owner.s_axil.slaveProps(Slave.WriteBurstTypes) = Set.empty[Int]
    owner.s_axil.slaveProps(Slave.WriteBurstSizes) = Set.empty[Int]
  }
  owner.s_axil.slaveProps.markUndefined(Slave.MemoryMap)

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
  private val MasterRequests = bindMaster(owner.m_axil)

  override def kind: String = "idle-master"
  override def resolver: String = "IdleMasterResolver"

  if (owner.axiCfg.read) {
    owner.m_axil.masterProps(Master.ReadOutstandingTransactions) = 0
    owner.m_axil.masterProps(Master.ReadThreads) = 0
    owner.m_axil.masterProps(Master.ReadBurstBeats) = 0
    owner.m_axil.masterProps(Master.ReadBurstNarrow) = false
    owner.m_axil.masterProps(Master.ReadBurstTypes) = Set.empty[Int]
    owner.m_axil.masterProps(Master.ReadBurstSizes) = Set.empty[Int]
  }
  if (owner.axiCfg.write) {
    owner.m_axil.masterProps(Master.WriteOutstandingTransactions) = 0
    owner.m_axil.masterProps(Master.WriteThreads) = 0
    owner.m_axil.masterProps(Master.WriteBurstBeats) = 0
    owner.m_axil.masterProps(Master.WriteBurstNarrow) = false
    owner.m_axil.masterProps(Master.WriteBurstTypes) = Set.empty[Int]
    owner.m_axil.masterProps(Master.WriteBurstSizes) = Set.empty[Int]
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
