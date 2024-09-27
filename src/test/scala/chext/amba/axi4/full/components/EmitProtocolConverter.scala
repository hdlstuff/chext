package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.amba.axi4
import chext.elastic
import chext.ip.sgdma._
import chext.ip.memory._

import axi4.Ops._
import elastic.ConnectOp._

object emitHdlinfo {
  def apply(module: hdlinfo.Module, targetDir: String): Unit = {
    import io.circe.syntax._
    import io.circe.generic.auto._
    import java.io.PrintWriter

    val pw = new PrintWriter(f"${targetDir}/${module.name}.hdlinfo.json")
    pw.write(module.asJson.toString())
    pw.close()
  }
}

object EmitProtocolConverterTop extends App {
  case class ProtocolConverterTopConfig(
      val numSgdma: Int = 4,
      val axiCfg1: axi4.Config = axi4.Config(),
      val axiCfg2: axi4.Config = axi4.Config(),
      val axiMemCfg: axi4.Config = axi4.Config(),
      val desiredName: String = "ProtocolConverterTop"
  ) {
    val sgdmaCfg = SgdmaConfig(axiCfg1, 14)

    val sgdmaMultiCfg = SgdmaMultiConfig(numSgdma, sgdmaCfg)

    val protocolConverter1Cfg = ProtocolConverterConfig(
      sgdmaMultiCfg.sgdmaCfg.axiMasterCfg,
      axiCfg2
    )

    val muxCfg = MuxConfig(axiCfg2, numSgdma)

    val protocolConverter2Cfg = ProtocolConverterConfig(
      muxCfg.axiMasterCfg,
      axiMemCfg
    )

    val rawMemCfg = RawMemConfig(
      axiMemCfg.wAddr - log2Ceil(axiMemCfg.wData) + 3,
      axiMemCfg.wData
    )

    val portCfg = PortConfig(16, 16)

    val hdlinfoModule: hdlinfo.Module = {
      import io.circe.syntax._
      import io.circe.generic.auto._

      import hdlinfo._

      val ports = scala.collection.mutable.ArrayBuffer.empty[Port]

      ports.addOne(Port("clock", PortDirection.input, PortKind.clock, PortSensitivity.clockRising))

      ports.addOne(
        Port("reset", PortDirection.input, PortKind.reset, PortSensitivity.resetActiveHigh)
      )

      val interfaces = scala.collection.mutable.ArrayBuffer.empty[Interface]

      interfaces.addOne(
        Interface(
          "S_AXI_MEM",
          InterfaceRole.slave,
          InterfaceKind.axi4,
          associatedClock = "clock",
          associatedReset = "reset",
          args = Map("config" -> TypedObject(axiMemCfg))
        )
      )

      interfaces.addOne(
        Interface(
          "S_AXIL_CTRL",
          InterfaceRole.slave,
          InterfaceKind.axi4,
          associatedClock = "clock",
          associatedReset = "reset",
          args = Map("config" -> TypedObject(sgdmaMultiCfg.axiCtrlCfg))
        )
      )

      interfaces.addOne(
        Interface(
          "S_AXI_DESC",
          InterfaceRole.slave,
          InterfaceKind.axi4,
          associatedClock = "clock",
          associatedReset = "reset",
          args = Map("config" -> TypedObject(sgdmaMultiCfg.axiDescCfg))
        )
      )

      Module(desiredName, ports.toSeq, interfaces.toSeq)
    }
  }

  class ProtocolConverterTop(cfg: ProtocolConverterTopConfig) extends Module {
    import cfg._

    val S_AXI_MEM = IO(axi4.Slave(axiMemCfg))

    val S_AXIL_CTRL = IO(axi4.Slave(sgdmaMultiCfg.axiCtrlCfg))
    val S_AXI_DESC = IO(axi4.Slave(sgdmaMultiCfg.axiDescCfg))

    override def desiredName: String = cfg.desiredName

    private val mem = Module(new TrueDualPortRAM(rawMemCfg, portCfg, portCfg))

    private val bridge1 = Module(new Axi4FullToReadWriteBridge(axiMemCfg))
    private val bridge2 = Module(new Axi4FullToReadWriteBridge(axiMemCfg))

    bridge1.read.req :=> mem.read1.req
    mem.read1.resp :=> bridge1.read.resp

    bridge1.write.req :=> mem.write1.req
    mem.write1.resp :=> bridge1.write.resp

    bridge2.read.req :=> mem.read2.req
    mem.read2.resp :=> bridge2.read.resp

    bridge2.write.req :=> mem.write2.req
    mem.write2.resp :=> bridge2.write.resp

    S_AXI_MEM :=> bridge1.s_axi

    private val sgdmaMulti = Module(new SgdmaMulti(sgdmaMultiCfg))
    sgdmaMulti.start := false.B

    S_AXIL_CTRL :=> sgdmaMulti.s_axil_ctrl
    S_AXI_DESC :=> sgdmaMulti.s_axi_desc

    private val protocolConverter1 = Seq.fill(numSgdma) {
      Module(new ProtocolConverter(protocolConverter1Cfg))
    }

    sgdmaMulti.m_axiN
      .zip(protocolConverter1.map { _.s_axi })
      .foreach { //
        case (master, slave) =>
          axi4.full.SlaveBuffer(master, axi4.BufferConfig.all(16)) :=> slave
      }

    private val mux = Module(new Mux(muxCfg))

    protocolConverter1
      .map { _.m_axi }
      .zip(mux.s_axi)
      .foreach { //
        case (master, slave) => master :=> slave
      }

    private val protocolConverter2 = Module(new ProtocolConverter(protocolConverter2Cfg))

    mux.m_axi :=> protocolConverter2.s_axi

    protocolConverter2.m_axi :=> bridge2.s_axi
  }

  val targetDir = "sim/protocolConverter/hdl/"

  def emit(cfg: ProtocolConverterTopConfig): Unit = {
    emitHdlinfo(cfg.hdlinfoModule, targetDir)
    emitVerilog(new ProtocolConverterTop(cfg), Array("--target-dir", targetDir))
  }

  val protocolConverterTopCfgs = Seq(
    ProtocolConverterTopConfig(
      numSgdma = 4,
      axiCfg1 = axi4.Config(wData = 128, wAddr = 16),
      axiCfg2 = axi4.Config(wData = 32, wAddr = 16),
      axiMemCfg = axi4.Config(wData = 64, wAddr = 16, wId = 2),
      "ProtocolConverterTest1"
    )
  )

  protocolConverterTopCfgs.foreach { emit }
}
