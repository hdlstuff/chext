package chext.amba.axi4.full.components

import chisel3._
import chisel3.experimental.prefix

import chext.amba.axi4
import chext.amba.axi4.full.ConnectOp._
import chext.amba.axi4.lite.{ConnectOp => LiteConnectOp}
import chext.amba.axi4.lite.components.RegisterBlock
import chext.amba.axi4.tracking.{PropertyState, ResolveRequest, ResolveResult, Resolver}
import chext.amba.axi4.tracking.properties.{Master, Slave}
import chext.amba.axi4.util.MemoryMap
import chext.util.ElaborationTest

import io.circe.generic.auto._
import io.circe.syntax._

import java.nio.file.Path

class DemuxMmTestTop(
    val numMasters: Int = 3,
    val read: Boolean = true,
    val write: Boolean = true,
    val permutation: Option[Seq[Int]] = None,
    val errorSlave: Option[Int] = None,
    val overallocatedMaster: Option[Int] = None,
    val allocationScheme: MemoryMap.AllocationScheme =
      MemoryMap.AllocationScheme.AlignedPacked
) extends Module
    with chext.AnnotatedModule {
  private val axiCfg =
    axi4.Config(wId = 2, wAddr = 16, wData = 32, read = read, write = write)

  val s_axi = IO(axi4.full.Slave(axiCfg))
  val m_axi = IO(axi4.full.Master.many(numMasters, axiCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)
  declareAxi4Interface(m_axi)

  val demux = Module(new DemuxMm(DemuxMmConfig(axiCfg, numMasters = numMasters)))
  chext.tracking.suggestInstanceName(demux, "demux")
  s_axi :=> demux.s_axi
  demux.m_axi :=> m_axi

  val sizes = Seq[BigInt](0x180, 0x300, 0x81)
  require(numMasters <= sizes.length)
  val masterMemoryMaps = m_axi.zipWithIndex.map { case (master, index) =>
    Option.unless(errorSlave.contains(index)) {
      val masterMemoryMap = MemoryMap(
        path = Seq(s"master$index"),
        offset = 0x1000,
        size = sizes(index) - Option.when(overallocatedMaster.contains(index))(BigInt(1)).getOrElse(0),
        segments = Seq(MemoryMap.Segment(Seq("memory"), 0, sizes(index)))
      )
      master.slaveProps(Slave.MemoryMap) = masterMemoryMap
      masterMemoryMap
    }
  }

  val derivedMemoryMap = demux.genDecoder(permutation, errorSlave, allocationScheme)
  val order = permutation.getOrElse((0 until numMasters).filterNot(errorSlave.contains))
  val expectedOffsets = (allocationScheme, order) match {
    case (MemoryMap.AllocationScheme.AlignedPacked, Seq(2, 0, 1)) =>
      Seq[BigInt](0, 0x200, 0x400)
    case (MemoryMap.AllocationScheme.AlignedPacked, Seq(2, 0)) =>
      Seq[BigInt](0, 0x200)
    case (MemoryMap.AllocationScheme.AlignedPacked, _) =>
      Seq[BigInt](0, 0x400, 0x800).take(numMasters)
    case (MemoryMap.AllocationScheme.AlignedLargest, _) =>
      order.indices.map(index => BigInt(index) * 0x400)
    case (MemoryMap.AllocationScheme.Tight, _) =>
      order
        .map(sizes)
        .scanLeft(BigInt(0))(_ + _)
        .dropRight(1)
  }
  assert(derivedMemoryMap.children.map(_.offset) == expectedOffsets)
  assert(derivedMemoryMap.children.map(_.path) == order.map(index => Seq(s"master$index")))
  def logicalSegments(memoryMap: MemoryMap) =
    memoryMap.segments.map(segment => (segment.path, segment.baseAddress, segment.size))
  assert(
    derivedMemoryMap.children.map(logicalSegments) ==
      order.map(index => logicalSegments(masterMemoryMaps(index).get))
  )
  assert(demux.s_axi.slaveProps(Slave.MemoryMap).get == derivedMemoryMap)

  if (!read) {
    assert(demux.s_axi.slaveProps(Slave.ReadThreads).state == PropertyState.Undefined)
    val slaveReadRequest = ResolveRequest(demux.s_axi, Slave.ReadThreads)
    assert(Resolver.resolve(slaveReadRequest).result == ResolveResult.Success())
    assert(slaveReadRequest.state == PropertyState.Undefined)

    demux.m_axi.foreach { output =>
      assert(output.masterProps(Master.ReadThreads).state == PropertyState.Undefined)
      val masterReadRequest = ResolveRequest(output, Master.ReadThreads)
      assert(Resolver.resolve(masterReadRequest).result == ResolveResult.Success())
      assert(masterReadRequest.state == PropertyState.Undefined)
    }
  }

  if (!write) {
    assert(demux.s_axi.slaveProps(Slave.WriteThreads).state == PropertyState.Undefined)
    val slaveWriteRequest = ResolveRequest(demux.s_axi, Slave.WriteThreads)
    assert(Resolver.resolve(slaveWriteRequest).result == ResolveResult.Success())
    assert(slaveWriteRequest.state == PropertyState.Undefined)

    demux.m_axi.foreach { output =>
      assert(output.masterProps(Master.WriteThreads).state == PropertyState.Undefined)
      val masterWriteRequest = ResolveRequest(output, Master.WriteThreads)
      assert(Resolver.resolve(masterWriteRequest).result == ResolveResult.Success())
      assert(masterWriteRequest.state == PropertyState.Undefined)
    }
  }
}

class DemuxMmTreeTestTop extends Module with chext.AnnotatedModule {
  import LiteConnectOp._

  private val axiCfg = axi4.Config(wId = 0, wAddr = 16, wData = 64)
  private val bufferCfg = axi4.BufferConfig.all(1)

  val s_axi = IO(axi4.full.Slave(axiCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)

  val rootDemux = Module(new DemuxMm(DemuxMmConfig(axiCfg, numMasters = 2)))
  val leftDemux = Module(new DemuxMm(DemuxMmConfig(axiCfg, numMasters = 2)))
  val rightDemux = Module(new DemuxMm(DemuxMmConfig(axiCfg, numMasters = 2)))
  chext.tracking.suggestInstanceName(rootDemux, "rootDemux")
  chext.tracking.suggestInstanceName(leftDemux, "leftDemux")
  chext.tracking.suggestInstanceName(rightDemux, "rightDemux")

  s_axi :=> rootDemux.s_axi
  rootDemux.m_axi(0) :=>
    axi4.full.RightBuffer(leftDemux.s_axi, bufferCfg, "leftBranchBuffer")
  rootDemux.m_axi(1) :=>
    axi4.full.RightBuffer(rightDemux.s_axi, bufferCfg, "rightBranchBuffer")

  val converters = Seq.tabulate(4) { index =>
    val converter = Module(
      new LiteConverter(
        LiteConverterConfig(axiCfg, wDataMaster = 32)
      )
    ).suggestName(s"liteConverter$index")
    chext.tracking.suggestInstanceName(converter, s"liteConverter$index")
    converter
  }
  leftDemux.m_axi :=> converters.take(2).map(_.s_axi)
  rightDemux.m_axi :=> converters.drop(2).map(_.s_axi)

  private val leafMasks = Seq(8, 9, 7, 10)
  val registerBlocks = leafMasks.zipWithIndex.map { case (wMask, index) =>
    prefix(s"registerBlock$index") {
      new RegisterBlock(
        wAddr = axiCfg.wAddr,
        wData = 32,
        wMask = wMask,
        memoryMapPath = Seq(s"leaf$index")
      )
    }
  }

  converters.zip(registerBlocks).foreach { case (converter, registerBlock) =>
    converter.m_axil :=> registerBlock.s_axil
  }

  registerBlocks.zipWithIndex.foreach { case (registerBlock, index) =>
    val storage = prefix(s"storage$index") { RegInit(0.U(32.W)) }
    val status = prefix(s"status$index") { WireDefault((0x100 + index).U(32.W)) }
    registerBlock.reg(storage, desc = "storage")
    registerBlock.reg(status, write = false, desc = "status")
    registerBlock.complete()
  }

  val leftMemoryMap = leftDemux.genDecoder(
    memoryMapPath = Seq("left"),
    captureResolutionTrace = true
  )
  val rightMemoryMap = rightDemux.genDecoder(
    memoryMapPath = Seq("right"),
    captureResolutionTrace = true
  )
  val rootMemoryMap = rootDemux.genDecoder(captureResolutionTrace = true)

  assert(rootMemoryMap.children.map(_.path) == Seq(Seq("left"), Seq("right")))
  assert(rootMemoryMap.children.map(_.offset) == Seq(0, 0x800))
  assert(
    rootMemoryMap.flatten.segments.map(segment => segment.path -> segment.baseAddress) == Seq(
      Seq("left", "leaf0", "registers") -> BigInt(0),
      Seq("left", "leaf1", "registers") -> BigInt(0x200),
      Seq("right", "leaf2", "registers") -> BigInt(0x800),
      Seq("right", "leaf3", "registers") -> BigInt(0xc00)
    )
  )
}

object DemuxMm_Test extends App with ElaborationTest {
  private val HasDemuxModule = "has-demux-module"
  private val MemoryMapJson = textArtifact("MEMORY MAP", "(not available)")
  private val FlattenedMemoryMapJson =
    textArtifact("FLATTENED MEMORY MAP", "(not available)")

  private val captureMemoryMap: (RawModule, ElaborationTest.ArtifactOutput) => Unit =
    (module, artifacts) => {
      val (interface, expectedMemoryMap) = module match {
        case top: DemuxMmTestTop => top.s_axi -> top.derivedMemoryMap
        case top: DemuxMmTreeTestTop => top.s_axi -> top.rootMemoryMap
        case _ => throw new AssertionError(s"Unsupported DemuxMm test top: $module")
      }
      val request = ResolveRequest(interface, Slave.MemoryMap)
      Resolver.resolve(request).result match {
        case ResolveResult.Success() => ()
        case ResolveResult.Failure(message, _) =>
          throw new AssertionError(s"Could not resolve top-level s_axi memory map: $message")
        case ResolveResult.Retry(_) =>
          throw new AssertionError("Top-level s_axi memory-map resolution returned Retry")
      }
      val memoryMap = request.valueOption.getOrElse {
        throw new AssertionError("Resolved top-level s_axi memory map has no value")
      }
      assert(memoryMap == expectedMemoryMap)
      artifacts.write(MemoryMapJson, memoryMap.asJson.spaces2)
      artifacts.write(FlattenedMemoryMapJson, memoryMap.flatten.asJson.spaces2)
    }

  suite(
    name = "demux-mm",
    outputDir = Path.of("output", "demux_mm"),
    commonChecks = Seq(
      (SystemVerilog contains "module DemuxMm").named(HasDemuxModule)
    ),
    reportArtifacts = Seq(
      SystemVerilog,
      ModuleGraphJson,
      MemoryMapJson,
      FlattenedMemoryMapJson,
      Log,
      Errors
    )
  )

  test(
    name = "default",
    gen = () => new DemuxMmTestTop,
    checks = Seq(SystemVerilog contains "module Decoder"),
    captureArtifacts = captureMemoryMap
  )

  test(
    name = "single_port",
    gen = () => new DemuxMmTestTop(numMasters = 1),
    captureArtifacts = captureMemoryMap
  )
  test(
    name = "read_only",
    gen = () => new DemuxMmTestTop(read = true, write = false),
    captureArtifacts = captureMemoryMap
  )
  test(
    name = "write_only",
    gen = () => new DemuxMmTestTop(read = false, write = true),
    captureArtifacts = captureMemoryMap
  )
  test(
    name = "permuted",
    gen = () => new DemuxMmTestTop(permutation = Some(Seq(2, 0, 1))),
    captureArtifacts = captureMemoryMap
  )
  test(
    name = "error_slave",
    gen = () => new DemuxMmTestTop(permutation = Some(Seq(2, 0)), errorSlave = Some(1)),
    captureArtifacts = captureMemoryMap
  )
  test(
    name = "aligned_largest",
    gen = () =>
      new DemuxMmTestTop(
        permutation = Some(Seq(2, 0, 1)),
        allocationScheme = MemoryMap.AllocationScheme.AlignedLargest
      ),
    captureArtifacts = captureMemoryMap
  )
  test(
    name = "tight",
    gen = () => new DemuxMmTestTop(allocationScheme = MemoryMap.AllocationScheme.Tight),
    captureArtifacts = captureMemoryMap
  )
  test(
    name = "overallocated",
    expected = Failure,
    gen = () => new DemuxMmTestTop(overallocatedMaster = Some(1)),
    checks = Seq(
      FailureMessage contains "Invalid aggregated memory map: overallocated",
      FailureMessage contains "segment '/master1/memory'"
    ),
    disabledCommonChecks = Set(HasDemuxModule)
  )
  test(
    name = "tree_with_buffers",
    gen = () => new DemuxMmTreeTestTop,
    checks = Seq(
      SystemVerilog occurs ("  DemuxMm ", 3),
      SystemVerilog occurs ("  LiteConverter ", 4),
      SystemVerilog contains "leftBranchBuffer0_axi4fBuffer0_arBuffer0_queue0",
      SystemVerilog contains "rightBranchBuffer0_axi4fBuffer0_arBuffer0_queue0",
      MemoryMapJson contains "\"origin\" : \"/rootDemux/s_axi\"",
      MemoryMapJson contains "\"leaf0\"",
      MemoryMapJson contains "\"interfaceFrom\" : \"/rootDemux/m_axi_0\"",
      MemoryMapJson contains "\"interfaceTo\" : \"/leftDemux/s_axi\"",
      MemoryMapJson contains "\"kind\" : \"buffer\"",
      MemoryMapJson contains "\"resolver\" : \"BufferResolver\"",
      MemoryMapJson contains "\"resolverPath\" : \"/leftBranchBuffer0_axi4fBuffer0\"",
      MemoryMapJson contains "\"resolver\" : \"LiteConverterResolver\"",
      FlattenedMemoryMapJson contains "\"registers\"",
      ModuleGraph.check("buffers and register blocks are tracked components") { graph =>
        val bufferPaths = graph.flatten.components.collect {
          case component if component.tpe == "Axi4f_Buffer" => component.path
        }
        val registerBlockPaths = graph.flatten.components.collect {
          case component if component.tpe == "Axi4l_RegisterBlock" => component.path
        }
        Option.unless(
          bufferPaths == Seq(
            "/leftBranchBuffer0_axi4fBuffer0",
            "/rightBranchBuffer0_axi4fBuffer0"
          )
        )(s"found buffer component paths: ${bufferPaths.mkString(", ")}").orElse(
          Option.unless(registerBlockPaths.size == 4)(
            s"found register-block component paths: ${registerBlockPaths.mkString(", ")}"
          )
        )
      }
    ),
    captureArtifacts = captureMemoryMap
  )

  runTests()
}
