package chext.elastic

import chisel3._
import chisel3.util._

object connect {
  def apply[T <: Data](source: ReadyValidIO[T], sink: ReadyValidIO[T]) = {
    source.ready := sink.ready
    sink.valid := source.valid
    sink.bits := source.bits
  }
}

object ConnectOps {
  implicit class connectTo[T <: Data](source: ReadyValidIO[T]) {
    def :=>(sink: ReadyValidIO[T]): Unit = {
      connect(source, sink)
    }
  }
}
