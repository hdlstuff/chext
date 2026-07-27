package chext.amba.axi4.tracking.values

import chext.amba.axi4
import chext.amba.axi4.tracking.Property

/** Burst-related limits and capabilities carried by one read or write property. */
final class BurstShape private[tracking] (
    private var burstBeats_ : Int,
    private var burstNarrow_ : Boolean,
    private var burstTypes_ : Set[Int],
    private var burstSizes_ : Set[Int]
) {
  def burstBeats: Int = burstBeats_
  def burstNarrow: Boolean = burstNarrow_
  def burstTypes: Set[Int] = burstTypes_
  def burstSizes: Set[Int] = burstSizes_

  private[tracking] def copyValue(): BurstShape =
    new BurstShape(burstBeats_, burstNarrow_, burstTypes_, burstSizes_)

  private[tracking] def burstBeats_=(value: Int): Unit = burstBeats_ = value
  private[tracking] def burstNarrow_=(value: Boolean): Unit = burstNarrow_ = value
  private[tracking] def burstTypes_=(value: Set[Int]): Unit = burstTypes_ = value
  private[tracking] def burstSizes_=(value: Set[Int]): Unit = burstSizes_ = value

  override def equals(other: Any): Boolean =
    other match {
      case that: BurstShape =>
        burstBeats == that.burstBeats &&
          burstNarrow == that.burstNarrow &&
          burstTypes == that.burstTypes &&
          burstSizes == that.burstSizes
      case _ => false
    }

  override def hashCode(): Int =
    (burstBeats, burstNarrow, burstTypes, burstSizes).##

  override def toString: String =
    s"BurstShape(burstBeats=$burstBeats, burstNarrow=$burstNarrow, " +
      s"burstTypes=$burstTypes, burstSizes=$burstSizes)"
}

object BurstShape {
  private val fixed = axi4.BurstType.FIXED.litValue.toInt
  private val incr = axi4.BurstType.INCR.litValue.toInt
  private val wrap = axi4.BurstType.WRAP.litValue.toInt

  /** Creates a complete burst shape. Defaults describe an empty stream or capability. */
  def apply(
      burstBeats: Int = 0,
      burstNarrow: Boolean = false,
      burstTypes: Set[Int] = Set.empty,
      burstSizes: Set[Int] = Set.empty
  ): BurstShape =
    new BurstShape(burstBeats, burstNarrow, burstTypes, burstSizes)

  def unapply(value: BurstShape): Option[(Int, Boolean, Set[Int], Set[Int])] =
    Some((value.burstBeats, value.burstNarrow, value.burstTypes, value.burstSizes))

  /** AXI transfer-size encoding for one full-width beat. */
  def fullSize(wData: Int): Int =
    Integer.numberOfTrailingZeros(wData / 8)

  /** Transfer-size encodings representable on an interface. */
  def validSizes(wData: Int): Set[Int] =
    (0 to fullSize(wData)).toSet

  /** Returns every internal-consistency or interface-specific validation problem. */
  def validationErrors(value: BurstShape, cfg: axi4.Config): Seq[String] = {
    val isEmpty =
      value.burstBeats == 0 && !value.burstNarrow &&
        value.burstTypes.isEmpty && value.burstSizes.isEmpty
    val protocolMaxBeats =
      if (cfg.lite) 1 else if (cfg.axi3Compat) 16 else 256
    val protocolTypes = if (cfg.lite) Set(incr) else Set(fixed, incr, wrap)
    val interfaceSizes =
      if (cfg.lite) Set(fullSize(cfg.wData)) else validSizes(cfg.wData)
    val expectedNarrow =
      value.burstSizes.exists(_ < fullSize(cfg.wData))

    if (isEmpty)
      Seq.empty
    else
      Seq(
        Option.when(value.burstBeats <= 0)(
          s"burstBeats must be positive for a non-empty shape (got ${value.burstBeats})"
        ),
        Option.when(value.burstBeats > protocolMaxBeats)(
          s"burstBeats ${value.burstBeats} exceeds the protocol limit $protocolMaxBeats"
        ),
        Option.when(value.burstTypes.isEmpty)(
          "burstTypes must not be empty for a non-empty shape"
        ),
        Option.when(value.burstSizes.isEmpty)(
          "burstSizes must not be empty for a non-empty shape"
        ),
        Option.when(!value.burstTypes.subsetOf(protocolTypes))(
          s"burstTypes ${value.burstTypes} contain encodings outside $protocolTypes"
        ),
        Option.when(!value.burstSizes.subsetOf(interfaceSizes))(
          s"burstSizes ${value.burstSizes} contain encodings outside $interfaceSizes"
        ),
        Option.when(value.burstNarrow != expectedNarrow)(
          s"burstNarrow ${value.burstNarrow} does not agree with burstSizes " +
            s"${value.burstSizes} at data width ${cfg.wData}"
        )
      ).flatten
  }

  /** Returns all reasons why a master burst shape exceeds a slave capability. */
  def compatibilityErrors(master: BurstShape, slave: BurstShape): Seq[String] =
    Seq(
      Option.when(master.burstBeats > slave.burstBeats)(
        s"master burstBeats ${master.burstBeats} exceeds slave burstBeats ${slave.burstBeats}"
      ),
      Option.when(master.burstNarrow && !slave.burstNarrow)(
        "master may generate narrow bursts but slave does not accept them"
      ),
      Option.when(!master.burstTypes.subsetOf(slave.burstTypes))(
        s"master burstTypes ${master.burstTypes} are not a subset of slave burstTypes ${slave.burstTypes}"
      ),
      Option.when(!master.burstSizes.subsetOf(slave.burstSizes))(
        s"master burstSizes ${master.burstSizes} are not a subset of slave burstSizes ${slave.burstSizes}"
      )
    ).flatten

