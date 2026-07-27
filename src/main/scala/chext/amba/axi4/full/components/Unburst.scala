package chext.amba.axi4.full.components

import chisel3._
import chisel3.experimental.{prefix, SourceInfo}

import chext.amba.axi4
import chext.elastic
import elastic.ConnectOp._

case class UnburstConfig(
    val axiCfg: axi4.Config,
    val numOutstandingRead: Int = 32,
    val numOutstandingWrite: Int = 32
) {
  private val require_ = chext.util.Require.inferred()

  require_(axiCfg.wId == 0, "axiCfg.wId must be zero!")
  require_(!axiCfg.lite, "axiCfg.lite must be false!")
  require_(numOutstandingRead >= 2, "AR queue capacity must be >= 2!")
  require_(numOutstandingWrite >= 2, "AW queue capacity must be >= 2!")

  require_(axiCfg.wUserB == 0, "user data is not supported on channel B.")

  val wAddr = axiCfg.wAddr
  val wData = axiCfg.wData
}

class Unburst(val cfg: UnburstConfig) extends Module with chext.AnnotatedModule {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiCfg))
  val m_axi = IO(axi4.full.Master(axiCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)
  declareAxi4Interface(m_axi)

  private val resolver = new Unburst_Resolver(this)

  dontTouch(s_axi)
  dontTouch(m_axi)

  private def implRead(): Unit = prefix("read") {
    val addressStrobeGenerator =
      Module(
        new AddressStrobeGenerator(wAddr, wData, chiselTypeOf(s_axi.ar.$bits))
      )

    val wire0 = elastic.EWire(UInt(axiCfg.wLen.W))

    def implAR(): Unit = prefix("ar") {
      val fork0 = new elastic.Fork(s_axi.ar) {
        val transform0 = new elastic.Transform(fork(), addressStrobeGenerator.source) {
          out.addr := in.addr
          out.len := in.len
          out.size := in.size
          out.burst := in.burst

          out.user := in
        }

        fork(in.len) :=> elastic.SinkBuffer(wire0, numOutstandingRead)
      }

      val transform1 = new elastic.Transform(addressStrobeGenerator.sink, m_axi.ar) {
        out := in.user

        out.addr := in.addr
        out.len := 0.U
        out.size := in.size
        out.burst := axi4.BurstType.INCR
      }
    }

    def implR(): Unit = prefix("r") {
      val lastRepeated = elastic.EWire(Bool())

      val repeat0 = new elastic.Repeat(wire0, lastRepeated, axiCfg.wLen + 1) {
        len { in => in +& 1.U }
        out { (in, _, _, last) => last }
      }

      val join0 = new elastic.Join(s_axi.r) {
        val r = join(m_axi.r)
        val last = join(lastRepeated)

        out := r
        out.last := last
      }
    }

    implAR()
    implR()
  }

  private def implWrite(): Unit = prefix("write") {
    val addressStrobeGenerator =
      Module(
        new AddressStrobeGenerator(wAddr, wData, chiselTypeOf(s_axi.aw.$bits))
      )

    val wire0 = elastic.EWire(UInt(axiCfg.wLen.W))

    def implAW(): Unit = {
      val fork0 = new elastic.Fork(s_axi.aw) {
        val transform0 = new elastic.Transform(fork(), addressStrobeGenerator.source) {
          out.addr := in.addr
          out.len := in.len
          out.size := in.size
          out.burst := in.burst
          out.user := in
        }

        fork(in.len) :=> elastic.SinkBuffer(wire0, numOutstandingWrite)
      }

      val transform0 = new elastic.Transform(addressStrobeGenerator.sink, m_axi.aw) {
        out := in.user

        out.addr := in.addr
        out.len := 0.U
        out.size := in.size
        out.burst := axi4.BurstType.INCR
      }
    }

    def implW(): Unit = {
      val transform1 = new elastic.Transform(s_axi.w, m_axi.w) {
        out := in
        out.last := true.B
      }
    }

    def implB(): Unit = {
      val lastRepeated = elastic.EWire(Bool())
      
      val repeat0 = new elastic.Repeat(wire0, lastRepeated, axiCfg.wLen + 1) {
        len { in => in +& 1.U }
        out { (in, _, _, last) => last }
      }

      val joined = elastic.Zip(m_axi.b, lastRepeated)

      val transducerReduceResp = new elastic.Transducer(joined, s_axi.b) {
        val respReg = RegInit(0.U(2.W))

        out := in._1
        out.resp := Mux(in._1.resp > respReg, in._1.resp, respReg)

        packet {
          when(in._2 /* last */ ) {
            accept {
              respReg := 0.U
            }
          }.otherwise {
            consume {
              respReg := out.resp
            }
          }
        }
      }
    }

    implAW()
    implW()
    implB()
  }

  if (axiCfg.read) implRead()
  if (axiCfg.write) implWrite()
}

private final class Unburst_Resolver(owner: Unburst)(implicit sourceInfo: SourceInfo)
    extends axi4.tracking.Resolver(owner) {
  import axi4.tracking._
  import axi4.BurstType.Encoding.{FIXED, INCR, WRAP}

  bindSlave(owner.s_axi)
  bindMaster(owner.m_axi)

  private val protocolBeats = if (owner.cfg.axiCfg.axi3Compat) 16 else 256
  private val protocolTypes = Seq(FIXED, INCR, WRAP)

  if (owner.cfg.axiCfg.read) {
    owner.s_axi.slaveProps(properties.Slave.ReadThreadMode) =
      values.ThreadMode.SingleThread
    owner.m_axi.masterProps(properties.Master.ReadThreadMode) =
      values.ThreadMode.SingleThread
  }
  if (owner.cfg.axiCfg.write) {
    owner.s_axi.slaveProps(properties.Slave.WriteThreadMode) =
      values.ThreadMode.SingleThread
    owner.m_axi.masterProps(properties.Master.WriteThreadMode) =
      values.ThreadMode.SingleThread
  }

  private def masterShape(input: values.BurstShape): values.BurstShape =
    if (input.size.isEmpty)
      values.BurstShape()
    else
      values.BurstShape(
        len = 1,
        tpe = Seq(INCR),
        size = input.size,
        align =
          if (
            input.len > 1 &&
            input.tpe.exists(_ != FIXED)
          )
            input.align min input.size.min
          else input.align
      )

  private def slaveShape(downstream: values.BurstShape): values.BurstShape = {
    val sizes =
      if (
        downstream.len >= 1 &&
        downstream.tpe.contains(INCR)
      )
        downstream.size.intersect(
          values.BurstShape.validSizes(owner.cfg.axiCfg.wData)
        ).filter(_ >= downstream.align)
      else Seq.empty

    if (sizes.isEmpty)
      values.BurstShape()
    else
      values.BurstShape(
        len = protocolBeats,
        tpe = protocolTypes,
        size = sizes,
        align = downstream.align
      )
  }

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case SlaveRequests(MemoryMap()) =>
        forwardTo(request, owner.m_axi)
      case Request(TrafficProfile()) =>
        request.incomplete()
      case MasterRequests(BurstShape()) =>
        mapFrom(request, owner.s_axi)(masterShape)
      case SlaveRequests(BurstShape()) =>
        mapFrom(request, owner.m_axi)(slaveShape)
      case _ =>
        missingCase(request)
    }
}
