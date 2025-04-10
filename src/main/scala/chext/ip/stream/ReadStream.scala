package chext.ip.stream

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix

import chext.{elastic2 => elastic}
import elastic.DataLast
import elastic.ConnectOp._

import chext.amba.axi4

class ReadStreamTask extends Bundle {
  val address = UInt(ReadStreamTask.wAddress.W)
  val length = UInt(ReadStreamTask.wLength.W)
}

object ReadStreamTask {
  val wAddress = 64
  val wLength = 64
}

private object readStreamImpl {
  def implFiltering(
      taskSource: elastic.Interface[ReadStreamTask],
      taskSink: elastic.Interface[ReadStreamTask]
  ): Unit =
    prefix("filtering") {
      val arrival = new elastic.Arrival(taskSource, taskSink) {
        out := in

        when(arrived) {
          when(in.length > 0.U) {
            accept()
          }.otherwise {
            consume()
          }
        }
      }
    }

  def implAddressPhase(
      maxBurstLength: Int,
      taskSource: elastic.Interface[ReadStreamTask],
      slave: axi4.full.Interface
  ): Unit =
    prefix("addressPhase") {
      val axiCfg = slave.cfg

      val addressIncrement = maxBurstLength * (axiCfg.wData / 8)

      val rGenerating = RegInit(false.B)
      val rRemaining = RegInit(0.U(ReadStreamTask.wLength.W))
      val rAddress = RegInit(0.U(ReadStreamTask.wAddress.W))

      new elastic.Arrival(taskSource, slave.ar) {
        out := 0.U.asTypeOf(out)

        // set up the transfer
        out.burst := 1.U
        out.size := (log2Ceil(axiCfg.wData) - 3).U

        when(arrived) {
          when(rGenerating) {
            when(rRemaining <= maxBurstLength.U) {
              rGenerating := false.B
              rRemaining := 0.U
              rAddress := 0.U

              out.addr := rAddress
              out.len := rRemaining - 1.U

              accept()
            }.otherwise {
              rGenerating := true.B
              rRemaining := rRemaining - maxBurstLength.U
              rAddress := rAddress + addressIncrement.U

              out.addr := rAddress
              out.len := (maxBurstLength - 1).U

              produce()
            }
          }.otherwise {
            when(in.length <= maxBurstLength.U) {
              rGenerating := false.B
              rRemaining := 0.U
              rAddress := 0.U

              out.addr := in.address
              out.len := in.length - 1.U

              accept()
            }.otherwise {
              rGenerating := true.B
              rRemaining := in.length - maxBurstLength.U
              rAddress := in.address + addressIncrement.U

              out.addr := in.address
              out.len := (maxBurstLength - 1).U

              produce()
            }
          }
        }
      }
    }

}

/** @param axiMasterCfg
  * @param queueLength
  *   Not used if last signal is not generated.
  */
case class ReadStreamConfig(
    val axiMasterCfg: axi4.Config,
    val maxBurstLength: Int = 256,
    val queueLength: Int = 16
) {
  require(!axiMasterCfg.lite)
  require(axiMasterCfg.read)
  require(axiMasterCfg.wAddr <= ReadStreamTask.wAddress)
  require(queueLength > 0)

  if (axiMasterCfg.axi3Compat) {
    require(maxBurstLength <= 16, "maxBurstLength <= 16")
  } else {
    require(maxBurstLength <= 256, "maxBurstLength <= 256")
  }

  val wDataSink = axiMasterCfg.wData
}

class ReadStream(val cfg: ReadStreamConfig) extends Module {
  import cfg._

  val genTaskSource = new ReadStreamTask
  val genDataSink = UInt(wDataSink.W)
  val genLength = UInt(ReadStreamTask.wLength.W)

  val m_axi = IO(axi4.full.Master(axiMasterCfg))

  val sourceTask = IO(elastic.Source(genTaskSource))
  val sinkData = IO(elastic.Sink(genDataSink))

  private val rvTask0 = Wire(chiselTypeOf(sourceTask))

  readStreamImpl.implFiltering(sourceTask, rvTask0)
  readStreamImpl.implAddressPhase(maxBurstLength, rvTask0, m_axi)

  prefix("dataPhase") {
    val rReceived = RegInit(0.U(ReadStreamTask.wLength.W))

    new elastic.Transform(m_axi.r, sinkData) {
      out := in.data
    }
  }

  if (axiMasterCfg.write) {
    m_axi.aw.noenq()
    m_axi.w.noenq()
    m_axi.b.nodeq()
  }
}

class ReadStreamWithLast(val cfg: ReadStreamConfig) extends Module {
  import cfg._

  val genTaskSource = new ReadStreamTask
  val genDataSink = DataLast(wDataSink)
  val genLength = UInt(ReadStreamTask.wLength.W)

  val m_axi = IO(axi4.full.Master(axiMasterCfg))

  val sourceTask = IO(elastic.Source(genTaskSource))
  val sinkData = IO(elastic.Sink(genDataSink))

  // Used for counting R packets
  private val qLength = elastic.Queue(genLength, queueLength)

  private val rvTask0 = Wire(chiselTypeOf(sourceTask))
  private val rvTask1 = Wire(chiselTypeOf(sourceTask))

  readStreamImpl.implFiltering(sourceTask, rvTask0)
  readStreamImpl.implAddressPhase(maxBurstLength, rvTask1, m_axi)

  private val fork0 = new elastic.Fork(rvTask0) {
    fork { in.length } :=> qLength.source
    fork { in } :=> rvTask1
  }

  prefix("dataPhase") {
    qLength.sink.nodeq()

    val rReceived = RegInit(0.U(ReadStreamTask.wLength.W))

    new elastic.Arrival(m_axi.r, sinkData) {
      out.data := in.data

      when(arrived) {
        when(qLength.sink.valid) {
          when(rReceived === (qLength.sink.bits - 1.U)) {
            out.last := true.B
            rReceived := 0.U

            qLength.sink.deq()
          }.otherwise {
            out.last := false.B
            rReceived := rReceived + 1.U
          }

          accept()
        }.otherwise {
          noAccept()
        }
      }
    }
  }

  if (axiMasterCfg.write) {
    m_axi.aw.noenq()
    m_axi.w.noenq()
    m_axi.b.nodeq()
  }
}
