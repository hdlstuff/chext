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
