package chext.ldstr

import chisel3._
import chisel3.util._
import chisel3.experimental.{prefix, SourceInfo}

import chext.elastic
import elastic.ConnectOp._

import chext.amba.axi4

case class LoadConfig[Tuser <: Data](
    val axiCfg: axi4.Config,
    val genUser: Tuser = UInt(0.W),
    val numOutstandingTasks: Int = 8
) {
  private val require_ = chext.util.Require.inferred()

  require_(!axiCfg.lite)
  require_(axiCfg.read)
  require_(!axiCfg.write)

  val genTask = new Task(genUser, axiCfg.wAddr)
  val genResult = new LoadResult(this)
}

class LoadResult[Tuser <: Data](cfg: LoadConfig[Tuser]) extends Bundle {
  val data = UInt(cfg.axiCfg.wData.W)
  val user = cfg.genUser.cloneType
}

class Load[Tuser <: Data](val cfg: LoadConfig[Tuser])
    extends Module
    with chext.AnnotatedModule {
  import cfg._

  val sourceTask = IO(elastic.Source(genTask))
  val sinkResult = IO(elastic.Sink(genResult))

  val m_axi = IO(axi4.full.Master(axiCfg))

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(sourceTask, "Task")
  declareElasticInterface(sinkResult, "Result")
  declareAxi4Interface(m_axi)

  private val resolver = new Load_Resolver(this)

  {
    val taskAR = elastic.EWire(genTask)
    val taskR = elastic.EWire(genTask)

    checkAlignment(sourceTask, axiCfg, "Load")

    val fork0 = new elastic.Fork(sourceTask) {
      fork() :=> taskAR
      fork() :=> taskR
    }

    prefix("ar") {
      val transform0 = new elastic.Transform(taskAR, m_axi.ar) {
        out := 0.U.asTypeOf(out)

        out.addr := in.address
        out.len := 0.U
        out.size := (log2Ceil(axiCfg.wData) - 3).U
        out.burst := axi4.BurstType.INCR
      }
    }

    prefix("r") {
      val wireUser = elastic.EWire(genUser)

      val transform0 =
        new elastic.Transform(taskR, elastic.SinkBuffer(wireUser, numOutstandingTasks)) {
          out := in.user
        }

      val join0 = new elastic.Join(sinkResult) {
        out.data := join(m_axi.r).data
        out.user := join(wireUser)
      }
    }
  }
}

private final class Load_Resolver[Tuser <: Data](owner: Load[Tuser])(implicit
    sourceInfo: SourceInfo
) extends axi4.tracking.Resolver(owner) {
  import axi4.BurstType.Encoding.INCR
  import axi4.tracking._
  import axi4.tracking.{properties => p, values => v}

  bindMaster(owner.m_axi)

  owner.m_axi.properties(p.MasterReadBurstShape) = v.BurstShape(
    maxBeats = 1,
    types = Seq(INCR),
    sizes = Seq(v.BurstShape.fullSize(owner.cfg.axiCfg.wData)),
    aligned = false
  )
  owner.m_axi.properties(p.MasterReadThreadMode) = v.ThreadMode.SingleThread

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case ResolveRequest(_, p.Key(p.Master, _, p.TrafficProfile)) =>
        request.incomplete()
      case _ =>
        request.missingCase()
    }
}
