package chext.ldstr

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

import chext.elastic
import elastic.ConnectOp._

import chext.amba.axi4

case class StoreConfig[Tuser <: Data](
    val axiCfg: axi4.Config,
    val genUser: Tuser = UInt(0.W),
    val numOutstandingTasks: Int = 8
) {
  private val require_ = chext.util.Require.inferred()

  require_(!axiCfg.lite)
  require_(axiCfg.write)

  val genTask = new Task(genUser, axiCfg.wAddr)
  val genResult = new StoreResult(this)

  val genData = UInt(axiCfg.wData.W)
}

class StoreResult[Tuser <: Data](cfg: StoreConfig[Tuser]) extends Bundle {
  val user = cfg.genUser.cloneType
}

class Store[Tuser <: Data](val cfg: StoreConfig[Tuser])
    extends Module
    with chext.AnnotatedModule {
  import cfg._

  val sourceTask = IO(elastic.Source(genTask))
  val sinkResult = IO(elastic.Sink(genResult))

  val sourceData = IO(elastic.Source(genData))

  val m_axi = IO(axi4.full.Master(axiCfg))

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(sourceTask, "Task")
  declareElasticInterface(sinkResult, "Result")
  declareElasticInterface(sourceData, "Data")
  declareAxi4Interface(m_axi)

  {
    val taskAW = elastic.EWire(genTask)
    val taskB = elastic.EWire(genTask)

    checkAlignment(sourceTask, axiCfg, "Store")

    val fork0 = new elastic.Fork(sourceTask) {
      fork() :=> taskAW
      fork() :=> taskB
    }

    prefix("aw") {
      val transform0 = new elastic.Transform(taskAW, m_axi.aw) {
        out := 0.U.asTypeOf(out)

        out.addr := in.address
        out.len := 0.U
        out.size := (log2Ceil(axiCfg.wData) - 3).U
        out.burst := axi4.BurstType.INCR
      }
    }

    prefix("w") {
      val transform0 = new elastic.Transform(sourceData, m_axi.w) {
        out.data := in
        out.strb := (-1).S(axiCfg.wStrobe.W).asUInt
        out.last := true.B
        out.user := 0.U
      }
    }

    prefix("b") {
      val wireUser = elastic.EWire(genUser)

      val transform0 =
        new elastic.Transform(taskB, elastic.SinkBuffer(wireUser, numOutstandingTasks)) {
          out := in.user
        }

      val join0 = new elastic.Join(sinkResult) {
        join(m_axi.b)
        out.user := join(wireUser)
      }
    }

    if (axiCfg.read) {
      val nullSourceAr = new elastic.NullSource(m_axi.ar)
      val nullSinkR = new elastic.NullSink(m_axi.r)
    }
  }
}
