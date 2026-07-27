package chext.amba.axi4.tracking

import chisel3.experimental.BaseModule

import chext.amba.axi4.tracking.properties.{Master, Slave}
import chext.amba.axi4.util.MemoryMap
import chext.util.MathUtils

object Properties_Test extends App {
  assert(MathUtils.nextPowerOfTwo(0x100) == 0x100)
  assert(MathUtils.nextPowerOfTwo(0x180) == 0x200)
  assert(MathUtils.alignUp(0x200, 0x400) == 0x400)
  assert(MathUtils.alignUp(0x401, 0x100) == 0x500)

  val memoryMap = MemoryMap(
    offset = 0x1000,
    size = 0x400,
    children = Seq(
      MemoryMap(
        path = Seq("peripheral"),
        offset = 0x200,
        size = 0x100,
        segments = Seq(MemoryMap.Segment(Seq("control"), 0x10, 0x20))
      )
    ),
    segments = Seq(MemoryMap.Segment(Seq("status"), 0x80, 0x10))
  )
  assert(memoryMap.offset == 0x1000)
  assert(memoryMap.segments.head.baseAddress == 0x80)
  assert(memoryMap.children.head.offset == 0x200)
  assert(memoryMap.children.head.segments.head.size == 0x20)

  val hierarchicalMemoryMap = MemoryMap(
    offset = 0x1000,
    size = 0x400,
    segments = Seq(MemoryMap.Segment(Seq("root"), 0x10, 0x10)),
    children = Seq(
      MemoryMap(
        path = Seq("peripheral"),
        offset = 0x200,
        size = 0x200,
        segments = Seq(MemoryMap.Segment(Seq("status"), 0x20, 0x10)),
        children = Seq(
          MemoryMap(
            path = Seq("registers"),
            offset = 0x80,
            size = 0x100,
            segments = Seq(MemoryMap.Segment(Seq("control"), 0x08, 0x10))
          )
        )
      )
    )
  )
  val flattenedOnce = hierarchicalMemoryMap.flattenOnce
  assert(flattenedOnce.offset == 0x1000)
  assert(flattenedOnce.children.map(_.path) == Seq(Seq("peripheral", "registers")))
  assert(flattenedOnce.children.map(_.offset) == Seq(0x280))
  assert(
    flattenedOnce.segments.map(_.path) ==
      Seq(Seq("root"), Seq("peripheral", "status"))
  )
  assert(flattenedOnce.segments.map(_.baseAddress) == Seq(0x10, 0x220))

  val flattened = hierarchicalMemoryMap.flatten
  assert(flattened.children.isEmpty)
  assert(
    flattened.segments.map(segment => segment.path -> segment.baseAddress) == Seq(
      Seq("root") -> BigInt(0x10),
      Seq("peripheral", "status") -> BigInt(0x220),
      Seq("peripheral", "registers", "control") -> BigInt(0x288)
    )
  )
  assert(
    flattened.segments.map(_.renderedPath) ==
      Seq("/root", "/peripheral/status", "/peripheral/registers/control")
  )

  val aggregated = MemoryMap.aggregate(
    Seq(
      MemoryMap(
        path = Seq("first"),
        size = 0x180,
        segments = Seq(MemoryMap.Segment(Seq("memory"), 0, 0x180))
      ),
      MemoryMap(
        path = Seq("second"),
        size = 0x300,
        segments = Seq(MemoryMap.Segment(Seq("memory"), 0, 0x300))
      ),
      MemoryMap(
        path = Seq("third"),
        size = 0x81,
        segments = Seq(MemoryMap.Segment(Seq("memory"), 0, 0x81))
      )
    )
  )
  assert(aggregated.children.map(_.offset) == Seq(0, 0x400, 0x800))
  assert(aggregated.occupiedSize == 0x881)
  assert(aggregated.allocatedSize == 0x900)
  assert(aggregated.alignedSize == 0x1000)
  assert(aggregated.validate == MemoryMap.ValidationResult.Success)

