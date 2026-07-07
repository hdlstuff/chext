package chext.elastic

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred
import chisel3.util._

import chext.tracking.{Container, withContainer}

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
    source: Interface[T],
    sink: Interface[T],
    lfsrBits: Int = 4,
    threshold: Int = 8
)(implicit si_ : SourceInfo)
    extends Container {
  private val require_ = chext.util.Require.inferred()

  val sourceInfo: SourceInfo = si_
  def tpe: String = "RandomStall"
  def namePrefix: String = "randomStall"

  require_(lfsrBits >= 4, "there should be at least 4 bits for LFSR.")
  require_(threshold >= 0 && threshold <= (1L << lfsrBits), "invalid threshold interval.")

  private val rand = random.LFSR(lfsrBits)

  withContainer(this) {
    val stall = new Stall(source, SinkBuffer(sink)) {
      out := in

      cond { rand > threshold.U }
    }
  }
}
