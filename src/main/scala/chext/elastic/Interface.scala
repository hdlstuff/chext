package chext.elastic

import chext.elastic.{tracking => t}

import chisel3._
import chisel3.experimental.{SourceInfo, requireIsChiselType, requireIsHardware}
import chisel3.internal.sourceinfo.SourceInfoTransform
import chisel3.reflect.DataMirror

import scala.collection.immutable.SeqMap
import scala.language.experimental.macros

import chext.util.NamedVec

class Interface[+T <: Data](gen: T)(implicit si_ : SourceInfo)
    extends Record
    with t.Tracked {
  private val ready_ = Input(Bool())
  private val valid_ = Output(Bool())
  private val bits_ = Output(gen.cloneType)

  def sourceInfo: SourceInfo = si_

  val tpe: String = f"chext.elastic.Interface[${gen.toString()}]"

  lazy val declaredRole: t.DeclaredRole = {
    (hacks.DataInternals.isIO(this), DataMirror.directionOf(this.$valid)) match {
      case (true, ActualDirection.Output) => t.DeclaredRole.Sink
      case (true, ActualDirection.Input)  => t.DeclaredRole.Source
      case _                              => t.DeclaredRole.None
    }
  }

  private def directAccess_[T](t: T, name: String)(implicit si: SourceInfo): T = {
    val pos = si.makeMessage(x => x)
    println(
      f"chext.elastic.Interface : direct access to '$name' of interface '$this' is discouraged $pos"
    )

    t
  }

  def elements: SeqMap[String, Data] = SeqMap(
    "ready" -> ready_,
    "valid" -> valid_,
    "bits" -> bits_
  )

  /** Indicates that the consumer is ready to accept the data this cycle
    *
    * @note
    *   Interface signals shall not be accessed directly. Calls to this function result in a
    *   warning.
    */
  def ready: Bool = macro SourceInfoTransform.noArg

  /** Indicates that the consumer is ready to accept the data this cycle
    *
    * @note
    *   Interface signals shall not be accessed directly. However, calls to this function do not
    *   result in a warning.
    */
  def $ready: Bool = ready_

  def do_ready(implicit si_ : SourceInfo): Bool = directAccess_(ready_, "ready")

  /** Indicates that the producer has put valid data in 'bits'
    *
    * @note
    *   Interface signals shall not be accessed directly. Calls to this function result in a
    *   warning.
    */
  def valid: Bool = macro SourceInfoTransform.noArg

  /** Indicates that the producer has put valid data in 'bits'
    *
    * @note
    *   Interface signals shall not be accessed directly. However, calls to this function do not
    *   result in a warning.
    */
  def $valid: Bool = valid_

  def do_valid(implicit si_ : SourceInfo): Bool = directAccess_(valid_, "valid")

  /** The data to be transferred when ready and valid are asserted at the same cycle
    *
    * @note
    *   Interface signals shall not be accessed directly. Calls to this function result in a
    *   warning.
    */
  def bits: T = macro SourceInfoTransform.noArg

  /** The data to be transferred when ready and valid are asserted at the same cycle
    *
    * @note
    *   Interface signals shall not be accessed directly. However, calls to this function do not
    *   result in a warning.
    */
  def $bits: T = bits_

  def do_bits(implicit si_ : SourceInfo): T = directAccess_(bits_, "bits")

  /** Indicates if IO is both ready and valid
    */
  def fire: Bool = $ready && $valid

  /** Push dat onto the output bits of this interface to let the consumer know it has happened.
    * @param dat
    *   the values to assign to bits.
    * @return
    *   dat.
    */
  def enq[T <: Data](dat: T): T = {
    $valid := true.B
    $bits := dat
    dat
  }

  /** Indicate no enqueue occurs. Valid is set to false, and bits are connected to an uninitialized
    * wire.
    */
  def noenq(): Unit = {
    $valid := false.B
    $bits := DontCare
  }

  /** Assert ready on this port and return the associated data bits. This is typically used when
    * valid has been asserted by the producer side.
    * @return
    *   The data bits.
    */
  def deq(): T = {
    $ready := true.B
    $bits
  }

  /** Indicate no dequeue occurs. Ready is set to false.
    */
  def nodeq(): Unit = {
    $ready := false.B
  }
}

object Interface {
  def apply[T <: Data](gen: T)(implicit sourceInfo: SourceInfo): Interface[T] = {
    requireIsChiselType(
      gen,
      sourceInfo.makeMessage((x) => "elastic.Interface: expected Chisel type for gen $x")
    )
    new Interface(gen)
  }

