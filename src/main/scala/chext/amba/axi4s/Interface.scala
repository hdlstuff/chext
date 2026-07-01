package chext.amba.axi4s

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.experimental.dataview._

import chext.elastic
import chext.util.NamedVec

case class Config(
    val wData: Int,
    val onlyRV: Boolean = false,
    val wId: Int = 0,
    val wDest: Int = 0,
    val wUser: Int = 0
) {
  require(wData % 8 == 0)

  val wStrobe = wData / 8
  val wKeep = wStrobe
}

class Interface(val cfg: Config)(implicit si: SourceInfo) extends Bundle {
  def sourceInfo: SourceInfo = si

  private[axi4s] val viewCalls_ =
    scala.collection.mutable.ArrayBuffer.empty[ViewCall]

  val TREADY = Input(Bool())
  val TVALID = Output(Bool())
  val TDATA = Output(UInt(cfg.wData.W))
  val TSTRB = if (!cfg.onlyRV) Some(Output(UInt(cfg.wStrobe.W))) else None
  val TKEEP = if (!cfg.onlyRV) Some(Output(UInt(cfg.wKeep.W))) else None
  val TLAST = if (!cfg.onlyRV) Some(Output(Bool())) else None
  val TID =
    if (!cfg.onlyRV && cfg.wId > 0) Some(Output(UInt(cfg.wId.W))) else None
  val TDEST =
    if (!cfg.onlyRV && cfg.wDest > 0) Some(Output(UInt(cfg.wDest.W))) else None
  val TUSER =
    if (!cfg.onlyRV && cfg.wUser > 0) Some(Output(UInt(cfg.wUser.W))) else None
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
