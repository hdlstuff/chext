package chext.amba.axi4.full.components

import chisel3._
import chisel3.experimental.prefix

import java.nio.file.Path

import chext.amba.axi4
import chext.amba.axi4.BurstType.Encoding.{FIXED, INCR, WRAP}
import chext.amba.axi4.full.ConnectOp._
import chext.amba.axi4.lite.{ConnectOp => LiteConnectOp}
import chext.amba.axi4.lite.components.RegisterBlock
import chext.amba.axi4.tracking.{PropertyState, ResolveRequest, ResolveResult, Resolver}
import chext.amba.axi4.tracking.properties.{Master, Slave}
import chext.amba.axi4.tracking.values.{BurstShape, MemoryMap, ThreadMode}
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
  private val fullInputShape = BurstShape(
    len = 256,
    tpe = Seq(FIXED, INCR, WRAP),
    size = Seq(BurstShape.fullSize(wDataSlave)),
    align = BurstShape.fullSize(wDataSlave)
  )
  s_axi.masterProps(Master.ReadBurstShape) = fullInputShape
  s_axi.masterProps(Master.WriteBurstShape) = fullInputShape
  s_axi.masterProps(Master.ReadThreadMode) = ThreadMode.SingleThread
  s_axi.masterProps(Master.WriteThreadMode) = ThreadMode.SingleThread
  m_axil.slaveProps(Slave.ReadThreadMode) = ThreadMode.SingleThread
  m_axil.slaveProps(Slave.WriteThreadMode) = ThreadMode.SingleThread

  private val request = ResolveRequest(s_axi, Slave.MemoryMap)
  assert(Resolver.resolve(request).result == ResolveResult.Success())
  assert(request.valueOption.contains(terminalMap))

  assert(
    converter.m_axil.masterProps(Master.ReadThreadMode).get ==
      ThreadMode.SingleThread
  )
  assert(
    converter.m_axil.masterProps(Master.ReadBurstShape).state ==
      PropertyState.Undefined
  )
  assert(
    converter.s_axi.slaveProps(Slave.ReadThreadMode).get ==
      ThreadMode.SingleThread
  )
  assert(converter.s_axi.slaveProps(Slave.ReadBurstShape).get.len == 256)
  assert(
    converter.s_axi.slaveProps(Slave.ReadBurstShape).get.align ==
      BurstShape.fullSize(wDataSlave)
  )
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
  s_axil.masterProps(Master.ReadThreadMode) = ThreadMode.SingleTransaction
  s_axil.masterProps(Master.WriteThreadMode) = ThreadMode.SingleTransaction

  private val storage = RegInit(0.U(32.W))
  private val status = WireDefault(0x1234.U(32.W))
  registerBlock.reg(storage, desc = "storage")
  registerBlock.reg(status, write = false, desc = "status")

  assert(registerBlock.memoryMapOption.isEmpty)
  private val memoryMapUnavailable =
    try {
      registerBlock.memoryMap
      false
    } catch {
      case _: IllegalStateException => true
    }
  assert(memoryMapUnavailable)
  assert(
    registerBlock.s_axil.slaveProps(Slave.ReadThreadMode).get ==
      ThreadMode.SingleTransaction
  )

  private val earlyRequest = Option.when(completeMode == 3) {
    ResolveRequest(registerBlock.s_axil, Slave.MemoryMap)
  }
  earlyRequest.foreach { request =>
    Resolver.resolve(request).result match {
      case ResolveResult.Failure(message, failedRequest) =>
        assert(message.contains("RegisterBlock.complete() was not called"))
        assert(failedRequest == request)
      case result => assert(false, result.toString)
    }
  }

  if (completeMode != 0) {
    val generatedMap = registerBlock.complete()
    assert(generatedMap.path == Seq("leaf"))
    assert(generatedMap.size == 0x100)
    assert(generatedMap.segments.map(_.path) == Seq(Seq("registers")))
    assert(registerBlock.memoryMapOption.contains(generatedMap))
    assert(registerBlock.memoryMap == generatedMap)

    val request =
      earlyRequest.getOrElse(ResolveRequest(registerBlock.s_axil, Slave.MemoryMap))
    assert(Resolver.resolve(request).result == ResolveResult.Success())
    assert(request.valueOption.contains(generatedMap))
  }
  if (completeMode == 2)
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
  test(
    name = "register_block_resolution_before_complete",
    gen = () => new RegisterBlockTestTop(completeMode = 3),
    checks = Seq(SystemVerilog contains "RegisterBlockTestTop")
  )

  runTests()
}
