package chext.util

import chisel3._
import chisel3.experimental.requireIsChiselType

/** @todo
  *   This class creates a "Vec" of a custom naming scheme. It is meant as a simple adapter, not to
  *   be used in general.
  */
class NamedVec[T <: Data](
    gen: T,
    n: Int,
    naming: NamedVec.Naming = NamedVec.Plain
) extends Record
    with IndexedSeq[T] {
  import scala.collection.immutable.SeqMap
  import chisel3.reflect.DataMirror
  import chisel3.experimental.requireIsChiselType

  requireIsChiselType(gen)

  private val names = Array.tabulate(n) { index => naming.name(index, n) }
  require(names.distinct.length == names.length, "NamedVec element names must be distinct.")

  val elements: SeqMap[String, Data] = SeqMap.from(
    Array
      .tabulate(n) {
        case (index) => {
          names(index) -> DataMirror.internal.chiselTypeClone(gen)
        }
      }
      .reverse
  )

  override def apply(index: Int): T = elements(names(index)).asInstanceOf[T]

  override def length: Int = n

  override def className: String = "NamedVec"

}

object NamedVec {
  sealed trait Naming {
    def name(index: Int, n: Int): String
  }

  case object Plain extends Naming {
    def name(index: Int, n: Int): String = index.toString()
  }

  case class ZeroExtended(numDigits: Int = 0) extends Naming {
    def name(index: Int, n: Int): String = {
      val digits = if (numDigits > 0) numDigits else (n - 1).toString().length()
      String.format(f"%%0${digits}d", index)
    }
  }

  case class Custom(fn: Int => String) extends Naming {
    def name(index: Int, n: Int): String = fn(index)
  }

  def apply[T <: Data](
      n: Int,
      gen: T,
      naming: Naming = Plain
  ): NamedVec[T] = {
    requireIsChiselType(gen)
    new NamedVec(gen, n, naming)
  }

  def plain[T <: Data](
      n: Int,
      gen: T
  ): NamedVec[T] = {
    apply(n, gen, Plain)
  }

  def zeroExtended[T <: Data](
      n: Int,
      gen: T,
      numDigits: Int = 0
  ): NamedVec[T] = {
    apply(n, gen, ZeroExtended(numDigits))
  }

  def many[T <: Data](
      n: Int,
      gen: T,
      naming: Naming = Plain
  ): NamedVec[T] = {
    apply(n, gen, naming)
  }
}
