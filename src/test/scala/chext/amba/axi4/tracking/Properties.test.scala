package chext.amba.axi4.tracking

import chisel3.experimental.BaseModule

import java.io.{ByteArrayOutputStream, PrintStream}

import chext.amba.axi4
import chext.amba.axi4.tracking.properties.{Master, Slave}
import chext.amba.axi4.tracking.values.{
  BurstShape,
  MemoryMap,
  ThreadMode,
  TrafficProfile
}

object Properties_Test extends App {
  private final class MissingCase_Resolver
      extends Resolver(null.asInstanceOf[BaseModule]) {
    def resolve[T](request: ResolveRequest[T]): ResolveResult =
      missingCase(request)
  }

  private val cfg = axi4.Config(wId = 4, wAddr = 16, wData = 64)
  private val fullSize = BurstShape.fullSize(cfg.wData)
  private val allTypes = Set(0, 1, 2)
  private val allSizes = BurstShape.validSizes(cfg.wData)
  private val shape = BurstShape(
    burstBeats = 16,
    burstNarrow = true,
    burstTypes = allTypes,
    burstSizes = allSizes
  )

  // MemoryMap remains the original immutable type through the values namespace.
  val map: MemoryMap = MemoryMap(
    size = 0x400,
    children = Seq(
      MemoryMap(
        path = Seq("peripheral"),
        offset = 0x100,
        size = 0x100,
        segments = Seq(MemoryMap.Segment(Seq("control"), 0x10, 0x20))
      )
    )
  )
  assert(map.validate == MemoryMap.ValidationResult.Success)
  assert(map.flatten.segments.head.path == Seq("peripheral", "control"))

  // Runtime value-type extractors distinguish aggregate property families.
  val BurstShapeProperty = PropertyValueType[BurstShape]
  val ThreadModeProperty = PropertyValueType[ThreadMode]
  val TrafficProfileProperty = PropertyValueType[TrafficProfile]
  val MemoryMapProperty = PropertyValueType[MemoryMap]
  assert(BurstShapeProperty.unapply(Master.ReadBurstShape))
  assert(BurstShapeProperty.unapply(Slave.WriteBurstShape))
  assert(!BurstShapeProperty.accepts(Master.ReadThreadMode))
  assert(ThreadModeProperty.unapply(Master.WriteThreadMode))
  assert(TrafficProfileProperty.unapply(Slave.ReadTrafficProfile))
  assert(MemoryMapProperty.unapply(Slave.MemoryMap))

  // Resolver trace metadata follows the conventional implementation class name.
  assert(Resolver.defaultResolverName("Buffer_Resolver") == "BufferResolver")
  assert(
    Resolver.defaultResolverName("CreditBuffer_Resolver") ==
      "CreditBufferResolver"
  )
  assert(Resolver.defaultResolverName("DemuxMm_Resolver") == "DemuxMmResolver")
  assert(Resolver.defaultResolverName("") == "Resolver")
  assert(Resolver.defaultKind("CreditBufferResolver") == "credit-buffer")
  assert(Resolver.defaultKind("DemuxMmResolver") == "demux-mm")
  assert(Resolver.defaultKind("URLParserResolver") == "url-parser")
  assert(Resolver.defaultKind("Resolver") == "resolver")

  private val missingCaseResolver = new MissingCase_Resolver
  private val missingCaseRequest =
    ResolveRequest(new Tracked { val cfg = Properties_Test.cfg }, Master.ReadBurstShape)
  assert(
    missingCaseResolver.resolve(missingCaseRequest) ==
      ResolveResult.Failure(
        "Resolver 'MissingCaseResolver' (kind 'missing-case') has no case for " +
          "'master.read_burstShape'",
        missingCaseRequest
      )
  )

  assert(Master.size == 6)
  assert(Slave.size == 7)
  assert(Master.readProperties == Master.all.collect {
    case key @ ReadProperty() => key
  }.toSet)
  assert(Master.writeProperties == Master.all.collect {
    case key @ WriteProperty() => key
  }.toSet)
  assert(Slave.readProperties == Slave.all.collect {
    case key @ ReadProperty() => key
  }.toSet)
  assert(Slave.writeProperties == Slave.all.collect {
    case key @ WriteProperty() => key
  }.toSet)

