package chext.stream

import chisel3._
import chisel3.util._
import chext.Prefix.prefix

import chext.elastic
import elastic.ConnectOp._

import chext.amba.axi4
import axi4.Ops._

abstract sealed case class WriteResultMode(val desc: String)

object WriteResultMode {

  /** Output result tokens for every single task.
    */
  object KeepAll extends WriteResultMode("KeepAll")

  /** Output results tokens even for only non-empty tasks.
    */
  object DropEmpty extends WriteResultMode("DropEmpty")
}

case class WriteConfig[Tuser <: Data](
    val axiCfg: axi4.Config,
    val genUser: Tuser = UInt(0.W),
    val resultMode: WriteResultMode = WriteResultMode.KeepAll,
    val maxBurstLength: Int = 256,
    val wLength: Int = 32,
    val numOutstandingTasks: Int = 8
) {
  require(!axiCfg.lite)
  require(axiCfg.write)

  if (axiCfg.axi3Compat)
    require(maxBurstLength <= 16, "maxBurstLength <= 16")
  else
    require(maxBurstLength <= 256, "maxBurstLength <= 256")

  val genTask = new Task(genUser, axiCfg.wAddr, wLength)
  val genResult = new WriteResult(this)

  val genData = UInt(axiCfg.wData.W)
}

class WriteResult[Tuser <: Data](cfg: WriteConfig[Tuser]) extends Bundle {
  val user = cfg.genUser.cloneType
}

final class Write0[Tuser <: Data](val cfg: WriteConfig[Tuser]) extends Module {
  import cfg._

  val sourceTask = IO(elastic.Source(genTask))
  val sinkResult = IO(elastic.Sink(genResult))

  val sourceData = IO(elastic.Source(genData))

  val m_axi = IO(axi4.full.Master(axiCfg))

  {
    val taskFiltered = Wire(elastic.Interface(genTask))

    val taskAW = Wire(elastic.Interface(genTask))
    val taskW = Wire(elastic.Interface(genTask))
    val taskB = Wire(elastic.Interface(genTask))

    checkAlignment(sourceTask, axiCfg, "Write")

    val drop0 = new elastic.Drop(sourceTask, taskFiltered) {
      cond { in.length === 0.U }

      out := in
    }

    val fork0 = new elastic.Fork(taskFiltered) {
      fork() :=> taskAW
      fork() :=> taskW
      fork() :=> taskB
    }

    prefix("aw") {
      val chunk0 = Module(
        new Chunk(
          ChunkConfig(
            wAddress = cfg.axiCfg.wAddr,
            wLength = cfg.wLength,
            wData = axiCfg.wData,
            maxBurstLength = cfg.maxBurstLength
          )
        )
      )

      val transform0 =
        new elastic.Transform(taskAW, elastic.SinkBuffer(chunk0.source, numOutstandingTasks)) {
          out.address := in.address
          out.length := in.length
          out.user := 0.U
        }

      val transform1 = new elastic.Transform(chunk0.sink, m_axi.aw) {
        out := 0.U.asTypeOf(out)

        out.addr := in.address
        out.len := in.length -% 1.U
        out.size := (log2Ceil(axiCfg.wData) - 3).U
        out.burst := axi4.BurstType.INCR
      }
    }

    prefix("b") {
      val chunk0 = Module(
        new Chunk(
          ChunkConfig(
            wAddress = 0,
            wLength = cfg.wLength,
            wData = axiCfg.wData,
            maxBurstLength = cfg.maxBurstLength
          )
        )
      )

      val transform0 =
        new elastic.Transform(taskB, elastic.SinkBuffer(chunk0.source, numOutstandingTasks)) {
          out.address := 0.U
          out.length := in.length
          out.user := in.user
        }

      val wireUserKeep = Wire(elastic.Interface(new Bundle {
        val user = genUser.cloneType
        val keep = Bool()
      }))

      val join0 = new elastic.Join(wireUserKeep) {
        val chunk = join(chunk0.sink)
        val b = join(m_axi.b)

        out.keep := chunk.last
        out.user := chunk.user
      }

      val drop0 = new elastic.Drop(wireUserKeep, sinkResult) {
        cond { !in.keep }

        out.user := in.user
      }
    }

    // W packets
    prefix("w") {
      val chunk0 = Module(
        new Chunk(
          ChunkConfig(
            wAddress = 0,
            wLength = cfg.wLength,
            wData = axiCfg.wData,
            maxBurstLength = cfg.maxBurstLength
          )
        )
      )

      val transform0 =
        new elastic.Transform(taskW, elastic.SinkBuffer(chunk0.source, numOutstandingTasks)) {
          out.address := 0.U
          out.length := in.length
          out.user := in.user
        }

      val wireLast = Wire(elastic.Interface(Bool()))

      val repeat0 = new elastic.Repeat(chunk0.sink, wireLast, wLength) {
        len { _.length }

        out { (_, _, _, last) => last }
      }

      val join0 = new elastic.Join(m_axi.w) {
        val data = join(sourceData)
        val last = join(wireLast)

        out.data := data
        out.strb := (-1).S(axiCfg.wStrobe.W).asUInt
        out.last := last
        out.user := 0.U
      }
    }

    if (axiCfg.read) {
      m_axi.ar.noenq()
      m_axi.r.nodeq()

      m_axi.ar.markSink()
      m_axi.r.markSource()
    }
  }
}

final class Write[Tuser <: Data](val cfg: WriteConfig[Tuser]) extends Module {
  import cfg._

  val sourceTask = IO(elastic.Source(genTask))
  val sinkResult = IO(elastic.Sink(genResult))

  val sourceData = IO(elastic.Source(genData))

  val m_axi = IO(axi4.full.Master(axiCfg))

  private val write0 = Module(new Write0(cfg))
  write0.m_axi :=> m_axi

  if (resultMode == WriteResultMode.DropEmpty) {
    sourceTask :=> write0.sourceTask
    write0.sinkResult :=> sinkResult

    sourceData :=> write0.sourceData

  } else if (resultMode == WriteResultMode.KeepAll) {
    sourceData :=> write0.sourceData

    val wireTask0 = Wire(elastic.Interface(genTask))
    val wireTask1 = Wire(elastic.Interface(genTask))

    val fork0 = new elastic.Fork(sourceTask) {
      fork() :=> wireTask0
      fork() :=> wireTask1
      fork() :=> write0.sourceTask
    }

    val wireSelect = Wire(elastic.Interface(UInt(1.W)))
    val wireSource1 = Wire(elastic.Interface(genResult))

    val transform0 =
      new elastic.Transform(wireTask0, elastic.SinkBuffer(wireSelect, numOutstandingTasks)) {
        out := Mux(
          in.length > 0.U,
          0.U,
          1.U
        )
      }

    val drop0 = new elastic.Drop(wireTask1, elastic.SinkBuffer(wireSource1, numOutstandingTasks)) {
      // not used if the length is > 0.U
      cond { in.length > 0.U }

      out.user := in.user
    }

    val mux0 = elastic.Mux(Seq(write0.sinkResult, wireSource1), sinkResult, wireSelect)

  } else
    throw new IllegalArgumentException(
      "stream.Write: Incorrect result mode. Valid ones: WriteResultMode.{DropEmpty, KeepAll}"
    )
}
