package chext.elastic

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.util._

import chext.tracking.{Component, withComponent}

/** Applies a random stall the elastic interface.
  *
  *   - If `threshold` is zero, always stalls. Higher the `threshold`, less likely to stall.
  *   - If `threshold` is `2**lfsrBits`, never stalls.
  *
  * Must hold: `0 <= threshold <= 2**lfsrBits`
  *
  * @param source
  * @param sink
  * @param lfsrBits
  * @param threshold
  */
final class RandomStall[T <: Data](
    val source: Interface[T],
    val sink: Interface[T],
    val lfsrBits: Int = 4,
    val threshold: Int = 8
)(implicit si_ : SourceInfo)
    extends Component {
  private val require_ = chext.util.Require.inferred()

  val sourceInfo: SourceInfo = si_
  def tpe: String = "RandomStall"
  def namePrefix: String = "randomStall"

  private val elasticState = trackingState(tracking.Tag)
  elasticState.addSource("source", source, boundary = true)
  elasticState.addSink("sink", sink, boundary = true)

  require_(lfsrBits >= 4, "there should be at least 4 bits for LFSR.")
  require_(threshold >= 0 && threshold <= (1L << lfsrBits), "invalid threshold interval.")

  private val rand = random.LFSR(lfsrBits)

  withComponent(this) {
    val stall = new Stall(source, SinkBuffer(sink)) {
      out := in

      cond { rand > threshold.U }
    }
  }
}
