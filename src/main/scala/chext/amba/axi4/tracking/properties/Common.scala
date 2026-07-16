package chext.amba.axi4.tracking.properties

import chext.amba.axi4.tracking.{CommonPropertyKey, PropertyKey}

object Common extends Iterable[PropertyKey[_]] {
  case object Config
      extends CommonPropertyKey[chext.amba.axi4.Config](
        "config",
        "AXI interface configuration shared by master and slave properties."
      )

  val all: Seq[PropertyKey[_]] = Seq(Config)

  def iterator: Iterator[PropertyKey[_]] = all.iterator
}
