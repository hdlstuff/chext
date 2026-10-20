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

class RRChooser(v: Vec[Bool]) extends Chooser(v) {
  private val lastChoice = RegInit(zeroChoice)

  override def choice: UInt = {
    Mux(v(rrChoice), rrChoice, priorityChoice)
  }
  override def updateState: Unit = (lastChoice := choice)

  private val priorityChoice = PriorityEncoder(v)
  // Next valid input above the last winner, wrapping to the lowest valid input. The old
  // form only wrapped when lastChoice was all ones, i.e. for a power-of-2 input count;
  // otherwise PriorityEncoder of an all-false vector returned n-1 and the last input kept
  // winning for as long as it stayed valid, starving every other input.
  private val higher = VecInit(v.zipWithIndex.map { case (x, i) => i.U > lastChoice && x })
  private val rrChoice = Mux(higher.asUInt.orR, PriorityEncoder(higher), priorityChoice)

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