  val alignedLargest = MemoryMap.aggregate(
    aggregated.children.map(_.copy(offset = 0)),
    MemoryMap.AllocationScheme.AlignedLargest
  )
  assert(alignedLargest.children.map(_.offset) == Seq(0, 0x400, 0x800))
  assert(alignedLargest.allocatedSize == 0xc00)

  val tight = MemoryMap.aggregate(
    aggregated.children.map(_.copy(offset = 0)),
    MemoryMap.AllocationScheme.Tight
  )
  assert(tight.children.map(_.offset) == Seq(0, 0x180, 0x480))
  assert(tight.allocatedSize == 0x501)

  val overallocated = MemoryMap(
    path = Seq("overallocated"),
    size = 0x100,
    segments = Seq(MemoryMap.Segment(Seq("memory"), 0x80, 0x81))
  ).validate
  assert(
    overallocated == MemoryMap.ValidationResult.Failure(
      id = MemoryMap.FailureId.Overallocated,
      path = Seq("overallocated", "memory"),
      kind = MemoryMap.EntryKind.Segment,
      offset = 0x80,
      size = 0x81,
      limit = Some(0x100)
    )
  )

  val overlapping = MemoryMap(
    size = 0x400,
    children = Seq(
      MemoryMap(path = Seq("registers"), offset = 0x100, size = 0x100)
    ),
    segments = Seq(MemoryMap.Segment(Seq("memory"), 0x180, 0x100))
  ).validate
  assert(
    overlapping == MemoryMap.ValidationResult.Failure(
      id = MemoryMap.FailureId.Overlap,
      path = Seq("memory"),
      kind = MemoryMap.EntryKind.Segment,
      offset = 0x180,
      size = 0x100,
      conflictingPath = Some(Seq("registers"))
    )
  )

  val properties = new PropertyManager

  properties(Master.ReadOutstandingTransactions) = 8
  properties(Master.ReadBurstBeats) = 16
  properties(Slave.ReadOutstandingTransactions) = 12

  val masterOutstanding = properties(Master.ReadOutstandingTransactions)
  val masterBurstBeats = properties(Master.ReadBurstBeats)
  val slaveOutstanding = properties(Slave.ReadOutstandingTransactions)
  val masterWriteThreads = properties(Master.WriteThreads)

  assert(masterOutstanding.get == 8)
  assert(masterOutstanding.state == PropertyState.Enforced(8))
  assert(masterBurstBeats.get == 16)
  assert(slaveOutstanding.get == 12)
  assert(masterWriteThreads.getOrElse(1) == 1)

  masterOutstanding.enforce(8)
  masterOutstanding.enforce(9)
  masterWriteThreads.enforce(2)
  assert(masterOutstanding.get == 8)
  assert(masterWriteThreads.get == 2)

  assert(
    properties.iterator.map(_.key.qualifiedName).toSeq == Seq(
      "master.read_outstandingTransactions",
      "master.read_burstBeats",
      "slave.read_outstandingTransactions",
      "master.write_threads"
    )
  )

