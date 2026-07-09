package chext.elastic

import chisel3._

/*
object TransactionSketch extends App {
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

object Transaction_Emit extends App {
  class MyModule extends Module {
    val incr = IO(Input(Bool()))
    val out = IO(Output(UInt(32.W)))

    val state = RegInit(0.U(32.W))
    out := state

    when(incr) {
      state := state +% 1.U
    }

    atModuleBodyEnd {
      hacks.ModuleInternals.getCommands(this).foreach { println(_) }
    }
  }

  emitVerilog(new MyModule)
}
