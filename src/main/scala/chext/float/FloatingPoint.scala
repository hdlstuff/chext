package chext.float

import chisel3._

/** Floating point type.
  *
  * @param wExponent
  *   Exponent width.
  * @param wMantissa
  *   Mantissa width (excluding the sign bit).
  */
case class FloatingPoint(val wExponent: Int, val wMantissa: Int) extends Bundle {
  val sign = Bool()
  val exponent = UInt(wExponent.W)
  val mantissa = UInt(wMantissa.W)

  val exponentOffset = (BigInt(1) << (wExponent - 1)) - 1

  def zero: FloatingPoint = {
    0.U.asTypeOf(this)
  }

  /** @return
    *   A string representation of the bundle.
    */
  override def toString(): String = s"fpe${wExponent}m${wMantissa}"
}

object FloatingPoint {
  def ieeeFp16 = FloatingPoint(5, 10)
  def ieeeFp32 = FloatingPoint(8, 23)
  def ieeeFp64 = FloatingPoint(11, 52)
  def fp18 = FloatingPoint(10, 7)
  def bfloat16 = FloatingPoint(8, 7)
}
