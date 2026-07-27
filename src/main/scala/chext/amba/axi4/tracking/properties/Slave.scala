package chext.amba.axi4.tracking.properties

import chext.amba.axi4.tracking.{
  PropertyKey,
  ReadProperty,
  SlavePropertyKey,
  SlaveReadProperty,
  SlaveWriteProperty,
  WriteProperty
}
import chext.amba.axi4.tracking.values.{
  BurstShape,
  MemoryMap,
  ThreadMode,
  TrafficProfile
}

object Slave extends Iterable[PropertyKey[_]] {
  case object MemoryMap
      extends SlavePropertyKey[MemoryMap](
        "memoryMap",
        "Memory map provided by the slave."
      )

  case object ReadBurstShape
      extends SlaveReadProperty[BurstShape](
        "read_burstShape",
        "Burst shapes the slave accepts for reads."
      )

  case object WriteBurstShape
      extends SlaveWriteProperty[BurstShape](
        "write_burstShape",
        "Burst shapes the slave accepts for writes."
      )

  case object ReadThreadMode
      extends SlaveReadProperty[ThreadMode](
        "read_threadMode",
        "ID relationship among read transactions accepted by the slave."
      )

  case object WriteThreadMode
      extends SlaveWriteProperty[ThreadMode](
        "write_threadMode",
        "ID relationship among write transactions accepted by the slave."
      )

  case object ReadTrafficProfile
      extends SlaveReadProperty[TrafficProfile](
        "read_trafficProfile",
        "Concurrency and latency of read traffic accepted by the slave."
      )

  case object WriteTrafficProfile
      extends SlaveWriteProperty[TrafficProfile](
        "write_trafficProfile",
        "Concurrency and latency of write traffic accepted by the slave."
      )

  val all: Seq[PropertyKey[_]] = Seq(
    MemoryMap,
    ReadBurstShape,
    WriteBurstShape,
    ReadThreadMode,
    WriteThreadMode,
    ReadTrafficProfile,
    WriteTrafficProfile
  )

  val readProperties: Set[PropertyKey[_]] =
    all.collect { case key @ ReadProperty() => key }.toSet

  val writeProperties: Set[PropertyKey[_]] =
    all.collect { case key @ WriteProperty() => key }.toSet

  def iterator: Iterator[PropertyKey[_]] = all.iterator
}
