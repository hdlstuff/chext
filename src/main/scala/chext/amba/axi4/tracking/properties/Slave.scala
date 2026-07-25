package chext.amba.axi4.tracking.properties

import chext.amba.axi4.tracking.{
  PropertyKey,
  SlavePropertyKey,
  SlaveReadProperty,
  SlaveWriteProperty
}

object Slave extends Iterable[PropertyKey[_]] {
  case object MemoryMap
      extends SlavePropertyKey[chext.amba.axi4.util.MemoryMap](
        "memoryMap",
        "Memory map provided by the slave."
      )

  case object ReadOutstandingTransactions
      extends SlaveReadProperty[Int](
        "read_outstandingTransactions",
        "Maximum number of read transactions the slave can accept."
      )

  case object WriteOutstandingTransactions
      extends SlaveWriteProperty[Int](
        "write_outstandingTransactions",
        "Maximum number of write transactions the slave can accept."
      )

  case object ReadThreads
      extends SlaveReadProperty[Int](
        "read_threads",
        "Maximum number of read threads the slave can accept."
      )

  case object WriteThreads
      extends SlaveWriteProperty[Int](
        "write_threads",
        "Maximum number of write threads the slave can accept."
      )

  case object ReadBurstBeats
      extends SlaveReadProperty[Int](
        "read_burstBeats",
        "Maximum read burst length in beats accepted by the slave."
      )

  case object WriteBurstBeats
      extends SlaveWriteProperty[Int](
        "write_burstBeats",
        "Maximum write burst length in beats accepted by the slave."
      )

  case object ReadBurstNarrow
      extends SlaveReadProperty[Boolean](
        "read_burstNarrow",
        "Whether the slave accepts narrow read bursts."
      )

  case object WriteBurstNarrow
      extends SlaveWriteProperty[Boolean](
        "write_burstNarrow",
        "Whether the slave accepts narrow write bursts."
      )

  case object ReadBurstTypes
      extends SlaveReadProperty[Set[Int]](
        "read_burstTypes",
        "AXI burst types the slave accepts for reads."
      )

  case object WriteBurstTypes
      extends SlaveWriteProperty[Set[Int]](
        "write_burstTypes",
        "AXI burst types the slave accepts for writes."
      )

  case object ReadBurstSizes
      extends SlaveReadProperty[Set[Int]](
        "read_burstSizes",
        "AXI transfer sizes the slave accepts for reads."
      )

  case object WriteBurstSizes
      extends SlaveWriteProperty[Set[Int]](
        "write_burstSizes",
        "AXI transfer sizes the slave accepts for writes."
      )

  /** All standard properties associated with read traffic. */
  val readProperties: Set[PropertyKey[_]] = Set(
    ReadOutstandingTransactions,
    ReadThreads,
    ReadBurstBeats,
    ReadBurstNarrow,
    ReadBurstTypes,
    ReadBurstSizes
  )

  /** All standard properties associated with write traffic. */
  val writeProperties: Set[PropertyKey[_]] = Set(
    WriteOutstandingTransactions,
    WriteThreads,
    WriteBurstBeats,
    WriteBurstNarrow,
    WriteBurstTypes,
    WriteBurstSizes
  )

  /** Properties that describe the shape of traffic accepted by a slave. */
  val shapeProperties: Set[PropertyKey[_]] = Set(
    ReadBurstBeats,
    WriteBurstBeats,
    ReadBurstNarrow,
    WriteBurstNarrow,
    ReadBurstTypes,
    WriteBurstTypes,
    ReadBurstSizes,
    WriteBurstSizes
  )

  /** Boolean extractor for keys in [[shapeProperties]]. */
  object ShapeProperty {
    def unapply(key: PropertyKey[_]): Boolean =
      shapeProperties.contains(key)
  }

  val all: Seq[PropertyKey[_]] = Seq(
    MemoryMap,
    ReadOutstandingTransactions,
    WriteOutstandingTransactions,
    ReadThreads,
    WriteThreads,
    ReadBurstBeats,
    WriteBurstBeats,
    ReadBurstNarrow,
    WriteBurstNarrow,
    ReadBurstTypes,
    WriteBurstTypes,
    ReadBurstSizes,
    WriteBurstSizes
  )

  def iterator: Iterator[PropertyKey[_]] = all.iterator
}
