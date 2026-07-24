package chext.amba.axi4.full.components

import chisel3._
import chisel3.experimental.BaseModule
import chisel3.util._

import chext.amba.axi4
import chext.amba.axi4.tracking._
import chext.amba.axi4.tracking.properties.{Master, Slave}
import chext.elastic
import chext.elastic.ConnectOp._
import chext.util.SimulationCheck
import axi4.full.ConnectOp._

/** Configuration for [[LiteConverter]].
  *
  * Stage-enable parameters permit hardware to be omitted when the corresponding restriction is
  * The Full interface must be ID-free (`wId == 0`). Burst and width-conversion stages are inferred
  * from the interface shapes and are instantiated whenever required. Simulation-check parameters
  * optionally verify that input transfers are full-width and naturally aligned.
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

/** Resolves properties that cross the Full/Lite protocol boundary.
  *
  * Traffic-shape properties are enforced directly on the applicable interface. Unresolved slave
  * properties, most importantly `Slave.MemoryMap`, flow from the Lite master interface to the Full
  * slave interface. Unresolved master properties flow in the opposite direction.
  */
private final class LiteConverterResolver(
    owner: BaseModule,
    upstream: axi4.tracking.Tracked,
    downstream: axi4.tracking.Tracked
) extends Resolver(owner) {
  upstream.addResolver(SlaveTag, this)
  downstream.addResolver(MasterTag, this)

  override def kind: String = "lite-converter"
  override def resolver: String = "LiteConverterResolver"

  def resolve[T](request: ResolveRequest[T]): ResolveResult = {
    val target =
      if ((request.interface eq upstream) && request.role == SlaveTag) downstream
      else if ((request.interface eq downstream) && request.role == MasterTag) upstream
      else
        return request.failure(
          s"LiteConverter cannot resolve '${request.qualifiedName}' from this endpoint"
        )

    val dependency = request.retarget(target)
    if (!dependency.isResolved)
      request.retry(Seq(dependency))
    else
      dependency.valueOption match {
        case Some(value) =>
          request.calculate(
            value,
            this,
            ResolutionStep(
              interfaceFrom = TrackingPath.interface(request.interface),
              interfaceTo = TrackingPath.interface(target),
              kind = kind,
              resolver = resolver,
              resolverPath = resolverPath
            ) +: dependency.resolutionSteps
          )
          ResolveResult.Success()
        case None =>
          dependency.state match {
            case PropertyState.Incomplete => request.incomplete()
            case PropertyState.Undefined  => request.undefined()
            case state =>
              request.failure(
                s"Converted property '${request.qualifiedName}' has unexpected state $state"
              )
          }
      }
  }
}

/** Converts an AXI4-Full slave interface into an AXI4-Lite master interface.
  *
  * The Full interface must have `wId == 0`; this converter does not instantiate `IdSerialize`.
  * Input bursts are always decomposed before width conversion. `Upscale` steers read data and shifts
  * write data and strobes into the addressed wider lanes. `Downscale` can create a new burst, so its
  * output is unbursted again. Input transfers must be full-width and naturally aligned; simulation
  * checks are controlled by [[LiteConverterConfig.simCheckNarrow]] and
  * [[LiteConverterConfig.simCheckAligned]].
  */
class LiteConverter(val cfg: LiteConverterConfig)
    extends Module
    with chext.AnnotatedModule {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axil = IO(axi4.lite.Master(axiMasterCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)
  declareAxi4Interface(m_axil)

  private val propertyResolver = new LiteConverterResolver(this, s_axi, m_axil)

  private val fullSizeSlave = log2Ceil(axiSlaveCfg.wData / 8)
  private val fullSizeMaster = log2Ceil(wDataMaster / 8)
  private val maxBurstBeats = if (axiSlaveCfg.axi3Compat) 16 else 256
  private val downscaleRatio = axiSlaveCfg.wData / wDataMaster

  private def cappedProduct(left: Int, right: Int): Int =
    (BigInt(left) * right min BigInt(Int.MaxValue)).toInt

  private def enforceMasterProperties(): Unit = {
    if (axiMasterCfg.read) {
      val outstanding =
        if (useDownscale) cappedProduct(numOutstandingRead, downscaleRatio)
        else if (useUpscale) numOutstandingRead
        else cappedProduct(numOutstandingRead, maxBurstBeats)
      m_axil.masterProps(Master.ReadOutstandingTransactions) = outstanding
      m_axil.masterProps(Master.ReadThreads) = 1
      m_axil.masterProps(Master.ReadBurstBeats) = 1
      m_axil.masterProps(Master.ReadBurstNarrow) = false
      m_axil.masterProps(Master.ReadBurstTypes) = Set(1)
      m_axil.masterProps(Master.ReadBurstSizes) = Set(fullSizeMaster)
    }
    if (axiMasterCfg.write) {
      val outstanding =
        if (useDownscale) cappedProduct(numOutstandingWrite, downscaleRatio)
        else if (useUpscale) numOutstandingWrite
        else cappedProduct(numOutstandingWrite, maxBurstBeats)
      m_axil.masterProps(Master.WriteOutstandingTransactions) = outstanding
      m_axil.masterProps(Master.WriteThreads) = 1
      m_axil.masterProps(Master.WriteBurstBeats) = 1
      m_axil.masterProps(Master.WriteBurstNarrow) = false
      m_axil.masterProps(Master.WriteBurstTypes) = Set(1)
      m_axil.masterProps(Master.WriteBurstSizes) = Set(fullSizeMaster)
    }
  }

  private def enforceSlaveProperties(): Unit = {
    // The input Unburst stage supports every AXI burst encoding.
    val acceptedBurstTypes = Set(0, 1, 2)

    if (axiSlaveCfg.read) {
      s_axi.slaveProps(Slave.ReadOutstandingTransactions) = numOutstandingRead
      s_axi.slaveProps(Slave.ReadThreads) = 1
      s_axi.slaveProps(Slave.ReadBurstBeats) = maxBurstBeats
      s_axi.slaveProps(Slave.ReadBurstNarrow) = false
      s_axi.slaveProps(Slave.ReadBurstTypes) = acceptedBurstTypes
      s_axi.slaveProps(Slave.ReadBurstSizes) = Set(fullSizeSlave)
    }
    if (axiSlaveCfg.write) {
      s_axi.slaveProps(Slave.WriteOutstandingTransactions) = numOutstandingWrite
      s_axi.slaveProps(Slave.WriteThreads) = 1
      s_axi.slaveProps(Slave.WriteBurstBeats) = maxBurstBeats
      s_axi.slaveProps(Slave.WriteBurstNarrow) = false
      s_axi.slaveProps(Slave.WriteBurstTypes) = acceptedBurstTypes
      s_axi.slaveProps(Slave.WriteBurstSizes) = Set(fullSizeSlave)
    }
  }

  enforceMasterProperties()
  enforceSlaveProperties()

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
