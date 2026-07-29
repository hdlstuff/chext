package chext.amba.axi4.tracking

import chisel3.experimental.{BaseModule, SourceInfo}

import scala.collection.mutable
import scala.reflect.ClassTag

import chext.tracking.Component
import chext.amba.axi4.tracking.{properties => p}

/** One property-resolution request.
  *
  * @tparam T
  *   value type of the requested property
  * @param tracked
  *   AXI interface that owns the property
  * @param cell
  *   realized property cell to resolve
  */
final case class ResolveRequest[T](
    tracked: Tracked,
    cell: p.Cell[T]
)

/** Convenience construction of [[ResolveRequest]] values from property keys. */
object ResolveRequest {

  /** Realizes `key` in `interface`'s manager and creates its request. */
  def apply[T](
      tracked: Tracked,
      key: p.Key[T]
  ): ResolveRequest[T] =
    ResolveRequest(tracked, tracked.properties(key))
}

/** Control result returned by one resolver step.
  *
  * This is a small protocol between a resolver and the recursive coordinator. `Success` means the
  * resolver put the request into a terminal property state. `Retry` exposes prerequisite requests
  * and asks the coordinator to revisit the original request. `Failure` aborts the entire resolution
  * operation.
  */
sealed trait ResolveResult

object ResolveResult {

  /** The requested property reached a terminal state. */
  final case class Success() extends ResolveResult

  /** Resolve prerequisite requests, then retry the original request.
    *
    * Values produced by resolver request operations contain dependencies first and the original
    * request last. Resolver implementations should use `forwardTo` or `mapFrom` rather than
    * construct this case class directly.
    */
  final case class Retry(requests: Seq[ResolveRequest[_]]) extends ResolveResult

  /** Resolution cannot proceed.
    *
    * @param message
    *   explanation suitable for an elaboration diagnostic
    * @param request
    *   request at which the failure was detected
    */
  final case class Failure(message: String, request: ResolveRequest[_]) extends ResolveResult
}

/** One boundary crossed while deriving a tracking property.
  *
  * Steps are ordered from the interface on which resolution was requested toward the interface that
  * supplied the value. Paths are absolute slash-separated tracking paths, while `kind` and
  * `resolver` are stable machine-readable identifiers suitable for diagnostics and serialized
  * memory-map arguments.
  *
  * @param interfaceFrom
  *   interface whose property was being calculated at this hop
  * @param interfaceTo
  *   dependency interface from which the property was obtained
  * @param kind
  *   category of boundary crossed, such as `connect` or `buffer`
  * @param resolver
  *   resolver implementation identifier
  * @param resolverPath
  *   absolute path of the component or module that owns the resolver
  */
final case class ResolutionStep(
    interfaceFrom: String,
    interfaceTo: String,
    kind: String,
    resolver: String,
    resolverPath: String
)

/** Ordered provenance of a calculated tracking property.
  *
  * The first element is nearest the original request and the last is nearest the interface that
  * supplied the value.
  */
final case class ResolutionTrace(steps: Seq[ResolutionStep])

/** Derives AXI tracking properties for one component or module boundary.
  *
  * A resolver is registered on one or more [[Tracked]] interfaces with the dedicated master, slave,
  * or role-free registration method. Registration is role-specific: slave properties conventionally
  * flow downstream-to-upstream, while master properties flow upstream-to-downstream. When multiple
  * resolvers are registered for the same interface and role, the tracking layer selects the
  * resolver owned by the shallowest hierarchy node so that an enclosing boundary controls
  * propagation. The latest registration wins when candidates have the same depth.
  *
  * [[resolve]] is intentionally one step of a dependency-driven operation. Implementations inspect
  * the request and use the inherited `RequestOps` methods to calculate a value, mark a valueless
  * terminal state, forward or transform a dependency, or fail. [[Resolver.resolve]] owns recursive
  * dependency traversal, cycle detection, retry validation, and the maximum dependency depth.
  *
  * Ownership also supplies stable trace metadata through [[resolverPath]] and participates in
  * resolver precedence. Use the `Component` constructor for tracked components and the `BaseModule`
  * constructor for module-owned resolvers.
  */
abstract class Resolver(private[tracking] val owner: Owner) {
  Tag.initialize()

  /** Exact resolver owner consumed implicitly by ordinary property-enforcement syntax. */
  protected implicit final val propertyOwner: Owner = owner

