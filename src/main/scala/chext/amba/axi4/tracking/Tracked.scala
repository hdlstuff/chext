package chext.amba.axi4.tracking

import chisel3.experimental.SourceInfo

import scala.collection.mutable.ArrayBuffer

import chext.amba.axi4.tracking.properties.Common

trait Tracked {
  def cfg: chext.amba.axi4.Config

  final lazy val properties: PropertyManager = {
    val result = new PropertyManager
    result(Common.Config).enforce(cfg)
    result
  }

  final def commonProps: PropertyManager = properties
  final def masterProps: PropertyManager = properties
  final def slaveProps: PropertyManager = properties

  private val resolverRegistrations_ = ArrayBuffer.empty[(Role, Resolver, SourceInfo)]
  private var commonResolver = Option.empty[Resolver]
  private var masterResolver = Option.empty[Resolver]
  private var slaveResolver = Option.empty[Resolver]

  private[tracking] final def resolverOption(role: Role): Option[Resolver] =
    role match {
      case CommonTag => commonResolver
      case MasterTag => masterResolver
      case SlaveTag  => slaveResolver
    }

  final def addResolver(role: Role, resolver: Resolver)(implicit
      sourceInfo: SourceInfo
  ): this.type = {
    val alreadyRegistered = resolverRegistrations_.exists {
      case (r, existing, _) => r == role && (existing eq resolver)
    }

    if (!alreadyRegistered) {
      resolverRegistrations_.addOne((role, resolver, sourceInfo))
      role match {
        case CommonTag =>
          commonResolver = prefer(commonResolver, resolver)
        case MasterTag =>
          masterResolver = prefer(masterResolver, resolver)
        case SlaveTag =>
          slaveResolver = prefer(slaveResolver, resolver)
      }
    }
    this
  }

  private def prefer(
      current: Option[Resolver],
      candidate: Resolver
  ): Option[Resolver] =
    current match {
      case Some(existing) if existing.hierarchyDepth <= candidate.hierarchyDepth => current
      case _ => Some(candidate)
    }
}
