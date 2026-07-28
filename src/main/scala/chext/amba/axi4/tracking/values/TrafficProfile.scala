package chext.amba.axi4.tracking.values

/** Placeholder aggregate for future concurrency and latency checking. */
final case class TrafficProfile(
    outstandingTransactions: Int = 0,
    threads: Int = 0,
    latencyCycles: Option[Double] = None
)

object TrafficProfile {
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
}
