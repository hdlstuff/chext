package chext.util

import chisel3._

/** Simulation-time diagnostic action. */
sealed trait SimulationCheck {
  def apply(cond: Bool, message: String): Unit =
    SimulationCheck(this, cond, message)
}

object SimulationCheck {
  case object Default extends SimulationCheck
  case object None extends SimulationCheck
  case object Printf extends SimulationCheck
  case object Assert extends SimulationCheck

  private var defaultPolicy: SimulationCheck = Printf

  /** Current process-wide default policy. */
  def default: SimulationCheck = defaultPolicy

  def setDefault(policy: SimulationCheck): Unit = {
    require(
      policy != Default,
      "Default cannot be the global simulation check policy"
    )
    defaultPolicy = policy
  }

  def resolve(policy: SimulationCheck): SimulationCheck =
    policy match {
      case Default => defaultPolicy
      case other   => other
    }

  def apply(
      policy: SimulationCheck,
      cond: Bool,
      message: String
  ): Unit =
    resolve(policy) match {
      case None =>
      case Printf =>
        when(!cond) {
          printf(message + "\n")
        }
      case Assert =>
        assert(cond, message)
      case Default =>
        throw new IllegalStateException("unresolved simulation check policy")
    }
}
