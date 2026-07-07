package chext.elastic

import chisel3._

import chext.elastic.ConnectOp._
import chext.elastic.{Mux => EMux}

/** @param genIn
  * @param genOut
  * @param log2n
  *   number of sharers.
  * @param log2k
  *   number of shared resources.
  * @param bufLength
  *   (only for non-deterministic) response buffer length.
  */
case class ShareConfig[Tin <: Data, Tout <: Data](
    val genIn: Tin,
    val genOut: Tout,
    val log2n: Int,
    val log2k: Int,
    val respQueueLength: Int = 2,
    val selQueueLength: Int = 8
) {
  private val require_ = chext.util.Require.inferred()

  require_(log2n >= log2k)
}

/** Deterministic sharing.
  *
  * @param cfg
  */
abstract class ShareD[Tin <: Data, Tout <: Data](
    cfg: ShareConfig[Tin, Tout]
) extends Module {
  import cfg._

  val source_N = IO(Source.many(1 << log2n, genIn))
  val sink_N = IO(Sink.many(1 << log2n, genOut))

  protected def instantiate(
      index: Int,
      source: Interface[Tin],
      sink: Interface[Tout]
  ): Unit

  private def impl0(
      index: Int,
      source_N: Seq[Interface[Tin]],
      sink_N: Seq[Interface[Tout]]
  ): Unit = {
    val sourceResource = EWire(genIn)
    val sinkResource = EWire(genOut)

    val mux0 = new EMux(source_N, sourceResource, Counter(1 << (log2n - log2k)))
    val demux0 =
      new Demux(sinkResource, sink_N, Counter(1 << (log2n - log2k)))

    instantiate(index, sourceResource, sinkResource)
  }

  private def impl(): Unit = {
    val source_N_grouped =
      source_N.grouped(1 << (log2n - log2k) /* elements per group */ )
    val sink_N_grouped =
      sink_N.grouped(1 << (log2n - log2k) /* elements per group */ )

    source_N_grouped.zip(sink_N_grouped).zipWithIndex.foreach { //
      case ((source_N, sink_N), index) => impl0(index, source_N, sink_N)
    }
  }

  impl()
}

abstract class ShareNd[Tin <: Data, Tout <: Data](
    cfg: ShareConfig[Tin, Tout]
) extends Module {
  import cfg._

  val source_N = IO(Source.many(1 << log2n, genIn))
  val sink_N = IO(Sink.many(1 << log2n, genOut))

  protected def instantiate(
      index: Int,
      source: Interface[Tin],
      sink: Interface[Tout]
  ): Unit

  private def impl0(
      index: Int,
      source_N: Seq[Interface[Tin]],
      sink_N: Seq[Interface[Tout]]
  ): Unit = {
    val sourceResource = EWire(genIn)
    val sinkResource = EWire(genOut)

    val ewireSelect = EWire(UInt((log2n - log2k).W))

    val arbiter0 =
      new Arbiter(source_N, sourceResource, ewireSelect, Chooser.rr)

    val demux0 =
      new Demux(sinkResource, sink_N, SourceBuffer(ewireSelect, selQueueLength))

    instantiate(index, sourceResource, sinkResource)
  }

  private def impl(): Unit = {
    val (source_N_, sink_N_) =
      if (respQueueLength == 0) {
        (source_N, sink_N)
      } else {
        val result = source_N
          .zip(sink_N)
          .zipWithIndex
          .map { //
            case ((source, sink), index) => { //
              val responseBuffer =
                Module(new ResponseBuffer(genIn, genOut, respQueueLength))

              source :=> responseBuffer.sourceReq
              responseBuffer.sinkResp :=> sink

              Seq(responseBuffer.sinkReq, responseBuffer.sourceResp)
            }
          }
          .transpose

        // this horrible syntax is not necessarily due to me
        assert(result.length == 2)

        (
          result(0).asInstanceOf[Seq[Interface[Tin]]],
          result(1).asInstanceOf[Seq[Interface[Tout]]]
        )
      }

    val source_N_grouped =
      source_N_.grouped(1 << (log2n - log2k) /* elements per group */ )
    val sink_N_grouped =
      sink_N_.grouped(1 << (log2n - log2k) /* elements per group */ )

    source_N_grouped.zip(sink_N_grouped).zipWithIndex.foreach { //
      case ((source_N, sink_N), index) => impl0(index, source_N, sink_N)
    }
  }

  impl()
}
