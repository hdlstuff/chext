package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.{prefix, SourceInfo}

import chext.amba.axi4
import chext.elastic

import elastic.ConnectOp._
import chext.util.BitOps._

import helpers.{SteerLeft, SteerRight}

case class UpscaleConfig(
    val axiSlaveCfg: axi4.Config,
    val wDataMaster: Int,
    val numOutstandingRead: Int = 32,
    val numOutstandingWrite: Int = 32
) {
  private val require_ = chext.util.Require.inferred()

  require_(axiSlaveCfg.wId == 0, "axiSlaveCfg.wId must be zero!")
  require_(!axiSlaveCfg.lite, "axiSlaveCfg.lite must be false!")
  require_(wDataMaster > axiSlaveCfg.wData, "wDataMaster must be > axiSlaveCfg.wData")

  require_(wDataMaster >= 8)
  require_(isPow2(wDataMaster))

  val wDataSlave = axiSlaveCfg.wData
  val wStrobeMaster = wDataMaster >> 3
  val wStrobeSlave = wDataSlave >> 3
  val wOffset = log2Ceil(wDataMaster) - log2Ceil(wDataSlave)
  val wAddr = axiSlaveCfg.wAddr
  val axiMasterCfg = axiSlaveCfg.copy(wData = wDataMaster)
}

class Upscale(val cfg: UpscaleConfig) extends Module with chext.AnnotatedModule {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.full.Master(axiMasterCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)
  declareAxi4Interface(m_axi)

  private val resolver = new Upscale_Resolver(this)

  private def implRead(): Unit = prefix("read") {
    val addressGenerator = Module(new AddressGenerator(log2Ceil(wDataMaster >> 3)))
    val ewireOffset = elastic.EWire(UInt(wOffset.W))

    def implAR(): Unit = prefix("ar") {
      val fork0 = new elastic.Fork(s_axi.ar) {
        val transform0 =
          new elastic.Transform(
            fork(),
            elastic.SinkBuffer(addressGenerator.source, numOutstandingRead)
          ) {
            out.addr := in.addr
            out.len := in.len
            out.size := in.size
            out.burst := in.burst
            out.user := 0.U
          }

        fork() :=> m_axi.ar
      }

      val transform0 = new elastic.Transform(addressGenerator.sink, ewireOffset) {
        out := in.addr.dropLsbN(log2Ceil(wDataSlave >> 3))
      }
    }

    def implR(): Unit = prefix("r") {
      val steerRight = Module(new SteerRight(wDataMaster, wDataSlave))

      val join0 = new elastic.Join(s_axi.r) {
        val beat = join(m_axi.r)
        val offset = join(ewireOffset)

        steerRight.dataIn := beat.data
        steerRight.offsetIn := offset

        out.data := steerRight.dataOut

        out.id := beat.id // must be zero
        out.resp := beat.resp
        out.user := beat.user
        out.last := beat.last
      }
    }

    implAR()
    implR()
  }

  private def implWrite(): Unit = prefix("write") {
    val addressGenerator = Module(new AddressGenerator(log2Ceil(wDataMaster >> 3)))
    val ewireOffset = elastic.EWire(UInt(wOffset.W))

    def implAW(): Unit = prefix("aw") {
      val fork0 = new elastic.Fork(s_axi.aw) {
        val transform0 = new elastic.Transform(
          fork(),
          elastic.SinkBuffer(addressGenerator.source, numOutstandingWrite)
        ) {
          out.addr := in.addr
          out.len := in.len
          out.size := in.size
          out.burst := in.burst
          out.user := 0.U
        }

        fork() :=> m_axi.aw
      }

      val transform0 = new elastic.Transform(addressGenerator.sink, ewireOffset) {
        out := in.addr.dropLsbN(log2Ceil(wDataSlave >> 3))
      }
    }

    def implW(): Unit = prefix("w") {
      val steerLeft = Module(new SteerLeft(wDataSlave, wDataMaster))
      val steerLeftStrobe = Module(new SteerLeft(wStrobeSlave, wStrobeMaster))

      val join0 = new elastic.Join(m_axi.w) {
        val beat = join(s_axi.w)
        val offset = join(ewireOffset)

        steerLeft.dataIn := beat.data
        steerLeft.offsetIn := offset

        steerLeftStrobe.dataIn := beat.strb
        steerLeftStrobe.offsetIn := offset

        out.data := steerLeft.dataOut
        out.strb := steerLeftStrobe.dataOut
        out.last := beat.last
        out.user := beat.user
      }
    }

    def implB(): Unit = prefix("b") {
      m_axi.b :=> s_axi.b
    }

    implAW()
    implW()
    implB()
  }

  if (axiSlaveCfg.read) implRead()
  if (axiSlaveCfg.write) implWrite()
}

private final class Upscale_Resolver(owner: Upscale)(implicit sourceInfo: SourceInfo)
    extends axi4.tracking.Resolver(owner) {
  import axi4.tracking._
  import axi4.tracking.{properties => p, values => v}

  bindSlave(owner.s_axi)
  bindMaster(owner.m_axi)

  if (owner.cfg.axiSlaveCfg.read) {
    owner.s_axi.properties(p.SlaveReadThreadMode) = v.ThreadMode.SingleThread
  }
  if (owner.cfg.axiSlaveCfg.write) {
    owner.s_axi.properties(p.SlaveWriteThreadMode) = v.ThreadMode.SingleThread
  }

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case ResolveRequest(_, p.Key(p.Slave, _, p.MemoryMap)) =>
        request.forwardTo(owner.m_axi)
      case ResolveRequest(_, p.Key(p.Slave, _, p.BurstShape)) =>
        request.mapFrom(owner.m_axi, p.BurstShape) { downstream =>
          val types =
            downstream.types.intersect(v.BurstShape.supportedTypesFor(owner.s_axi.cfg))
          val sizes =
            downstream.sizes.intersect(v.BurstShape.supportedSizesFor(owner.s_axi.cfg))
          if (downstream.maxBeats <= 0 || types.isEmpty || sizes.isEmpty)
            v.BurstShape()
          else
            v.BurstShape(
              maxBeats =
                math.min(downstream.maxBeats, v.BurstShape.maxBeatsFor(owner.s_axi.cfg)),
              types = types,
              sizes = sizes,
              aligned = downstream.aligned
            )
        }
      case ResolveRequest(_, p.Key(p.Master, _, p.BurstShape | p.ThreadMode)) =>
        request.forwardTo(owner.s_axi)
      case ResolveRequest(_, p.Key(_, _, p.TrafficProfile)) =>
        request.incomplete()
      case _ =>
        request.missingCase()
    }
}
