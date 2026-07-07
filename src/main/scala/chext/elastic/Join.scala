package chext.elastic

import chext.elastic.{tracking => t}

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred

import scala.collection.mutable.ListBuffer

import chext.deadlock
import chext.tracking.Component

abstract class Join[T <: Data](
    val sink: Interface[T]
)(implicit si_ : SourceInfo)
    extends Component {

  private val elasticState = trackingState(t.Tag)
  import elasticState._

  addSinkPort("sink", sink)

  val sourceInfo: SourceInfo = si_
  def tpe: String = "Join"
  def namePrefix: String = "join"

  private val require_ = chext.util.Require.inferred(sourceInfo)

  private val sourceList = ListBuffer.empty[Interface[Data]]

  protected val out: T = sink.$bits

  protected final def onJoin: Unit = throw new NotImplementedError("Shall not be used!")

  /** Adds a new elastic interface to join.
    *
    * @param sink
    * @return
    */
  def join[TT <: Data](source: Interface[TT]): TT = {
    addSourcePort(f"source_${sourceList.length}", source)
    sourceList.addOne(source)
    source.$bits
  }

  deferred {
    require_(sourceList.nonEmpty, "no sources are specified for the join!")

    joinImpl.join(sourceList.toSeq, sink, false)

    val monitor0 = new deadlock.Monitor(this) {
      sourceList.zipWithIndex.foreach { //
        case (source, i) =>
          source.waitValid := true.B
      }
    }
  }

}

private[elastic] object joinImpl {

  /** Implements a join.
    *
    * @param sources
    * @param sink
    */
  def join[T <: Data](
      sources: Seq[Interface[Data]],
      sink: Interface[Data],
      mark: Boolean = true
  )(implicit si: SourceInfo): Unit = {
    if (mark) {
      sources.foreach { _.markSource() }
      sink.markSink()
    }

    val allValid =
      VecInit(sources.map { _.$valid }).reduceTree(_ && _)
    val fire = sink.$ready && allValid
    sources.foreach { _.$ready := fire }
    sink.$valid := allValid
  }
}
