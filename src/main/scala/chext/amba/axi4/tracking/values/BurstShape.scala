package chext.amba.axi4.tracking.values

import chext.amba.axi4
import chext.amba.axi4.BurstType.Encoding.{FIXED, INCR, WRAP}

/** Burst-related limits and capabilities carried by one read or write property. */
final case class BurstShape private (
    maxBeats: Int,
    types: Seq[Int],
    sizes: Seq[Int],
    aligned: Boolean
) {

  /** Copies this shape while preserving normalized sequence fields. */
  def copy(
      maxBeats: Int = this.maxBeats,
      types: Seq[Int] = this.types,
      sizes: Seq[Int] = this.sizes,
      aligned: Boolean = this.aligned
  ): BurstShape =
    BurstShape(maxBeats, types, sizes, aligned)
}

object BurstShape {
  private def normalizeSeq(values: Seq[Int]): Seq[Int] =
    values.distinct.sorted

  /** Combines several slave burst-shape properties into one slave property.
    *
    * The combined property accepts exactly the transactions accepted by every supplied slave
    * property: its beat limit is the smallest limit, its burst types and transfer sizes are the
    * common sets, and it requires natural alignment when any supplied property does. If no
    * transaction can be accepted, the result is the canonical empty shape.
    */
  private[axi4] def intersect(values: Seq[BurstShape]): BurstShape = {
    require(values.nonEmpty, "BurstShape.intersect needs at least one value")

    val maxBeats = values.map(_.maxBeats).min
    val types = values.map(_.types.toSet).reduce(_ intersect _).toSeq
    val sizes = values.map(_.sizes.toSet).reduce(_ intersect _).toSeq

    if (maxBeats <= 0 || types.isEmpty || sizes.isEmpty)
      BurstShape()
    else
      BurstShape(
        maxBeats = maxBeats,
        types = types,
        sizes = sizes,
        aligned = values.exists(_.aligned)
      )
  }

  /** Creates a complete burst shape.
    *
    * `aligned` describes natural transfer alignment: when true, every transaction satisfies `AxADDR
    * % (1 << AxSIZE) == 0`. On a master property, false means transactions may be unaligned. On a
    * slave property, false means unaligned transactions are accepted; it does not mean that
    * transactions must be unaligned.
    *
    * Defaults describe an empty stream or capability.
    */
  def apply(
      maxBeats: Int = 0,
      types: Seq[Int] = Seq.empty,
      sizes: Seq[Int] = Seq.empty,
      aligned: Boolean = false
  ): BurstShape =
    normalize(new BurstShape(maxBeats, types, sizes, aligned))

  /** Returns the canonical form of a burst shape.
    *
    * Burst-type and transfer-size encodings are deduplicated and sorted. The remaining fields are
    * already canonical and are preserved.
    */
  def normalize(value: BurstShape): BurstShape =
    new BurstShape(
      value.maxBeats,
      normalizeSeq(value.types),
      normalizeSeq(value.sizes),
      value.aligned
    )

  /** AXI transfer-size encoding for one full-width beat. */
  def fullSize(wData: Int): Int =
    Integer.numberOfTrailingZeros(wData / 8)

  /** Burst-type encodings supported by an interface's protocol mode. */
  def supportedTypesFor(cfg: axi4.Config): Seq[Int] =
    if (cfg.lite) Seq(INCR) else Seq(FIXED, INCR, WRAP)

  /** Transfer-size encodings supported by an interface's protocol mode and data width. */
  def supportedSizesFor(cfg: axi4.Config): Seq[Int] =
    if (cfg.lite) Seq(fullSize(cfg.wData)) else 0 to fullSize(cfg.wData)

  /** Maximum number of beats supported by an interface's protocol mode. */
  def maxBeatsFor(cfg: axi4.Config): Int =
    if (cfg.lite) 1 else if (cfg.axi3Compat) 16 else 256

  /** Most permissive burst shape supported by an interface configuration. */
  def all(cfg: axi4.Config): BurstShape =
    BurstShape(
      maxBeats = maxBeatsFor(cfg),
      types = supportedTypesFor(cfg),
      sizes = supportedSizesFor(cfg),
      aligned = false
    )

  /** Checks that a burst shape is internally consistent and valid for an interface configuration. */
  def checkConfig(value: BurstShape, cfg: axi4.Config): CheckResult = {
    val isEmpty =
      value.maxBeats == 0 &&
        value.types.isEmpty &&
        value.sizes.isEmpty &&
        !value.aligned
    val protocolMaxBeats = maxBeatsFor(cfg)
    val protocolTypes = supportedTypesFor(cfg).toSet
    val interfaceSizes = supportedSizesFor(cfg).toSet

    val errors =
      if (isEmpty)
        Seq.empty
      else
        Seq(
        Option.when(value.maxBeats <= 0)(
          s"maxBeats must be positive for a non-empty shape (got ${value.maxBeats})"
        ),
        Option.when(value.maxBeats > protocolMaxBeats)(
          s"maxBeats ${value.maxBeats} exceeds the protocol limit $protocolMaxBeats"
        ),
        Option.when(value.types.isEmpty)(
          "types must not be empty for a non-empty shape"
        ),
        Option.when(value.sizes.isEmpty)(
          "sizes must not be empty for a non-empty shape"
        ),
        Option.when(!value.types.toSet.subsetOf(protocolTypes))(
          s"types ${value.types} contains encodings outside $protocolTypes"
        ),
        Option.when(!value.sizes.toSet.subsetOf(interfaceSizes.toSet))(
          s"sizes ${value.sizes} contains encodings outside $interfaceSizes"
        )
        ).flatten

    CheckResult.from(errors)
  }

  /** Checks whether every transaction described by a master property is accepted by a slave
    * property.
    *
    * A failed result contains every mismatch between the initiated burst shapes and the accepted
    * burst shapes.
    */
  def checkCompatible(master: BurstShape, slave: BurstShape): CheckResult =
    CheckResult.from(Seq(
      Option.when(master.maxBeats > slave.maxBeats)(
        s"master maxBeats ${master.maxBeats} exceeds slave maxBeats ${slave.maxBeats}"
      ),
      Option.when(!master.types.toSet.subsetOf(slave.types.toSet))(
        s"master types ${master.types} is not a subset of " +
          s"slave types ${slave.types}"
      ),
      Option.when(!master.sizes.toSet.subsetOf(slave.sizes.toSet))(
        s"master sizes ${master.sizes} is not a subset of " +
          s"slave sizes ${slave.sizes}"
      ),
      Option.when(master.maxBeats > 0 && slave.aligned && !master.aligned)(
        "master may issue unaligned transactions, but slave requires natural alignment"
      )
    ).flatten)
}
