package chext.axi4

import chisel3.experimental.dataview._

object Casts {
  implicit class viewInterfaceAs(x: Interface) {
    def asLite = x.viewAs[lite.Interface]
  }
}