  /** Resolver-facing operations for one request.
    *
    * This class deliberately does not extend `AnyVal`. It is nested in [[Resolver]] so it captures
    * the enclosing resolver instance, while a Scala value class cannot be a member of another
    * class. The captured instance supplies calculation ownership, trace metadata, and diagnostics
    * without an explicit resolver argument at each call site.
    */
  implicit final class RequestOps[T](
      private val request: ResolveRequest[T]
  ) {
    private def key: p.Key[T] = request.cell.key
    private def qualifiedName: String = key.qualifiedName

    private def calculateCell[T0](
        cell: p.Cell[T0],
        value: T0,
        resolutionSteps: Seq[ResolutionStep],
        enforcedFrom: Option[p.Cell[_]]
    ): ResolveResult =
      cell.calculate(value, Resolver.this, resolutionSteps, enforcedFrom) match {
        case p.CalculateResult.Success =>
          ResolveResult.Success()
        case rejected =>
          failure(
            s"Resolver '$resolver' (kind '$kind') could not calculate " +
              s"'$qualifiedName': $rejected"
          )
      }

    /** Calculates this request from a value produced by the enclosing resolver. */
    def calculate(value: T): ResolveResult =
      calculateCell(request.cell, value, Seq.empty, None)

    /** Calculates through a key whose singleton type was widened by generic code. */
    def calculate[T0](
        targetKey: p.Key[T0],
        value: T0
    ): ResolveResult = {
      require(
        key == targetKey,
        s"ResolveRequest targets '$qualifiedName', not '${targetKey.qualifiedName}'"
      )
      calculateCell(
        request.cell.asInstanceOf[p.Cell[T0]],
        value,
        Seq.empty,
        None
      )
    }

    /** Calculates a value whose type was selected by a property pattern. */
    def calculate[T0: ClassTag](value: T0): ResolveResult = {
      val actualType = implicitly[ClassTag[T0]].runtimeClass
      require(
        key.valueType.tpe.isInstance(value),
        s"ResolveRequest targets '$qualifiedName' with value type '${key.valueType.name}', " +
          s"not '${actualType.getName}'"
      )
      calculateCell(
        request.cell,
        key.valueType.tpe.cast(value),
        Seq.empty,
        None
      )
    }

    /** Resolves this property as intentionally unnecessary for the tracking model. */
    def dontCare(message: String): ResolveResult = {
      request.cell.markDontCare(message)
      ResolveResult.Success()
    }

    /** Resolves this property as applicable but not yet modeled completely. */
    def incomplete(): ResolveResult = {
      request.cell.markIncomplete()
      ResolveResult.Success()
    }

    /** Resolves this property as not defined for the interface or topology. */
    def undefined(): ResolveResult = {
      request.cell.markUndefined()
      ResolveResult.Success()
    }

    /** Stops resolution with a diagnostic tied to this request. */
    def failure(message: String): ResolveResult =
      ResolveResult.Failure(message, request)

    /** Forwards this request to the same property on `target`. */
    def forwardTo(target: Tracked): ResolveResult =
      mapFrom(target)(identity)

    /** Resolves the same property on `target` and transforms its value at this boundary. */
    def mapFrom(target: Tracked)(transform: T => T): ResolveResult =
      mapCellFrom(request.cell, target)(transform)

    /** Resolves and transforms a value type selected by a property pattern.
      *
      * This is the typed Scala 2 counterpart to matching a stable [[properties.ValueType]].
      */
    def mapFrom[T0](
        target: Tracked,
        valueType: p.ValueType[T0]
    )(transform: T0 => T0): ResolveResult = {
      require(
        key.valueType == valueType,
        s"ResolveRequest targets '$qualifiedName', not value type '${valueType.name}'"
      )
      mapCellFrom(
        request.cell.asInstanceOf[p.Cell[T0]],
        target
      )(transform)
    }

    private def mapCellFrom[T0](
        cell: p.Cell[T0],
        target: Tracked
    )(transform: T0 => T0): ResolveResult = {
      val dependency = ResolveRequest(target, cell.key)
      if (!dependency.cell.isResolved)
        ResolveResult.Retry(Seq(dependency, request))
      else
        dependency.cell.valueOption match {
          case Some(value) =>
            calculateCell(
              cell,
              transform(value),
              ResolutionStep(
                interfaceFrom = request.tracked.trackingPath,
                interfaceTo = target.trackingPath,
                kind = kind,
                resolver = resolver,
                resolverPath = resolverPath
              ) +: dependency.cell.resolutionSteps,
              Some(dependency.cell)
            )
          case None =>
            dependency.cell.state match {
              case p.State.DontCare(message) => dontCare(message)
              case p.State.Incomplete        => incomplete()
              case p.State.Undefined         => undefined()
              case state =>
                failure(
                  s"Forwarded property '$qualifiedName' has unexpected state $state"
                )
            }
        }
    }

    /** Fails a request that reaches the catch-all branch of the enclosing resolver. */
    def missingCase(): ResolveResult =
      failure(
        s"Resolver '$resolver' (kind '$kind') has no case for '$qualifiedName'"
      )
  }

