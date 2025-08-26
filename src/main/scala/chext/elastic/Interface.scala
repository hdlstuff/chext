package chext.elastic

import chisel3._

import chisel3.reflect.DataMirror
import chisel3.experimental.SourceInfo
import chisel3.experimental.requireIsChiselType
import chisel3.experimental.requireIsHardware

import chisel3.internal.sourceinfo.SourceInfoTransform

import scala.language.experimental.macros

import scala.collection.immutable.SeqMap

import chext.tracking

class Interface[+T <: Data](gen: T)(implicit si_ : SourceInfo)
    extends Record
    with tracking.Tracked {
  private val ready_ = Input(Bool())
  private val valid_ = Output(Bool())
  private val bits_ = Output(gen.cloneType)

  def sourceInfo: SourceInfo = si_

  val tpe: String = f"chext.elastic.Interface[${gen.toString()}]"

  lazy val declaredRole: tracking.DeclaredRole = {
    (hacks.DataInternals.isIO(this), DataMirror.directionOf(this.$valid)) match {
      case (true, ActualDirection.Output) => tracking.DeclaredRole.Sink
      case (true, ActualDirection.Input)  => tracking.DeclaredRole.Source
      case _                              => tracking.DeclaredRole.None
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
}

object Source {
  def apply[T <: Data](gen: T)(implicit sourceInfo: SourceInfo): Interface[T] = {
    requireIsChiselType(
      gen,
      sourceInfo.makeMessage((x) => "elastic.Source: expected Chisel type for gen $x")
    )
    Flipped(new Interface(gen))
  }

  def io[T <: Data](gen: T)(implicit sourceInfo: SourceInfo): Interface[T] = IO(apply(gen))

  def like[T <: Data](hw: Interface[T])(implicit sourceInfo: SourceInfo) = {
    requireIsHardware(
      hw,
      sourceInfo.makeMessage((x) => "elastic.Source: expected hardware for hw $x")
    )
    Source(chiselTypeOf(hw.$bits))
  }

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

  def like[T <: Data](hw: Interface[T])(implicit sourceInfo: SourceInfo) = {
    requireIsHardware(
      hw,
      sourceInfo.makeMessage((x) => "elastic.Sink: expected hardware for hw $x")
    )
    Sink(chiselTypeOf(hw.$bits))
  }

}

object EWire {
  def apply[T <: Data](gen: T)(implicit sourceInfo: SourceInfo) = {
    requireIsChiselType(
      gen,
      sourceInfo.makeMessage((x) => "elastic.EWire: expected Chisel type for gen $x")
    )
    Wire(Interface(gen))
  }

  def like[T <: Data](hw: Interface[T])(implicit sourceInfo: SourceInfo) = {
    requireIsHardware(
      hw,
      sourceInfo.makeMessage((x) => "elastic.EWire: expected hardware for hw $x")
    )
    Wire(Interface(chiselTypeOf(hw.$bits)))
  }
}

private object InterfaceApp extends App {
  class Module2 extends Module {
    val source = IO(Source(UInt(32.W)))
    val sink1 = IO(Sink(UInt(32.W)))
    val sink2 = IO(Sink(UInt(32.W)))

    val fork0 = new Fork(source) {
      val transform0 = new Transform(fork(), sink1) {
        out := in + 1.U
      }

      val transform1 = new Transform(fork(), sink2) {
        out := in + 7.U
      }
    }
  }

  class Module1 extends Module {
    val source = IO(Source(UInt(32.W)))
    val sink = IO(Sink(UInt(32.W)))

    val transform0 = new Transform(source, sink) {
      out := in + 1.U
    }

    val module = Module(new Module2)
  }

  class Module0 extends Module {
    val source = IO(Source(UInt(32.W)))
    val sink = IO(Sink(UInt(32.W)))

    val x_source = IO(Source(UInt(32.W)))
    val x_sink = IO(Sink(UInt(32.W)))

    val module = Module(new Module1)

    val transform0 = new Transform(source, sink) {
      out := in + 1.U
    }
  }

  emitVerilog(new Module0, Array("--target-dir", "output/"))
}

private object XApp extends App {
  import hdlinfo.TypedObject

  class ModuleY extends Module {
    val in0 = IO(Input(UInt(6.W)))
    val out0 = IO(Output(UInt(32.W)))

    out0 := in0
  }

  class ModuleX extends Module {
    val interface0_a0 = Wire(UInt(32.W))
    interface0_a0 := DontCare
    // dontTouch(interface0_a0)

    val moduleY = Module(new ModuleY)
    moduleY.in0 := 98.U
    dontTouch(moduleY.out0)
    dontTouch(moduleY.in0)

    val interface0 = Wire(new Bundle { val a0 = UInt(64.W) })
    interface0 := DontCare
    // dontTouch(interface0)

    val bla = {
      val interface1 = Wire(Vec(1, new Bundle { val a0 = new Bundle { val a0 = UInt(64.W) } }))
      interface1 := DontCare
      // dontTouch(interface1)
      println(
        interface1.toString(),
        chisel3.hacks.DataInternals.earlyName(interface1)
      )

      println(
        interface1.head.a0.toString(),
        chisel3.hacks.DataInternals.earlyName(interface1.head.a0)
      )

      println(
        interface1.head.a0.a0.toString(),
        chisel3.hacks.DataInternals.earlyName(interface1.head.a0.a0)
      )

      dontTouch(interface1.head.a0.a0)

      interface1
    }

    println(
      moduleY.in0.toString(),
      chisel3.hacks.DataInternals.earlyName(moduleY.in0)
    )

    // println(chisel3.DataInternals.earlyName(interface0_a0))
    // println(chisel3.DataInternals.earlyName(interface0))
    // println(chisel3.DataInternals.earlyName(interface0.a0))

    chisel3.experimental.prefix("interface0") {
      val a0 = Wire(UInt(32.W))
      a0 := DontCare
      // println(chisel3.DataInternals.earlyName(a0))
      // dontTouch(a0)
    }

    atModuleBodyEnd {
      // I think as long as we rely on deferred, we are good with naming

      // chisel3.ModuleInternals.getIds(this).foreach {
      //   println(_)
      // }

      println("atModuleBodyEnd")

      chisel3.hacks.ModuleInternals.getWires(this).foreach {
        println(_)
      }

      chisel3.hacks.DataInternals.getChildrenOfType[UInt](bla).foreach { x =>
        println(chisel3.hacks.DataInternals.earlyName(x), x.toString())
      }

      println(chisel3.hacks.DataInternals.earlyName(bla), bla.toString())
    }
  }

  emitVerilog(new ModuleX, Array("--target-dir", "output/"))

}

private object YApp extends App {
  import chext.amba.axi4

  class ModuleX extends Module {
    val s_axi = IO(axi4.full.Slave(axi4.Config()))
    val s_axi_1 = IO(axi4.full.Slave(axi4.Config()))
    val m_axi = IO(axi4.full.Master(axi4.Config()))

    chext.tracking.register()

    import axi4.Ops._
    s_axi :=> m_axi
  }

  emitVerilog(new ModuleX, Array("--target-dir", "output/"))
}
