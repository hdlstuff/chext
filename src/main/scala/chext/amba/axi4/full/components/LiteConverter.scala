package chext.amba.axi4.full.components

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.util._

import chext.amba.axi4
import chext.elastic
import chext.util.SimulationCheck
import axi4.full.ConnectOp._

/** Configuration for [[LiteConverter]].
  *
  * Stage-enable parameters permit hardware to be omitted when the corresponding restriction does
  * not apply. The Full interface must be ID-free (`wId == 0`). Burst and width-conversion stages
  * are inferred from the interface shapes and are instantiated whenever required. Simulation-check
  * parameters optionally verify that transfers accepted at `s_axi` are full-width and naturally
  * aligned.
  */
case class LiteConverterConfig(
    axiSlaveCfg: axi4.Config,
    wDataMaster: Int = 32,
    numOutstandingRead: Int = 2,
    numOutstandingWrite: Int = 2,
    simCheckNarrow: SimulationCheck = SimulationCheck.Default,
    simCheckAligned: SimulationCheck = SimulationCheck.Default
) {
  private val require_ = chext.util.Require.inferred()

  require_(!axiSlaveCfg.lite, "axiSlaveCfg must describe an AXI4-Full interface")
  require_(axiSlaveCfg.read || axiSlaveCfg.write, "at least one channel group is required")
  require_(wDataMaster == 32 || wDataMaster == 64, "AXI4-Lite data width must be 32 or 64")
  require_(axiSlaveCfg.wId == 0, "axiSlaveCfg.wId must be zero")
  require_(numOutstandingRead >= 2, "read outstanding capacity must be at least two")
  require_(numOutstandingWrite >= 2, "write outstanding capacity must be at least two")

  private val userWidths = Seq(
    "AR" -> axiSlaveCfg.wUserAR,
    "R" -> axiSlaveCfg.wUserR,
    "AW" -> axiSlaveCfg.wUserAW,
    "W" -> axiSlaveCfg.wUserW,
    "B" -> axiSlaveCfg.wUserB
  )
  require_(
    userWidths.forall(_._2 == 0),
    "LiteConverter does not support AXI user data",
    userWidths.map { case (channel, width) => s"wUser$channel = $width" }
  )

  val useUpscale: Boolean = axiSlaveCfg.wData < wDataMaster
  val useDownscale: Boolean = axiSlaveCfg.wData > wDataMaster

  val axiMasterCfg: axi4.Config = axiSlaveCfg.copy(
    wId = 0,
    wData = wDataMaster,
    lite = true,
    axi3Compat = false,
    wUserAR = 0,
    wUserR = 0,
    wUserAW = 0,
    wUserW = 0,
    wUserB = 0
  )
}

/** Converts an AXI4-Full slave interface into an AXI4-Lite master interface.
  *
  * The Full interface must have `wId == 0`; this converter does not instantiate `IdSerialize`.
  * Bursts accepted at `s_axi` are always decomposed before width conversion. `Upscale` steers read
  * data and shifts write data and strobes into the addressed wider lanes. `Downscale` can create a
  * new burst, so its `m_axi` interface is followed by another unburst stage. Transfers accepted at
  * `s_axi` must be full-width and naturally aligned; simulation checks are controlled by
  * [[LiteConverterConfig.simCheckNarrow]] and [[LiteConverterConfig.simCheckAligned]].
  */