  // Field updates own the lifecycle transition and never expose public mutable fields.
  val properties = new PropertyManager
  val burstProperty = properties(Master.ReadBurstShape)
  burstProperty.burstBeats = 8
  assert(burstProperty.state == PropertyState.Enforced(BurstShape(burstBeats = 8)))
  burstProperty.burstNarrow = true
  burstProperty.burstTypes = Set(1, 2)
  burstProperty.burstSizes = allSizes
  assert(
    burstProperty.get ==
      BurstShape(8, true, Set(1, 2), allSizes)
  )

  val trafficProperty = properties(Master.ReadTrafficProfile)
  trafficProperty.outstandingTransactions = 4
  trafficProperty.threads = 2
  trafficProperty.latencyCycles = Some(3.5)
  assert(trafficProperty.get == TrafficProfile(4, 2, Some(3.5)))
  assert(TrafficProfile.validationErrors(trafficProperty.get).isEmpty)

  val dummyResolver =
    new Resolver(null.asInstanceOf[BaseModule]) {
      def resolve[T](request: ResolveRequest[T]): ResolveResult =
        request.failure("not used")
    }
  val calculated = properties(Master.WriteBurstShape)
  val calculatedStep =
    ResolutionStep("/calculated", "/source", "test", "TestResolver", "/")
  assert(
    calculated.calculate(shape, dummyResolver, Seq(calculatedStep)) ==
      CalculateResult.Success
  )
  assert(calculated.state == PropertyState.Calculated(shape, dummyResolver))
  calculated.burstBeats = 12
  assert(calculated.state == PropertyState.Enforced(shape.copyForTest(burstBeats = 12)))
  assert(calculated.resolutionSteps.isEmpty)

  val terminalTracked = new Tracked { val cfg = Properties_Test.cfg }
  val terminalRequest = ResolveRequest(terminalTracked, Slave.ReadBurstShape)
  terminalRequest.incomplete()
  terminalTracked.slaveProps(Slave.ReadBurstShape).burstBeats = 1
  assert(
    terminalTracked.slaveProps(Slave.ReadBurstShape).state ==
      PropertyState.Enforced(BurstShape(burstBeats = 1))
  )

  val dontCareTracked = new Tracked { val cfg = Properties_Test.cfg }
  ResolveRequest(dontCareTracked, Slave.ReadBurstShape).dontCare("test")
  dontCareTracked.slaveProps(Slave.ReadBurstShape).burstTypes = Set(1)
  assert(
    dontCareTracked.slaveProps(Slave.ReadBurstShape).state ==
      PropertyState.Enforced(BurstShape(burstTypes = Set(1)))
  )

  val undefinedTracked = new Tracked { val cfg = Properties_Test.cfg }
  ResolveRequest(undefinedTracked, Slave.ReadBurstShape).undefined()
  undefinedTracked.slaveProps(Slave.ReadBurstShape).burstSizes = Set(fullSize)
  assert(
    undefinedTracked.slaveProps(Slave.ReadBurstShape).state ==
      PropertyState.Enforced(BurstShape(burstSizes = Set(fullSize)))
  )

  val authoritative = properties(Slave.ReadBurstShape).enforce(shape)
  authoritative.enforce(shape)
  val conflictingReenforcementRejected =
    try {
      authoritative.enforce(BurstShape(8, true, allTypes, allSizes))
      false
    } catch {
      case _: IllegalStateException => true
    }
  assert(conflictingReenforcementRejected)

  // Validation and compatibility return every applicable problem.
  assert(BurstShape.validationErrors(BurstShape(), cfg).isEmpty)
  private val liteCfg = cfg.copy(wId = 0, wData = 32, lite = true)
  assert(
    BurstShape
      .validationErrors(
        BurstShape(1, false, Set(1), Set(BurstShape.fullSize(liteCfg.wData))),
        liteCfg
      )
      .isEmpty
  )
  assert(
    BurstShape
      .validationErrors(BurstShape(2, false, Set(0), Set(0)), liteCfg)
      .length == 4
  )
  val invalidShape = BurstShape(
    burstBeats = 300,
    burstNarrow = true,
    burstTypes = Set(3),
    burstSizes = Set(fullSize + 1)
  )
  assert(BurstShape.validationErrors(invalidShape, cfg).length == 4)

  val masterMismatch = BurstShape(
    burstBeats = 32,
    burstNarrow = true,
    burstTypes = Set(0, 1, 2),
    burstSizes = Set(0, 1, 2, 3)
  )
  val slaveMismatch = BurstShape(
    burstBeats = 8,
    burstNarrow = false,
    burstTypes = Set(1),
    burstSizes = Set(3)
  )
  assert(BurstShape.compatibilityErrors(masterMismatch, slaveMismatch).length == 4)

