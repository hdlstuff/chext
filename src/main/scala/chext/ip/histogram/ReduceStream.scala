package chext.ip.histogram

import chisel3._
import chisel3.util._

import chext.{elastic2 => elastic}
import elastic._

import chext.amba.axi4
import axi4.Ops._

case class ReduceStreamConfig() {
  val genItem = new Item
}

class ReduceStream(cfg: ReduceStreamConfig) extends Module {
  import cfg._

  val source = IO(elastic.Source(genItem))
  val sink = IO(elastic.Sink(genItem))

  private val isFirst = RegInit(true.B)

  private val lastBucket = RegInit(64.U(64.W))
  private val lastValue = RegInit(64.U(64.W))

  val arrival = new elastic.Arrival(source, sink) {
    when(arrived) {
      assert(in.last === in.zero)

      when(isFirst) {
        when(in.last) {
          out.bucket := DontCare
          out.value := DontCare
          out.zero := true.B
          out.last := true.B

          accept()
        }.otherwise {
          lastBucket := in.bucket
          lastValue := in.value
          isFirst := false.B

          drop()
        }
      }.otherwise {
        when(in.last) {
          out.bucket := lastBucket
          out.value := lastValue
          out.zero := false.B
          out.last := true.B

          isFirst := true.B

          accept()
        }.otherwise {
          when(in.bucket === lastBucket) {
            lastValue := lastValue + in.value

            drop()
          }.otherwise {
            out.bucket := lastBucket
            out.value := lastValue
            out.zero := false.B
            out.last := false.B

            lastBucket := in.bucket
            lastValue := in.value

            accept()
          }
        }
      }
    }
  }
}
