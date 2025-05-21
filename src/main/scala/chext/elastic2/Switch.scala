package chext.elastic2

import chext.{elastic2 => elastic}

import chisel3._
import chisel3.util._

import chisel3.hacks.deferred
import chisel3.experimental.AffectsChiselPrefix

abstract class Switch[T1 <: Data, T2 <: Data](
    sourceIn: Interface[T1],
    sinkOut: Interface[T2],
    queueLength: Int = 2
) extends AffectsChiselPrefix {
  private val casesBuffer = scala.collection.mutable.ArrayBuffer.empty[Case]
  private var doneCalled = false
  protected val in = sourceIn.bits

  protected abstract class Case(val cond: Bool) {
    val source = Wire(chiselTypeOf(sourceIn))
    val sink = Wire(chiselTypeOf(sinkOut))

    casesBuffer.addOne(this)
  }

  protected def done() = {
    require(!doneCalled, "Switch: done() shall be called exactly once!")

    import ConnectOp._

    val cases = casesBuffer.toSeq
    val numCases = cases.length

    require(numCases >= 1)

    // at least one case must be valid
    chisel3.assert(
      VecInit(cases.map { _.cond }).reduceTree(_ || _),
      "At least one case must be valid!"
    )

    if (numCases == 1) {
      val thisCase = cases(0)

      sourceIn :=> thisCase.source
      thisCase.sink :=> sinkOut
    } else {
      val genIndex = UInt(log2Ceil(numCases).W)

      val queue = elastic.Queue(genIndex, queueLength, flow = true)

      new elastic.Fork(sourceIn) {
        val index = PriorityEncoder(VecInit(cases.map { _.cond }))

        elastic.Demux(
          sourceIn,
          cases.map { _.source },
          fork { index }
        )

        fork { index } :=> queue.source
      }

      elastic.Mux(
        cases.map { _.sink },
        sinkOut,
        queue.sink
      )
    }

    doneCalled = true
  }

  deferred {
    require(doneCalled, "Switch requires you to call done after creating all the cases!")
  }
}
