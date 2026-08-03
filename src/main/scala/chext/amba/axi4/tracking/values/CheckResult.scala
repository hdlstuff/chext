package chext.amba.axi4.tracking.values

/** Result of checking an AXI tracking value.
  *
  * A successful check has no errors. A failed check retains every error found so callers can report
  * all problems together.
  */
sealed trait CheckResult {
  def isSuccess: Boolean
  def errors: Seq[String]
}

object CheckResult {
  case object Success extends CheckResult {
    override val isSuccess: Boolean = true
    override val errors: Seq[String] = Seq.empty
  }

  final case class Error(errors: Seq[String]) extends CheckResult {
    require(errors.nonEmpty, "CheckResult.Error needs at least one error")
    override val isSuccess: Boolean = false
  }

  /** Creates a successful result for an empty sequence and an error result otherwise. */
  def from(errors: Seq[String]): CheckResult =
    if (errors.isEmpty) Success else Error(errors)
}
