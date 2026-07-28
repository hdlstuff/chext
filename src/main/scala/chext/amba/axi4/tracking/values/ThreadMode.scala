package chext.amba.axi4.tracking.values

import chext.amba.axi4

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
      case (SingleTransaction, _)                         => true
      case (SingleThread, SingleThread | Unconstrained)   => true
      case (UniqueThreads, UniqueThreads | Unconstrained) => true
      case (Unconstrained, Unconstrained)                 => true
      case _                                              => false
    }
}
