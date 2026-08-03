package chext.amba.axi4.tracking

import chisel3.experimental.BaseModule

import java.io.{ByteArrayOutputStream, PrintStream}

import chext.amba.axi4
import chext.amba.axi4.BurstType.Encoding.{FIXED, INCR, WRAP}
import chext.amba.axi4.tracking.{properties => p, values => v}

object Properties_Test extends App {
  private final class MissingCase_Resolver extends Resolver(null.asInstanceOf[BaseModule]) {
    def resolve[T](request: ResolveRequest[T]): ResolveResult =
      request.missingCase()
  }

  private val cfg = axi4.Config(wId = 4, wAddr = 16, wData = 64)
  private val fullSize = v.BurstShape.fullSize(cfg.wData)
  private val allTypes = Seq(FIXED, INCR, WRAP)
  private val allSizes = v.BurstShape.supportedSizesFor(cfg)
  private val shape = v.BurstShape(
    maxBeats = 16,
    types = allTypes,
    sizes = allSizes,
    aligned = true
  )
  assert(FIXED == 0 && INCR == 1 && WRAP == 2)
  assert(v.BurstShape.supportedTypesFor(cfg) == allTypes)
  assert(v.BurstShape.supportedSizesFor(cfg) == 0.to(fullSize))
  assert(v.BurstShape.maxBeatsFor(cfg) == 256)
  assert(
    v.BurstShape.all(cfg) ==
      v.BurstShape(256, allTypes, 0.to(fullSize), aligned = false)
  )
  private val axi3Cfg = cfg.copy(axi3Compat = true)
  assert(v.BurstShape.maxBeatsFor(axi3Cfg) == 16)
  private val helperLiteCfg = cfg.copy(wId = 0, lite = true)
  assert(v.BurstShape.supportedTypesFor(helperLiteCfg) == Seq(INCR))
  assert(v.BurstShape.supportedSizesFor(helperLiteCfg) == Seq(fullSize))
  assert(v.BurstShape.maxBeatsFor(helperLiteCfg) == 1)
  assert(
    v.BurstShape.all(helperLiteCfg) ==
      v.BurstShape(1, Seq(INCR), Seq(fullSize), aligned = false)
  )
  assert(
    v.BurstShape(
      maxBeats = 1,
      types = Seq(WRAP, INCR, INCR),
      sizes = Seq(2, 0, 2),
      aligned = false
    ) == v.BurstShape(
      maxBeats = 1,
      types = Seq(INCR, WRAP),
      sizes = Seq(0, 2),
      aligned = false
    )
  )
  assert(v.BurstShape.normalize(shape) == shape)
  assert(
    v.BurstShape.intersect(
      Seq(
        v.BurstShape(16, Seq(INCR, WRAP), Seq(0, 1, 2, 3), false),
        v.BurstShape(8, Seq(INCR), Seq(1, 2, 3), true)
      )
    ) == v.BurstShape(8, Seq(INCR), Seq(1, 2, 3), true)
  )
  assert(
    v.BurstShape.intersect(
      Seq(
        v.BurstShape(16, Seq(INCR), Seq(0), false),
        v.BurstShape(16, Seq(WRAP), Seq(0), false)
      )
    ) == v.BurstShape()
  )
  assert(
    v.ThreadMode.intersect(
      Seq(v.ThreadMode.SingleThread, v.ThreadMode.UniqueThreads)
    ) == v.ThreadMode.SingleTransaction
  )
  assert(
    v.ThreadMode.intersect(
      Seq(v.ThreadMode.Unconstrained, v.ThreadMode.UniqueThreads)
    ) == v.ThreadMode.UniqueThreads
  )
  assert(
    Seq(
      v.ThreadMode.SingleTransaction,
      v.ThreadMode.SingleThread,
      v.ThreadMode.UniqueThreads,
      v.ThreadMode.Unconstrained
    ).map(v.ThreadMode.idlessForward) == Seq(
      v.ThreadMode.SingleTransaction,
      v.ThreadMode.SingleThread,
      v.ThreadMode.SingleThread,
      v.ThreadMode.SingleThread
    )
  )
  assert(
    Seq(
      v.ThreadMode.SingleTransaction,
      v.ThreadMode.SingleThread,
      v.ThreadMode.UniqueThreads,
      v.ThreadMode.Unconstrained
    ).map(v.ThreadMode.idlessBackward) == Seq(
      v.ThreadMode.SingleTransaction,
      v.ThreadMode.Unconstrained,
      v.ThreadMode.SingleTransaction,
      v.ThreadMode.Unconstrained
    )
  )

