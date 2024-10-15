package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.amba.axi4
import chext.elastic
import chext.ip.memory

import axi4.Ops._
import elastic.ConnectOp._

class UnburstTestTop1(override val desiredName: String)
    extends Module
    with chext.HasHdlinfoModule {
  // (2 ** 10) * 16B = 16 KB of memory (14 bits)
  val rawMemCfg = memory.RawMemConfig(10, 128, 1, 1)
  val portCfg = memory.PortConfig(8, 8, () => new memory.BasicReadWriteArbiter(32))
  val axiCfg = axi4.Config(0, 14, 128)

  val S_AXI_NORMAL = IO(axi4.Slave(axiCfg))
  val S_AXI_SPLIT = IO(axi4.Slave(axiCfg))

  private val mem = Module(
    new memory.TrueDualPortRAM(rawMemCfg, portCfg, portCfg)
  )

  private val axiBridge1 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg))

  S_AXI_NORMAL :=> axiBridge1.s_axi

  axiBridge1.read.req :=> mem.read1.req
  mem.read1.resp :=> axiBridge1.read.resp

  axiBridge1.write.req :=> mem.write1.req
  mem.write1.resp :=> axiBridge1.write.resp

  private val axiBridge2 = Module(new memory.Axi4FullToReadWriteBridge(axiCfg))

  axiBridge2.read.req :=> mem.read2.req
  mem.read2.resp :=> axiBridge2.read.resp

  axiBridge2.write.req :=> mem.write2.req
  mem.write2.resp :=> axiBridge2.write.resp

  private val unburstCfg = axi4.full.components.UnburstConfig(axiCfg)
  private val unburst = Module(new axi4.full.components.Unburst(unburstCfg))
  S_AXI_SPLIT :=> unburst.s_axi
  unburst.m_axi :=> axiBridge2.s_axi

  override def hdlinfoModule: hdlinfo.Module = {
    import hdlinfo._
    import io.circe.generic.auto._
    import scala.collection.mutable.ArrayBuffer

    val ports = ArrayBuffer.empty[Port]
    val interfaces = ArrayBuffer.empty[Interface]

    ports.append(
      Port(
        "clock",
        PortDirection.input,
        PortKind.clock,
        PortSensitivity.clockRising,
        associatedReset = "reset"
      )
    )
    ports.append(
      Port(
        "reset",
        PortDirection.input,
        PortKind.reset,
        PortSensitivity.resetActiveHigh,
        associatedClock = "clock"
      )
    )

    interfaces.append(
      Interface(
        "S_AXI_NORMAL",
        InterfaceRole.slave,
        InterfaceKind("axi4"),
        associatedClock = "clock",
        associatedReset = "reset",
        args = Map("cfg" -> TypedObject(axiCfg))
      )
    )

    interfaces.append(
      Interface(
        "S_AXI_SPLIT",
        InterfaceRole.slave,
        InterfaceKind("axi4"),
        associatedClock = "clock",
        associatedReset = "reset",
        args = Map("cfg" -> TypedObject(axiCfg))
      )
    )

    Module(
      desiredName,
      ports.toSeq,
      interfaces.toSeq,
      Map()
    )
  }
}

object Unburst_TB extends chext.TestBench {
  emit(new UnburstTestTop1("UnburstTestTop1_1"))
}
