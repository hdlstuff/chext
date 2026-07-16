package chext.amba.axi4.util

import chext.util.MathUtils

/** An immutable, hierarchical AXI address map.
  *
  * Child offsets and segment base addresses are relative to their immediate
  * containing map. `path` names a map relative to its parent. Segment paths
  * are also relative to their containing map. A segment's absolute address is
  * therefore its base address plus all map offsets on its path from the root.
  *
  * `size` is the declared extent of this map. It is independent of how much of
  * the map is occupied by children and segments.
  */
final case class MemoryMap(
    path: Seq[String] = Seq.empty,
    offset: BigInt = 0,
    size: BigInt,
    children: Seq[MemoryMap] = Seq.empty,
    segments: Seq[MemoryMap.Segment] = Seq.empty,
    origin: String = "",
    args: Map[String, hdlinfo.TypedObject] = Map.empty
) {
  import MemoryMap._

  require(path.forall(_.nonEmpty), "Memory map path elements must not be empty")
  require(
    path.forall(element => !element.contains('/')),
    "Memory map path elements must not contain '/'"
  )
  require(offset >= 0, "Memory map offset must not be negative")
  require(size >= 0, "Memory map size must not be negative")
  require(origin.isEmpty || origin.startsWith("/"), "Memory map origin must start with '/'")

  /** Extent occupied by direct children and segments, relative to this map's
    * base. A child occupies its complete declared extent.
    */
  lazy val occupiedSize: BigInt =
    (
      children.map(child => child.offset + child.allocatedSize) ++
        segments.map(segment => segment.baseAddress + segment.size)
    ).maxOption.getOrElse(BigInt(0))

  /** Extent allocated to this map when it is placed in a parent. */
  lazy val allocatedSize: BigInt = size

  /** Smallest power-of-two extent capable of containing this map. */
  lazy val alignedSize: BigInt =
    if (allocatedSize == 0) 0 else MathUtils.nextPowerOfTwo(allocatedSize)

  /** Validates declared bounds, direct-entry overlap, and all child maps. */
  def validate: ValidationResult = validateAt(Seq.empty)

  /** Adds an absolute origin to nodes and segments which do not already have
    * one. Existing, more specific origins are preserved.
    */
  def withDefaultOrigin(value: String): MemoryMap = {
    require(value.startsWith("/"), "Memory map origin must start with '/'")
    copy(
      children = children.map(_.withDefaultOrigin(value)),
      segments = segments.map(_.withDefaultOrigin(value)),
      origin = if (origin.isEmpty) value else origin
    )
  }

  private def validateAt(parentPath: Seq[String]): ValidationResult = {
    val currentPath = parentPath ++ path
    val entries =
      children.map { child =>
        Entry(
          path = currentPath ++ child.path,
          kind = EntryKind.Child,
          offset = child.offset,
          size = child.allocatedSize
        )
      } ++ segments.map { segment =>
        Entry(
          path = currentPath ++ segment.path,
          kind = EntryKind.Segment,
          offset = segment.baseAddress,
          size = segment.size
        )
      }

    entries.find(_.end > allocatedSize) match {
      case Some(entry) =>
        return ValidationResult.Failure(
          id = FailureId.Overallocated,
          path = entry.path,
          kind = entry.kind,
          offset = entry.offset,
          size = entry.size,
          limit = Some(allocatedSize)
        )
      case None => ()
    }

    val sortedEntries = entries.sortBy(entry => (entry.offset, entry.end))
    sortedEntries.sliding(2).foreach {
      case Seq(left, right) if left.end > right.offset =>
        return ValidationResult.Failure(
          id = FailureId.Overlap,
          path = right.path,
          kind = right.kind,
          offset = right.offset,
          size = right.size,
          conflictingPath = Some(left.path)
        )
      case _ => ()
    }

    children.iterator
      .map(_.validateAt(currentPath))
      .collectFirst { case failure: ValidationResult.Failure => failure }
      .getOrElse(ValidationResult.Success)
  }

  /** Moves direct-child segments and grandchildren into this map, adjusting
    * offsets and paths to remain relative to this map.
    */
  def flattenOnce: MemoryMap = {
    children.foreach { child =>
      require(child.path.nonEmpty, "Child memory maps must have a non-empty path")
    }

    val adoptedSegments = children.flatMap { child =>
      child.segments.map { segment =>
        segment.copy(
          path = child.path ++ segment.path,
          baseAddress = child.offset + segment.baseAddress
        )
      }
    }
    val adoptedChildren = children.flatMap { child =>
      child.children.map { grandchild =>
        grandchild.copy(
          path = child.path ++ grandchild.path,
          offset = child.offset + grandchild.offset
        )
      }
    }

    copy(children = adoptedChildren, segments = segments ++ adoptedSegments)
  }

  /** Recursively flattens this map until it contains no child maps. */
  def flatten: MemoryMap = {
    @scala.annotation.tailrec
    def loop(memoryMap: MemoryMap): MemoryMap =
      if (memoryMap.children.isEmpty) memoryMap
      else loop(memoryMap.flattenOnce)

    loop(this)
  }
}

