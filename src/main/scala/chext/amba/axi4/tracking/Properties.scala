package chext.amba.axi4.tracking

import scala.collection.mutable

sealed trait Role {
  def name: String
}

case object CommonTag extends Role {
  val name = "common"
}

case object MasterTag extends Role {
  val name = "master"
}

case object SlaveTag extends Role {
  val name = "slave"
}

abstract class PropertyKey[T](
    val name: String,
    val role: Role,
    val description: String
) {
  PropertyKey.register(this)

  final def qualifiedName: String = s"${role.name}.$name"
  final override def toString: String = qualifiedName
}

private object PropertyKey {
  private val keysByQualifiedName = mutable.HashMap.empty[String, PropertyKey[_]]

  def register(key: PropertyKey[_]): Unit = synchronized {
    require(key.name.nonEmpty, "AXI4 tracking property names must not be empty")
    require(
      key.description.nonEmpty,
      s"AXI4 tracking property '${key.qualifiedName}' needs a description"
    )

    keysByQualifiedName.get(key.qualifiedName) match {
      case Some(existing) if existing ne key =>
        throw new IllegalArgumentException(
          s"AXI4 tracking property '${key.qualifiedName}' is already defined by $existing"
        )
      case _ => keysByQualifiedName.update(key.qualifiedName, key)
    }
  }
}

abstract class CommonPropertyKey[T](name: String, description: String)
    extends PropertyKey[T](name, CommonTag, description)

abstract class MasterPropertyKey[T](name: String, description: String)
    extends PropertyKey[T](name, MasterTag, description)

abstract class SlavePropertyKey[T](name: String, description: String)
    extends PropertyKey[T](name, SlaveTag, description)

sealed trait PropertyState[+T] {
  def isResolved: Boolean
}

object PropertyState {
  case object Unresolved extends PropertyState[Nothing] {
    val isResolved = false
  }

  sealed trait Resolved[+T] extends PropertyState[T] {
    final val isResolved = true
  }

  final case class Enforced[T](value: T) extends Resolved[T]
  final case class Calculated[T](value: T, resolver: Resolver) extends Resolved[T]
  case object Incomplete extends Resolved[Nothing]
  case object Undefined extends Resolved[Nothing]
}

sealed trait CalculateResult {
  def isSuccess: Boolean
}

object CalculateResult {
  case object Success extends CalculateResult {
    val isSuccess = true
  }

  sealed trait Rejected extends CalculateResult {
    final val isSuccess = false
  }

  case object AlreadyEnforced extends Rejected
  case object AlreadyCalculated extends Rejected
  case object Incomplete extends Rejected
  case object Undefined extends Rejected
}

final class Property[T] private[tracking] (val key: PropertyKey[T]) {
  import PropertyState._

  private var state_ : PropertyState[T] = Unresolved
  private var resolutionSteps_ = Seq.empty[ResolutionStep]

  def state: PropertyState[T] = state_
  def isResolved: Boolean = state_.isResolved
  private[tracking] def resolutionSteps: Seq[ResolutionStep] = resolutionSteps_

  def valueOption: Option[T] =
    state_ match {
      case Enforced(value)      => Some(value)
      case Calculated(value, _) => Some(value)
      case _                    => None
    }

  def isDefined: Boolean = valueOption.isDefined
  def get: T = valueOption.get
  def getOrElse[B >: T](default: => B): B = valueOption.getOrElse(default)
  def update(value: T): Unit = enforce(value)

  def enforce(value: T): this.type = {
    state_ match {
      case Unresolved | Calculated(_, _) | Incomplete | Undefined =>
        state_ = Enforced(value)
        resolutionSteps_ = Seq.empty
      case Enforced(_) => ()
    }
    this
  }

  private[tracking] def calculate(
      value: T,
      resolver: Resolver,
      resolutionSteps: Seq[ResolutionStep]
  ): CalculateResult =
    state_ match {
      case Unresolved =>
        state_ = Calculated(value, resolver)
        resolutionSteps_ = resolutionSteps
        CalculateResult.Success
      case Enforced(_)      => CalculateResult.AlreadyEnforced
      case Calculated(_, _) => CalculateResult.AlreadyCalculated
      case Incomplete       => CalculateResult.Incomplete
      case Undefined        => CalculateResult.Undefined
    }

  private[tracking] def markIncomplete(): Unit =
    state_ match {
      case Unresolved | Incomplete => state_ = Incomplete
      case _ =>
        throw new IllegalStateException(
          s"AXI4 property '${key.qualifiedName}' is already resolved as $state_"
        )
    }

  private[tracking] def markUndefined(): Unit =
    state_ match {
      case Unresolved | Undefined => state_ = Undefined
      case _ =>
        throw new IllegalStateException(
          s"AXI4 property '${key.qualifiedName}' is already resolved as $state_"
        )
    }
}

final class PropertyManager private[tracking] () extends Iterable[Property[_]] {
  private val propertiesByKey = mutable.LinkedHashMap.empty[PropertyKey[_], Property[_]]

  def apply[T](key: PropertyKey[T]): Property[T] =
    propertiesByKey
      .getOrElseUpdate(key, new Property(key))
      .asInstanceOf[Property[T]]

  def update[T](key: PropertyKey[T], value: T): Unit =
    apply(key).enforce(value)

  private[tracking] def containsProperty(property: Property[_]): Boolean =
    propertiesByKey.get(property.key).exists(_ eq property)

  override def iterator: Iterator[Property[_]] = propertiesByKey.valuesIterator
}