  // v.MemoryMap remains the original immutable type through the values namespace.
  val map: v.MemoryMap = v.MemoryMap(
    size = 0x400,
    children = Seq(
      v.MemoryMap(
        path = Seq("peripheral"),
        offset = 0x100,
        size = 0x100,
        segments = Seq(v.MemoryMap.Segment(Seq("control"), 0x10, 0x20))
      )
    )
  )
  assert(map.validate == v.MemoryMap.ValidationResult.Success)
  assert(map.flatten.segments.head.path == Seq("peripheral", "control"))

  // Roles, accesses, and value types are ordinary selectors.
  assert(p.Role.Master == p.Master)
  assert(p.Role.Slave == p.Slave)
  assert(p.Access.Read == p.Read)
  assert(p.Access.Write == p.Write)
  assert(p.Access.None == p.NoAccess)
  assert(p.MasterReadBurstShape.valueType == p.BurstShape)
  assert(p.SlaveWriteBurstShape.valueType == p.BurstShape)
  assert(p.MasterReadThreadMode.valueType == p.ThreadMode)
  assert(p.SlaveReadTrafficProfile.valueType == p.TrafficProfile)
  assert(p.SlaveMemoryMap.valueType == p.MemoryMap)

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
    ResolveRequest(new Tracked { val cfg = Properties_Test.cfg }, p.MasterReadBurstShape)
  assert(
    missingCaseResolver.resolve(missingCaseRequest) ==
      ResolveResult.Failure(
        "Resolver 'MissingCaseResolver' (kind 'missing-case') has no case for " +
          "'master.read_burstShape'",
        missingCaseRequest
      )
  )

  assert(p.KnownKeys.count(_.role == p.Master) == 6)
  assert(p.KnownKeys.count(_.role == p.Slave) == 7)
  assert(p.KnownKeys.count(_.access == p.Read) == 6)
  assert(p.KnownKeys.count(_.access == p.Write) == 6)
  assert(p.KnownKeys.count(_.access == p.NoAccess) == 1)

  // Aggregate property values are immutable and are enforced as complete values.
  val properties = new p.Manager
  val burstProperty = properties(p.MasterReadBurstShape)
  assert(properties.select(p.Master).contains(burstProperty))
  assert(properties.select(p.Read).contains(burstProperty))
  assert(properties.select(p.BurstShape).contains(burstProperty))
  assert(properties.select(p.MemoryMap).map(_.key) == Seq(p.SlaveMemoryMap))
  assert(
    burstProperty match {
      case original @ p.Key(p.Master, p.Read, p.BurstShape) =>
        original eq burstProperty
      case _ => false
    }
  )
  assert(
    burstProperty match {
      case p.Key(_, _, p.BurstShape | p.ThreadMode) => true
      case _                                        => false
    }
  )
  val adjustedShape = v.BurstShape(
    maxBeats = 8,
    types = Seq(INCR, WRAP),
    sizes = allSizes,
    aligned = true
  )
  burstProperty.enforce(adjustedShape)
  assert(burstProperty.get == adjustedShape)

  val trafficProperty = properties(p.MasterReadTrafficProfile)
  trafficProperty.enforce(v.TrafficProfile(4, 2, Some(3.5)))
  assert(trafficProperty.get == v.TrafficProfile(4, 2, Some(3.5)))
  assert(v.TrafficProfile.validationErrors(trafficProperty.get).isEmpty)

  val dummyResolver =
    new Resolver(null.asInstanceOf[BaseModule]) {
      def resolve[T](request: ResolveRequest[T]): ResolveResult =
        request.failure("not used")
    }
  private def ops[T](request: ResolveRequest[T]): dummyResolver.RequestOps[T] =
    new dummyResolver.RequestOps(request)

