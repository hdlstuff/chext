package chext

import chisel3._
import chisel3.util._

package object float {
  private val debug = false

  implicit class fixedPointHelpers(val fx: SInt) extends AnyVal {
    def to_floating_point(genFp: FloatingPoint): FloatingPoint = {
      val result = Wire(genFp)
      val wFixedPoint = fx.getWidth
      val wMantissa = genFp.wMantissa

      // by extending the mantissa, we automatically get rid of the preceding 1
      // which is not part of the floating point representation
      val extendedMantissa = Wire(UInt((wFixedPoint + 1).W))

      val sign = fx(wFixedPoint - 1)

      when(sign === 0.B) {
        extendedMantissa := Cat(0.B, fx.asUInt)
      }.otherwise {
        extendedMantissa := Cat(0.B, (-fx).asUInt)
      }

      val shift = PriorityEncoder(Reverse(extendedMantissa))
      val shifted = (extendedMantissa << shift)

      if (wFixedPoint >= wMantissa) {
        result.mantissa := shifted(
          wFixedPoint - 1,
          (wFixedPoint - 1) - wMantissa + 1
        )
      } else {
        result.mantissa := Cat(
          shifted(wFixedPoint - 1, 0),
          0.U((wMantissa - wFixedPoint).W)
        )
      }

      result.sign := sign
      when(fx === 0.S) {
        result.exponent := 0.U
      }.otherwise {
        result.exponent := wFixedPoint.U +& result.exponentOffset.U -& shift
      }

      if (debug) {
        dontTouch(extendedMantissa.suggestName("_D_extendedMantissa"))
        dontTouch(shift.suggestName("_D_shift"))
        dontTouch(shifted.suggestName("_D_shifted"))
        dontTouch(
          shifted(wFixedPoint - 1, 0).suggestName("_D_shifted_slice")
        )
        dontTouch(result.mantissa.suggestName("_D_result_mantissa"))
        println(s"padding = ${wMantissa - wFixedPoint}")
      }

      result
    }
  }

  implicit class fixedPointHelpersU(val fx: UInt) extends AnyVal {
    def to_floating_point(genFp: FloatingPoint): FloatingPoint = {
      val result = Wire(genFp)
      val wFixedPoint = fx.getWidth
      val wMantissa = genFp.wMantissa
      val extendedMantissa = Cat(0.B, fx)

      val shift = PriorityEncoder(Reverse(extendedMantissa))
      val shifted = (extendedMantissa << shift)

      if (wFixedPoint >= wMantissa) {
        result.mantissa := shifted(
          wFixedPoint - 1,
          (wFixedPoint - 1) - wMantissa + 1
        )
      } else {
        result.mantissa := Cat(
          shifted(wFixedPoint - 1, 0),
          0.U((wMantissa - wFixedPoint).W)
        )
      }

      result.sign := 0.B
      when(fx === 0.U) {
        result.exponent := 0.U
      }.otherwise {
        result.exponent := wFixedPoint.U +& result.exponentOffset.U -& shift
      }
      result
    }
  }

  implicit class floatingPointHelpers(val fp: FloatingPoint) extends AnyVal {
    def to_unsigned_fixed_point(genFx: UInt): UInt = {
      val result = Wire(genFx)
      val wFixedPoint = genFx.getWidth
      val wMantissa = fp.wMantissa
      val extendedMantissa = Cat(1.B, fp.mantissa)
      val breakpt = fp.exponentOffset + wFixedPoint - 1

      val a = Wire(genFx)

      if (wFixedPoint > wMantissa) {
        val constant_0 = wFixedPoint - wMantissa - 1
        if (constant_0 > 0)
          a := Cat(1.B, fp.mantissa, 0.U(constant_0.W))
        else
          a := Cat(1.B, fp.mantissa)
      } else {
        a := Cat(
          1.B,
          fp.mantissa(
            wMantissa - 1,
            (wMantissa - 1) - (wFixedPoint - 1) + 1
          )
        )
      }

      when(fp.exponent === 0.U) {
        result := 0.U
      }.elsewhen(fp.exponent > breakpt.U) {
        result := (-1).S(wFixedPoint.W).asUInt
      }.otherwise /* floating_point.exponent <= breakpt.U */ {
        result := a >> (breakpt.U -% fp.exponent)
      }

      result
    }

    def to_fixed_point(genFx: SInt): SInt = {
      val result = Wire(genFx)
      val wFixedPoint = genFx.getWidth

      val a = Wire(genFx)
      val a_neg = Wire(genFx)

      a := to_unsigned_fixed_point(UInt(wFixedPoint.W)).asSInt
      a_neg := -a

      when(~fp.sign) {
        when(a(wFixedPoint - 1)) {
          // overflow
          result := Cat(0.B, (-1).S((wFixedPoint - 1).W)).asSInt
        }.otherwise {
          result := a
        }
      }.otherwise {
        when(~a_neg(wFixedPoint - 1)) {
          // overflow
          result := Cat(1.B, (0).S((wFixedPoint - 1).W)).asSInt
        }.otherwise {
          result := a_neg
        }
      }

      result
    }
  }
}
