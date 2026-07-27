package chext.amba.axi4.tracking

import scala.collection.mutable
import scala.reflect.ClassTag

/** Typed identity and documentation for one AXI tracking property.
  *
  * Keys are normally declared as singleton objects. Object identity, rather than a string lookup,
  * preserves the relationship between the key and its value type `T`. Every key derives from one
  * of the master- or slave-property bases below. Construction registers the key globally and
  * rejects empty metadata or a second key with the same qualified name.
  *
  * @tparam T
  *   value type stored for this property
  * @param name
  *   stable name within the key's property family
  * @param description
  *   nonempty human-readable meaning of the property
  */
sealed abstract class PropertyKey[T: ClassTag](
    val name: String,
    val description: String
) {
  PropertyKey.register(this)

  /** Runtime token used by resolver policies that operate on an aggregate value family. */
  final val valueClass: Class[_] =
    implicitly[ClassTag[T]].runtimeClass

  /** Stable namespace supplied by the key's master/slave base class. */
  private[tracking] def namespace: String

  /** Stable family-qualified name used in diagnostics. */
  final def qualifiedName: String = s"$namespace.$name"
  final override def toString: String = qualifiedName
}

/** Extracts property keys by their runtime value type. */
final class PropertyValueType[T: ClassTag] {
  private val expected = implicitly[ClassTag[T]].runtimeClass

  /** Tests an arbitrary property key without requiring compile-time value-type agreement. */
  def accepts(key: PropertyKey[_]): Boolean =
    key.valueClass == expected

  /** Typed pattern extractor that preserves the key's value type in the matched branch. */
  def unapply(key: PropertyKey[T]): Boolean =
    accepts(key)
}

object PropertyValueType {
  def apply[T: ClassTag]: PropertyValueType[T] =
    new PropertyValueType[T]
}

/** Registry that prevents ambiguous family-qualified property names. */
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

/** Marker shared by property keys that describe AXI read traffic. */
sealed trait ReadProperty

/** Boolean extractor for read-classified property keys.
  *
  * It composes with resolver bindings for extension-key policies. Standard disabled read keys are
  * classified eagerly from the `Master.readProperties` and `Slave.readProperties` catalogs.
  */
object ReadProperty {
  def unapply(key: PropertyKey[_]): Boolean =
    key.isInstanceOf[ReadProperty]
}

/** Marker shared by property keys that describe AXI write traffic. */
sealed trait WriteProperty

/** Boolean extractor for write-classified property keys. */
object WriteProperty {
  def unapply(key: PropertyKey[_]): Boolean =
    key.isInstanceOf[WriteProperty]
}

/** Marker shared by requirements or capabilities originating at an AXI master. */
sealed trait MasterProperty

/** Boolean extractor and internal predicate for master-property keys. */
object MasterProperty {
  def unapply(key: PropertyKey[_]): Boolean = accepts(key)

  private[tracking] def accepts(key: PropertyKey[_]): Boolean =
    key.isInstanceOf[MasterProperty]
}

/** Marker shared by requirements or capabilities originating at an AXI slave. */
sealed trait SlaveProperty

/** Boolean extractor and internal predicate for slave-property keys. */
object SlaveProperty {
  def unapply(key: PropertyKey[_]): Boolean = accepts(key)

  private[tracking] def accepts(key: PropertyKey[_]): Boolean =
    key.isInstanceOf[SlaveProperty]
}

/** Convenience base for master-property keys. */
abstract class MasterPropertyKey[T: ClassTag](name: String, description: String)
    extends PropertyKey[T](name, description)
    with MasterProperty {
  private[tracking] final override def namespace: String = "master"
}

/** Convenience base for slave-property keys. */
abstract class SlavePropertyKey[T: ClassTag](name: String, description: String)
    extends PropertyKey[T](name, description)
    with SlaveProperty {
  private[tracking] final override def namespace: String = "slave"
}

/** Convenience base for master read-property keys. */
abstract class MasterReadProperty[T: ClassTag](name: String, description: String)
    extends MasterPropertyKey[T](name, description)
    with ReadProperty

/** Convenience base for master write-property keys. */
abstract class MasterWriteProperty[T: ClassTag](name: String, description: String)
    extends MasterPropertyKey[T](name, description)
    with WriteProperty

/** Convenience base for slave read-property keys. */
abstract class SlaveReadProperty[T: ClassTag](name: String, description: String)
    extends SlavePropertyKey[T](name, description)
    with ReadProperty

