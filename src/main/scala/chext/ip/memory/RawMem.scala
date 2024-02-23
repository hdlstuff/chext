package chext.ip.memory

import chisel3._
import chisel3.util._

class RawMemConfig(
    val wAddr: Int,
    val wData: Int,
    val latencyRead: Int,

    /** @note Not used in case of ROM. */
    val latencyWrite: Int

    /** @note
      *   TODO: To support ROMs, extend this structure with an initial content field. Maybe, it
      *   should be loaded from a file or the contents are given inline?
      */
) {
  require(latencyRead >= 0)
  require(latencyWrite >= 0)

  assert(wData >= 8 && wData % 8 == 0)
}

object RawMemConfig {
  def apply(wAddr: Int = 6, wData: Int = 32, latencyRead: Int = 2, latencyWrite: Int = 1) =
    new RawMemConfig(
      wAddr = wAddr,
      wData = wData,
      latencyRead = latencyRead,
      latencyWrite = latencyWrite
    )
}

trait RawMem extends Module {
  def cfg: RawMemConfig
  def getPorts: Seq[RawInterface]
}
