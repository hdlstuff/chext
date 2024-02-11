package chext.axi4

import chisel3.experimental.dataview._
import full.Interface

object Casts {
  implicit class viewInterfaceAs(x: RawInterface) {
    def asFull = x.viewAs[full.Interface]
    def asLite = x.viewAs[lite.Interface]
  }
}
