package chext.amba.axi4.full.components

import chisel3._
import chisel3.experimental.prefix
import chisel3.util.log2Ceil

import java.nio.file.Path

import chext.amba.axi4
import chext.amba.axi4.full.ConnectOp._
import chext.amba.axi4.lite.{ConnectOp => LiteConnectOp}
import chext.amba.axi4.lite.components.RegisterBlock
import chext.amba.axi4.tracking.{ResolveRequest, ResolveResult, Resolver}
import chext.amba.axi4.tracking.properties.{Master, Slave}
import chext.amba.axi4.util.MemoryMap
import chext.util.{ElaborationTest, SimulationCheck}

private class LiteConverterTestTop(
    wId: Int,
    wDataSlave: Int,
    wDataMaster: Int = 32,
    simCheckNarrow: SimulationCheck = SimulationCheck.Default,
    simCheckAligned: SimulationCheck = SimulationCheck.Default
) extends Module
    with chext.AnnotatedModule {
  import LiteConnectOp._

  private val fullCfg = axi4.Config(wId = wId, wAddr = 16, wData = wDataSlave)
  private val liteCfg =
    fullCfg.copy(wId = 0, wData = wDataMaster, lite = true, axi3Compat = false)

  val s_axi = IO(axi4.full.Slave(fullCfg))
  val m_axil = IO(axi4.lite.Master(liteCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)
  declareAxi4Interface(m_axil)

  val converter = Module(
    new LiteConverter(
      LiteConverterConfig(
        fullCfg,
        wDataMaster = wDataMaster,
        simCheckNarrow = simCheckNarrow,
        simCheckAligned = simCheckAligned
      )
    )
  )
  chext.tracking.suggestInstanceName(converter, "converter")

  s_axi :=> converter.s_axi
  converter.m_axil :=> m_axil

  private val terminalMap = MemoryMap(
    path = Seq("registers"),
    size = 0x100,
    segments = Seq(MemoryMap.Segment(Seq("all"), 0, 0x100))
  )
  m_axil.slaveProps(Slave.MemoryMap) = terminalMap

  private val request = ResolveRequest(s_axi, Slave.MemoryMap)
  assert(Resolver.recursiveResolve(request) == ResolveResult.Success())
  assert(request.valueOption.contains(terminalMap))

  assert(converter.m_axil.masterProps(Master.ReadThreads).get == 1)
  assert(converter.m_axil.masterProps(Master.ReadBurstBeats).get == 1)
  assert(
    converter.m_axil.masterProps(Master.ReadBurstSizes).get ==
      Set(log2Ceil(wDataMaster / 8))
  )
  private val expectedMasterOutstanding =
    if (wDataSlave > wDataMaster) 2 * (wDataSlave / wDataMaster)
    else if (wDataSlave < wDataMaster) 2
    else 2 * 256
  assert(
    converter.m_axil.masterProps(Master.ReadOutstandingTransactions).get ==
      expectedMasterOutstanding
  )
  assert(converter.s_axi.slaveProps(Slave.ReadThreads).get == 1)
  assert(converter.s_axi.slaveProps(Slave.ReadBurstBeats).get == 256)
}

private class RegisterBlockTestTop(completeMode: Int)
    extends Module
    with chext.AnnotatedModule {
  import LiteConnectOp._

  private val cfg = axi4.Config(wAddr = 16, wData = 32, lite = true)
  val s_axil = IO(axi4.lite.Slave(cfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axil)

  val registerBlock = prefix("registerBlock") {
    new RegisterBlock(wAddr = 16, wData = 32, wMask = 8, memoryMapPath = Seq("leaf"))
  }
  s_axil :=> registerBlock.s_axil

  private val storage = RegInit(0.U(32.W))
  private val status = WireDefault(0x1234.U(32.W))
  registerBlock.reg(storage, desc = "storage")
  registerBlock.reg(status, write = false, desc = "status")

  if (completeMode >= 1) {
    val generatedMap = registerBlock.complete()
    assert(generatedMap.path == Seq("leaf"))
    assert(generatedMap.size == 0x100)
    assert(generatedMap.segments.map(_.path) == Seq(Seq("registers")))
  }
  if (completeMode >= 2)
    registerBlock.complete()
}

object LiteConverter_Test extends App with ElaborationTest {
  suite(
    name = "lite-converter",
    outputDir = Path.of("output", "lite_converter")
  )

  test(
    name = "conversion_stages",
    gen = () =>
      new LiteConverterTestTop(
        wId = 0,
        wDataSlave = 64
      ),
    checks = Seq(
      SystemVerilog excludes "module IdSerialize",
      SystemVerilog contains "module Downscale",
      SystemVerilog contains "module Unburst",
      SystemVerilog contains "Unburst unburstInput",
      SystemVerilog contains "Unburst_1 unburstOutput",
      SystemVerilog contains "axi4.full.components.Downscale: ARLEN must be zero",
      SystemVerilog contains "axi4.full.components.Downscale: AWLEN must be zero"
    )
  )

  test(
    name = "upscale",
    gen = () =>
      new LiteConverterTestTop(
        wId = 0,
        wDataSlave = 32,
        wDataMaster = 64
      ),
    checks = Seq(
      SystemVerilog contains "module Upscale",
      SystemVerilog contains "module SteerLeft",
      SystemVerilog contains "module SteerRight",
      SystemVerilog excludes "module Downscale",
      SystemVerilog contains "Unburst unburst"
    )
  )

  test(
    name = "transfer_shape_checks",
    gen = () =>
      new LiteConverterTestTop(
        wId = 0,
        wDataSlave = 32,
        simCheckNarrow = SimulationCheck.Printf,
        simCheckAligned = SimulationCheck.Printf
      ),
    checks = Seq(
      SystemVerilog excludes "module IdSerialize",
      SystemVerilog excludes "module Downscale",
      SystemVerilog excludes "module Upscale",
      SystemVerilog contains "module Unburst",
      SystemVerilog contains "Unburst unburst",
      SystemVerilog contains
        "axi4.full.components.LiteConverter: ARSIZE must describe a full-width transfer",
      SystemVerilog contains
        "axi4.full.components.LiteConverter: AWSIZE must describe a full-width transfer",
      SystemVerilog contains
        "axi4.full.components.LiteConverter: ARADDR must be naturally aligned",
      SystemVerilog contains
        "axi4.full.components.LiteConverter: AWADDR must be naturally aligned"
    )
  )

  test(
    name = "nonzero_id_width",
    expected = Failure,
    gen = () =>
      new LiteConverterTestTop(
        wId = 2,
        wDataSlave = 32
      ),
    checks = Seq(
      FailureMessage contains "axiSlaveCfg.wId must be zero"
    )
  )

  test(
    name = "register_block_complete",
    gen = () => new RegisterBlockTestTop(completeMode = 1),
    checks = Seq(SystemVerilog contains "RegisterBlockTestTop")
  )
  test(
    name = "register_block_missing_complete",
    expected = Failure,
    gen = () => new RegisterBlockTestTop(completeMode = 0),
    checks = Seq(FailureMessage contains "RegisterBlock.complete() must be called exactly once")
  )
  test(
    name = "register_block_duplicate_complete",
    expected = Failure,
    gen = () => new RegisterBlockTestTop(completeMode = 2),
    checks = Seq(FailureMessage contains "RegisterBlock.complete() must be called exactly once")
  )

  runTests()
}
