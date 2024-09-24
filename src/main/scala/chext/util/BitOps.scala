package chext.util

import chisel3._
import chisel3.util._

object BitOps {
  implicit class UIntBitOps(x: UInt) {
    val width = x.getWidth

    /** Creates a mask whose last n bits are set.
      *
      * @param n
      * @return
      */
    private def makeMask(n: Int): UInt = {
      if (n == width)
        ~0.U(width.W)
      else
        (0.U((width - n - 1).W) ## 1.U(1.W) ## 0.U(n.W)) -% 1.U
    }

    def resetLastN(n: Int): UInt = {
      require(n >= 0 && n <= width)
      x & ~makeMask(n)
    }

    def setLastN(n: Int): UInt = {
      require(n >= 0 && n <= width)
      x | makeMask(n)
    }
  }
}