  /** Creates a resolver owned by a unified Chext component. */
  def this(owner: Component) =
    this(Owner(owner))

  /** Creates a resolver owned directly by a Chisel module. */
  def this(owner: BaseModule) =
    this(Owner(owner))

  private[tracking] final lazy val hierarchyDepth: Int =
    owner.hierarchyDepth

  private lazy val inferredResolverName =
    Resolver.defaultResolverName(getClass.getSimpleName)

  private lazy val inferredKind =
    Resolver.defaultKind(resolver)

  /** Stable category recorded in each resolution-trace hop.
    *
    * By default this is the kebab-case resolver class name without its `_Resolver` or `Resolver`
    * suffix. Override only when the public category intentionally differs from that spelling.
    */
  def kind: String = inferredKind

  /** Stable implementation name recorded in each resolution-trace hop.
    *
    * By default this normalizes the runtime class name to `<Name>Resolver`.
    */
  def resolver: String = inferredResolverName

  /** Absolute path of the owning component or module. */
  final lazy val resolverPath: String = owner.path

  /** Performs one resolution step.
    *
    * Implementations must return success only after placing `request` in a terminal state. They
    * must not recursively call another resolver; request operations expose unresolved prerequisites
    * to the coordinator so it can detect cycles and enforce its depth bound.
    */
  def resolve[T](request: ResolveRequest[T]): ResolveResult

  /** Registers this resolver for master properties on one interface. */
  protected final def bindMaster(
      interface: Tracked
  )(implicit sourceInfo: SourceInfo): Unit = {
    initializeApplicability(interface, p.Master)
    interface.addMasterResolver(this)
  }

  /** Registers this resolver for master properties on every interface in `interfaces`. */
  protected final def bindMaster(
      interfaces: Seq[Tracked]
  )(implicit sourceInfo: SourceInfo): Unit =
    interfaces.foreach(interface => bindMaster(interface))

  /** Registers this resolver for slave properties on one interface. */
  protected final def bindSlave(
      interface: Tracked
  )(implicit sourceInfo: SourceInfo): Unit = {
    initializeApplicability(interface, p.Slave)
    interface.addSlaveResolver(this)
  }

  /** Registers this resolver for slave properties on every interface in `interfaces`. */
  protected final def bindSlave(
      interfaces: Seq[Tracked]
  )(implicit sourceInfo: SourceInfo): Unit =
    interfaces.foreach(interface => bindSlave(interface))

  /** Registers this resolver for properties without a master/slave role on one interface. */
  protected final def bindNoRole(
      interface: Tracked
  )(implicit sourceInfo: SourceInfo): Unit = {
    initializeApplicability(interface, p.NoRole)
    interface.addNoRoleResolver(this)
  }

  /** Registers this resolver for role-free properties on every interface in `interfaces`. */
  protected final def bindNoRole(
      interfaces: Seq[Tracked]
  )(implicit sourceInfo: SourceInfo): Unit =
    interfaces.foreach(interface => bindNoRole(interface))

  /** Eagerly classifies standard properties that do not apply to this configuration and role. */
  private def initializeApplicability(interface: Tracked, role: p.Role): Unit = {
    val roleKeys = p.KnownKeys.filter(_.role == role)

    if (interface.cfg.lite)
      interface.properties.markUndefined(
        roleKeys.filter(_.valueType == p.BurstShape)
      )
    if (!interface.cfg.read)
      interface.properties.markUndefined(
        roleKeys.filter(_.access == p.Read)
      )
    if (!interface.cfg.write)
      interface.properties.markUndefined(
        roleKeys.filter(_.access == p.Write)
      )
  }
}

/** Recursive coordination for [[Resolver]] implementations. */
object Resolver {
  private[tracking] def defaultResolverName(simpleClassName: String): String = {
    val normalizedClassName = simpleClassName.stripSuffix("$")
    if (
      normalizedClassName.isEmpty ||
      normalizedClassName.contains("$anon")
    )
      "Resolver"
    else {
      val baseName =
        if (normalizedClassName.endsWith("_Resolver"))
          normalizedClassName.stripSuffix("_Resolver")
        else
          normalizedClassName.stripSuffix("Resolver")

      if (baseName.isEmpty) "Resolver"
      else s"${baseName}Resolver"
    }
  }

