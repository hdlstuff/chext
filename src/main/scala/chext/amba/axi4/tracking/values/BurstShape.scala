package chext.amba.axi4.tracking.values

import chext.amba.axi4
import chext.amba.axi4.BurstType.Encoding.{FIXED, INCR, WRAP}

/** Burst-related limits and capabilities carried by one read or write property. */
final case class BurstShape private (
    maxBeats: Int,
    burstTypes: Seq[Int],
    transferSizes: Seq[Int],
    aligned: Boolean
) {

  /** Copies this shape while preserving normalized sequence fields. */
  def copy(
      maxBeats: Int = this.maxBeats,
      burstTypes: Seq[Int] = this.burstTypes,
      transferSizes: Seq[Int] = this.transferSizes,
      aligned: Boolean = this.aligned
  ): BurstShape =
    BurstShape(maxBeats, burstTypes, transferSizes, aligned)
}

object BurstShape {
  private def normalize(values: Seq[Int]): Seq[Int] =
    values.distinct.sorted

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
      burstTypes: Seq[Int] = Seq.empty,
      transferSizes: Seq[Int] = Seq.empty,
      aligned: Boolean = false
  ): BurstShape =
    new BurstShape(
      maxBeats,
      normalize(burstTypes),
      normalize(transferSizes),
      aligned
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

  /** Returns every internal-consistency or interface-specific validation problem. */
  def validationErrors(value: BurstShape, cfg: axi4.Config): Seq[String] = {
    val isEmpty =
      value.maxBeats == 0 &&
        value.burstTypes.isEmpty &&
        value.transferSizes.isEmpty &&
        !value.aligned
    val protocolMaxBeats = maxBeatsFor(cfg)
    val protocolTypes = supportedTypesFor(cfg).toSet
    val interfaceSizes = supportedSizesFor(cfg).toSet

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
        Option.when(value.burstTypes.isEmpty)(
          "burstTypes must not be empty for a non-empty shape"
        ),
        Option.when(value.transferSizes.isEmpty)(
          "transferSizes must not be empty for a non-empty shape"
        ),
        Option.when(!value.burstTypes.toSet.subsetOf(protocolTypes))(
          s"burstTypes ${value.burstTypes} contains encodings outside $protocolTypes"
        ),
        Option.when(!value.transferSizes.toSet.subsetOf(interfaceSizes.toSet))(
          s"transferSizes ${value.transferSizes} contains encodings outside $interfaceSizes"
        )
      ).flatten
  }

  /** Returns all reasons why a master burst shape exceeds a slave capability. */
  def compatibilityErrors(master: BurstShape, slave: BurstShape): Seq[String] =
    Seq(
      Option.when(master.maxBeats > slave.maxBeats)(
        s"master maxBeats ${master.maxBeats} exceeds slave maxBeats ${slave.maxBeats}"
      ),
      Option.when(!master.burstTypes.toSet.subsetOf(slave.burstTypes.toSet))(
        s"master burstTypes ${master.burstTypes} is not a subset of " +
          s"slave burstTypes ${slave.burstTypes}"
      ),
      Option.when(!master.transferSizes.toSet.subsetOf(slave.transferSizes.toSet))(
        s"master transferSizes ${master.transferSizes} is not a subset of " +
          s"slave transferSizes ${slave.transferSizes}"
      ),
      Option.when(master.maxBeats > 0 && slave.aligned && !master.aligned)(
        "master may issue unaligned transactions, but slave requires natural alignment"
      )
    ).flatten
}