  val modes = Seq(
    ThreadMode.SingleTransaction,
    ThreadMode.SingleThread,
    ThreadMode.UniqueThreads,
    ThreadMode.Unconstrained
  )
  val compatibleModes = Map[ThreadMode, Set[ThreadMode]](
    ThreadMode.SingleTransaction -> modes.toSet,
    ThreadMode.SingleThread -> Set(ThreadMode.SingleThread, ThreadMode.Unconstrained),
    ThreadMode.UniqueThreads -> Set(ThreadMode.UniqueThreads, ThreadMode.Unconstrained),
    ThreadMode.Unconstrained -> Set(ThreadMode.Unconstrained)
  )
  modes.foreach { master =>
    modes.foreach { slave =>
      assert(
        ThreadMode.compatible(master, slave) ==
          compatibleModes(master).contains(slave),
        s"$master -> $slave"
      )
    }
  }
  assert(
    ThreadMode.validationErrors(ThreadMode.SingleTransaction, liteCfg).isEmpty
  )
  assert(ThreadMode.validationErrors(ThreadMode.SingleThread, liteCfg).isEmpty)
  assert(
    ThreadMode.validationErrors(ThreadMode.UniqueThreads, liteCfg).nonEmpty
  )
  assert(
    ThreadMode.validationErrors(ThreadMode.Unconstrained, liteCfg).nonEmpty
  )
  assert(
    modes.forall(ThreadMode.validationErrors(_, cfg).isEmpty)
  )

  // Disabled property catalogs are classified automatically by resolver binding.
  val writeOnly = new Tracked {
    val cfg = Properties_Test.cfg.copy(read = false, write = true)
  }
  new Resolver(null.asInstanceOf[BaseModule]) {
    bindMaster(writeOnly)
    bindSlave(writeOnly)
    def resolve[T](request: ResolveRequest[T]): ResolveResult =
      request match {
        case MasterRequests(_) | SlaveRequests(_) => request.incomplete()
        case _                                     => request.failure("wrong endpoint")
      }
  }
  assert(
    Master.readProperties.forall(
      writeOnly.masterProps(_).state == PropertyState.Undefined
    )
  )
  assert(
    Slave.readProperties.forall(
      writeOnly.slaveProps(_).state == PropertyState.Undefined
    )
  )

  // AXI4-Lite has no burst-shape signals; binding classifies those properties as inapplicable.
  val liteBound = new Tracked {
    val cfg = Properties_Test.cfg.copy(wId = 0, lite = true)
  }
  new Resolver(null.asInstanceOf[BaseModule]) {
    bindMaster(liteBound)
    bindSlave(liteBound)
    def resolve[T](request: ResolveRequest[T]): ResolveResult =
      missingCase(request)
  }
  assert(
    Seq(Master.ReadBurstShape, Master.WriteBurstShape).forall(
      liteBound.masterProps(_).state == PropertyState.Undefined
    )
  )
  assert(
    Seq(Slave.ReadBurstShape, Slave.WriteBurstShape).forall(
      liteBound.slaveProps(_).state == PropertyState.Undefined
    )
  )
  liteBound.masterProps(Master.ReadThreadMode) = ThreadMode.SingleTransaction
  liteBound.masterProps(Master.WriteThreadMode) = ThreadMode.SingleTransaction
  liteBound.slaveProps(Slave.ReadThreadMode) = ThreadMode.SingleThread
  liteBound.slaveProps(Slave.WriteThreadMode) = ThreadMode.SingleThread
  liteBound.slaveProps.markUndefined(Slave.MemoryMap)
  CompatibilityChecker.check(Seq(liteBound))

  // Interface-aware family extractors identify the matching member of a sequence binding.
  val interfaceAware = Seq.fill(2)(new Tracked { val cfg = Properties_Test.cfg })
  var extractedSlaveInterface = Option.empty[Tracked]
  var extractedMasterInterface = Option.empty[Tracked]
  val interfaceAwareResolver =
    new Resolver(null.asInstanceOf[BaseModule]) {
      bindSlave(interfaceAware)
      bindMaster(interfaceAware)

      def resolve[T](request: ResolveRequest[T]): ResolveResult =
        request match {
          case SlaveRequests.withInterface(interface, Slave.MemoryMap) =>
            extractedSlaveInterface = Some(interface)
            request.incomplete()
          case MasterRequests.withInterface(interface, Master.ReadBurstShape) =>
            extractedMasterInterface = Some(interface)
            request.incomplete()
          case _ => request.failure("unexpected interface-aware request")
        }
    }

