package chext.elastic.tracking

import chisel3.Data
import chisel3.experimental.SourceInfo
import chext.elastic.Interface

import scala.collection.mutable.ArrayBuffer

/** A component-local name and classification for a referenced Elastic interface. */
final case class ComponentInterface(
    name: String,
    interface: Interface[Data],
    boundary: Boolean
)

final class ComponentState private[tracking] (component: chext.tracking.Component)
    extends chext.tracking.ComponentState {
  private val sources_ = ArrayBuffer.empty[ComponentInterface]
  private val sinks_ = ArrayBuffer.empty[ComponentInterface]

  /** Registers a named source port. A boundary port is serialized but does not mark the interface
    * as an operational source.
    */
  def addSource(name: String, source: Interface[Data], boundary: Boolean = false)(implicit
      sourceInfo: SourceInfo
  ): Unit = {
    sources_.addOne(ComponentInterface(name, source, boundary))
    if (!boundary)
      source.markSource()
  }

  /** Registers a named sink port. A boundary port is serialized but does not mark the interface as
    * an operational sink.
    */
  def addSink(name: String, sink: Interface[Data], boundary: Boolean = false)(implicit
      sourceInfo: SourceInfo
  ): Unit = {
    sinks_.addOne(ComponentInterface(name, sink, boundary))
    if (!boundary)
      sink.markSink()
  }

  def sources: Seq[ComponentInterface] =
    sources_.toSeq

  def sinks: Seq[ComponentInterface] =
    sinks_.toSeq
}