  def many[T <: Data](
      n: Int,
      gen: T,
      naming: NamedVec.Naming = NamedVec.Plain
  )(implicit sourceInfo: SourceInfo): NamedVec[Interface[T]] =
    NamedVec.many(n, apply(gen), naming)
}

object Source {
  def apply[T <: Data](gen: T)(implicit sourceInfo: SourceInfo): Interface[T] = {
    requireIsChiselType(
      gen,
      sourceInfo.makeMessage((x) => "elastic.Source: expected Chisel type for gen $x")
    )
    Flipped(new Interface(gen))
  }

  def many[T <: Data](
      n: Int,
      gen: T,
      naming: NamedVec.Naming = NamedVec.Plain
  )(implicit sourceInfo: SourceInfo): NamedVec[Interface[T]] =
    NamedVec.many(n, apply(gen), naming)

  def io[T <: Data](gen: T)(implicit sourceInfo: SourceInfo): Interface[T] = IO(apply(gen))

  def like[T <: Data](hw: Interface[T])(implicit sourceInfo: SourceInfo) = {
    requireIsHardware(
      hw,
      sourceInfo.makeMessage((x) => "elastic.Source: expected hardware for hw $x")
    )
    Source(chiselTypeOf(hw.$bits))
  }

  def manyLike[T <: Data](
      n: Int,
      hw: Interface[T],
      naming: NamedVec.Naming = NamedVec.Plain
  )(implicit sourceInfo: SourceInfo): NamedVec[Interface[T]] =
    NamedVec.many(n, like(hw), naming)

  def ioLike[T <: Data](hw: Interface[T])(implicit sourceInfo: SourceInfo) = IO(like(hw))
}

object Sink {
  def apply[T <: Data](gen: T)(implicit sourceInfo: SourceInfo): Interface[T] = {
    requireIsChiselType(
      gen,
      sourceInfo.makeMessage((x) => "elastic.Sink: expected Chisel type for gen $x")
    )
    new Interface(gen)
  }

  def many[T <: Data](
      n: Int,
      gen: T,
      naming: NamedVec.Naming = NamedVec.Plain
  )(implicit sourceInfo: SourceInfo): NamedVec[Interface[T]] =
    NamedVec.many(n, apply(gen), naming)

  def like[T <: Data](hw: Interface[T])(implicit sourceInfo: SourceInfo) = {
    requireIsHardware(
      hw,
      sourceInfo.makeMessage((x) => "elastic.Sink: expected hardware for hw $x")
    )
    Sink(chiselTypeOf(hw.$bits))
  }

  def manyLike[T <: Data](
      n: Int,
      hw: Interface[T],
      naming: NamedVec.Naming = NamedVec.Plain
  )(implicit sourceInfo: SourceInfo): NamedVec[Interface[T]] =
    NamedVec.many(n, like(hw), naming)

}

object EWire {
  def apply[T <: Data](gen: T)(implicit sourceInfo: SourceInfo) = {
    requireIsChiselType(
      gen,
      sourceInfo.makeMessage((x) => "elastic.EWire: expected Chisel type for gen $x")
    )
    dontTouch { Wire(Interface(gen)) }
  }

  def many[T <: Data](
      n: Int,
      gen: T,
      naming: NamedVec.Naming = NamedVec.Plain
  )(implicit sourceInfo: SourceInfo): NamedVec[Interface[T]] = {
    requireIsChiselType(
      gen,
      sourceInfo.makeMessage((x) => "elastic.EWire: expected Chisel type for gen $x")
    )
    dontTouch { Wire(NamedVec.many(n, Interface(gen), naming)) }
  }

  def like[T <: Data](hw: Interface[T])(implicit sourceInfo: SourceInfo) = {
    requireIsHardware(
      hw,
      sourceInfo.makeMessage((x) => "elastic.EWire: expected hardware for hw $x")
    )
    dontTouch { Wire(Interface(chiselTypeOf(hw.$bits))) }
  }

  def manyLike[T <: Data](
      n: Int,
      hw: Interface[T],
      naming: NamedVec.Naming = NamedVec.Plain
  )(implicit sourceInfo: SourceInfo): NamedVec[Interface[T]] = {
    requireIsHardware(
      hw,
      sourceInfo.makeMessage((x) => "elastic.EWire: expected hardware for hw $x")
    )
    dontTouch { Wire(NamedVec.many(n, Interface(chiselTypeOf(hw.$bits)), naming)) }
  }
}
