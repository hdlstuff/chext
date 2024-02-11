package chext.elastic

import chisel3._
import chisel3.util._
import chisel3.experimental._

class RandomDelayer[T <: Data](gen: T, lfsrBits: Int) extends Module {
  require(lfsrBits >= 4, "LFSR can only be initialized with at least 4 bits.")

  val io = IO(new Bundle {
    val input = Flipped(Decoupled(gen))
    val output = Decoupled(gen)
  })

  val input = SourceBuffer(io.input, 16)
  val stPass :: stBlock :: Nil = Enum(2)

  val randomDelay = util.random.LFSR(lfsrBits)

  val counter = RegInit(0.U(lfsrBits.W))
  val stateReg = RegInit(stPass)

  input.nodeq()
  io.output.noenq()

  switch(stateReg) {
    is(stPass) {
      counter := randomDelay
      stateReg := stBlock
    }

    is(stBlock) {
      when(counter =/= 0.U) {
        counter := counter - 1.U
        stateReg := stBlock
      }.otherwise {
        stateReg := stPass
      }
    }

  }

  when(input.valid && io.output.ready && (stateReg === stPass)) {
    io.output.enq(input.deq())
  }
}
