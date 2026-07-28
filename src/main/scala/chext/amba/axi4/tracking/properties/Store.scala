package chext.amba.axi4.tracking.properties

import scala.collection.mutable

import chext.amba.axi4.tracking.{ResolutionStep, Resolver}

/** Lifecycle state of one realized [[Cell]]. */
sealed trait State[+T] {
  def isResolved: Boolean
}

object State {
  case object Unresolved extends State[Nothing] {
    val isResolved = false
  }

  sealed trait Resolved[+T] extends State[T] {
    final val isResolved = true
  }

  final case class Enforced[T](value: T) extends Resolved[T]
  final case class Calculated[T](value: T, resolver: Resolver) extends Resolved[T]
  final case class DontCare(message: String) extends Resolved[Nothing]
  case object Incomplete extends Resolved[Nothing]
  case object Undefined extends Resolved[Nothing]
}

/** Outcome of attempting to calculate a property. */
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
  final case class DontCare(message: String) extends Rejected
  case object Incomplete extends Rejected
  case object Undefined extends Rejected
}

/** Realized mutable state for one typed property key on one tracked interface. */
final class Cell[T] private[tracking] (val key: Key[T]) {
  import State._

  private var state_ : State[T] = Unresolved
  private var resolutionSteps_ = Seq.empty[ResolutionStep]

  def state: State[T] = state_
  def isResolved: Boolean = state_.isResolved

  private[tracking] def resolutionSteps: Seq[ResolutionStep] =
    resolutionSteps_

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

/** Heterogeneous property store for one tracked AXI interface. */
final class Manager private[tracking] () extends Iterable[Cell[_]] {
  private val propertiesByKey =
    mutable.LinkedHashMap.empty[Key[_], Cell[_]]

  private def cellFor[T](key: Key[T]): Cell[T] =
    propertiesByKey
      .getOrElseUpdate(key, new Cell(key))
      .asInstanceOf[Cell[T]]

  def apply[T](key: Key[T]): Cell[T] =
    cellFor(key)

  def update[T](key: Key[T], value: T): Unit =
    cellFor(key).enforce(value)

  def select(selector: Selector): Seq[Cell[_]] =
    selectableKeys.filter(selector.accepts).map(cellFor(_))

  def select[T](valueType: ValueType[T]): Seq[Cell[T]] =
    selectableKeys
      .filter(valueType.accepts)
      .map(key => cellFor(key.asInstanceOf[Key[T]]))

  def markUndefined(key: Key[_]): this.type = {
    cellFor(key).markUndefined()
    this
  }

  def markUndefined(keys: Iterable[Key[_]]): this.type = {
    keys.foreach(key => cellFor(key).markUndefined())
    this
  }

  override def iterator: Iterator[Cell[_]] =
    propertiesByKey.valuesIterator

  private def selectableKeys: Seq[Key[_]] =
    (KnownKeys.iterator ++ propertiesByKey.keysIterator).toSeq.distinct
}
