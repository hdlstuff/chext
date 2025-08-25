package chext.util

import chisel3._
import chisel3.experimental.requireIsChiselType

import scala.collection.immutable.ListMap

object MixedVec {

  /** Create a MixedVec type from a Seq of Chisel types.
    */
  def apply[T <: Data](eltsIn: Seq[T]): MixedVec[T] = new MixedVec(eltsIn)

  /** Create a MixedVec type from a varargs list of Chisel types.
    */
  def apply[T <: Data](val0: T, vals: T*): MixedVec[T] = new MixedVec(val0 +: vals.toSeq)

  /** Create a new MixedVec type from an unbound MixedVec type.
    */
  def apply[T <: Data](mixedVec: MixedVec[T]): MixedVec[T] = new MixedVec(mixedVec.elts)

  /** Create a MixedVec type from the type of the given Vec.
    *
    * @example
    *   {{{MixedVec(Vec(2, UInt(8.W))) = MixedVec(Seq.fill(2){UInt(8.W)})}}}
    */
  def apply[T <: Data](vec: Vec[T]): MixedVec[T] = {
    MixedVec(Seq.fill(vec.length)(hacks.VecSampleElement(vec)))
  }

  /** Create a MixedVec wire from a Seq of values.
    */
  def from[T <: Data](vals: Seq[T]): MixedVec[T] = {
    // Create a wire of this type.
    val hetVecWire = Wire(MixedVec(vals.map(hacks.CloneTypeFull(_))))
    // Assign the given vals to this new wire.
    for ((a, b) <- hetVecWire.zip(vals)) {
      a := b
    }
    hetVecWire
  }

  /** Create a MixedVec wire from a varargs list of values.
    */
  def from[T <: Data](val0: T, vals: T*): MixedVec[T] = from(val0 +: vals.toSeq)
}

/** A hardware array of elements that can hold values of different types/widths, unlike Vec which
  * can only hold elements of the same type/width.
  *
  * @param eltsIn
  *   Element types. Must be Chisel types.
  *
  * @example
  *   {{{ val v = Wire(MixedVec(Seq(UInt(8.W), UInt(16.W), UInt(32.W)))) v(0) := 100.U(8.W) v(1) :=
  *   10000.U(16.W) v(2) := 101.U(32.W) }}}
  *
  * @note
  *   This implementation reverses the `elements` map so that the order is expected in the generated
  *   code.
  */
final class MixedVec[T <: Data](private val eltsIn: Seq[T])
    extends Record
    with collection.immutable.IndexedSeq[T] {
  // We want to create MixedVec only with Chisel types.
  eltsIn.foreach(e => requireIsChiselType(e))

  // In Scala 2.13, this is protected in IndexedSeq, must override as public because it's public in
  // Record
  override def className: String = "MixedVec"

  // Clone the inputs so that we have our own references.
  private val elts: IndexedSeq[T] = eltsIn.map(hacks.CloneTypeFull(_)).toIndexedSeq

  /** Statically (elaboration-time) retrieve the element at the given index.
    * @param index
    *   Index with which to retrieve.
    * @return
    *   Retrieved index.
    */
  def apply(index: Int): T = elts(index)

  /** Strong bulk connect, assigning elements in this MixedVec from elements in a Seq.
    *
    * @note
    *   the lengths of this and that must match
    * @group connection
    */
  def :=(that: Seq[T]): Unit = {
    require(this.length == that.length)
    for ((a, b) <- this.zip(that))
      a := b
  }

  /** Get the length of this MixedVec.
    * @return
    *   Number of elements in this MixedVec.
    */
  def length: Int = elts.length

  override val elements = ListMap(elts.zipWithIndex.reverse.map { //
    case (element, index) => (index.toString, element)
  }: _*)

  // IndexedSeq has its own hashCode/equals that we must not use
  override def hashCode: Int = super[Record].hashCode

  override def equals(that: Any): Boolean = super[Record].equals(that)
}
