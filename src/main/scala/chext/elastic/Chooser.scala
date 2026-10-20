package chext.elastic

import chisel3._
import chisel3.util._

abstract trait Chooser {
  def apply(valid: Vec[Bool], ready: Bool): UInt
}

object Chooser {
  object rr extends Chooser {
    def apply(valid: Vec[Bool], ready: Bool): UInt = {
      val wChoice = log2Ceil(valid.length)

      if (valid.length == 1)
        return 0.U(0.W)

      val last = RegInit(0.U(wChoice.W))

      val priority0 = PriorityEncoder(valid)
      // Next valid input above the last winner, wrapping to the lowest valid input. The old
      // form only wrapped when `last` was all ones, i.e. for a power-of-2 input count;
      // otherwise PriorityEncoder of an all-false vector returned n-1 and the last input kept
      // winning for as long as it stayed valid, starving every other input.
      val higher = VecInit(valid.zipWithIndex.map { case (x, i) => i.U > last && x })
      val rr0 = Mux(higher.asUInt.orR, PriorityEncoder(higher), priority0)
      val rr = Mux(valid(rr0), rr0, priority0)

      val locked = RegInit(false.B)
      val lockedChoice = RegInit(0.U(wChoice.W))

      val thisChoice = Mux(locked, lockedChoice, rr)

      val shouldLock = valid(thisChoice) && !ready
      locked := shouldLock
      lockedChoice := thisChoice

      when(valid(thisChoice) && ready) {
        last := thisChoice
      }

      thisChoice
    }
  }

  object priority extends Chooser {
    def apply(valid: Vec[Bool], ready: Bool): UInt = {
      val wChoice = log2Ceil(valid.length)

      if (valid.length == 1)
        return 0.U(0.W)

      val priority = PriorityEncoder(valid)

      val locked = RegInit(false.B)
      val lockedChoice = RegInit(0.U(wChoice.W))

      val thisChoice = WireInit(Mux(locked, lockedChoice, priority))

      val shouldLock = valid(thisChoice) && !ready
      locked := shouldLock
      lockedChoice := thisChoice

      thisChoice
    }
  }
}
