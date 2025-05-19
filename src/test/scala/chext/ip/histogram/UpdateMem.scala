package chext.ip.histogram

import chisel3._
import chisel3.util._

import chext.{elastic2 => elastic}
import elastic.ConnectOp._

import chext.amba.axi4
import axi4.Ops._

import chext.util.BitOps._

import chext.{HasHdlinfoModule, TestBench}
import chext.util.TestApp.encodedData

class UpdateMemTop1(
    val cfg: UpdateMemConfig = UpdateMemConfig(),
    override val desiredName: String = "UpdateMemTop1"
) extends Module
    with HasHdlinfoModule {
  private val axiCfg = cfg.axiCfg.copy(wAddr = 16)

  private val dut = Module(new UpdateMem(cfg))

  val sourceItem = IO(elastic.Source(cfg.genItem))
  val sinkResult = IO(elastic.Sink(cfg.genResult))

  val sourceTest = IO(elastic.Source(new Bundle {
    val f0 = UInt(0.W)
    val f1 = UInt(0.W)
  }))

  val sourceZero = IO(elastic.Source(UInt(0.W)))

  val S_AXI = IO(axi4.Slave(axiCfg))

  sourceTest.nodeq()
  sourceZero.nodeq()

  private val (s_axi1, s_axi2) = {
    import chext.ip.memory._

    val rawMemCfg = RawMemConfig(axiCfg.wAddr - (log2Ceil(axiCfg.wData) - 3), axiCfg.wData, 1, 1)
    val portCfg = PortConfig(4, 4, () => new BasicReadWriteArbiter(8))

    val mem = Module(new TrueDualPortRAM(rawMemCfg, portCfg, portCfg))

    val axiBridge1 = Module(new Axi4FullToReadWriteBridge(axiCfg))
    val axiBridge2 = Module(new Axi4FullToReadWriteBridge(axiCfg))

    axiBridge1.read.req :=> mem.read1.req
    mem.read1.resp :=> axiBridge1.read.resp

    axiBridge1.write.req :=> mem.write1.req
    mem.write1.resp :=> axiBridge1.write.resp

    axiBridge2.read.req :=> mem.read2.req
    mem.read2.resp :=> axiBridge2.read.resp

    axiBridge2.write.req :=> mem.write2.req
    mem.write2.resp :=> axiBridge2.write.resp

    (axiBridge1.s_axi, axiBridge2.s_axi)
  }

  dut.m_axi :=> s_axi1
  S_AXI :=> s_axi2

  sourceItem :=> dut.sourceItem
  dut.sinkResult :=> sinkResult

  def hdlinfoModule: hdlinfo.Module = {
    import hdlinfo._
    import io.circe.generic.auto._

    val encodedDataBuilder = new chext.util.EncodedDataBuilder()
    encodedDataBuilder.add("Item", cfg.genItem)
    encodedDataBuilder.add("Result", cfg.genResult)
    encodedDataBuilder.add("Test", chiselTypeOf(sourceTest.bits))
    encodedDataBuilder.add("Zero", chiselTypeOf(sourceZero.bits))

    val ports = Seq(
      Port(
        "clock",
        PortDirection.input,
        PortKind.clock,
        PortSensitivity.clockRising,
        associatedReset = "reset"
      ),
      Port(
        "reset",
        PortDirection.input,
        PortKind.reset,
        PortSensitivity.resetActiveHigh,
        associatedClock = "clock"
      )
    )

    val interfaces = Seq(
      Interface(
        "sourceItem",
        InterfaceRole("source"),
        InterfaceKind(f"readyValid[Item]"),
        associatedClock = "clock",
        associatedReset = "reset"
      ),
      Interface(
        "sinkResult",
        InterfaceRole("sink"),
        InterfaceKind(f"readyValid[Result]"),
        associatedClock = "clock",
        associatedReset = "reset"
      ),
      Interface(
        "sourceTest",
        InterfaceRole("source"),
        InterfaceKind(f"readyValid[Test]"),
        associatedClock = "clock",
        associatedReset = "reset"
      ),
      Interface(
        "sourceZero",
        InterfaceRole("source"),
        InterfaceKind(f"readyValid[Zero]"),
        associatedClock = "clock",
        associatedReset = "reset"
      ),
      Interface(
        "S_AXI",
        InterfaceRole.slave,
        InterfaceKind("axi4"),
        associatedClock = "clock",
        associatedReset = "reset",
        args = Map("config" -> TypedObject(axiCfg))
      )
    )
    val args = Map(
      "cfg" -> TypedObject(cfg),
      encodedDataBuilder.build()
    )

    Module(desiredName, ports, interfaces, args)
  }
}

object UpdateMem_TB extends chext.TestBench {
  emit(new UpdateMemTop1())
}
