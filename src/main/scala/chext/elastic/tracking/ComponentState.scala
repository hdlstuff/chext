package chext.elastic.tracking

import chisel3.experimental.SourceInfo

import scala.collection.mutable.ArrayBuffer

final class ComponentState private[tracking] (component: chext.tracking.Component)
    extends chext.tracking.ComponentState {
  private val sourcePorts_ = ArrayBuffer.empty[(String, Tracked)]
  private val sinkPorts_ = ArrayBuffer.empty[(String, Tracked)]

  def addSourcePort(name: String, source: Tracked)(implicit sourceInfo: SourceInfo): Unit = {
    sourcePorts_.addOne(name -> source)
    source.markSource()
  }

  def addSinkPort(name: String, sink: Tracked)(implicit sourceInfo: SourceInfo): Unit = {
    sinkPorts_.addOne(name -> sink)
    sink.markSink()
  }

  def sourcePorts: Seq[(String, Tracked)] =
    sourcePorts_.toSeq

  def sinkPorts: Seq[(String, Tracked)] =
    sinkPorts_.toSeq
}
