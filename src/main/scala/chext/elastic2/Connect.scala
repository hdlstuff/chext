package chext.elastic2

import chisel3._

object connect {
  def apply[T <: Data](source: Interface[T], sink: Interface[T]) = {
    source.ready := sink.ready
    sink.valid := source.valid
    sink.bits := source.bits
  }
}

object ConnectOp {
  implicit class elastic_connect_op[T <: Data](source: Interface[T]) {
    def :=>(sink: Interface[T]): Unit = {
      connect(source, sink)
    }
  }
}