  assert(
    Master.iterator.map(_.name).toSeq == Slave.iterator.drop(1).map(_.name).toSeq
  )
  assert(Master.forall(_.description.nonEmpty))
  assert(Slave.forall(_.description.nonEmpty))
  assert(Master.size == 12)
  assert(Slave.size == 13)
  val shapePropertyNames = Set(
    "read_burstBeats",
    "write_burstBeats",
    "read_burstNarrow",
    "write_burstNarrow",
    "read_burstTypes",
    "write_burstTypes",
    "read_burstSizes",
    "write_burstSizes"
  )
  val readPropertyNames = Set(
    "read_outstandingTransactions",
    "read_threads",
    "read_burstBeats",
    "read_burstNarrow",
    "read_burstTypes",
    "read_burstSizes"
  )
  val writePropertyNames = Set(
    "write_outstandingTransactions",
    "write_threads",
    "write_burstBeats",
    "write_burstNarrow",
    "write_burstTypes",
    "write_burstSizes"
  )
  assert(Master.readProperties.map(_.name) == readPropertyNames)
  assert(Master.writeProperties.map(_.name) == writePropertyNames)
  assert(Slave.readProperties.map(_.name) == readPropertyNames)
  assert(Slave.writeProperties.map(_.name) == writePropertyNames)
  assert(Master.shapeProperties.map(_.name) == shapePropertyNames)
  assert(Slave.shapeProperties.map(_.name) == shapePropertyNames)
  assert(
    Master.shapeProperties.forall {
      case Master.ShapeProperty() => true
      case _                      => false
    }
  )
  assert(
    Slave.shapeProperties.forall {
      case Slave.ShapeProperty() => true
      case _                     => false
    }
  )
  assert(
    Slave.MemoryMap match {
      case Slave.ShapeProperty() => false
      case _                     => true
    }
  )
  assert(
    Master.forall {
      case MasterProperty() => true
      case _                => false
    }
  )
  assert(
    Slave.forall {
      case SlaveProperty() => true
      case _               => false
    }
  )
  assert(
    Master.forall {
      case SlaveProperty() => false
      case _               => true
    }
  )
  assert(
    Slave.forall {
      case MasterProperty() => false
      case _                => true
    }
  )
  assert(
    Master.count {
      case ReadProperty() => true
      case _              => false
    } == 6
  )
  assert(
    Master.count {
      case WriteProperty() => true
      case _               => false
    } == 6
  )
  assert(
    Slave.count {
      case ReadProperty() => true
      case _              => false
    } == 6
  )
  assert(
    Slave.count {
      case WriteProperty() => true
      case _               => false
    } == 6
  )
  assert(
    Slave.MemoryMap match {
      case ReadProperty() | WriteProperty() => false
      case _                                => true
    }
  )
  val duplicateName = "test_duplicate_property_name"
  new MasterPropertyKey[Int](duplicateName, "Master test property.") {}
  new SlavePropertyKey[String](duplicateName, "Slave test property with the same local name.") {}

  val duplicateRejected =
    try {
      new MasterPropertyKey[String](duplicateName, "Duplicate master test property.") {}
      false
    } catch {
      case _: IllegalArgumentException => true
    }

  assert(duplicateRejected)

  val staticallyUndefined = new Tracked {
    val cfg = chext.amba.axi4.Config()
  }
  staticallyUndefined.masterProps.markUndefined(Master.readProperties)
  staticallyUndefined.slaveProps.markUndefined(Slave.WriteThreads)
  assert(
    Master.readProperties.forall(key =>
      staticallyUndefined.masterProps(key).state == PropertyState.Undefined
    )
  )
  assert(
    staticallyUndefined.slaveProps(Slave.WriteThreads).state ==
      PropertyState.Undefined
  )

  val tracked = new Tracked {
    val cfg = chext.amba.axi4.Config()
  }
  assert(tracked.masterProps eq tracked.properties)
  assert(tracked.slaveProps eq tracked.properties)
  assert(tracked.properties.isEmpty)

  val trackedProperty = tracked.properties(Master.ReadBurstBeats)
  val testResolver =
    new Resolver(null.asInstanceOf[BaseModule]) {
      def resolve[T](request: ResolveRequest[T]): ResolveResult = {
        assert(
          request.calculate(Master.ReadBurstBeats, 4, this) ==
            CalculateResult.Success
        )
        ResolveResult.Success()
      }
    }

  val otherResolver =
    new Resolver(null.asInstanceOf[BaseModule]) {
      def resolve[T](request: ResolveRequest[T]): ResolveResult =
        request.failure("unexpected fallback resolver")
    }

