package chext.amba.axi4.tracking

import chisel3.experimental.{BaseModule, SourceInfo}

import scala.collection.mutable

import chext.tracking.Component
import chext.amba.axi4.tracking.properties.{Master, Slave}

/** Type-safe resolver view of one property on one AXI interface.
  *
  * A request is the unit of work passed to [[Resolver.resolve]]. It keeps the requested
  * [[Property]] and its owning [[Tracked]] interface together, exposes their resolver-relevant
  * metadata, and provides the only operations a resolver normally needs to complete the request.
  *
  * Resolvers that derive a property from another interface should call [[retarget]], inspect the
  * returned dependency, and return [[retry]] while that dependency is unresolved. The recursive
  * coordinator resolves those dependencies depth-first and invokes the original resolver again.
  * Once the required facts are available, the resolver terminates the request with `calculate`,
  * [[dontCare]], [[incomplete]], or [[undefined]]. It may instead return [[failure]] when the
  * topology or property state is invalid.
  *
  * Requests use interface and property identity for equality. Consequently, independently
  * constructed requests for the same realized property compare equal and participate correctly in
  * dependency-cycle detection.
  *
  * @tparam T
  *   value type of the requested property
  * @param interface
  *   AXI interface that owns the property
  * @param property
  *   realized property to resolve
  */
final class ResolveRequest[T] private[tracking] (
    val interface: Tracked,
    val property: Property[T]
) {

  /** AXI configuration of the interface on which this request originated. */
  final def cfg: chext.amba.axi4.Config = interface.cfg

  /** Complete property manager of the request's interface. */
  final def properties: PropertyManager = interface.properties

  /** Typed key identifying the requested property. */
  final def key: PropertyKey[T] = property.key

  /** Unqualified property name. */
  final def name: String = key.name

  /** Family-qualified property name, for example `slave.memoryMap`. */
  final def qualifiedName: String = key.qualifiedName

  /** Human-readable description supplied by the property key. */
  final def description: String = key.description

  /** Current state of the realized property. */
  final def state: PropertyState[T] = property.state

  /** Whether the property has reached any terminal state, including valueless states. */
  final def isResolved: Boolean = property.isResolved

  /** Calculated or enforced value, or `None` for valueless and unresolved states. */
  final def valueOption: Option[T] = property.valueOption

  /** Ordered trace accumulated while calculating the property. */
  final def resolutionSteps: Seq[ResolutionStep] = property.resolutionSteps

  /** Calculates this property from facts handled by `resolver`.
    *
    * Calculation succeeds only while the property is unresolved. An explicitly enforced value and
    * every other terminal state take precedence and produce the corresponding [[CalculateResult]]
    * rejection. `resolutionSteps` should describe the hop performed by `resolver`, followed by any
    * dependency trace that led to `value`.
    *
    * A resolver normally returns [[ResolveResult.Success]] after calling this method. The
    * recursive coordinator verifies that a reported success really left the property in a
    * terminal state.
    */
  final def calculate(
      value: T,
      resolver: Resolver,
      resolutionSteps: Seq[ResolutionStep] = Seq.empty
  ): CalculateResult =
    property.calculate(value, resolver, resolutionSteps)

  /** Calculates this request through a key whose singleton type was widened by generic code.
    *
    * The runtime identity check guarantees that `targetKey` is the key represented by this request
    * before the localized type cast is made. Prefer the simpler `calculate(value, resolver)`
    * overload when the request's `T` is already available.
    */
  final def calculate[T0](
      targetKey: PropertyKey[T0],
      value: T0,
      resolver: Resolver
  ): CalculateResult = {
    require(
      key.asInstanceOf[AnyRef] eq targetKey.asInstanceOf[AnyRef],
      s"ResolveRequest targets '$qualifiedName', not '${targetKey.qualifiedName}'"
    )
    property.asInstanceOf[Property[T0]].calculate(value, resolver, Seq.empty)
  }

  /** Creates a dependency request for the same property key on `target`.
    *
    * The returned request points at `target`'s realized property while preserving `T`. No
    * resolution is performed by this method.
    */
  final def retarget(target: Tracked): ResolveRequest[T] =
    ResolveRequest(target, key)

  /** Resolves this property as intentionally unnecessary for the tracking model.
    *
    * A don't-care result is different from missing information: the resolver deliberately does not
    * derive this property because no planned compatibility check needs a synthetic value at this
    * boundary.
    */
  final def dontCare(message: String): ResolveResult = {
    property.markDontCare(message)
    ResolveResult.Success()
  }

  /** Resolves this property as intentionally incomplete.
    *
    * Incomplete means that the interface participates in tracking, but the design has not supplied
    * enough information to compute this property. It is a successful terminal state with no value.
    */
  final def incomplete(): ResolveResult = {
    property.markIncomplete()
    ResolveResult.Success()
  }

  /** Resolves this property as not defined for this interface or topology.
    *
    * Undefined is a successful terminal state with no value. Use [[incomplete]] instead when the
    * property concept applies but required information is missing.
    */
  final def undefined(): ResolveResult = {
    property.markUndefined()
    ResolveResult.Success()
  }

  /** Asks the coordinator to resolve `dependencies` and then invoke this resolver again.
    *
    * The original request is appended as a continuation marker. Callers supply only true
    * dependencies; at least one is required. [[Resolver.resolve]] validates the shape,
    * detects repeated retries and cycles, and resolves dependencies in sequence.
    */
  final def retry(dependencies: Seq[ResolveRequest[_]]): ResolveResult =
    ResolveResult.Retry(dependencies :+ this)

  /** Stops resolution with a diagnostic tied to this request. */
  final def failure(message: String): ResolveResult =
    ResolveResult.Failure(message, this)

  final override def equals(other: Any): Boolean =
    other match {
      case that: ResolveRequest[_] =>
        (interface.asInstanceOf[AnyRef] eq that.interface.asInstanceOf[AnyRef]) &&
          (property.asInstanceOf[AnyRef] eq that.property.asInstanceOf[AnyRef])
      case _ => false
    }

  final override def hashCode(): Int =
    31 * System.identityHashCode(interface) + System.identityHashCode(property)

  final override def toString: String =
    s"ResolveRequest($qualifiedName, interface=$interface)"
}