class LiteConverter(val cfg: LiteConverterConfig) extends Module with chext.AnnotatedModule {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axil = IO(axi4.lite.Master(axiMasterCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)
  declareAxi4Interface(m_axil)

  private val resolver = new LiteConverter_Resolver(this)

  private val fullSizeSlave = log2Ceil(axiSlaveCfg.wData / 8)

  private type FullStage = (axi4.full.Interface, axi4.full.Interface)
  private val widthConvertedCfg = axiSlaveCfg.copy(wData = wDataMaster)

  private def unburstStage(stageCfg: axi4.Config, name: String): FullStage = {
    val module = Module(
      new Unburst(
        UnburstConfig(
          stageCfg,
          numOutstandingRead = numOutstandingRead,
          numOutstandingWrite = numOutstandingWrite
        )
      )
    ).suggestName(name)
    (module.s_axi, module.m_axi)
  }

  private def checkTransferShape(): Unit = {
    def check(
        valid: Bool,
        size: UInt,
        addr: UInt,
        channel: String
    ): Unit = {
      simCheckNarrow(
        !valid || size === fullSizeSlave.U,
        s"axi4.full.components.LiteConverter: ${channel}SIZE must describe a full-width transfer"
      )
      if (fullSizeSlave > 0)
        simCheckAligned(
          !valid || addr(fullSizeSlave - 1, 0) === 0.U,
          s"axi4.full.components.LiteConverter: ${channel}ADDR must be naturally aligned"
        )
    }

    if (axiSlaveCfg.read)
      check(s_axi.ar.$valid, s_axi.ar.$bits.size, s_axi.ar.$bits.addr, "AR")
    if (axiSlaveCfg.write)
      check(s_axi.aw.$valid, s_axi.aw.$bits.size, s_axi.aw.$bits.addr, "AW")
  }

  checkTransferShape()

  private val inputUnburstStage =
    unburstStage(axiSlaveCfg, if (useDownscale) "unburstInput" else "unburst")

  private val upscaleStage = Option.when(useUpscale) {
    val module = Module(
      new Upscale(
        UpscaleConfig(
          axiSlaveCfg,
          wDataMaster,
          numOutstandingRead = numOutstandingRead,
          numOutstandingWrite = numOutstandingWrite
        )
      )
    ).suggestName("upscale")
    (module.s_axi, module.m_axi): FullStage
  }

  private val downscaleStage = Option.when(useDownscale) {
    val module = Module(
      new Downscale(
        DownscaleConfig(
          axiSlaveCfg,
          wDataMaster,
          numOutstandingRead = numOutstandingRead,
          numOutstandingWrite = numOutstandingWrite
        )
      )
    ).suggestName("downscale")
    (module.s_axi, module.m_axi): FullStage
  }

  private val widthStage = upscaleStage.orElse(downscaleStage)
  private val outputUnburstStage = Option.when(useDownscale) {
    unburstStage(widthConvertedCfg, "unburstOutput")
  }

  private val stages: Seq[(axi4.full.Interface, axi4.full.Interface)] =
    Seq(inputUnburstStage) ++ widthStage.toSeq ++ outputUnburstStage.toSeq

  private val converted = stages.foldLeft(s_axi: axi4.full.Interface) {
    case (upstream, (stageSlave, stageMaster)) =>
      upstream :=> stageSlave
      stageMaster
  }
  private val bridgeResolver = new LiteConverterBridge_Resolver(this, converted)

  private def bridgeRead(): Unit = {
    val transformAr = new elastic.Transform(converted.ar, m_axil.ar) {
      out.addr := in.addr
      out.prot := in.prot
    }

    val transformR = new elastic.Transform(m_axil.r, converted.r) {
      out.id := 0.U
      out.data := in.data
      out.resp := in.resp
      out.last := true.B
      out.user := 0.U
    }
  }

  private def bridgeWrite(): Unit = {
    val transformAw = new elastic.Transform(converted.aw, m_axil.aw) {
      out.addr := in.addr
      out.prot := in.prot
    }

    val transformW = new elastic.Transform(converted.w, m_axil.w) {
      out.data := in.data
      out.strb := in.strb
    }

    val transformB = new elastic.Transform(m_axil.b, converted.b) {
      out.id := 0.U
      out.resp := in.resp
      out.user := 0.U
    }
  }

  if (axiSlaveCfg.read) bridgeRead()
  if (axiSlaveCfg.write) bridgeWrite()
}

/** Resolves properties and initializes interface properties for one [[LiteConverter]].
  *
  * Slave properties, most importantly `p.SlaveMemoryMap`, flow from `m_axil` to `s_axi`, while
  * master properties flow from `s_axi` to `m_axil`.
  */
private final class LiteConverter_Resolver(owner: LiteConverter)(implicit sourceInfo: SourceInfo)
    extends axi4.tracking.Resolver(owner) {
  import axi4.tracking._
  import axi4.tracking.{properties => p, values => v}

  import owner.cfg._

  bindSlave(owner.s_axi)
  bindMaster(owner.m_axil)

  private val fullSizeSlave = v.BurstShape.fullSize(axiSlaveCfg.wData)
  private val fullShape = v.BurstShape(
    maxBeats = v.BurstShape.maxBeatsFor(axiSlaveCfg),
    types = v.BurstShape.supportedTypesFor(axiSlaveCfg),
    sizes = Seq(fullSizeSlave),
    aligned = true
  )

  if (axiSlaveCfg.read) {
    owner.s_axi.properties(p.SlaveReadBurstShape) = fullShape
    owner.s_axi.properties(p.SlaveReadThreadMode) = v.ThreadMode.SingleThread
    owner.m_axil.properties(p.MasterReadThreadMode) = v.ThreadMode.SingleThread
  }
  if (axiSlaveCfg.write) {
    owner.s_axi.properties(p.SlaveWriteBurstShape) = fullShape
    owner.s_axi.properties(p.SlaveWriteThreadMode) = v.ThreadMode.SingleThread
    owner.m_axil.properties(p.MasterWriteThreadMode) = v.ThreadMode.SingleThread
  }

  /** Forwards one non-traffic-shape property across the protocol boundary. */
  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case ResolveRequest(_, p.Key(p.Slave, _, p.MemoryMap)) =>
        request.forwardTo(owner.m_axil)
      case ResolveRequest(_, p.Key(_, _, p.TrafficProfile)) =>
        request.incomplete()
      case _ =>
        request.missingCase()
    }
}

/** Propagates slave properties across the manually wired Full-to-Lite channel bridge. */
private final class LiteConverterBridge_Resolver(
    owner: LiteConverter,
    converted: axi4.full.Interface
)(implicit sourceInfo: SourceInfo)
    extends axi4.tracking.Resolver(owner) {
  import axi4.tracking._
  import axi4.tracking.{properties => p, values => v}
  import axi4.BurstType.Encoding.INCR

  bindSlave(converted)

  private val convertedSizes = v.BurstShape.supportedSizesFor(converted.cfg)
  private val fullBridgeShape = v.BurstShape(
    maxBeats = 1,
    types = Seq(INCR),
    sizes = convertedSizes,
    aligned = false
  )

  if (converted.cfg.read)
    converted.properties(p.SlaveReadBurstShape) = fullBridgeShape
  if (converted.cfg.write)
    converted.properties(p.SlaveWriteBurstShape) = fullBridgeShape

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case ResolveRequest(_, p.Key(p.Slave, _, p.MemoryMap | p.ThreadMode | p.TrafficProfile)) =>
        request.forwardTo(owner.m_axil)
      case _ =>
        request.missingCase()
    }
}