  tracked
    .addMasterResolver(otherResolver)
    .addMasterResolver(testResolver)
    .addMasterResolver(testResolver)
  tracked.addSlaveResolver(otherResolver)

  val request = ResolveRequest(tracked, trackedProperty)
  assert(request.interface eq tracked)
  assert(request.cfg == tracked.cfg)
  assert(request.properties eq tracked.properties)
  assert(request.property eq trackedProperty)
  assert(request.key eq Master.ReadBurstBeats)
  assert(
    request.key match {
      case MasterProperty() => true
      case _                => false
    }
  )
  assert(request.name == "read_burstBeats")
  assert(request.qualifiedName == "master.read_burstBeats")
  assert(request.description == Master.ReadBurstBeats.description)
  assert(request.state == PropertyState.Unresolved)

  assert(Resolver.resolve(request).result == ResolveResult.Success())
  assert(request.state == PropertyState.Calculated(4, testResolver))
  assert(request.valueOption.contains(4))
  assert(
    request.calculate(Master.ReadBurstBeats, 5, testResolver) ==
      CalculateResult.AlreadyCalculated
  )
  assert(request.valueOption.contains(4))

  val bound = new Tracked { val cfg = chext.amba.axi4.Config() }
  val unbound = new Tracked { val cfg = chext.amba.axi4.Config() }
  val bindingResolver =
    new Resolver(null.asInstanceOf[BaseModule]) {
      private val MasterRequests = bindMaster(bound)

      def resolve[T](request: ResolveRequest[T]): ResolveResult =
        request match {
          case MasterRequests(Master.ReadBurstBeats) =>
            request.calculate(8, this)
            ResolveResult.Success()
          case MasterRequests(_) =>
            request.undefined()
          case _ =>
            request.failure("request did not match the registered binding")
        }
    }

  val boundRequest = ResolveRequest(bound, Master.ReadBurstBeats)
  assert(Resolver.resolve(boundRequest).result == ResolveResult.Success())
  assert(boundRequest.valueOption.contains(8))

  val unboundRequest = ResolveRequest(unbound, Master.ReadBurstBeats)
  bindingResolver.resolve(unboundRequest) match {
    case ResolveResult.Failure(message, failedRequest) =>
      assert(message.contains("did not match"))
      assert(failedRequest == unboundRequest)
    case result =>
      throw new AssertionError(s"expected binding mismatch failure, got $result")
  }

  val wrongFamilyRequest = ResolveRequest(bound, Slave.ReadBurstBeats)
  bindingResolver.resolve(wrongFamilyRequest) match {
    case ResolveResult.Failure(message, failedRequest) =>
      assert(message.contains("did not match"))
      assert(failedRequest == wrongFamilyRequest)
    case result =>
      throw new AssertionError(s"expected property-family mismatch failure, got $result")
  }

  val grouped0 = new Tracked { val cfg = chext.amba.axi4.Config() }
  val grouped1 = new Tracked { val cfg = chext.amba.axi4.Config() }
  val groupedBindingResolver =
    new Resolver(null.asInstanceOf[BaseModule]) {
      private val MasterRequests = bindMaster(Seq(grouped0, grouped1))

      def resolve[T](request: ResolveRequest[T]): ResolveResult =
        request match {
          case MasterRequests(_, Master.ReadThreads) =>
            request.calculate(3, this)
            ResolveResult.Success()
          case MasterRequests(_, _) =>
            request.undefined()
          case _ =>
            request.failure("request did not match the grouped binding")
        }
    }

  Seq(grouped0, grouped1).foreach { interface =>
    val groupedRequest = ResolveRequest(interface, Master.ReadThreads)
    assert(Resolver.resolve(groupedRequest).result == ResolveResult.Success())
    assert(groupedRequest.valueOption.contains(3))
  }

