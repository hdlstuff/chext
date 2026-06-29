package chext.elastic

import chisel3._
import chisel3.util._

import chext.elastic
import elastic.ConnectOp._

/** Stops issuing requests if there is no sufficient buffer space. Useful for
  * avoiding deadlocks in case of a shared resource.
  *
  * @param genReq
  * @param genResp
  * @param numEntries
  */
class ResponseBuffer[Req <: Data, Resp <: Data](
    genReq: Req,
    genResp: Resp,
    numEntries: Int
) extends Module {
  val sourceReq = IO(elastic.Source(genReq))
  val sinkResp = IO(elastic.Sink(genResp))

  val sinkReq = IO(elastic.Sink(genReq))
  val sourceResp = IO(elastic.Source(genResp))

  private def impl(): Unit = {
    val ctr = Module(new chext.util.Counter(numEntries + 1))

    ctr.noInc()
    ctr.noDec()

    val stall0 = new elastic.Stall(sourceReq, sinkReq) {
      out := in

      cond { ctr.full }
      fire { ctr.inc() }
    }

    val connect0 =
      new elastic.Connect(SourceBuffer(sourceResp, numEntries), sinkResp) {
        fire { ctr.dec() }
      }
  }

  impl()
}