/** Constructs validated [[ResolveRequest]] values. */
object ResolveRequest {
  /** Creates a request for an already-realized property.
    *
    * The property must belong to `interface`; this guards against accidentally pairing a property
    * instance from one interface with another interface.
    */
  def apply[T](
      interface: Tracked,
      property: Property[T]
  ): ResolveRequest[T] = {
    require(
      interface.properties.containsProperty(property),
      s"Property '${property.key.qualifiedName}' does not belong to the requested interface"
    )
    new ResolveRequest(interface, property)
  }

  /** Realizes `key` in `interface`'s manager and creates its request. */
  def apply[T](
      interface: Tracked,
      key: PropertyKey[T]
  ): ResolveRequest[T] =
    apply(interface, interface.properties(key))
}

/** Control result returned by one invocation of [[Resolver.resolve]].
  *
  * This is a small protocol between a resolver and the recursive coordinator. `Success` means
  * the resolver put the request into a terminal property state. `Retry` exposes prerequisite
  * requests and asks the coordinator to revisit the original request. `Failure` aborts the entire
  * resolution operation.
  */
sealed trait ResolveResult

object ResolveResult {
  /** The requested property reached a terminal state. */
  final case class Success() extends ResolveResult

  /** Resolve prerequisite requests, then retry the original request.
    *
    * Values produced by [[ResolveRequest.retry]] contain dependencies first and the original
    * request last. Resolver implementations should use that helper rather than construct this case
    * class directly.
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

/** Derives AXI tracking properties for one component or module boundary.
  *
  * A resolver is registered on one or more [[Tracked]] interfaces with the dedicated master or
  * slave registration method. Registration is family-specific: slave properties conventionally
  * flow downstream-to-upstream,
  * while master properties flow upstream-to-downstream. When multiple resolvers are registered for
  * the same interface and family, the tracking layer selects the resolver owned by the shallowest
  * hierarchy node so that an enclosing boundary controls propagation. The latest registration
  * wins when candidates have the same depth.
  *
  * [[resolve]] is intentionally one step of a dependency-driven operation. Implementations
  * inspect the request, create typed dependencies with [[ResolveRequest.retarget]], and use the
  * request helpers to calculate a value, mark a valueless terminal state, retry after dependencies,
  * or fail. [[Resolver.resolve]] owns recursive dependency traversal, cycle detection, retry
  * validation, and the maximum dependency depth.
  *
  * Ownership also supplies stable trace metadata through [[resolverPath]] and participates in
  * resolver precedence. Use the `Component` constructor for tracked components and the
  * `BaseModule` constructor for module-owned resolvers.
  */
abstract class Resolver private (private[tracking] val owner: Resolver.Owner) {
  Tag.initialize()