  assert(
    interfaceAwareResolver.resolve(
      ResolveRequest(interfaceAware(1), Slave.MemoryMap)
    ) == ResolveResult.Success()
  )
  assert(extractedSlaveInterface.exists(_ eq interfaceAware(1)))
  assert(
    interfaceAwareResolver.resolve(
      ResolveRequest(interfaceAware.head, Master.ReadBurstShape)
    ) == ResolveResult.Success()
  )
  assert(extractedMasterInterface.exists(_ eq interfaceAware.head))

  // The exhaustive checker visits every interface and every initially checked property.
  val visited = scala.collection.mutable.ArrayBuffer.empty[(Tracked, String)]
  val interfaces = Seq.fill(3)(new Tracked { val cfg = Properties_Test.cfg })
  interfaces.foreach { interface =>
    val resolver =
      new Resolver(null.asInstanceOf[BaseModule]) {
        bindMaster(interface)
        bindSlave(interface)

        def resolve[T](request: ResolveRequest[T]): ResolveResult = {
          visited += interface -> request.qualifiedName
          request match {
            case MasterRequests(Master.ReadBurstShape) =>
              request.calculate(Master.ReadBurstShape, shape, this)
              ResolveResult.Success()
            case MasterRequests(Master.WriteBurstShape) =>
              request.calculate(Master.WriteBurstShape, shape, this)
              ResolveResult.Success()
            case SlaveRequests(Slave.ReadBurstShape) =>
              request.calculate(Slave.ReadBurstShape, shape, this)
              ResolveResult.Success()
            case SlaveRequests(Slave.WriteBurstShape) =>
              request.calculate(Slave.WriteBurstShape, shape, this)
              ResolveResult.Success()
            case MasterRequests(Master.ReadThreadMode) =>
              request.calculate(
                Master.ReadThreadMode,
                values.ThreadMode.SingleTransaction,
                this
              )
              ResolveResult.Success()
            case MasterRequests(Master.WriteThreadMode) =>
              request.calculate(
                Master.WriteThreadMode,
                values.ThreadMode.SingleTransaction,
                this
              )
              ResolveResult.Success()
            case SlaveRequests(Slave.ReadThreadMode) =>
              request.calculate(
                Slave.ReadThreadMode,
                values.ThreadMode.Unconstrained,
                this
              )
              ResolveResult.Success()
            case SlaveRequests(Slave.WriteThreadMode) =>
              request.calculate(
                Slave.WriteThreadMode,
                values.ThreadMode.Unconstrained,
                this
              )
              ResolveResult.Success()
            case SlaveRequests(Slave.MemoryMap) =>
              request.calculate(
                Slave.MemoryMap,
                values.MemoryMap(size = 0x100),
                this
              )
              ResolveResult.Success()
            case _ => request.failure("unexpected checked property")
          }
        }
      }
  }
  CompatibilityChecker.check(interfaces)
  assert(visited.groupBy(_._1).values.forall(_.length == 9))

  // Resolution diagnostics distinguish missing, incomplete, undefined, and incompatibility,
  // and failed comparisons print provenance from both sides.
  def capturedFailure(body: => Unit): String = {
    val bytes = new ByteArrayOutputStream()
    val stream = new PrintStream(bytes)
    val failed =
      try {
        Console.withOut(stream)(body)
        false
      } catch {
        case _: IllegalArgumentException => true
      }
    stream.flush()
    assert(failed)
    bytes.toString("UTF-8")
  }

  val missing = new Tracked { val cfg = Properties_Test.cfg }
  val missingLog = capturedFailure(CompatibilityChecker.check(Seq(missing)))
  assert(missingLog.contains("no resolver is registered"))

  val invalidLiteModes = new Tracked {
    val cfg = Properties_Test.liteCfg
  }
  new Resolver(null.asInstanceOf[BaseModule]) {
    bindMaster(invalidLiteModes)
    bindSlave(invalidLiteModes)
    def resolve[T](request: ResolveRequest[T]): ResolveResult =
      missingCase(request)
  }
  invalidLiteModes.masterProps(Master.ReadThreadMode) = ThreadMode.UniqueThreads
  invalidLiteModes.slaveProps(Slave.ReadThreadMode) = ThreadMode.SingleThread
  invalidLiteModes.masterProps(Master.WriteThreadMode) = ThreadMode.SingleTransaction
  invalidLiteModes.slaveProps(Slave.WriteThreadMode) = ThreadMode.Unconstrained
  invalidLiteModes.slaveProps.markUndefined(Slave.MemoryMap)
  val invalidLiteLog =
    capturedFailure(CompatibilityChecker.check(Seq(invalidLiteModes)))
  assert(
    invalidLiteLog.contains(
      "master.read_threadMode: AXI4-Lite thread mode UniqueThreads is invalid"
    )
  )
  assert(
    invalidLiteLog.contains(
      "slave.write_threadMode: AXI4-Lite thread mode Unconstrained is invalid"
    )
  )