  /** Field-oriented syntax for `Property[BurstShape]`. */
  implicit final class PropertyOps(private val property: Property[BurstShape]) extends AnyVal {
    private def current: BurstShape =
      property.valueOption.getOrElse(BurstShape())

    def burstBeats: Int = current.burstBeats
    def burstNarrow: Boolean = current.burstNarrow
    def burstTypes: Set[Int] = current.burstTypes
    def burstSizes: Set[Int] = current.burstSizes

    def burstBeats_=(value: Int): Unit =
      property.mutate(BurstShape(), _.copyValue())(_.burstBeats_ = value)

    def burstNarrow_=(value: Boolean): Unit =
      property.mutate(BurstShape(), _.copyValue())(_.burstNarrow_ = value)

    def burstTypes_=(value: Set[Int]): Unit =
      property.mutate(BurstShape(), _.copyValue())(_.burstTypes_ = value)

    def burstSizes_=(value: Set[Int]): Unit =
      property.mutate(BurstShape(), _.copyValue())(_.burstSizes_ = value)
  }
}

/** Constraint on the IDs of simultaneously outstanding transactions. */
sealed trait ThreadMode

object ThreadMode {
  case object SingleTransaction extends ThreadMode
  case object SingleThread extends ThreadMode
  case object UniqueThreads extends ThreadMode
  case object Unconstrained extends ThreadMode

  /** Returns configuration-specific validity errors for one thread mode.
    *
    * AXI4-Lite has one implicit ID, so it can express only a single outstanding transaction or
    * multiple outstanding transactions in one thread.
    */
  def validationErrors(value: ThreadMode, cfg: axi4.Config): Seq[String] =
    Seq(
      Option.when(
        cfg.lite &&
          value != SingleTransaction &&
          value != SingleThread
      )(
        s"AXI4-Lite thread mode $value is invalid; expected SingleTransaction or SingleThread"
      )
    ).flatten

  /** Whether a master's guarantee is accepted by a slave's supported mode. */
  def compatible(master: ThreadMode, slave: ThreadMode): Boolean =
    (master, slave) match {
      case (SingleTransaction, _) => true
      case (SingleThread, SingleThread | Unconstrained) => true
      case (UniqueThreads, UniqueThreads | Unconstrained) => true
      case (Unconstrained, Unconstrained) => true
      case _ => false
    }
}

/** Placeholder aggregate for future concurrency and latency checking. */
final class TrafficProfile private[tracking] (
    private var outstandingTransactions_ : Int,
    private var threads_ : Int,
    private var latencyCycles_ : Option[Double]
) {
  def outstandingTransactions: Int = outstandingTransactions_
  def threads: Int = threads_
  def latencyCycles: Option[Double] = latencyCycles_

  private[tracking] def copyValue(): TrafficProfile =
    new TrafficProfile(outstandingTransactions_, threads_, latencyCycles_)

  private[tracking] def outstandingTransactions_=(value: Int): Unit =
    outstandingTransactions_ = value
  private[tracking] def threads_=(value: Int): Unit = threads_ = value
  private[tracking] def latencyCycles_=(value: Option[Double]): Unit =
    latencyCycles_ = value

  override def equals(other: Any): Boolean =
    other match {
      case that: TrafficProfile =>
        outstandingTransactions == that.outstandingTransactions &&
          threads == that.threads &&
          latencyCycles == that.latencyCycles
      case _ => false
    }

  override def hashCode(): Int =
    (outstandingTransactions, threads, latencyCycles).##

  override def toString: String =
    s"TrafficProfile(outstandingTransactions=$outstandingTransactions, " +
      s"threads=$threads, latencyCycles=$latencyCycles)"
}

object TrafficProfile {
  def apply(
      outstandingTransactions: Int = 0,
      threads: Int = 0,
      latencyCycles: Option[Double] = None
  ): TrafficProfile =
    new TrafficProfile(outstandingTransactions, threads, latencyCycles)

  def unapply(value: TrafficProfile): Option[(Int, Int, Option[Double])] =
    Some((value.outstandingTransactions, value.threads, value.latencyCycles))

  def validationErrors(value: TrafficProfile): Seq[String] =
    Seq(
      Option.when(value.outstandingTransactions < 0)(
        "outstandingTransactions must not be negative"
      ),
      Option.when(value.threads < 0)("threads must not be negative"),
      Option.when(value.latencyCycles.exists(x => x.isNaN || x.isInfinity || x < 0.0))(
        "latencyCycles must be finite and non-negative"
      )
    ).flatten

  implicit final class PropertyOps(private val property: Property[TrafficProfile])
      extends AnyVal {
    private def current: TrafficProfile =
      property.valueOption.getOrElse(TrafficProfile())

    def outstandingTransactions: Int = current.outstandingTransactions
    def threads: Int = current.threads
    def latencyCycles: Option[Double] = current.latencyCycles

    def outstandingTransactions_=(value: Int): Unit =
      property.mutate(TrafficProfile(), _.copyValue())(
        _.outstandingTransactions_ = value
      )

    def threads_=(value: Int): Unit =
      property.mutate(TrafficProfile(), _.copyValue())(_.threads_ = value)

    def latencyCycles_=(value: Option[Double]): Unit =
      property.mutate(TrafficProfile(), _.copyValue())(_.latencyCycles_ = value)
  }
}
