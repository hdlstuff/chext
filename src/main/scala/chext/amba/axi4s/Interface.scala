package chext.amba.axi4s

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.experimental.dataview._

import chext.elastic
import chext.util.NamedVec

case class Config(
    val wData: Int,
    val wId: Int = 0,
    val wDest: Int = 0,
    val wUser: Int = 0,
    val hasReady: Boolean = true,
    val hasStrobe: Boolean = true,
    val hasKeep: Boolean = true,
    val hasLast: Boolean = true
) {
  private val require_ = chext.util.Require.inferred()

  require_(wData % 8 == 0)
  require_(wId >= 0)
  require_(wDest >= 0)
  require_(wUser >= 0)
  require_(hasReady)

  private def _maybeZero(p: Boolean, w: Int) = if (p) w else 0

  val wStrobe = _maybeZero(hasStrobe, wData / 8)
  val wKeep = _maybeZero(hasKeep, wData / 8)
  val wLast = _maybeZero(hasLast, 1)
}

object Config {
  def full(
      wData: Int = 32,
      wId: Int = 0,
      wDest: Int = 0,
      wUser: Int = 0
  ): Config =
    Config(wData = wData, wId = wId, wDest = wDest, wUser = wUser)

  def lite(wData: Int = 32): Config =
    Config(
      wData = wData,
      hasStrobe = false,
      hasKeep = false,
      hasLast = false
    )

  def liteLast(wData: Int = 32): Config =
    Config(
      wData = wData,
      hasStrobe = false,
      hasKeep = false,
      hasLast = true
    )
}

class Interface(val cfg: Config)(implicit si: SourceInfo) extends Bundle {
  def sourceInfo: SourceInfo = si

  private[axi4s] val viewCalls_ =
    scala.collection.mutable.ArrayBuffer.empty[ViewCall]

  val TREADY = Input(Bool())
  val TVALID = Output(Bool())
  val TDATA = Output(UInt(cfg.wData.W))
  val TSTRB = Output(UInt(cfg.wStrobe.W))
  val TKEEP = Output(UInt(cfg.wKeep.W))
  val TLAST = Output(UInt(cfg.wLast.W))
  val TID = Output(UInt(cfg.wId.W))
  val TDEST = Output(UInt(cfg.wDest.W))
  val TUSER = Output(UInt(cfg.wUser.W))
}

object Master {

  /** create an AXI4-stream master interface with the given configuration. */
  def apply(cfg: Config)(implicit si: SourceInfo) = Interface(cfg)

  def many(
      n: Int,
      cfg: Config,
      naming: NamedVec.Naming = NamedVec.Plain
  )(implicit si: SourceInfo): NamedVec[Interface] =
    NamedVec.many(n, apply(cfg), naming)
}

object Slave {

  /** create an AXI4-stream slave interface with the given configuration. */
  def apply(cfg: Config)(implicit si: SourceInfo) = Flipped(Interface(cfg))

  def many(
      n: Int,
      cfg: Config,
      naming: NamedVec.Naming = NamedVec.Plain
  )(implicit si: SourceInfo): NamedVec[Interface] =
    NamedVec.many(n, apply(cfg), naming)
}

object Interface {
  def apply(cfg: Config)(implicit si: SourceInfo): Interface = new Interface(cfg)

  def many(
      n: Int,
      cfg: Config,
      naming: NamedVec.Naming = NamedVec.Plain
  )(implicit si: SourceInfo): NamedVec[Interface] =
    NamedVec.many(n, apply(cfg), naming)

  @annotation.nowarn /* suppress warning: Implicit definition should have explicit type */
  implicit val view = DataView[Interface, elastic.Interface[UInt]](
    interface => elastic.Interface(UInt(interface.cfg.wData.W)),
    _.TREADY -> _.$ready,
    _.TVALID -> _.$valid,
    _.TDATA -> _.$bits
  )
}
