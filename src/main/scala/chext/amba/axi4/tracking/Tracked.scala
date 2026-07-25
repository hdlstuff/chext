package chext.amba.axi4.tracking

import chisel3.experimental.SourceInfo

import scala.collection.mutable.ArrayBuffer

import chext.amba.axi4.tracking.properties.Common

/** Property-tracking capability mixed into AXI interfaces.
  *
  * Every tracked interface lazily owns one [[PropertyManager]]. The `commonProps`, `masterProps`,
  * and `slaveProps` accessors are family-oriented views of that same manager rather than separate
  * stores; each [[PropertyKey]] carries its family through a sealed marker.
  *
  * Common, master, and slave resolvers are registered through distinct methods. More than one
  * component boundary may register a candidate for the same property family as an interface is
  * connected into a hierarchy. The selected resolver is the candidate with the smallest owner
  * hierarchy depth. At equal depth, the most recently registered candidate takes precedence.
  * Duplicate registration of the same resolver in one family is ignored.
  */
trait Tracked {
  /** Static AXI interface configuration, also enforced as `Common.Config`. */
  def cfg: chext.amba.axi4.Config

  /** Lazily realized property store for this interface.
    *
    * Initialization enforces `Common.Config` from [[cfg]], making the
    * interface configuration available through the same typed property mechanism as derived facts.
    */
  final lazy val properties: PropertyManager = {
    val result = new PropertyManager
    result(Common.Config).enforce(cfg)
    result
  }

  /** Common-property view of [[properties]]. */
  final def commonProps: PropertyManager = properties

  /** Master-property view of [[properties]]. */
  final def masterProps: PropertyManager = properties

  /** Slave-property view of [[properties]]. */
  final def slaveProps: PropertyManager = properties

  private val commonResolverRegistrations_ =
    ArrayBuffer.empty[(Resolver, SourceInfo)]
  private val masterResolverRegistrations_ =
    ArrayBuffer.empty[(Resolver, SourceInfo)]
  private val slaveResolverRegistrations_ =
    ArrayBuffer.empty[(Resolver, SourceInfo)]

  private var commonResolver = Option.empty[Resolver]
  private var masterResolver = Option.empty[Resolver]
  private var slaveResolver = Option.empty[Resolver]

  /** Returns the selected resolver for the family carried by `key`. */
  private[tracking] final def resolverOption(key: PropertyKey[_]): Option[Resolver] =
    key match {
      case _: CommonPropertyKey[_] => commonResolver
      case _: MasterPropertyKey[_] => masterResolver
      case _: SlavePropertyKey[_]  => slaveResolver
    }

  /** Registers a resolver candidate for common properties. */
  final def addCommonResolver(resolver: Resolver)(implicit
      sourceInfo: SourceInfo
  ): this.type = {
    commonResolver = registerResolver(
      commonResolverRegistrations_,
      commonResolver,
      resolver,
      sourceInfo
    )
    this
  }

  /** Registers a resolver candidate for master properties. */
  final def addMasterResolver(resolver: Resolver)(implicit
      sourceInfo: SourceInfo
  ): this.type = {
    masterResolver = registerResolver(
      masterResolverRegistrations_,
      masterResolver,
      resolver,
      sourceInfo
    )
    this
  }

  /** Registers a resolver candidate for slave properties. */
  final def addSlaveResolver(resolver: Resolver)(implicit
      sourceInfo: SourceInfo
  ): this.type = {
    slaveResolver = registerResolver(
      slaveResolverRegistrations_,
      slaveResolver,
      resolver,
      sourceInfo
    )
    this
  }

  /** Records one resolver candidate and applies hierarchy precedence.
    *
    * Registration records the implicit source location for diagnostics and updates the cached
    * selected resolver when `resolver` is owned by a shallower hierarchy node or is the latest
    * candidate at the same depth.
    *
    * @return
    *   the selected resolver after registration
    */
  private def registerResolver(
      registrations: ArrayBuffer[(Resolver, SourceInfo)],
      current: Option[Resolver],
      resolver: Resolver,
      sourceInfo: SourceInfo
  ): Option[Resolver] = {
    val alreadyRegistered = registrations.exists {
      case (existing, _) => existing eq resolver
    }

    if (!alreadyRegistered) {
      registrations.addOne((resolver, sourceInfo))
      prefer(current, resolver)
    } else
      current
  }

  /** Applies hierarchy precedence to one resolver candidate. */
  private def prefer(
      current: Option[Resolver],
      candidate: Resolver
  ): Option[Resolver] =
    current match {
      case Some(existing) if existing.hierarchyDepth < candidate.hierarchyDepth => current
      case _ => Some(candidate)
    }
}
