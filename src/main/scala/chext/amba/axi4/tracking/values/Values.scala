package chext.amba.axi4.tracking.values

import chext.amba.axi4
import chext.amba.axi4.BurstType.Encoding.{FIXED, INCR, WRAP}
import chext.amba.axi4.tracking.Property

/** Burst-related limits and capabilities carried by one read or write property. */
final class BurstShape private[tracking] (
    private var len_ : Int,
    private var tpe_ : Seq[Int],
    private var size_ : Seq[Int],
    private var align_ : Int
) {
  def len: Int = len_
  def tpe: Seq[Int] = tpe_
  def size: Seq[Int] = size_
  def align: Int = align_

  private[tracking] def copyValue(): BurstShape =
    new BurstShape(len_, tpe_, size_, align_)

  private[tracking] def len_=(value: Int): Unit = len_ = value
  private[tracking] def tpe_=(value: Seq[Int]): Unit =
    tpe_ = BurstShape.normalize(value)
  private[tracking] def size_=(value: Seq[Int]): Unit =
    size_ = BurstShape.normalize(value)
  private[tracking] def align_=(value: Int): Unit = align_ = value

  override def equals(other: Any): Boolean =
    other match {
      case that: BurstShape =>
        len == that.len &&
          tpe == that.tpe &&
          size == that.size &&
          align == that.align
      case _ => false
    }

  override def hashCode(): Int =
    (len, tpe, size, align).##

  override def toString: String =
    s"BurstShape(len=$len, tpe=$tpe, size=$size, align=$align)"
}

object BurstShape {
  private def normalize(values: Seq[Int]): Seq[Int] =
    values.distinct.sorted

  /** Creates a complete burst shape.
    *
    * `align` is the base-2 logarithm of the minimum byte alignment of every transaction's starting
    * address. For example, `3` means 8-byte alignment. On a master property it is a guarantee; on
    * a slave property it is a requirement.
    *
    * Defaults describe an empty stream or capability.
    */
  def apply(
      len: Int = 0,
      tpe: Seq[Int] = Seq.empty,
      size: Seq[Int] = Seq.empty,
      align: Int = 0
  ): BurstShape =
    new BurstShape(len, normalize(tpe), normalize(size), align)

  def unapply(value: BurstShape): Option[(Int, Seq[Int], Seq[Int], Int)] =
    Some((value.len, value.tpe, value.size, value.align))

  /** AXI transfer-size encoding for one full-width beat. */
  def fullSize(wData: Int): Int =
    Integer.numberOfTrailingZeros(wData / 8)

  /** Transfer-size encodings representable on an interface. */
  def validSizes(wData: Int): Seq[Int] =
    0 to fullSize(wData)

  /** Returns every internal-consistency or interface-specific validation problem. */
  def validationErrors(value: BurstShape, cfg: axi4.Config): Seq[String] = {
    val isEmpty =
      value.len == 0 && value.tpe.isEmpty && value.size.isEmpty && value.align == 0
    val protocolMaxBeats =
      if (cfg.lite) 1 else if (cfg.axi3Compat) 16 else 256
    val protocolTypes = if (cfg.lite) Set(INCR) else Set(FIXED, INCR, WRAP)
    val interfaceSizes =
      if (cfg.lite) Set(fullSize(cfg.wData)) else validSizes(cfg.wData)

    if (isEmpty)
      Seq.empty
    else
      Seq(
        Option.when(value.len <= 0)(
          s"len must be positive for a non-empty shape (got ${value.len})"
        ),
        Option.when(value.len > protocolMaxBeats)(
          s"len ${value.len} exceeds the protocol limit $protocolMaxBeats"
        ),
        Option.when(value.tpe.isEmpty)(
          "tpe must not be empty for a non-empty shape"
        ),
        Option.when(value.size.isEmpty)(
          "size must not be empty for a non-empty shape"
        ),
        Option.when(value.align < 0)(
          s"align must not be negative (got ${value.align})"
        ),
        Option.when(value.align > cfg.wAddr)(
          s"align ${value.align} exceeds address width ${cfg.wAddr}"
        ),
        Option.when(!value.tpe.toSet.subsetOf(protocolTypes))(
          s"tpe ${value.tpe} contains encodings outside $protocolTypes"
        ),
        Option.when(!value.size.toSet.subsetOf(interfaceSizes.toSet))(
          s"size ${value.size} contains encodings outside $interfaceSizes"
        )
      ).flatten
  }

  /** Returns all reasons why a master burst shape exceeds a slave capability. */
  def compatibilityErrors(master: BurstShape, slave: BurstShape): Seq[String] =
    Seq(
      Option.when(master.len > slave.len)(
        s"master len ${master.len} exceeds slave len ${slave.len}"
      ),
      Option.when(!master.tpe.toSet.subsetOf(slave.tpe.toSet))(
        s"master tpe ${master.tpe} is not a subset of slave tpe ${slave.tpe}"
      ),
      Option.when(!master.size.toSet.subsetOf(slave.size.toSet))(
        s"master size ${master.size} is not a subset of slave size ${slave.size}"
      ),
      Option.when(master.len > 0 && master.align < slave.align)(
        s"master align ${master.align} does not meet slave align ${slave.align}"
      )
    ).flatten

  /** Field-oriented syntax for `Property[BurstShape]`. */
  implicit final class PropertyOps(private val property: Property[BurstShape]) extends AnyVal {
    private def current: BurstShape =
      property.valueOption.getOrElse(BurstShape())

    def len: Int = current.len
    def tpe: Seq[Int] = current.tpe
    def size: Seq[Int] = current.size
    def align: Int = current.align

    def len_=(value: Int): Unit =
      property.mutate(BurstShape(), _.copyValue())(_.len_ = value)

    def tpe_=(value: Seq[Int]): Unit =
      property.mutate(BurstShape(), _.copyValue())(_.tpe_ = value)

    def size_=(value: Seq[Int]): Unit =
      property.mutate(BurstShape(), _.copyValue())(_.size_ = value)

    def align_=(value: Int): Unit =
      property.mutate(BurstShape(), _.copyValue())(_.align_ = value)
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
