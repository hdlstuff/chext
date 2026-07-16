package chext.amba.axi4.tracking

import chisel3.experimental.BaseModule

import chext.amba.axi4.tracking.properties.{Common, Master, Slave}
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
  assert(Common.iterator.map(_.name).toSeq == Seq("config"))

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

  val tracked = new Tracked {
    val cfg = chext.amba.axi4.Config()
  }
  assert(tracked.commonProps eq tracked.properties)
  assert(tracked.masterProps eq tracked.properties)
  assert(tracked.slaveProps eq tracked.properties)
  assert(tracked.properties(Common.Config).get == tracked.cfg)
  assert(tracked.properties(Common.Config).key.role == CommonTag)

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

  tracked.addResolver(CommonTag, testResolver)
  tracked
    .addResolver(MasterTag, testResolver)
    .addResolver(MasterTag, otherResolver)
    .addResolver(MasterTag, testResolver)
  tracked.addResolver(SlaveTag, otherResolver)

  val request = ResolveRequest(tracked, trackedProperty)
  assert(request.interface eq tracked)
  assert(request.cfg == tracked.cfg)
  assert(request.properties eq tracked.properties)
  assert(request.property eq trackedProperty)
  assert(request.key eq Master.ReadBurstBeats)
  assert(request.role == MasterTag)
  assert(request.name == "read_burstBeats")
  assert(request.qualifiedName == "master.read_burstBeats")
  assert(request.description == Master.ReadBurstBeats.description)
  assert(request.state == PropertyState.Unresolved)

  assert(Resolver.recursiveResolve(request) == ResolveResult.Success())
  assert(request.state == PropertyState.Calculated(4, testResolver))
  assert(request.valueOption.contains(4))
  assert(
    request.calculate(Master.ReadBurstBeats, 5, testResolver) ==
      CalculateResult.AlreadyCalculated
  )
  assert(request.valueOption.contains(4))

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

  dependency.addResolver(MasterTag, dependencyResolver)
  dependent.addResolver(MasterTag, dependentResolver)
  assert(Resolver.recursiveResolve(dependentRequest) == ResolveResult.Success())
  assert(dependencyRequest.valueOption.contains(7))
  assert(dependentRequest.valueOption.contains(14))

  val stalled = new Tracked { val cfg = chext.amba.axi4.Config() }
  val resolvedDependency = new Tracked { val cfg = chext.amba.axi4.Config() }
  val stalledRequest = ResolveRequest(stalled, Master.ReadThreads)
  val resolvedDependencyRequest =
    ResolveRequest(resolvedDependency, Master.ReadThreads)
  resolvedDependency.properties(Master.ReadThreads) = 1
  stalled.addResolver(MasterTag, new Resolver(null.asInstanceOf[BaseModule]) {
    def resolve[T](request: ResolveRequest[T]): ResolveResult =
      request.retry(Seq(resolvedDependencyRequest))
  })

  Resolver.recursiveResolve(stalledRequest) match {
    case ResolveResult.Failure(message, failedRequest) =>
      assert(message.contains("repeated the same Retry"))
      assert(failedRequest == stalledRequest)
    case result => assert(false, result)
  }

  val cycleA = new Tracked { val cfg = chext.amba.axi4.Config() }
  val cycleB = new Tracked { val cfg = chext.amba.axi4.Config() }
  val cycleARequest = ResolveRequest(cycleA, Master.ReadThreads)
  val cycleBRequest = ResolveRequest(cycleB, Master.ReadThreads)
  cycleA.addResolver(MasterTag, new Resolver(null.asInstanceOf[BaseModule]) {
    def resolve[T](request: ResolveRequest[T]): ResolveResult = request.retry(Seq(cycleBRequest))
  })
  cycleB.addResolver(MasterTag, new Resolver(null.asInstanceOf[BaseModule]) {
    def resolve[T](request: ResolveRequest[T]): ResolveResult = request.retry(Seq(cycleARequest))
  })

  Resolver.recursiveResolve(cycleARequest) match {
    case ResolveResult.Failure(message, failedRequest) =>
      assert(message.contains("cycle"))
      assert(failedRequest == cycleARequest)
    case result => assert(false, result)
  }
}