  val rejectedCalculationTracked =
    new Tracked { val cfg = Properties_Test.cfg }
  rejectedCalculationTracked.properties(p.MasterReadBurstShape) = shape
  val rejectedCalculationRequest =
    ResolveRequest(rejectedCalculationTracked, p.MasterReadBurstShape)
  assert(
    ops(rejectedCalculationRequest).calculate(shape) ==
      ResolveResult.Failure(
        "Resolver 'Resolver' (kind 'resolver') could not calculate " +
          "'master.read_burstShape': AlreadyEnforced",
        rejectedCalculationRequest
      )
  )

  val mismatchedCalculationRequest =
    ResolveRequest(
      new Tracked { val cfg = Properties_Test.cfg },
      p.MasterReadBurstShape
    )
  val mismatchedCalculationMessage =
    try {
      ops(mismatchedCalculationRequest).calculate(v.ThreadMode.SingleThread)
      throw new AssertionError("mismatched calculation value type was accepted")
    } catch {
      case exception: IllegalArgumentException =>
        exception.getMessage
    }
  assert(
    mismatchedCalculationMessage.contains(
      "ResolveRequest targets 'master.read_burstShape' with value type 'burstShape'"
    )
  )

  val calculated = properties(p.MasterWriteBurstShape)
  val calculatedStep =
    ResolutionStep("/calculated", "/source", "test", "TestResolver", "/")
  assert(
    calculated.calculate(shape, dummyResolver, Seq(calculatedStep)) ==
      p.CalculateResult.Success
  )
  assert(calculated.state == p.State.Calculated(shape, dummyResolver))
  calculated.enforce(shape.copy(maxBeats = 12))
  assert(calculated.state == p.State.Enforced(shape.copy(maxBeats = 12)))
  assert(calculated.resolutionSteps.isEmpty)

  val terminalTracked = new Tracked { val cfg = Properties_Test.cfg }
  val terminalRequest = ResolveRequest(terminalTracked, p.SlaveReadBurstShape)
  ops(terminalRequest).incomplete()
  terminalTracked.properties(p.SlaveReadBurstShape).enforce(v.BurstShape(maxBeats = 1))
  assert(
    terminalTracked.properties(p.SlaveReadBurstShape).state ==
      p.State.Enforced(v.BurstShape(maxBeats = 1))
  )

  val dontCareTracked = new Tracked { val cfg = Properties_Test.cfg }
  ops(ResolveRequest(dontCareTracked, p.SlaveReadBurstShape)).dontCare("test")
  dontCareTracked
    .properties(p.SlaveReadBurstShape)
    .enforce(
      v.BurstShape(types = Seq(INCR))
    )
  assert(
    dontCareTracked.properties(p.SlaveReadBurstShape).state ==
      p.State.Enforced(v.BurstShape(types = Seq(INCR)))
  )

  val undefinedTracked = new Tracked { val cfg = Properties_Test.cfg }
  ops(ResolveRequest(undefinedTracked, p.SlaveReadBurstShape)).undefined()
  undefinedTracked
    .properties(p.SlaveReadBurstShape)
    .enforce(
      v.BurstShape(sizes = Seq(fullSize))
    )
  assert(
    undefinedTracked.properties(p.SlaveReadBurstShape).state ==
      p.State.Enforced(v.BurstShape(sizes = Seq(fullSize)))
  )

  val authoritative = properties(p.SlaveReadBurstShape).enforce(shape)
  authoritative.enforce(shape)
  val conflictingReenforcementRejected =
    try {
      authoritative.enforce(v.BurstShape(8, allTypes, allSizes, true))
      false
    } catch {
      case _: IllegalStateException => true
    }
  assert(conflictingReenforcementRejected)

