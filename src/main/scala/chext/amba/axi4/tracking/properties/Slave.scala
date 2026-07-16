package chext.amba.axi4.tracking.properties

import chext.amba.axi4.tracking.{PropertyKey, SlavePropertyKey}

object Slave extends Iterable[PropertyKey[_]] {
  case object MemoryMap
      extends SlavePropertyKey[chext.amba.axi4.util.MemoryMap](
        "memoryMap",
        "Memory map provided by the slave."
      )

  case object ReadOutstandingTransactions
      extends SlavePropertyKey[Int](
        "read_outstandingTransactions",
        "Maximum number of read transactions the slave can accept."
      )

  case object WriteOutstandingTransactions
      extends SlavePropertyKey[Int](
        "write_outstandingTransactions",
        "Maximum number of write transactions the slave can accept."
      )

  case object ReadThreads
      extends SlavePropertyKey[Int](
        "read_threads",
        "Maximum number of read threads the slave can accept."
      )

  case object WriteThreads
      extends SlavePropertyKey[Int](
        "write_threads",
        "Maximum number of write threads the slave can accept."
      )

  case object ReadBurstBeats
      extends SlavePropertyKey[Int](
        "read_burstBeats",
        "Maximum read burst length in beats accepted by the slave."
      )

  case object WriteBurstBeats
      extends SlavePropertyKey[Int](
        "write_burstBeats",
        "Maximum write burst length in beats accepted by the slave."
      )

  case object ReadBurstNarrow
      extends SlavePropertyKey[Boolean](
        "read_burstNarrow",
        "Whether the slave accepts narrow read bursts."
      )

  case object WriteBurstNarrow
      extends SlavePropertyKey[Boolean](
        "write_burstNarrow",
        "Whether the slave accepts narrow write bursts."
      )

  case object ReadBurstTypes
      extends SlavePropertyKey[Set[Int]](
        "read_burstTypes",
        "AXI burst types the slave accepts for reads."
      )

  case object WriteBurstTypes
      extends SlavePropertyKey[Set[Int]](
        "write_burstTypes",
        "AXI burst types the slave accepts for writes."
      )

  case object ReadBurstSizes
      extends SlavePropertyKey[Set[Int]](
        "read_burstSizes",
        "AXI transfer sizes the slave accepts for reads."
      )

  case object WriteBurstSizes
      extends SlavePropertyKey[Set[Int]](
        "write_burstSizes",
        "AXI transfer sizes the slave accepts for writes."
      )

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
