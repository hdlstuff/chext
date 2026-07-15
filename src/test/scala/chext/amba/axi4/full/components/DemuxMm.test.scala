package chext.amba.axi4.full.components

import chisel3._

import chext.amba.axi4
import chext.amba.axi4.full.ConnectOp._
import chext.amba.axi4.tracking.properties.Slave
import chext.amba.axi4.util.MemoryMap
import chext.util.ElaborationTest

import java.nio.file.Path

class DemuxMmTestTop(
    val numMasters: Int = 3,
    val read: Boolean = true,
    val write: Boolean = true,
    val permutation: Option[Seq[Int]] = None,
    val errorSlave: Option[Int] = None
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
  s_axi :=> demux.s_axi
  demux.m_axi :=> m_axi

  val sizes = Seq[BigInt](0x180, 0x300, 0x81)
  require(numMasters <= sizes.length)
  val masterMemoryMaps = m_axi.zipWithIndex.map { case (master, index) =>
    Option.unless(errorSlave.contains(index)) {
      val masterMemoryMap = MemoryMap(
        path = Seq(s"master$index"),
        offset = 0x1000,
        segments = Seq(MemoryMap.Segment(Seq("memory"), 0, sizes(index)))
      )
      master.slaveProps(Slave.MemoryMap) = masterMemoryMap
      masterMemoryMap
    }
  }

  val derivedMemoryMap = demux.genDecoder(permutation, errorSlave)
  val order = permutation.getOrElse((0 until numMasters).filterNot(errorSlave.contains))
  val expectedOffsets =
    if (order == Seq(2, 0, 1)) Seq[BigInt](0, 0x200, 0x400)
    else if (order == Seq(2, 0)) Seq[BigInt](0, 0x200)
    else Seq[BigInt](0, 0x400, 0x800).take(numMasters)
  assert(derivedMemoryMap.children.map(_.offset) == expectedOffsets)
  assert(derivedMemoryMap.children.map(_.path) == order.map(index => Seq(s"master$index")))
  assert(
    derivedMemoryMap.children.map(_.segments) ==
      order.map(index => masterMemoryMaps(index).get.segments)
  )
  assert(demux.s_axi.slaveProps(Slave.MemoryMap).get == derivedMemoryMap)
}

object DemuxMm_Test extends App with ElaborationTest {
  suite(
    name = "demux-mm",
    outputDir = Path.of("output", "demux_mm"),
    commonChecks = Seq(SystemVerilog contains "module DemuxMm")
  )

  test(
    name = "default",
    gen = () => new DemuxMmTestTop,
    checks = Seq(SystemVerilog contains "module Decoder")
  )

  test(name = "single_port", gen = () => new DemuxMmTestTop(numMasters = 1))
  test(name = "read_only", gen = () => new DemuxMmTestTop(read = true, write = false))
  test(name = "write_only", gen = () => new DemuxMmTestTop(read = false, write = true))
  test(
    name = "permuted",
    gen = () => new DemuxMmTestTop(permutation = Some(Seq(2, 0, 1)))
  )
  test(
    name = "error_slave",
    gen = () => new DemuxMmTestTop(permutation = Some(Seq(2, 0)), errorSlave = Some(1))
  )

  runTests()
}
