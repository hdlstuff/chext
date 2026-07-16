package chext.amba.axi4.tracking

import chisel3.experimental.{BaseModule, SourceInfo}

import scala.collection.mutable

import chext.tracking.Component

final class ResolveRequest[T] private[tracking] (
    val interface: Tracked,
    val property: Property[T]
) {

  final def cfg: chext.amba.axi4.Config = interface.cfg
  final def properties: PropertyManager = interface.properties
  final def key: PropertyKey[T] = property.key
  final def role: Role = key.role
  final def name: String = key.name
  final def qualifiedName: String = key.qualifiedName
  final def description: String = key.description
  final def state: PropertyState[T] = property.state
  final def isResolved: Boolean = property.isResolved
  final def valueOption: Option[T] = property.valueOption
  final def resolutionSteps: Seq[ResolutionStep] = property.resolutionSteps

  final def calculate(
      value: T,
      resolver: Resolver,
      resolutionSteps: Seq[ResolutionStep] = Seq.empty
  ): CalculateResult =
    property.calculate(value, resolver, resolutionSteps)

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

  /** Creates the corresponding request for the same property on another AXI
    * interface while preserving the property's value type.
    */
  final def retarget(target: Tracked): ResolveRequest[T] =
    ResolveRequest(target, key)

  final def incomplete(): ResolveResult = {
    property.markIncomplete()
    ResolveResult.Success()
  }

  final def undefined(): ResolveResult = {
    property.markUndefined()
    ResolveResult.Success()
  }

  final def retry(dependencies: Seq[ResolveRequest[_]]): ResolveResult =
    ResolveResult.Retry(dependencies :+ this)

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

object ResolveRequest {
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

  def apply[T](
      interface: Tracked,
      key: PropertyKey[T]
  ): ResolveRequest[T] =
    apply(interface, interface.properties(key))
}

sealed trait ResolveResult

object ResolveResult {
  final case class Success() extends ResolveResult

  final case class Retry(requests: Seq[ResolveRequest[_]]) extends ResolveResult

  final case class Failure(message: String, request: ResolveRequest[_]) extends ResolveResult
}

abstract class Resolver private (private[tracking] val owner: Resolver.Owner) {
  def this(owner: Component) =
    this(Resolver.Owner.ComponentOwner(owner))

  def this(owner: BaseModule) =
    this(Resolver.Owner.Module(owner))

  private[tracking] final lazy val hierarchyDepth: Int =
    owner.hierarchyDepth

  def kind: String = "resolver"
  def resolver: String = "Resolver"
  final lazy val resolverPath: String = owner.resolverPath
  def resolve[T](request: ResolveRequest[T]): ResolveResult
}

/** Transparently forwards role-specific properties between two AXI endpoints.
  *
  * Slave properties flow from the slave endpoint toward the master endpoint.
  * Master properties flow from the master endpoint toward the slave endpoint.
  */
final class ForwardingResolver(
    owner: Component,
    master: Tracked,
    slave: Tracked,
    override val kind: String,
    override val resolver: String
)(implicit sourceInfo: SourceInfo)
    extends Resolver(owner) {
  master.addResolver(SlaveTag, this)
  slave.addResolver(MasterTag, this)

  def resolve[T](request: ResolveRequest[T]): ResolveResult = {
    val target =
      if ((request.interface eq master) && request.role == SlaveTag) slave
      else if ((request.interface eq slave) && request.role == MasterTag) master
      else
        return request.failure(
          s"AXI $kind resolver cannot forward '${request.qualifiedName}' from this endpoint"
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
                s"Forwarded property '${request.qualifiedName}' has unexpected state $state"
              )
          }
      }
  }
}

object Resolver {
  final case class Resolution(
      result: ResolveResult,
      steps: Seq[ResolutionStep]
  )

  def recursiveResolve(
      initial: ResolveRequest[_],
      maxStackSize: Int = 1024
  ): ResolveResult =
    new ResolveContext(maxStackSize).resolve(initial)

  def recursiveResolveWithTrace(
      initial: ResolveRequest[_],
      maxStackSize: Int = 1024
  ): Resolution = {
    val context = new ResolveContext(maxStackSize)
    val result = context.resolve(initial)
    Resolution(result, initial.resolutionSteps)
  }

  private final class ResolveContext(val maxStackSize: Int) {
    require(maxStackSize > 0, "Resolver.recursiveResolve maxStackSize must be positive")

    private val active = mutable.LinkedHashSet.empty[ResolveRequest[_]]

    def resolve(initial: ResolveRequest[_]): ResolveResult =
      resolveOne(initial, depth = 1)

    private def resolveOne(request: ResolveRequest[_], depth: Int): ResolveResult = {
      if (request.isResolved)
        return ResolveResult.Success()

      if (depth > maxStackSize)
        return ResolveResult.Failure(
          s"resolution stack exceeded maxStackSize=$maxStackSize",
          request
        )

      if (active.contains(request)) {
        val cycle = (active.dropWhile(_ != request).toSeq :+ request).mkString(" -> ")
        return ResolveResult.Failure(s"resolution dependency cycle: $cycle", request)
      }

      val resolver = request.interface.resolverOption(request.role).getOrElse {
        return ResolveResult.Failure(
          s"no resolver is registered for role '${request.role.name}'",
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
                resolveOne(iterator.next(), depth + 1) match {
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

  private[tracking] sealed trait Owner {
    def hierarchyDepth: Int
    def resolverPath: String
  }

  private[tracking] object Owner {
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

    final case class Module(module: BaseModule) extends Owner {
      val hierarchyDepth: Int = 0
      lazy val resolverPath: String =
        if (module eq null) "/"
        else TrackingPath.module(module)
    }
  }
}