  /** Stable value-family extractors shared by standard component resolver policies. */
  protected final val MemoryMap = PropertyValueType[values.MemoryMap]
  protected final val BurstShape = PropertyValueType[values.BurstShape]
  protected final val ThreadMode = PropertyValueType[values.ThreadMode]
  protected final val TrafficProfile = PropertyValueType[values.TrafficProfile]

  private val masterInterfaces = mutable.ArrayBuffer.empty[Tracked]
  private val slaveInterfaces = mutable.ArrayBuffer.empty[Tracked]

  /** Exposes a request's property key to a nested value-family extractor.
    *
    * Resolver selection has already established the applicable interface and master/slave family,
    * allowing family-independent policies to read as `Request(TrafficProfile())`.
    */
  protected object Request {
    def unapply[T](request: ResolveRequest[T]): Some[PropertyKey[T]] =
      Some(request.key)
  }

  /** Matches a master-property request registered on this resolver. */
  protected object MasterRequests {
    def unapply[T](request: ResolveRequest[T]): Option[PropertyKey[T]] =
      if (
        masterInterfaces.exists(_ eq request.interface) &&
        MasterProperty.accepts(request.key)
      )
        Some(request.key)
      else
        None

    /** Matches a master request and also extracts its bound interface. */
    object withInterface {
      def unapply[T](
          request: ResolveRequest[T]
      ): Option[(Tracked, PropertyKey[T])] =
        MasterRequests.unapply(request).map(request.interface -> _)
    }
  }

  /** Matches a slave-property request registered on this resolver. */
  protected object SlaveRequests {
    def unapply[T](request: ResolveRequest[T]): Option[PropertyKey[T]] =
      if (
        slaveInterfaces.exists(_ eq request.interface) &&
        SlaveProperty.accepts(request.key)
      )
        Some(request.key)
      else
        None

    /** Matches a slave request and also extracts its bound interface. */
    object withInterface {
      def unapply[T](
          request: ResolveRequest[T]
      ): Option[(Tracked, PropertyKey[T])] =
        SlaveRequests.unapply(request).map(request.interface -> _)
    }
  }

  /** Creates a resolver owned by a unified Chext component. */
  def this(owner: Component) =
    this(Resolver.Owner.ComponentOwner(owner))

  /** Creates a resolver owned directly by a Chisel module. */
  def this(owner: BaseModule) =
    this(Resolver.Owner.Module(owner))

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
  final lazy val resolverPath: String = owner.resolverPath

  /** Performs one resolution step.
    *
    * Implementations must return success only after placing `request` in a terminal state. They
    * must not recursively call another resolver; unresolved prerequisites are returned with
    * [[ResolveRequest.retry]] so the coordinator can detect cycles and enforce its depth bound.
    */
  def resolve[T](request: ResolveRequest[T]): ResolveResult

  /** Registers this resolver for master properties on one interface.
    *
    * Concrete resolvers match registered requests through the inherited [[MasterRequests]]
    * extractor.
    */
  protected final def bindMaster(
      interface: Tracked
  )(implicit sourceInfo: SourceInfo): Unit = {
    initializeMasterApplicability(interface)
    interface.addMasterResolver(this)
    masterInterfaces += interface
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
    initializeSlaveApplicability(interface)
    interface.addSlaveResolver(this)
    slaveInterfaces += interface
  }

  /** Registers this resolver for slave properties on every interface in `interfaces`. */
  protected final def bindSlave(
      interfaces: Seq[Tracked]
  )(implicit sourceInfo: SourceInfo): Unit =
    interfaces.foreach(interface => bindSlave(interface))

  /** Fails a request which reached the catch-all branch of this resolver's policy.
    *
    * The diagnostic identifies the derived resolver implementation and kind, so concrete
    * resolvers do not need to repeat endpoint-specific boilerplate.
    */
  protected final def missingCase[T](
      request: ResolveRequest[T]
  ): ResolveResult =
    request.failure(
      s"Resolver '$resolver' (kind '$kind') has no case for '${request.qualifiedName}'"
    )

