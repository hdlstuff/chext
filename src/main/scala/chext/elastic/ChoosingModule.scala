package chext.elastic

import chisel3._
import chisel3.util._
import chisel3.experimental.AffectsChiselPrefix

abstract class Chooser(val v: Vec[Bool]) extends AffectsChiselPrefix {
  protected val wChoice = chisel3.util.log2Up(v.length)
  protected val genChoice = UInt(wChoice.W)
  protected val zeroChoice = 0.U(wChoice.W)

  /** The chooser makes a choice based on `v`s and its current internal state.
    *
    * @return
    */
  def choice: UInt

  /** Logic active when a selection is to be made.
    */
  def updateState: Unit
}

private[elastic] trait InputToOutputSelectModule {
  protected val sourceBurst = RegInit(false.B)

  protected def sourceValid: Bool
  protected def sourceLast: Bool
  protected def sourceReady: Bool

  protected def selectValid: Bool
  protected def selectReady: Bool

  protected def sinkValid: Bool
  protected def sinkReady: Bool

  /** Initializes the data path and connects the control signals.
    */
  protected def implementDataPlane(): Unit

  /** Logic executed when a Burst starts.
    */
  protected def onBurst: Unit

  /** Initializes the control signals
    */
  protected def implementControlPlane(): Unit = {
    val sinkSent = RegInit(false.B)
    val selectSent = RegInit(false.B)

    def isSentLogic(sinkReady: Bool, sinkSent: Bool) = {
      sinkSent := (sinkReady || sinkSent) && sourceValid && !sourceReady
    }

    isSentLogic(sinkReady, sinkSent)
    isSentLogic(selectReady, selectSent)

    selectValid := sourceValid && !selectSent && sourceLast
    sinkValid := sourceValid && !sinkSent

    sourceReady := (sinkSent || sinkReady) &&
      (!sourceLast || sourceLast && (selectSent || selectReady))

    when(sourceValid && sourceReady) {
      when(sourceLast) {
        sourceBurst := false.B
      }.otherwise {
        sourceBurst := true.B
      }

      when(!sourceBurst) {
        onBurst
      }
    }
  }
}

private[elastic] trait ChoosingModule extends InputToOutputSelectModule {
  protected val n: Int
  require(n > 0)

  protected val genSelect = UInt(chisel3.util.log2Up(n).W)

  protected def chooser: Chooser

  protected def onBurst: Unit = chooser.updateState

  protected lazy val lastChoice = Reg(genSelect)
  protected lazy val choice = Mux(sourceBurst, lastChoice, chooser.choice)

  protected def implementChoiceLogic() = {
    lastChoice := choice
  }
}

class RRChooser(v: Vec[Bool]) extends Chooser(v) {
  private val lastChoice = RegInit(zeroChoice)
  private val choiceMax = (-1).S(wChoice.W).asUInt

  override def choice: UInt = {
    Mux(v(rrChoice), rrChoice, priorityChoice)
  }
  override def updateState: Unit = (lastChoice := choice)

  private val rrChoice =
    Mux(
      lastChoice === choiceMax,
      zeroChoice,
      PriorityEncoder(v.zipWithIndex.map { case (x, i) =>
        i.U > lastChoice && x
      })
    )
  private val priorityChoice = PriorityEncoder(v)

}

class PriorityChooser(v: Vec[Bool]) extends Chooser(v) {
  def choice: UInt = {
    PriorityEncoder(v)
  }

  def updateState: Unit = { /* stateless */ }
}

object Chooser {
  type ChooserFn = (Vec[Bool]) => Chooser

  def rr(v: Vec[Bool]) = new RRChooser(v)
  def priority(v: Vec[Bool]) = new PriorityChooser(v)
}
