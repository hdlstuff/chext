package chext.amba.axi4s

import chisel3._
import chisel3.experimental.dataview.PartialDataView

import chext.elastic

class FullChannel(cfg: Config) extends Bundle {
  val data = Bits(cfg.wData.W)
  val strobe = UInt(cfg.wStrobe.W)
  val keep = UInt(cfg.wKeep.W)
  val last = UInt(cfg.wLast.W)
  val id = UInt(cfg.wId.W)
  val dest = UInt(cfg.wDest.W)
  val user = UInt(cfg.wUser.W)
}

object FullChannel {
  @annotation.nowarn /* suppress warning: Implicit definition should have explicit type */
  implicit val view1 = PartialDataView.mapping[Interface, elastic.Interface[FullChannel]](
    interface => elastic.Interface(new FullChannel(interface.cfg)),
    (interface, elasticInterface) => Seq[Tuple2[Data, Data]](
      interface.TREADY -> elasticInterface.$ready,
      interface.TVALID -> elasticInterface.$valid,
      interface.TDATA -> elasticInterface.$bits.data,
      interface.TSTRB -> elasticInterface.$bits.strobe,
      interface.TKEEP -> elasticInterface.$bits.keep,
      interface.TLAST -> elasticInterface.$bits.last,
      interface.TID -> elasticInterface.$bits.id,
      interface.TDEST -> elasticInterface.$bits.dest,
      interface.TUSER -> elasticInterface.$bits.user
    )
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
