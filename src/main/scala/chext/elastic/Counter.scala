package chext.elastic

import chisel3._
import util.log2Ceil

import chext.tracking.Component
import chisel3.experimental.SourceInfo
import chisel3.hacks.deferred

import chext.tracking.uniquePrefix

/** A small elastic counter that emits the current count on `sink`.
  *
  * The counter starts at `start`, increments every time the sink fires, and
  * wraps back to zero after reaching `maxValueExclusive - 1`.
  *
  * @param sink
  *   elastic output interface carrying the counter value
  * @param maxValueExclusive
  *   exclusive upper bound for the count; `-1` means use the full bit width
  * @param start
  *   initial counter value
  */
final class Counter(
    val sink: Interface[UInt],
    val maxValueExclusive: Long = -1,
    val start: Long = 0
)(implicit si_ : SourceInfo)
    extends Component {

  override def tpe: String = "Counter"
  override def namePrefix: String = "counter"
  override val sourceInfo: SourceInfo = si_

  require(sink.$bits.widthKnown)
  require((maxValueExclusive == -1) || (maxValueExclusive > 0))

  private val wCount = (sink.$bits.getWidth)
  private val maxValueExclusive_ =
    if (maxValueExclusive == -1) (1L << wCount)
    else maxValueExclusive

  require(start < maxValueExclusive_)

  addSinkPort("sink", sink)

  deferred {
    val counter = RegInit(start.U(sink.$bits.getWidth.W))

    sink.enq(counter)

    when(sink.fire) {
      when(counter === (maxValueExclusive_ - 1).U) {
        counter := 0.U
      }.otherwise {
        counter := counter + 1.U
      }
    }

    new chext.deadlock.DeadlockMonitor(this)
  }
}

object Counter {
  def apply(maxValueExclusive: Int = 0, start: Int = 0) = {
    val sinkCounter = EWire(UInt(log2Ceil(maxValueExclusive).W))
    val counter0 = new Counter(sinkCounter, maxValueExclusive, start)
    sinkCounter
  }

  def fromWidth(width: Int = 1, start: Int = 0) = {
    val sinkCounter = EWire(UInt(width.W))
    val counter0 = new Counter(sinkCounter, -1, start)
    sinkCounter
  }
}
