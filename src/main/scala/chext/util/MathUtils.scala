package chext.util

object MathUtils {
  def nextPowerOfTwo(value: BigInt): BigInt = {
    require(value > 0, "value must be positive")
    if ((value & (value - 1)) == 0) value
    else BigInt(1) << value.bitLength
  }

  def alignUp(value: BigInt, alignment: BigInt): BigInt = {
    require(value >= 0, "value must not be negative")
    require(alignment > 0, "alignment must be positive")
    ((value + alignment - 1) / alignment) * alignment
  }
}