  val enforcedProperty = tracked.masterProps(Master.ReadThreads)
  enforcedProperty.enforce(2)
  val enforcedRequest = ResolveRequest(tracked, enforcedProperty)
  assert(
    enforcedRequest.calculate(Master.ReadThreads, 3, testResolver) ==
      CalculateResult.AlreadyEnforced
  )
  assert(enforcedProperty.state == PropertyState.Enforced(2))

  val undefinedProperty = tracked.slaveProps(Slave.WriteThreads)
  val undefinedRequest = ResolveRequest(tracked, undefinedProperty)
  assert(undefinedRequest.undefined() == ResolveResult.Success())
  assert(undefinedRequest.state == PropertyState.Undefined)
  assert(undefinedRequest.isResolved)
  assert(
    undefinedRequest.calculate(Slave.WriteThreads, 1, testResolver) ==
      CalculateResult.Undefined
  )

  val incompleteProperty = tracked.slaveProps(Slave.ReadThreads)
  val incompleteRequest = ResolveRequest(tracked, incompleteProperty)
  assert(incompleteRequest.incomplete() == ResolveResult.Success())
  assert(incompleteRequest.state == PropertyState.Incomplete)
  assert(
    incompleteRequest.calculate(Slave.ReadThreads, 1, testResolver) ==
      CalculateResult.Incomplete
  )

  val dontCareMessage = "No synthetic aggregate is needed for this boundary"
  val dontCareProperty = tracked.slaveProps(Slave.ReadBurstTypes)
  val dontCareRequest = ResolveRequest(tracked, dontCareProperty)
  assert(dontCareRequest.dontCare(dontCareMessage) == ResolveResult.Success())
  assert(dontCareRequest.state == PropertyState.DontCare(dontCareMessage))
  assert(dontCareRequest.isResolved)
  assert(dontCareRequest.valueOption.isEmpty)
  assert(
    dontCareRequest.calculate(Slave.ReadBurstTypes, Set(1), testResolver) ==
      CalculateResult.DontCare(dontCareMessage)
  )

  val dontCareSource = new Tracked {
    val cfg = chext.amba.axi4.Config()
  }
  val dontCareSink = new Tracked {
    val cfg = chext.amba.axi4.Config()
  }
  val dontCareSourceResolver =
    new Resolver(null.asInstanceOf[BaseModule]) {
      private val MasterRequests = bindMaster(dontCareSource)

      def resolve[T](request: ResolveRequest[T]): ResolveResult =
        request match {
          case MasterRequests(_) => request.dontCare(dontCareMessage)
          case _                 => request.failure("unexpected source request")
        }
    }
  val dontCareSinkResolver =
    new Resolver(null.asInstanceOf[BaseModule]) {
      private val MasterRequests = bindMaster(dontCareSink)

      def resolve[T](request: ResolveRequest[T]): ResolveResult =
        request match {
          case MasterRequests(_) => forwardTo(request, dontCareSource)
          case _                 => request.failure("unexpected sink request")
        }
    }
  val forwardedDontCare = ResolveRequest(dontCareSink, Master.ReadBurstTypes)
  assert(Resolver.resolve(forwardedDontCare).result == ResolveResult.Success())
  assert(forwardedDontCare.state == PropertyState.DontCare(dontCareMessage))

  val dependency = new Tracked {
    val cfg = chext.amba.axi4.Config()
  }
  val dependent = new Tracked {
    val cfg = chext.amba.axi4.Config()
  }
  val dependencyRequest = ResolveRequest(dependency, Master.ReadThreads)
  val dependentRequest = ResolveRequest(dependent, Master.ReadBurstBeats)

