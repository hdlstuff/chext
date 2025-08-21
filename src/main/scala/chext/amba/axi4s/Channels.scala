package chext.amba.axi4s

import chisel3._
import chisel3.experimental.dataview.PartialDataView

import chext.elastic

class FullChannel(cfg: Config) extends Bundle {
  val data = Bits(cfg.wData.W)
  val strobe = UInt(cfg.wStrobe.W)
  val keep = UInt(cfg.wKeep.W)
  val last = Bool()
  val id = if (cfg.wId > 0) Some(UInt(cfg.wId.W)) else None
  val dest = if (cfg.wDest > 0) Some(UInt(cfg.wDest.W)) else None
  val user = if (cfg.wUser > 0) Some(UInt(cfg.wUser.W)) else None
}

object FullChannel {
  @annotation.nowarn /* suppress warning: Implicit definition should have explicit type */
  implicit val view1 = PartialDataView.mapping[Interface, elastic.Interface[FullChannel]](
    interface => elastic.Interface(new FullChannel(interface.cfg)),
    (interface, elasticInterface) => Seq[Tuple2[Option[Data], Option[Data]]](
      Some(interface.TREADY) -> Some(elasticInterface.$ready),
      Some(interface.TVALID) -> Some(elasticInterface.$valid),
      Some(interface.TDATA) -> Some(elasticInterface.$bits.data),
      interface.TSTRB -> Some(elasticInterface.$bits.strobe),
      interface.TKEEP -> Some(elasticInterface.$bits.keep),
      interface.TLAST -> Some(elasticInterface.$bits.last),
      interface.TID -> elasticInterface.$bits.id,
      interface.TDEST -> elasticInterface.$bits.dest,
      interface.TUSER -> elasticInterface.$bits.user
    )
      .filter { case (a, b) => a.nonEmpty && b.nonEmpty }
      .map { case (a, b) => a.get -> b.get }
  )
}

object BasicChannel {
  @annotation.nowarn /* suppress warning: Implicit definition should have explicit type */
  implicit val view2 = PartialDataView.mapping[Interface, elastic.Interface[Bits]](
    interface => elastic.Interface(Bits(interface.cfg.wData.W)),
    (interface, elasticInterface) => Seq[Tuple2[Data, Data]](
      interface.TREADY -> elasticInterface.$ready,
      interface.TVALID -> elasticInterface.$valid,
      interface.TDATA -> elasticInterface.$bits
    )
  )
}