/** Convenience base for slave write-property keys. */
abstract class SlaveWriteProperty[T: ClassTag](name: String, description: String)
    extends SlavePropertyKey[T](name, description)
    with WriteProperty

/** Lifecycle state of one realized [[Property]].
  *
  * `Unresolved` is the only nonterminal state. A property can terminate with an explicit
  * `Enforced` value, a resolver-derived `Calculated` value, or one of the valueless states
  * `DontCare`, `Incomplete`, and `Undefined`.
  */
sealed trait PropertyState[+T] {
  /** Whether no further resolver work is required for this state. */
  def isResolved: Boolean
}

object PropertyState {
  /** No value or terminal classification has been supplied yet. */
  case object Unresolved extends PropertyState[Nothing] {
    val isResolved = false
  }

  /** Marker shared by every terminal property state. */
  sealed trait Resolved[+T] extends PropertyState[T] {
    final val isResolved = true
  }

  /** Explicit value attached by component or user code.
    *
    * Enforcement has precedence over prior calculations and valueless terminal states.
    */
  final case class Enforced[T](value: T) extends Resolved[T]

  /** Value derived by a resolver, retaining its provenance. */
  final case class Calculated[T](value: T, resolver: Resolver) extends Resolved[T]

  /** The property is intentionally outside the information needed from this boundary. */
  final case class DontCare(message: String) extends Resolved[Nothing]

  /** The property applies, but the design did not provide enough information to derive it. */
  case object Incomplete extends Resolved[Nothing]

  /** The property is not defined for this interface or topology. */
  case object Undefined extends Resolved[Nothing]
}

/** Outcome of attempting to calculate a property.
  *
  * Calculation never overwrites a terminal state. The rejection cases expose which state prevented
  * the write, allowing resolver code and tests to distinguish explicit enforcement from an earlier
  * derivation or a valueless terminal classification.
  */
sealed trait CalculateResult {
  /** Whether the calculated value was stored. */
  def isSuccess: Boolean
}

object CalculateResult {
  /** The property was unresolved and now contains the calculated value. */
  case object Success extends CalculateResult {
    val isSuccess = true
  }

  /** A terminal state prevented calculation. */
  sealed trait Rejected extends CalculateResult {
    final val isSuccess = false
  }

  /** An explicitly enforced value already exists. */
  case object AlreadyEnforced extends Rejected

  /** A resolver-calculated value already exists. */
  case object AlreadyCalculated extends Rejected

  /** The property was already classified as intentionally unnecessary. */
  final case class DontCare(message: String) extends Rejected

  /** The property was already marked incomplete. */
  case object Incomplete extends Rejected

  /** The property was already marked undefined. */
  case object Undefined extends Rejected
}

/** Realized mutable state for one typed property key on one AXI interface.
  *
  * Instances are created and owned by [[PropertyManager]]. User and component code normally calls
  * [[enforce]] (or manager assignment) to provide authoritative facts. Resolver-only transitions
  * are package-private and are exposed safely through [[ResolveRequest]].
  *
  * @tparam T
  *   property value type fixed by `key`
  */
final class Property[T] private[tracking] (val key: PropertyKey[T]) {
  import PropertyState._

  private var state_ : PropertyState[T] = Unresolved
  private var resolutionSteps_ = Seq.empty[ResolutionStep]

  /** Current lifecycle state. */
  def state: PropertyState[T] = state_

  /** Whether this property is in any terminal state. */
  def isResolved: Boolean = state_.isResolved

  /** Trace recorded for a calculated value. */
  private[tracking] def resolutionSteps: Seq[ResolutionStep] = resolutionSteps_

  /** Returns the value of enforced and calculated states.
    *
    * Unresolved, don't-care, incomplete, and undefined properties return `None`.
    */
  def valueOption: Option[T] =
    state_ match {
      case Enforced(value)      => Some(value)
      case Calculated(value, _) => Some(value)
      case _                    => None
    }

  /** Whether this property has a value. */
  def isDefined: Boolean = valueOption.isDefined

  /** Returns the current value or throws `NoSuchElementException` for a valueless state. */
  def get: T = valueOption.get

  /** Returns the current value, or lazily evaluates `default` for a valueless state. */
  def getOrElse[B >: T](default: => B): B = valueOption.getOrElse(default)

  /** Assignment-style alias for [[enforce]]. */
  def update(value: T): Unit = enforce(value)