  val dependencyResolver =
    new Resolver(null.asInstanceOf[BaseModule]) {
      def resolve[T](request: ResolveRequest[T]): ResolveResult = {
        request.calculate(Master.ReadThreads, 7, this)
        ResolveResult.Success()
      }
    }
  val dependentResolver =
    new Resolver(null.asInstanceOf[BaseModule]) {
      def resolve[T](request: ResolveRequest[T]): ResolveResult =
        if (!dependencyRequest.isResolved)
          request.retry(Seq(dependencyRequest))
        else {
          request.calculate(
            Master.ReadBurstBeats,
            dependency.properties(Master.ReadThreads).get * 2,
            this
          )
          ResolveResult.Success()
        }
    }

  dependency.addMasterResolver(dependencyResolver)
  dependent.addMasterResolver(dependentResolver)
  assert(Resolver.resolve(dependentRequest).result == ResolveResult.Success())
  assert(dependencyRequest.valueOption.contains(7))
  assert(dependentRequest.valueOption.contains(14))

  // A dependency path exercises recursive resolution at the configured depth limit.
  val resolutionDepth = 64
  val resolutionTracked: Vector[Tracked] =
    Vector.fill(resolutionDepth)(new Tracked { val cfg = chext.amba.axi4.Config() })
  val resolutionRequests =
    resolutionTracked.map(interface => ResolveRequest(interface, Master.ReadThreads))
  resolutionTracked.last.masterProps(Master.ReadThreads) = 1
  resolutionTracked.indices.dropRight(1).foreach { index =>
    val dependencyRequest = resolutionRequests(index + 1)
    resolutionTracked(index).addMasterResolver(
      new Resolver(null.asInstanceOf[BaseModule]) {
        def resolve[T](request: ResolveRequest[T]): ResolveResult =
          if (!dependencyRequest.isResolved)
            request.retry(Seq(dependencyRequest))
          else {
            request.calculate(
              Master.ReadThreads,
              dependencyRequest.valueOption.get + 1,
              this
            )
            ResolveResult.Success()
          }
      }
    )
  }
  assert(
    Resolver.resolve(resolutionRequests.head, maxStackSize = resolutionDepth).result ==
      ResolveResult.Success()
  )
  assert(resolutionRequests.head.valueOption.contains(resolutionDepth))

  val stalled = new Tracked { val cfg = chext.amba.axi4.Config() }
  val resolvedDependency = new Tracked { val cfg = chext.amba.axi4.Config() }
  val stalledRequest = ResolveRequest(stalled, Master.ReadThreads)
  val resolvedDependencyRequest =
    ResolveRequest(resolvedDependency, Master.ReadThreads)
  resolvedDependency.properties(Master.ReadThreads) = 1
  stalled.addMasterResolver(new Resolver(null.asInstanceOf[BaseModule]) {
    def resolve[T](request: ResolveRequest[T]): ResolveResult =
      request.retry(Seq(resolvedDependencyRequest))
  })

  Resolver.resolve(stalledRequest).result match {
    case ResolveResult.Failure(message, failedRequest) =>
      assert(message.contains("repeated the same Retry"))
      assert(failedRequest == stalledRequest)
    case result => assert(false, result)
  }

  val cycleA = new Tracked { val cfg = chext.amba.axi4.Config() }
  val cycleB = new Tracked { val cfg = chext.amba.axi4.Config() }
  val cycleARequest = ResolveRequest(cycleA, Master.ReadThreads)
  val cycleBRequest = ResolveRequest(cycleB, Master.ReadThreads)
  cycleA.addMasterResolver(new Resolver(null.asInstanceOf[BaseModule]) {
    def resolve[T](request: ResolveRequest[T]): ResolveResult = request.retry(Seq(cycleBRequest))
  })
  cycleB.addMasterResolver(new Resolver(null.asInstanceOf[BaseModule]) {
    def resolve[T](request: ResolveRequest[T]): ResolveResult = request.retry(Seq(cycleARequest))
  })

  Resolver.resolve(cycleARequest).result match {
    case ResolveResult.Failure(message, failedRequest) =>
      assert(message.contains("cycle"))
      assert(failedRequest == cycleARequest)
    case result => assert(false, result)
  }
}
