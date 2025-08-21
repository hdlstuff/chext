package chext.elastic

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.internal.sourceinfo.SourceInfoTransform

import scala.language.experimental.macros

class Transaction() {
  implicit class ops[T <: Data](interface: Interface[T]) {
    def peek: T = macro SourceInfoTransform.noArg
    def do_peek(implicit si: SourceInfo): T = {
      // also mark it as a source interface
      interface.markSource()
      assert(interface.$valid, "peek called without checking valid!")
      interface.$bits
    }

    def poke: T = macro SourceInfoTransform.noArg
    def do_poke(implicit si: SourceInfo): T = {
      interface.markSink() // if not already marked
      // set an internal variable that says
      // yes, there is a valid value written
      interface.$bits
    }

    def hasData: Bool = interface.$valid

    def pop(): Unit = ???
    def push(): Unit = ???
  }

  def source[T <: Data](interface: Interface[T]): Unit = {
    // add the interface to the list of sources
    // mark it as a source
  }

  def sink[T <: Data](interface: Interface[T]): Unit = {
    // add the interface to the list of sinks
    // mark it as a sink

    interface.bits := 6.U
  }

  // overload the when function for adding extra checks
  // or no need!

  // the state must be handled somehow...
  // let's say that writing to registers is invalid here?
  def block(fn: => Unit): Unit = ???
}
/*
object Test extends App {
  val sourceA = Interface(UInt(32.W))
  val sourceB = Interface(UInt(32.W))
  val sinkA = Interface(UInt(32.W))
  val sinkB = Interface(UInt(32.W))

  val transaction0 = new Transaction {
    // OK, valid checks MUST BE explicit, because we can make decisions based on it
    when(sourceA.hasData && sourceB.hasData) {
      // peek also adds an assertion to make sure that .$valid's are asserted

      when(sourceA.peek(7, 0) > sourceB.peek) {
        sinkA.poke := sourceA.peek + 9.U

        block {
          sourceA.pop()
          sourceB.pop()
          sinkA.push()
        }
      }.otherwise {
        block {
          sourceA.pop()
          sourceB.pop()
          sinkB.push()
        }
      }
    }.otherwise {

    }
  }
}
 */

object Test extends App {
  class MyModule extends Module {
    val incr = IO(Input(Bool()))
    val out = IO(Output(UInt(32.W)))

    val state = RegInit(0.U(32.W))
    out := state

    when(incr) {
      state := state +% 1.U
    }

    atModuleBodyEnd {
      ModuleInternals.getCommands(this).foreach { println(_) }
    }
  }

  emitVerilog(new MyModule)
}
