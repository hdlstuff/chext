package chext.elastic.tracking

import chisel3.experimental.SourceInfo
import chext.elastic.Interface

import scala.collection.mutable.ArrayBuffer

final class ComponentState private[tracking] (component: chext.tracking.Component)
    extends chext.tracking.ComponentState {
  private val sources_ = ArrayBuffer.empty[(String, Interface[_])]
  private val sinks_ = ArrayBuffer.empty[(String, Interface[_])]

  def addSource(name: String, source: Interface[_])(implicit sourceInfo: SourceInfo): Unit = {
    sources_.addOne(name -> source)
    source.markSource()
  }

  def addSink(name: String, sink: Interface[_])(implicit sourceInfo: SourceInfo): Unit = {
    sinks_.addOne(name -> sink)
    sink.markSink()
  }

  def sources: Seq[(String, Interface[_])] =
    sources_.toSeq

  def sinks: Seq[(String, Interface[_])] =
    sinks_.toSeq
}
