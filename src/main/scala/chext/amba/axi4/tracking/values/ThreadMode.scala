package chext.amba.axi4.tracking.values

import chext.amba.axi4

/** Constraint on the IDs of simultaneously outstanding transactions. */
sealed trait ThreadMode

object ThreadMode {
  case object SingleTransaction extends ThreadMode
  case object SingleThread extends ThreadMode
  case object UniqueThreads extends ThreadMode
  case object Unconstrained extends ThreadMode

  /** Every thread-mode value in stable declaration order. */
  val values: Seq[ThreadMode] =
    Seq(SingleTransaction, SingleThread, UniqueThreads, Unconstrained)

  /** Combines several slave thread-mode properties into one slave property.
    *
    * The combined property accepts an initiated transaction pattern only when every supplied slave
    * property accepts that pattern. For example, combining `SingleThread` and `UniqueThreads`
    * yields `SingleTransaction`, because that is the broadest pattern accepted by both properties.
    */
  private[axi4] def intersect(slaves: Seq[ThreadMode]): ThreadMode = {
    require(slaves.nonEmpty, "ThreadMode.intersect needs at least one value")
    val accepted =
      values.filter(master => slaves.forall(slave => isCompatible(master, slave))).toSet
    values
      .find(slave => values.filter(master => isCompatible(master, slave)).toSet == accepted)
      .getOrElse(
        throw new IllegalArgumentException(
          s"ThreadMode intersection is not representable: ${slaves.mkString(", ")}"
        )
      )
  }

  /** Computes the master property after a boundary removes all ID bits.
    *
    * The master property before the boundary describes the transactions that may be initiated.
    * After ID removal, all of those transactions have the same implicit ID. Therefore:
    *
    *   - `SingleTransaction` stays `SingleTransaction`;
    *   - every mode that may have several outstanding transactions becomes `SingleThread`.
    */
  private[axi4] def idlessForward(master: ThreadMode): ThreadMode =
    master match {
      case SingleTransaction => SingleTransaction
      case _                 => SingleThread
    }

  /** Computes the slave property before a boundary that removes all ID bits.
    *
    * After ID removal, every accepted transaction has the same implicit ID. Therefore:
    *
    *   - if the slave property after the boundary accepts `SingleThread`, the slave property before
    *     it may be `Unconstrained`;
    *   - otherwise, the slave property before it must be `SingleTransaction`.
    *
    * A slave property accepts `SingleThread` when its mode is `SingleThread` or `Unconstrained`.
    * This propagates an acceptance constraint toward the initiating master; it does not restore the
    * removed IDs.
    */
  private[axi4] def idlessBackward(slave: ThreadMode): ThreadMode =
    slave match {
      case SingleThread | Unconstrained       => Unconstrained
      case SingleTransaction | UniqueThreads  => SingleTransaction
    }

  /** Thread modes supported by an interface configuration. */
  def supportedModesFor(cfg: axi4.Config): Seq[ThreadMode] =
    if (cfg.lite) Seq(SingleTransaction, SingleThread) else values

  /** Most permissive thread mode supported by an interface configuration. */
  def all(cfg: axi4.Config): ThreadMode =
    if (cfg.lite) SingleThread else Unconstrained

  /** Returns the canonical form of a thread mode.
    *
    * Thread modes have no redundant representation, so normalization preserves the value.
    */
  def normalize(value: ThreadMode): ThreadMode = value

  /** Checks whether a thread mode is valid for an interface configuration.
    *
    * AXI4-Lite has one implicit ID, so it can express only a single outstanding transaction or
    * multiple outstanding transactions in one thread.
    */
  def checkConfig(value: ThreadMode, cfg: axi4.Config): CheckResult =
    CheckResult.from(Seq(
      Option.when(
        cfg.lite &&
          value != SingleTransaction &&
          value != SingleThread
      )(
        s"AXI4-Lite thread mode $value is invalid; expected SingleTransaction or SingleThread"
      )
    ).flatten)

  private def isCompatible(master: ThreadMode, slave: ThreadMode): Boolean =
    (master, slave) match {
      case (SingleTransaction, _)                         => true
      case (SingleThread, SingleThread | Unconstrained)   => true
      case (UniqueThreads, UniqueThreads | Unconstrained) => true
      case (Unconstrained, Unconstrained)                 => true
      case _                                              => false
    }

  /** Checks whether every transaction pattern described by a master property is accepted by a
    * slave property.
    */
  def checkCompatible(master: ThreadMode, slave: ThreadMode): CheckResult =
    CheckResult.from(
      Option
        .when(!isCompatible(master, slave))(
          s"thread modes are incompatible: master=$master, slave=$slave"
        )
        .toSeq
    )
}
