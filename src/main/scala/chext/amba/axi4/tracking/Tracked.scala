package chext.amba.axi4.tracking

import chisel3.experimental.SourceInfo

import scala.collection.mutable.ArrayBuffer

import chext.amba.axi4.tracking.{properties => p}
import chext.tracking.Path

/** Property-tracking capability mixed into AXI interfaces.
  *
  * Every tracked interface lazily owns one [[properties.Manager]]. Each [[properties.Key]] carries
  * its role, access, and value type.
  *
  * Master, slave, and role-free resolvers are registered through distinct methods. More than one
  * component boundary may register a candidate for the same property role as an interface is
  * connected into a hierarchy. The selected resolver is the candidate with the smallest owner
  * hierarchy depth. At equal depth, the most recently registered candidate takes precedence.
  * Duplicate registration of the same resolver in one role is ignored.
  */
trait Tracked {

  /** Static AXI interface configuration. */
  def cfg: chext.amba.axi4.Config

  /** Lazily realized property store for this interface. */
  final lazy val properties: p.Manager = new p.Manager(this)

  /** Absolute path used by tracking diagnostics and resolution traces. */
  private[axi4] final def trackingPath: String =
    Path.data(this.asInstanceOf[chisel3.Data])

  private val masterResolverRegistrations_ =
    ArrayBuffer.empty[(Resolver, SourceInfo)]
  private val slaveResolverRegistrations_ =
    ArrayBuffer.empty[(Resolver, SourceInfo)]
  private val noRoleResolverRegistrations_ =
    ArrayBuffer.empty[(Resolver, SourceInfo)]

  private var masterResolver = Option.empty[Resolver]
  private var slaveResolver = Option.empty[Resolver]
  private var noRoleResolver = Option.empty[Resolver]

  /** Returns the selected resolver for the role carried by `key`. */
  private[tracking] final def resolverOption(key: p.Key[_]): Option[Resolver] =
    key.role match {
      case p.Master => masterResolver
      case p.Slave  => slaveResolver
      case p.NoRole => noRoleResolver
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

  /** Registers a resolver candidate for properties without a master/slave role. */
  final def addNoRoleResolver(resolver: Resolver)(implicit
      sourceInfo: SourceInfo
  ): this.type = {
    noRoleResolver = registerResolver(
      noRoleResolverRegistrations_,
      noRoleResolver,
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
    val alreadyRegistered = registrations.exists { case (existing, _) =>
      existing eq resolver
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
      case _                                                                    => Some(candidate)
    }
}
