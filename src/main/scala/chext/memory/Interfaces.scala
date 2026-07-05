package chext.memory

import chisel3._
import chisel3.util._
import chisel3.experimental.SourceInfo

import chext.elastic

// TODO: also support EN
class RawInterface(
    val wAddr: Int,
    val wData: Int,
    val supportsRead: Boolean = true,
    val supportsWrite: Boolean = true
) extends Bundle {
  private val require_ = chext.util.Require.inferred()

  require_(wData >= 8 && (wData % 8) == 0)
  require_(supportsRead || supportsWrite)

  val wStrobe = (wData >> 3)

  /** Address (index RAM words of size wData) */
  val addr = Input(UInt(wAddr.W))

  /** Data input */
  val dIn = Input(Bits(wData.W))

  /** Data output */
  val dOut = Output(Bits(wData.W))

  /** Write strobe */
  val wstrb = Input(UInt(wStrobe.W))
}

class ReadInterface(
    val wAddr: Int,
    val wData: Int
)(implicit si: SourceInfo)
    extends Bundle {
  private val require_ = chext.util.Require.inferred()

  require_(wData >= 8 && (wData % 8) == 0)

  val req = elastic.Source(UInt(wAddr.W))
  val resp = elastic.Sink(UInt(wData.W))
}

class WriteRequest(wAddr: Int, wData: Int) extends Bundle {
  private val require_ = chext.util.Require.inferred()

  require_(wData >= 8 && (wData % 8) == 0)

  val wStrobe = (wData >> 3)

  val addr = UInt(wAddr.W)
  val data = UInt(wData.W)
  val strb = UInt(wStrobe.W)
}

class WriteInterface(
    val wAddr: Int,
    val wData: Int
)(implicit si: SourceInfo)
    extends Bundle {
  val req = elastic.Source(new WriteRequest(wAddr, wData))
  val resp = elastic.Sink(UInt(0.W))
}