  private[tracking] def defaultKind(resolverName: String): String = {
    val baseName = resolverName.stripSuffix("Resolver")
    if (baseName.isEmpty)
      "resolver"
    else
      baseName
        .replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2")
        .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
        .toLowerCase(java.util.Locale.ROOT)
  }

  /** Result of resolution together with the final calculated trace.
    *
    * The trace is empty for failures that occur before calculation and for properties resolved to
    * enforced, don't-care, incomplete, or undefined states.
    */
  final case class Resolution(
      result: ResolveResult,
      steps: Seq[ResolutionStep]
  )

  /** Resolves `initial` and every dependency it reports.
    *
    * A fresh `ResolveContext` is created for each call, so active requests and retry history never
    * leak between top-level resolutions. Dependencies are traversed depth-first. Cycles, repeated
    * no-progress retries, malformed success results, missing property-family resolvers, and
    * excessive depth are converted into [[ResolveResult.Failure]].
    *
    * @param initial
    *   top-level property request
    * @param maxStackSize
    *   maximum number of simultaneously nested requests, including `initial`
    * @return
    *   terminal result and the resolution trace stored on `initial`
    */
  def resolve(
      initial: ResolveRequest[_],
      maxStackSize: Int = 1024
  ): Resolution = {
    val context = new ResolveContext(maxStackSize)
    val result = context.resolve(initial)
    Resolution(result, initial.cell.resolutionSteps)
  }

  /** Mutable state for one top-level resolution.
    *
    * The context tracks the active recursive dependency path for cycle detection. Each active
    * request also gets a retry-history set, preventing a resolver from returning the same
    * dependency list twice without making progress. The class is private because callers should use
    * [[resolve]]; exposing a reusable context would make its lifetime and state semantics
    * ambiguous.
    *
    * @param maxStackSize
    *   maximum number of requests permitted on the active dependency path
    */
  private final class ResolveContext(val maxStackSize: Int) {
    require(maxStackSize > 0, "Resolver.resolve maxStackSize must be positive")

    private val active = mutable.LinkedHashSet.empty[ResolveRequest[_]]

    def resolve(initial: ResolveRequest[_]): ResolveResult =
      resolveRequest(initial, depth = 1)

    private def resolveRequest(
        request: ResolveRequest[_],
        depth: Int
    ): ResolveResult = {
      if (request.cell.isResolved)
        return ResolveResult.Success()

      if (depth > maxStackSize)
        return ResolveResult.Failure(
          s"resolution stack exceeded maxStackSize=$maxStackSize",
          request
        )

      if (active.contains(request)) {
        val cycle = (active.dropWhile(_ != request).toSeq :+ request).mkString(" -> ")
        return ResolveResult.Failure(
          s"resolution dependency cycle: $cycle",
          request
        )
      }

      val resolver =
        request.tracked.resolverOption(request.cell.key).getOrElse {
          return ResolveResult.Failure(
            s"no resolver is registered for property '${request.cell.key.qualifiedName}'",
            request
          )
        }

      active.add(request)
      try {
        val retries = mutable.HashSet.empty[Seq[ResolveRequest[_]]]

        while (true) {
          resolver.resolve(request) match {
            case success @ ResolveResult.Success() =>
              if (request.cell.isResolved)
                return success
              return ResolveResult.Failure(
                "resolver returned Success without resolving the property",
                request
              )

            case failure: ResolveResult.Failure =>
              return failure

            case ResolveResult.Retry(requests) =>
              assert(
                requests.nonEmpty && requests.last == request,
                "Retry must contain the original request at the back"
              )

              val dependencies = requests.dropRight(1)
              assert(dependencies.nonEmpty, "Retry must contain at least one dependency")

              if (!retries.add(requests))
                return ResolveResult.Failure(
                  "resolver repeated the same Retry without resolving the property",
                  request
                )

              val iterator = dependencies.iterator
              while (iterator.hasNext) {
                resolveRequest(iterator.next(), depth + 1) match {
                  case ResolveResult.Success() => ()
                  case failure                 => return failure
                }
              }
          }
        }

        throw new AssertionError("unreachable")
      } finally {
        active.remove(request)
      }
    }
  }

}