  // Configuration and compatibility checks return every applicable problem.
  assert(v.CheckResult.from(Seq.empty) == v.CheckResult.Success)
  assert(
    v.CheckResult.from(Seq("first", "second")).errors == Seq("first", "second")
  )
  assert(!v.CheckResult.from(Seq("problem")).isSuccess)
  assert(v.BurstShape.checkConfig(v.BurstShape(), cfg).isSuccess)
  private val liteCfg = cfg.copy(wId = 0, wData = 32, lite = true)
  assert(
    v.BurstShape
      .checkConfig(
        v.BurstShape(
          1,
          Seq(INCR),
          Seq(v.BurstShape.fullSize(liteCfg.wData)),
          false
        ),
        liteCfg
      )
      .isSuccess
  )
  assert(
    v.BurstShape
      .checkConfig(v.BurstShape(2, Seq(FIXED), Seq(0), false), liteCfg)
      .errors
      .length == 3
  )
  val invalidShape = v.BurstShape(
    maxBeats = 300,
    types = Seq(3),
    sizes = Seq(fullSize + 1),
    aligned = false
  )
  assert(v.BurstShape.checkConfig(invalidShape, cfg).errors.length == 3)

  val masterMismatch = v.BurstShape(
    maxBeats = 32,
    types = Seq(FIXED, INCR, WRAP),
    sizes = Seq(0, 1, 2, 3),
    aligned = false
  )
  val slaveMismatch = v.BurstShape(
    maxBeats = 8,
    types = Seq(INCR),
    sizes = Seq(3),
    aligned = true
  )
  assert(v.BurstShape.checkCompatible(masterMismatch, slaveMismatch).errors.length == 4)
  assert(
    v.BurstShape
      .checkCompatible(
        shape.copy(aligned = true),
        shape.copy(aligned = false)
      )
      .isSuccess
  )
  assert(
    v.BurstShape
      .checkCompatible(v.BurstShape(), shape.copy(aligned = true))
      .isSuccess
  )

