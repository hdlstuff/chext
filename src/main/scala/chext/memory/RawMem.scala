package chext.memory

import chisel3._

case class RawMemConfig(
    val wAddr: Int = 10,
    val wData: Int = 32,
    val latencyRead: Int = 2,

    /** @note Not used in case of ROM. */
    val latencyWrite: Int = 1

    /** @note
      *   TODO: To support ROMs, extend this structure with an initial content field. Maybe, it
      *   should be loaded from a file or the contents are given inline?
      */
) {
  private val require_ = chext.util.Require.inferred()

  require_(latencyRead >= 0)
  require_(latencyWrite >= 0)

  require_(wData >= 8 && wData % 8 == 0)
}

trait RawMem extends Module {
  def cfg: RawMemConfig
  def getPorts: Seq[RawInterface]
}