  /** Stores an authoritative value.
    *
    * Enforcement may replace a calculated, don't-care, incomplete, or undefined state and clears
    * any resolution trace. Re-enforcing an equal value is idempotent; re-enforcing a different
    * authoritative value is an error.
    *
    * @return
    *   this property, allowing calls to be chained
    */
  def enforce(value: T): this.type = {
    state_ match {
      case Unresolved | Calculated(_, _) | DontCare(_) | Incomplete | Undefined =>
        state_ = Enforced(value)
        resolutionSteps_ = Seq.empty
      case Enforced(existing) if existing == value => ()
      case Enforced(existing) =>
        throw new IllegalStateException(
          s"AXI4 property '${key.qualifiedName}' is already enforced as $existing; " +
            s"cannot enforce conflicting value $value"
        )
    }
    this
  }

  /** Applies one controlled field update and leaves this property authoritatively enforced.
    *
    * Aggregate-value companions use this operation to provide assignment syntax without exposing
    * public mutable fields. Copying before mutation prevents a value shared by multiple properties
    * from being modified through the wrong owner.
    */
  private[tracking] def mutate(
      default: => T,
      copyValue: T => T
  )(mutation: T => Unit): Unit = {
    val value =
      state_ match {
        case Enforced(existing)      => copyValue(existing)
        case Calculated(existing, _) => copyValue(existing)
        case Unresolved | DontCare(_) | Incomplete | Undefined => default
      }

    mutation(value)
    state_ = Enforced(value)
    resolutionSteps_ = Seq.empty
  }

  /** Attempts the resolver-only transition from unresolved to calculated. */
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
      case Enforced(_)       => CalculateResult.AlreadyEnforced
      case Calculated(_, _)  => CalculateResult.AlreadyCalculated
      case DontCare(message) => CalculateResult.DontCare(message)
      case Incomplete        => CalculateResult.Incomplete
      case Undefined         => CalculateResult.Undefined
    }

  /** Performs the resolver-only transition to an intentional don't-care state. */
  private[tracking] def markDontCare(message: String): Unit = {
    require(message.nonEmpty, "AXI4 property DontCare needs an explanatory message")
    state_ match {
      case Unresolved  => state_ = DontCare(message)
      case DontCare(_) => ()
      case _ =>
        throw new IllegalStateException(
          s"AXI4 property '${key.qualifiedName}' is already resolved as $state_"
        )
    }
  }

  /** Performs the resolver-only transition to incomplete. */
  private[tracking] def markIncomplete(): Unit =
    state_ match {
      case Unresolved | Incomplete => state_ = Incomplete
      case _ =>
        throw new IllegalStateException(
          s"AXI4 property '${key.qualifiedName}' is already resolved as $state_"
        )
    }

  /** Performs the resolver-only transition to undefined. */
  private[tracking] def markUndefined(): Unit =
    state_ match {
      case Unresolved | Undefined => state_ = Undefined
      case _ =>
        throw new IllegalStateException(
          s"AXI4 property '${key.qualifiedName}' is already resolved as $state_"
        )
    }
}

/** Lazily realized heterogeneous property store for one AXI interface.
  *
  * The manager localizes the one type cast needed for heterogeneous storage. A [[PropertyKey]]
  * carries its value type, and [[apply]] always returns the unique [[Property]] associated with that
  * exact key object. Iteration preserves first-realization order.
  */
final class PropertyManager private[tracking] () extends Iterable[Property[_]] {
  private val propertiesByKey = mutable.LinkedHashMap.empty[PropertyKey[_], Property[_]]

  /** Returns the unique property for `key`, creating its unresolved state on first access. */
  def apply[T](key: PropertyKey[T]): Property[T] =
    propertiesByKey
      .getOrElseUpdate(key, new Property(key))
      .asInstanceOf[Property[T]]

  /** Enforces `value` for `key`, enabling `manager(key) = value` syntax. */
  def update[T](key: PropertyKey[T], value: T): Unit =
    apply(key).enforce(value)

  /** Marks one property as statically undefined.
    *
    * This declaration form is intended for component construction, when applicability is known
    * without dependency resolution.
    */
  def markUndefined(key: PropertyKey[_]): this.type = {
    apply(key).markUndefined()
    this
  }

  /** Marks every property in `keys` as statically undefined. */
  def markUndefined(keys: Iterable[PropertyKey[_]]): this.type = {
    keys.foreach(key => apply(key).markUndefined())
    this
  }

  /** Tests ownership by realized property identity. */
  private[tracking] def containsProperty(property: Property[_]): Boolean =
    propertiesByKey.get(property.key).exists(_ eq property)

  /** Iterates realized properties in first-access order. */
  override def iterator: Iterator[Property[_]] = propertiesByKey.valuesIterator
}
