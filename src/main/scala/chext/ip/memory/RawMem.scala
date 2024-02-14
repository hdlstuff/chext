package chext.ip.memory

import chisel3._
import chisel3.util._

case class RawMemConfig(
    val wAddr: Int = 6,
    val wData: Int = 32,
    val readLatency: Int = 2,
    val writeLatency: Int = 1
)

trait RawMem extends Module {
  def cfg: RawMemConfig
  def getPorts: Seq[RawInterface]
}