  val invalidResolvedLiteMode = new Tracked {
    val cfg = Properties_Test.liteCfg.copy(write = false)
  }
  new Resolver(null.asInstanceOf[BaseModule]) {
    bindMaster(invalidResolvedLiteMode)
    bindSlave(invalidResolvedLiteMode)

    def resolve[T](request: ResolveRequest[T]): ResolveResult =
      request match {
        case MasterRequests(ThreadModeProperty()) =>
          request.calculate(values.ThreadMode.UniqueThreads, this)
          ResolveResult.Success()
        case SlaveRequests(ThreadModeProperty()) =>
          request.calculate(values.ThreadMode.SingleThread, this)
          ResolveResult.Success()
        case _ =>
          missingCase(request)
      }
  }
  invalidResolvedLiteMode.slaveProps.markUndefined(Slave.MemoryMap)
  val invalidResolvedLiteLog =
    capturedFailure(CompatibilityChecker.check(Seq(invalidResolvedLiteMode)))
  assert(
    invalidResolvedLiteLog.contains(
      "master.read_threadMode: AXI4-Lite thread mode UniqueThreads is invalid"
    )
  )

  val stateDiagnostics = new Tracked { val cfg = Properties_Test.cfg }
  stateDiagnostics.masterProps(Master.ReadBurstShape).enforce(shape)
  stateDiagnostics.slaveProps(Slave.ReadBurstShape).markUndefined()
  stateDiagnostics.masterProps(Master.ReadThreadMode).enforce(ThreadMode.SingleTransaction)
  stateDiagnostics.slaveProps(Slave.ReadThreadMode).enforce(ThreadMode.Unconstrained)
  stateDiagnostics.masterProps(Master.WriteBurstShape).enforce(shape)
  ResolveRequest(stateDiagnostics, Slave.WriteBurstShape).incomplete()
  stateDiagnostics.masterProps(Master.WriteThreadMode).enforce(ThreadMode.SingleTransaction)
  stateDiagnostics.slaveProps(Slave.WriteThreadMode).enforce(ThreadMode.Unconstrained)
  stateDiagnostics.slaveProps(Slave.MemoryMap).markUndefined()
  val stateLog = capturedFailure(CompatibilityChecker.check(Seq(stateDiagnostics)))
  assert(stateLog.contains("property is undefined"))
  assert(stateLog.contains("property resolution is incomplete"))

  val traced = new Tracked { val cfg = Properties_Test.cfg.copy(write = false) }
  val masterTrace = ResolutionStep("/checked", "/master", "test", "MasterResolver", "/master")
  val slaveTrace = ResolutionStep("/checked", "/slave", "test", "SlaveResolver", "/slave")
  traced
    .masterProps(Master.ReadBurstShape)
    .calculate(masterMismatch, dummyResolver, Seq(masterTrace))
  traced
    .slaveProps(Slave.ReadBurstShape)
    .calculate(slaveMismatch, dummyResolver, Seq(slaveTrace))
  traced
    .masterProps(Master.ReadThreadMode)
    .enforce(ThreadMode.SingleTransaction)
  traced.slaveProps(Slave.ReadThreadMode).enforce(ThreadMode.Unconstrained)
  traced.slaveProps(Slave.MemoryMap).markUndefined()
  val traceLog = capturedFailure(CompatibilityChecker.check(Seq(traced)))
  assert(traceLog.contains("master trace"))
  assert(traceLog.contains("slave trace"))
  assert(traceLog.contains("MasterResolver"))
  assert(traceLog.contains("SlaveResolver"))

  implicit final class BurstShapeTestOps(private val value: BurstShape) extends AnyVal {
    def copyForTest(
        burstBeats: Int = value.burstBeats,
        burstNarrow: Boolean = value.burstNarrow,
        burstTypes: Set[Int] = value.burstTypes,
        burstSizes: Set[Int] = value.burstSizes
    ): BurstShape =
      BurstShape(burstBeats, burstNarrow, burstTypes, burstSizes)
  }
}
