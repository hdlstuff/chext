package chext.util

import chisel3._
import chisel3.experimental.AffectsChiselPrefix
import chisel3.util._

class Counter(maxExclusive: Int) extends AffectsChiselPrefix {
  val wCounter = log2Up(maxExclusive)

  val incEn = Wire(Bool())
  val decEn = Wire(Bool())
  val empty = Wire(Bool())
  val full_ = Wire(Bool())

  dontTouch(incEn)
  dontTouch(decEn)
  dontTouch(empty)
  dontTouch(full_)

  incEn := DontCare
  decEn := DontCare

  private val rCounter = RegInit(0.U(wCounter.W))

  private def impl(): Unit = {
    when(incEn && decEn) {}
      .elsewhen(incEn) {
        rCounter := rCounter + 1.U
      }
      .elsewhen(decEn) {
        rCounter := rCounter - 1.U
      }

    empty := rCounter === 0.U
    full_ := rCounter === (maxExclusive - 1).U
  }

  impl()

  def zero = empty
  def notZero = !empty

  def full = full_
  def notFull = !full_

  def noInc() = incEn := false.B
  def inc() = incEn := true.B

  def noDec() = decEn := false.B
  def dec() = decEn := true.B
}

class CounterEx(maxExclusive: Int) extends AffectsChiselPrefix {
  val wCounter = log2Ceil(maxExclusive)

  val upEn = Wire(UInt(wCounter.W))
  val downEn = Wire(UInt(wCounter.W))
  val used = Wire(UInt(wCounter.W))
  val left = Wire(UInt(wCounter.W))

  dontTouch(upEn)
  dontTouch(downEn)
  dontTouch(used)
  dontTouch(left)

  upEn := DontCare
  downEn := DontCare

  private val rUsed = RegInit(0.U(wCounter.W))
  private val rLeft = RegInit((maxExclusive - 1).U(wCounter.W))

  private def impl(): Unit = {
    when(upEn > downEn) {
      rUsed := rUsed + (upEn - downEn)
      rLeft := rLeft - (upEn - downEn)
    }.otherwise {
      rUsed := rUsed - (downEn - upEn)
      rLeft := rLeft + (downEn - upEn)
    }

    used := rUsed
    left := rLeft
  }

  impl()

  def canUp(x: UInt) = left >= x
  def canDown(x: UInt) = used >= x

  def up(x: UInt) = upEn := x
  def down(x: UInt) = downEn := x

  def noUp() = up(0.U)
  def noDown() = down(0.U)
}