  val modes = Seq(
    v.ThreadMode.SingleTransaction,
    v.ThreadMode.SingleThread,
    v.ThreadMode.UniqueThreads,
    v.ThreadMode.Unconstrained
  )
  assert(v.ThreadMode.values == modes)
  assert(v.ThreadMode.supportedModesFor(cfg) == modes)
  assert(
    v.ThreadMode.supportedModesFor(liteCfg) ==
      Seq(v.ThreadMode.SingleTransaction, v.ThreadMode.SingleThread)
  )
  assert(v.ThreadMode.all(cfg) == v.ThreadMode.Unconstrained)
  assert(v.ThreadMode.all(liteCfg) == v.ThreadMode.SingleThread)
  assert(modes.forall(mode => v.ThreadMode.normalize(mode) == mode))
  val compatibleModes = Map[v.ThreadMode, Set[v.ThreadMode]](
    v.ThreadMode.SingleTransaction -> modes.toSet,
    v.ThreadMode.SingleThread -> Set(v.ThreadMode.SingleThread, v.ThreadMode.Unconstrained),
    v.ThreadMode.UniqueThreads -> Set(v.ThreadMode.UniqueThreads, v.ThreadMode.Unconstrained),
    v.ThreadMode.Unconstrained -> Set(v.ThreadMode.Unconstrained)
  )
  modes.foreach { master =>
    modes.foreach { slave =>
      assert(
        v.ThreadMode.checkCompatible(master, slave).isSuccess ==
          compatibleModes(master).contains(slave),
        s"$master -> $slave"
      )
    }
  }
  assert(
    v.ThreadMode.checkConfig(v.ThreadMode.SingleTransaction, liteCfg).isSuccess
  )
  assert(v.ThreadMode.checkConfig(v.ThreadMode.SingleThread, liteCfg).isSuccess)
  assert(
    !v.ThreadMode.checkConfig(v.ThreadMode.UniqueThreads, liteCfg).isSuccess
  )
  assert(
    !v.ThreadMode.checkConfig(v.ThreadMode.Unconstrained, liteCfg).isSuccess
  )
  assert(
    modes.forall(v.ThreadMode.checkConfig(_, cfg).isSuccess)
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
        case ResolveRequest(_, p.Key(_, _, _)) => request.incomplete()
      }
  }
  assert(
    p.KnownKeys
      .filter(key => key.role == p.Master && key.access == p.Read)
      .forall(
        writeOnly.properties(_).state == p.State.Undefined
      )
  )
  assert(
    p.KnownKeys
      .filter(key => key.role == p.Slave && key.access == p.Read)
      .forall(
        writeOnly.properties(_).state == p.State.Undefined
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
      request.missingCase()
  }
  assert(
    Seq(p.MasterReadBurstShape, p.MasterWriteBurstShape).forall(
      liteBound.properties(_).state == p.State.Undefined
    )
  )
  assert(
    Seq(p.SlaveReadBurstShape, p.SlaveWriteBurstShape).forall(
      liteBound.properties(_).state == p.State.Undefined
    )
  )
  liteBound.properties(p.MasterReadThreadMode) = v.ThreadMode.SingleTransaction
  liteBound.properties(p.MasterWriteThreadMode) = v.ThreadMode.SingleTransaction
  liteBound.properties(p.SlaveReadThreadMode) = v.ThreadMode.SingleThread
  liteBound.properties(p.SlaveWriteThreadMode) = v.ThreadMode.SingleThread
  liteBound.properties.markUndefined(p.SlaveMemoryMap)
  Checker.check(Seq(liteBound))

  // ResolveRequest directly exposes the matching member of a sequence binding.
  val interfaceAware = Seq.fill(2)(new Tracked { val cfg = Properties_Test.cfg })
  var extractedSlaveInterface = Option.empty[Tracked]
  var extractedMasterInterface = Option.empty[Tracked]
  val interfaceAwareResolver =
    new Resolver(null.asInstanceOf[BaseModule]) {
      bindSlave(interfaceAware)
      bindMaster(interfaceAware)

      def resolve[T](request: ResolveRequest[T]): ResolveResult =
        request match {
          case ResolveRequest(
                interface,
                p.Key(p.Slave, p.NoAccess, p.MemoryMap)
              ) =>
            extractedSlaveInterface = Some(interface)
            request.incomplete()
          case ResolveRequest(
                interface,
                p.Key(p.Master, p.Read, p.BurstShape)
              ) =>
            extractedMasterInterface = Some(interface)
            request.incomplete()
          case _ => request.failure("unexpected interface-aware request")
        }
    }

  assert(
    interfaceAwareResolver.resolve(
      ResolveRequest(interfaceAware(1), p.SlaveMemoryMap)
    ) == ResolveResult.Success()
  )
  assert(extractedSlaveInterface.exists(_ eq interfaceAware(1)))
  assert(
    interfaceAwareResolver.resolve(
      ResolveRequest(interfaceAware.head, p.MasterReadBurstShape)
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
          visited += interface -> request.cell.key.qualifiedName
          request match {
            case ResolveRequest(
                  _,
                  p.Key(p.Master, p.Read, p.BurstShape)
                ) =>
              request.calculate(p.MasterReadBurstShape, shape)
            case ResolveRequest(
                  _,
                  p.Key(p.Master, p.Write, p.BurstShape)
                ) =>
              request.calculate(p.MasterWriteBurstShape, shape)
            case ResolveRequest(
                  _,
                  p.Key(p.Slave, p.Read, p.BurstShape)
                ) =>
              request.calculate(p.SlaveReadBurstShape, shape)
            case ResolveRequest(
                  _,
                  p.Key(p.Slave, p.Write, p.BurstShape)
                ) =>
              request.calculate(p.SlaveWriteBurstShape, shape)
            case ResolveRequest(
                  _,
                  p.Key(p.Master, p.Read, p.ThreadMode)
                ) =>
              request.calculate(
                p.MasterReadThreadMode,
                v.ThreadMode.SingleTransaction
              )
            case ResolveRequest(
                  _,
                  p.Key(p.Master, p.Write, p.ThreadMode)
                ) =>
              request.calculate(
                p.MasterWriteThreadMode,
                v.ThreadMode.SingleTransaction
              )
            case ResolveRequest(
                  _,
                  p.Key(p.Slave, p.Read, p.ThreadMode)
                ) =>
              request.calculate(
                p.SlaveReadThreadMode,
                v.ThreadMode.Unconstrained
              )
            case ResolveRequest(
                  _,
                  p.Key(p.Slave, p.Write, p.ThreadMode)
                ) =>
              request.calculate(
                p.SlaveWriteThreadMode,
                v.ThreadMode.Unconstrained
              )
            case ResolveRequest(
                  _,
                  p.Key(p.Slave, p.NoAccess, p.MemoryMap)
                ) =>
              request.calculate(
                p.SlaveMemoryMap,
                v.MemoryMap(size = 0x100)
              )
            case _ => request.failure("unexpected checked property")
          }
        }
      }
  }
  Checker.check(interfaces)
  assert(visited.groupBy(_._1).values.forall(_.length == 9))

  // Resolution diagnostics distinguish missing, incomplete, undefined, and incompatibility,
  // and failed comparisons print provenance from both sides.
  def capturedDiagnostics(body: => Unit): String = {
    val bytes = new ByteArrayOutputStream()
    val stream = new PrintStream(bytes)
    Console.withOut(stream)(body)
    stream.flush()
    bytes.toString("UTF-8")
  }

  val missing = new Tracked { val cfg = Properties_Test.cfg }
  val missingLog = capturedDiagnostics(Checker.check(Seq(missing)))
  assert(missingLog.contains("axi4.tracking : error:"))
  assert(!missingLog.contains("elastic.tracking"))
  assert(missingLog.contains("no resolver is registered"))

  val invalidLiteModes = new Tracked {
    val cfg = Properties_Test.liteCfg
  }
  new Resolver(null.asInstanceOf[BaseModule]) {
    bindMaster(invalidLiteModes)
    bindSlave(invalidLiteModes)
    def resolve[T](request: ResolveRequest[T]): ResolveResult =
      request.missingCase()
  }
  invalidLiteModes.properties(p.MasterReadThreadMode) = v.ThreadMode.UniqueThreads
  invalidLiteModes.properties(p.SlaveReadThreadMode) = v.ThreadMode.SingleThread
  invalidLiteModes.properties(p.MasterWriteThreadMode) = v.ThreadMode.SingleTransaction
  invalidLiteModes.properties(p.SlaveWriteThreadMode) = v.ThreadMode.Unconstrained
  invalidLiteModes.properties.markUndefined(p.SlaveMemoryMap)
  val invalidLiteLog =
    capturedDiagnostics(Checker.check(Seq(invalidLiteModes)))
  assert(
    invalidLiteLog.contains("Property: master.read_threadMode")
  )
  assert(
    invalidLiteLog.contains("error: AXI4-Lite thread mode UniqueThreads is invalid")
  )
  assert(
    invalidLiteLog.contains("Property: slave.write_threadMode")
  )
  assert(
    invalidLiteLog.contains("error: AXI4-Lite thread mode Unconstrained is invalid")
  )

  val invalidResolvedLiteMode = new Tracked {
    val cfg = Properties_Test.liteCfg.copy(write = false)
  }
  new Resolver(null.asInstanceOf[BaseModule]) {
    bindMaster(invalidResolvedLiteMode)
    bindSlave(invalidResolvedLiteMode)

    def resolve[T](request: ResolveRequest[T]): ResolveResult =
      request match {
        case ResolveRequest(_, p.Key(p.Master, _, p.ThreadMode)) =>
          request.calculate(v.ThreadMode.UniqueThreads)
        case ResolveRequest(_, p.Key(p.Slave, _, p.ThreadMode)) =>
          request.calculate(v.ThreadMode.SingleThread)
        case _ =>
          request.missingCase()
      }
  }
  invalidResolvedLiteMode.properties.markUndefined(p.SlaveMemoryMap)
  val invalidResolvedLiteLog =
    capturedDiagnostics(Checker.check(Seq(invalidResolvedLiteMode)))
  assert(!invalidResolvedLiteLog.contains("master.read_threadMode"))
  assert(!invalidResolvedLiteLog.contains("UniqueThreads is invalid"))

  val stateDiagnostics = new Tracked { val cfg = Properties_Test.cfg }
  stateDiagnostics.properties(p.MasterReadBurstShape).enforce(shape)
  stateDiagnostics.properties(p.SlaveReadBurstShape).markUndefined()
  stateDiagnostics.properties(p.MasterReadThreadMode).enforce(v.ThreadMode.SingleTransaction)
  stateDiagnostics.properties(p.SlaveReadThreadMode).enforce(v.ThreadMode.Unconstrained)
  stateDiagnostics.properties(p.MasterWriteBurstShape).enforce(shape)
  ops(ResolveRequest(stateDiagnostics, p.SlaveWriteBurstShape)).incomplete()
  stateDiagnostics.properties(p.MasterWriteThreadMode).enforce(v.ThreadMode.SingleTransaction)
  stateDiagnostics.properties(p.SlaveWriteThreadMode).enforce(v.ThreadMode.Unconstrained)
  stateDiagnostics.properties(p.SlaveMemoryMap).markUndefined()
  val stateLog = capturedDiagnostics(Checker.check(Seq(stateDiagnostics)))
  assert(stateLog.contains("property is undefined"))
  assert(stateLog.contains("property resolution is incomplete"))

  val naturalAlignmentMismatch = new Tracked {
    val cfg = Properties_Test.cfg.copy(write = false)
  }
  naturalAlignmentMismatch
    .properties(p.MasterReadBurstShape)
    .enforce(shape.copy(aligned = false))
  naturalAlignmentMismatch
    .properties(p.SlaveReadBurstShape)
    .enforce(shape.copy(aligned = true))
  naturalAlignmentMismatch
    .properties(p.MasterReadThreadMode)
    .enforce(v.ThreadMode.SingleTransaction)
  naturalAlignmentMismatch
    .properties(p.SlaveReadThreadMode)
    .enforce(v.ThreadMode.Unconstrained)
  naturalAlignmentMismatch.properties(p.SlaveMemoryMap).markUndefined()
  val alignmentLog =
    capturedDiagnostics(Checker.check(Seq(naturalAlignmentMismatch)))
  assert(
    alignmentLog.contains(
      "master may issue unaligned transactions, but slave requires natural alignment"
    )
  )

  val traced = new Tracked { val cfg = Properties_Test.cfg.copy(write = false) }
  val masterTrace = ResolutionStep("/checked", "/master", "test", "MasterResolver", "/master")
  val slaveTrace = ResolutionStep("/checked", "/slave", "test", "SlaveResolver", "/slave")
  traced
    .properties(p.MasterReadBurstShape)
    .calculate(masterMismatch, dummyResolver, Seq(masterTrace))
  traced
    .properties(p.SlaveReadBurstShape)
    .enforce(slaveMismatch)
  traced
    .properties(p.MasterReadThreadMode)
    .enforce(v.ThreadMode.SingleTransaction)
  traced.properties(p.SlaveReadThreadMode).enforce(v.ThreadMode.Unconstrained)
  traced.properties(p.SlaveMemoryMap).markUndefined()
  val traceLog = capturedDiagnostics(Checker.check(Seq(traced)))
  assert(traceLog.contains("Master trace"))
  assert(traceLog.contains("Slave trace"))
  assert(traceLog.contains("MasterResolver"))
  assert(!traceLog.contains("SlaveResolver"))
  assert(traceLog.contains("Owner: (property was enforced outside a Resolver)"))

  val bothInferred = new Tracked {
    val cfg = Properties_Test.cfg.copy(write = false)
  }
  bothInferred
    .properties(p.MasterReadBurstShape)
    .calculate(masterMismatch, dummyResolver, Seq(masterTrace))
  bothInferred
    .properties(p.SlaveReadBurstShape)
    .calculate(slaveMismatch, dummyResolver, Seq(slaveTrace))
  bothInferred
    .properties(p.MasterReadThreadMode)
    .enforce(v.ThreadMode.SingleTransaction)
  bothInferred.properties(p.SlaveReadThreadMode).enforce(v.ThreadMode.Unconstrained)
  bothInferred.properties(p.SlaveMemoryMap).markUndefined()
  val bothInferredLog =
    capturedDiagnostics(Checker.check(Seq(bothInferred)))
  assert(!bothInferredLog.contains("burst shapes are incompatible"))

}