  /** Eagerly classifies inapplicable standard master properties from the interface configuration. */
  private def initializeMasterApplicability(interface: Tracked): Unit = {
    if (interface.cfg.lite)
      interface.masterProps.markUndefined(
        Seq(Master.ReadBurstShape, Master.WriteBurstShape)
      )
    if (!interface.cfg.read)
      interface.masterProps.markUndefined(Master.readProperties)
    if (!interface.cfg.write)
      interface.masterProps.markUndefined(Master.writeProperties)
  }

  /** Eagerly classifies inapplicable standard slave properties from the interface configuration. */
  private def initializeSlaveApplicability(interface: Tracked): Unit = {
    if (interface.cfg.lite)
      interface.slaveProps.markUndefined(
        Seq(Slave.ReadBurstShape, Slave.WriteBurstShape)
      )
    if (!interface.cfg.read)
      interface.slaveProps.markUndefined(Slave.readProperties)
    if (!interface.cfg.write)
      interface.slaveProps.markUndefined(Slave.writeProperties)
  }

  /** Forwards `request` to the same property on `target`.
    *
    * The helper implements the common retry/copy/trace protocol while the concrete component
    * resolver remains responsible for selecting and validating the target endpoint.
    */
  protected final def forwardTo[T](
      request: ResolveRequest[T],
      target: Tracked
  ): ResolveResult =
    mapFrom(request, target)(identity)

  /** Resolves a dependency for the same key and transforms its value at this boundary. */
  protected final def mapFrom[T](
      request: ResolveRequest[T],
      target: Tracked
  )(transform: T => T): ResolveResult = {
    val dependency = request.retarget(target)
    if (!dependency.isResolved)
      request.retry(Seq(dependency))
    else
      dependency.valueOption match {
        case Some(value) =>
          request.calculate(
            transform(value),
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
            case PropertyState.DontCare(message) => request.dontCare(message)
            case PropertyState.Incomplete        => request.incomplete()
            case PropertyState.Undefined         => request.undefined()
            case state =>
              request.failure(
                s"Forwarded property '${request.qualifiedName}' has unexpected state $state"
              )
          }
      }
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
    Resolution(result, initial.resolutionSteps)
  }

  /** Mutable state for one top-level resolution.
    *
    * The context tracks the active recursive dependency path for cycle detection. Each active
    * request also gets a retry-history set, preventing a resolver from returning the same dependency
    * list twice without making progress. The class is private because callers should use
    * [[resolve]]; exposing a reusable context would make its lifetime and state semantics ambiguous.
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
      if (request.isResolved)
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

      val resolver = request.interface.resolverOption(request.key).getOrElse {
        return ResolveResult.Failure(
          s"no resolver is registered for property '${request.qualifiedName}'",
          request
        )
      }

      active.add(request)
      try {
        val retries = mutable.HashSet.empty[Seq[ResolveRequest[_]]]

        while (true) {
          resolver.resolve(request) match {
            case success @ ResolveResult.Success() =>
              if (request.isResolved)
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

  /** Uniform owner abstraction used for precedence and trace paths. */
  private[tracking] sealed trait Owner {
    def hierarchyDepth: Int
    def resolverPath: String
  }

  private[tracking] object Owner {
    /** Resolver ownership by a unified tracked component. */
    final case class ComponentOwner(component: Component) extends Owner {
      lazy val resolverPath: String = TrackingPath.component(component)

      lazy val hierarchyDepth: Int = {
        @scala.annotation.tailrec
        def loop(current: Component, depth: Int): Int =
          current.parentOption match {
            case Some(parent) => loop(parent, depth + 1)
            case None         => depth
          }

        loop(component, 1)
      }
    }

    /** Resolver ownership by a Chisel module.
      *
      * Modules use depth zero because they form the enclosing resolution boundary. A null module is
      * accepted only for lightweight unit-test resolvers and is represented by the root path.
      */
    final case class Module(module: BaseModule) extends Owner {
      val hierarchyDepth: Int = 0
      lazy val resolverPath: String =
        if (module eq null) "/"
        else TrackingPath.module(module)
    }
  }
}