object MemoryMap {
  sealed trait AllocationScheme

  object AllocationScheme {
    /** Reserves each child map's individual power-of-two-aligned extent. */
    case object AlignedPacked extends AllocationScheme

    /** Reserves one common extent based on the largest aligned child map. */
    case object AlignedLargest extends AllocationScheme

    /** Places child maps consecutively using their declared extents. */
    case object Tight extends AllocationScheme
  }

  sealed abstract class FailureId(val name: String)

  object FailureId {
    case object Overallocated extends FailureId("overallocated")
    case object Overlap extends FailureId("overlap")
  }

  sealed abstract class EntryKind(val name: String)

  object EntryKind {
    case object Child extends EntryKind("child")
    case object Segment extends EntryKind("segment")
  }

  sealed trait ValidationResult

  object ValidationResult {
    case object Success extends ValidationResult

    final case class Failure(
        id: FailureId,
        path: Seq[String],
        kind: EntryKind,
        offset: BigInt,
        size: BigInt,
        limit: Option[BigInt] = None,
        conflictingPath: Option[Seq[String]] = None
    ) extends ValidationResult {
      def renderedPath: String = path.mkString("/", "/", "")

      def render: String = {
        val range = s"offset=0x${offset.toString(16)}, size=0x${size.toString(16)}"
        id match {
          case FailureId.Overallocated =>
            val renderedLimit = limit.map(_.toString(16)).getOrElse("?")
            s"${id.name}: ${kind.name} '$renderedPath' ($range) exceeds " +
              s"map size 0x$renderedLimit"
          case FailureId.Overlap =>
            val other = conflictingPath.map(_.mkString("/", "/", "")).getOrElse("?")
            s"${id.name}: ${kind.name} '$renderedPath' ($range) overlaps '$other'"
        }
      }
    }
  }

  private final case class Entry(
      path: Seq[String],
      kind: EntryKind,
      offset: BigInt,
      size: BigInt
  ) {
    def end: BigInt = offset + size
  }

  final case class Segment(
      path: Seq[String],
      baseAddress: BigInt,
      size: BigInt,
      origin: String = "",
      args: Map[String, hdlinfo.TypedObject] = Map.empty
  ) {
    require(path.nonEmpty, "Memory map segment path must not be empty")
    require(path.forall(_.nonEmpty), "Memory map segment path elements must not be empty")
    require(
      path.forall(element => !element.contains('/')),
      "Memory map segment path elements must not contain '/'"
    )
    require(baseAddress >= 0, "Memory map segment base address must not be negative")
    require(size > 0, "Memory map segment size must be positive")
    require(origin.isEmpty || origin.startsWith("/"), "Memory map origin must start with '/'")

    def renderedPath: String = path.mkString("/", "/", "")

    def withDefaultOrigin(value: String): Segment = {
      require(value.startsWith("/"), "Memory map origin must start with '/'")
      if (origin.isEmpty) copy(origin = value) else this
    }
  }

  /** Allocates child maps in the supplied order according to `scheme`. */
  def aggregate(
      memoryMaps: Seq[MemoryMap],
      scheme: AllocationScheme = AllocationScheme.AlignedPacked
  ): MemoryMap = {
    require(memoryMaps.nonEmpty, "At least one memory map is required")
    memoryMaps.zipWithIndex.foreach { case (memoryMap, index) =>
      require(memoryMap.path.nonEmpty, s"Memory map $index must have a non-empty path")
      require(memoryMap.allocatedSize > 0, s"Memory map $index must not be empty")
    }

    val largestAlignedSize = memoryMaps.map(_.alignedSize).max
    var nextOffset = BigInt(0)
    val children = memoryMaps.map { memoryMap =>
      val (offset, reservedSize) = scheme match {
        case AllocationScheme.AlignedPacked =>
          val offset = MathUtils.alignUp(nextOffset, memoryMap.alignedSize)
          offset -> memoryMap.alignedSize
        case AllocationScheme.AlignedLargest =>
          val offset = MathUtils.alignUp(nextOffset, largestAlignedSize)
          offset -> largestAlignedSize
        case AllocationScheme.Tight =>
          nextOffset -> memoryMap.allocatedSize
      }
      nextOffset = offset + reservedSize
      memoryMap.copy(offset = offset)
    }

    MemoryMap(size = nextOffset, children = children)
  }
}
