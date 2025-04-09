package chext.elastic2


import chisel3._
import chisel3.experimental.AffectsChiselPrefix

trait Handle {
  def init: Unit
  def cond: Bool
  def exec: Unit

  def &&(b: Handle): Handle = {
    val a = this

    new Handle {
      def init: Unit = {
        a.init
        b.init
      }

      def cond: Bool = a.cond && b.cond

      def exec: Unit = {
        a.exec
        b.exec
      }
    }
  }

  def ||(b: Handle): Handle = {
    val a = this

    new Handle {
      def init: Unit = {
        a.init
        b.init
      }

      def cond: Bool = a.cond || b.cond

      def exec: Unit = {
        a.exec
        b.exec
      }
    }
  }

  def otherwise(b: Handle): Handle = {
    val a = this

    new Handle {
      def init: Unit = {
        a.init
        b.init
      }

      def cond: Bool = a.cond || b.cond

      def exec: Unit = {
        when(a.cond) {
          a.exec
        }.otherwise {
          b.exec
        }
      }
    }
  }
}

abstract class Transaction extends AffectsChiselPrefix {
  def init(fn: => Unit): Handle = {
    new Handle {
      def init: Unit = fn
      def cond: Bool = true.B
      def exec: Unit = ()
    }
  }

  def cond(fn: => Bool): Handle = {
    new Handle {
      def init: Unit = ()
      def cond: Bool = fn
      def exec: Unit = ()
    }
  }

  def exec(fn: => Unit): Handle = {
    new Handle {
      def init: Unit = ()
      def cond: Bool = true.B
      def exec: Unit = fn
    }
  }

  def block(fn: => Handle): Unit = {
    var handle: Handle = fn

    handle.init

    when(handle.cond) {
      handle.exec
    }
  }

  def block(cond: Bool)(fn: => Handle): Unit = {
    when(cond) {
      var handle: Handle = fn

      handle.init

      when(handle.cond) {
        handle.exec
      }
    }
  }

  def recv[T <: Data](source: Interface[T]): Tuple2[T, Handle] = {
    (
      source.bits,
      new Handle {
        def init: Unit = source.ready := false.B
        def cond: Bool = source.valid
        def exec: Unit = source.ready := true.B
      }
    )
  }

  def recvKeep[T <: Data](source: Interface[T]): (T, Handle) = {
    (
      source.bits,
      new Handle {
        def init: Unit = source.ready := false.B
        def cond: Bool = source.valid
        def exec: Unit = source.ready := false.B
      }
    )
  }

  def send[T <: Data](sink: Interface[T])(fn: (T) => Unit): Handle = {
    new Handle {
      val queue = Queue(chiselTypeOf(sink.bits), 2)
      queue.module.suggestName(chisel3.reflect.DataMirror.queryNameGuess(sink))

      def init: Unit = {
        import ConnectOp._
        queue.sink :=> sink
        fn(queue.source.bits)
        queue.source.valid := false.B
      }

      def cond: Bool = queue.source.ready
      def exec: Unit = queue.source.valid := true.B
    }
  }
}

class DataLast[T <: Data](gen: T) extends Bundle {
  val bits = gen.cloneType
  val last = Bool()
}

class DataFirstLast[T <: Data](gen: T) extends Bundle {
  val bits = gen.cloneType
  val first = Bool()
  val last = Bool()
  val zero = Bool()
}

class TransactionTestModule extends Module {
  val source1 = IO(Source(UInt(32.W)))
  val source2 = IO(Source(UInt(32.W)))

  val sink1 = IO(Sink(UInt(32.W)))
  val sink2 = IO(Sink(UInt(32.W)))

  val state = RegInit(true.B)

  source1.nodeq()
  source2.nodeq()
  sink1.noenq()
  sink2.noenq()

  val sourceA = IO(Source(new DataLast(UInt(32.W))))
  val sinkA = IO(Sink(new DataFirstLast(UInt(32.W))))

  sourceA.nodeq()
  sinkA.noenq()

  val sentinelTransaction = new Transaction {
    val state = RegInit(0.U(2.W))

    block(state === 0.U) {
      val (_, h1) = recvKeep(sourceA)
      val h2 = send(sinkA) {
        case (x) => {
          x.bits := DontCare
          x.first := true.B
          x.last := false.B
          x.zero := true.B
        }
      }

      h1 && h2 && exec { state := 1.U }
    }

    block(state === 1.U) {
      val (data, h1) = recv(sourceA)
      val h2 = send(sinkA) {
        case (x) => {
          x.bits := data.bits
          x.first := false.B
          x.last := false.B
          x.zero := false.B
        }
      }

      h1 && h2 && exec { state := Mux(data.last, 2.U, 1.U) }
    }

    block(state === 2.U) {
      val h1 = send(sinkA) {
        case (x) => {
          x.bits := DontCare
          x.first := false.B
          x.last := true.B
          x.zero := true.B
        }
      }

      h1 && exec { state := 0.U }
    }
  }

  val dummyTransaction = new Transaction {
    when(state) {
      block {
        val (data1, r) = recv(source1)

        val s1 = send(sink1) { case (out) => out := data1 + 8.U }
        val s2 = send(sink2) { case (out) => out := data1 }

        r && s1 && s2 && exec {
          state := !state
        }
      }
    }.otherwise {

      // this block will be run when the conditions are satisfied
      block {
        // since our IO is irrevocable, it does not make sense to have an `any` primitive
        // or, maybe it does.
        // any { send(...), send(...) }
        val (data1, r) = recv(source2)

        // sends to the same sinks go through an arbiter
        val s1 = send(sink1) { case (out) => out := data1 + 8.U }
        val s2 = send(sink2) { case (out) => out := data1 }

        // basically means that r1 must happen, either s1 or s2 can happen
        // in a single block, there is not anteriority
        //

        // this must be a demux, functionally
        r && s1.otherwise(s2) && exec {
          state := !state
        }

        // r & (s1 | s2)
        // when expression: source2.valid & (sink1.ready | sink2.ready)
        //        executed: source2.ready = 1, sink1.valid = 1, sink2.valid = 1

        // r & onlyOne(s1, s2)
        // when expression: source2.valid & (sink1.ready | sink2.ready)
        //        executed: source2.ready = 1, sink1.valid = sink1.ready, sink2.valid = !sink1.ready && sink2.ready
      }
    }
  }
}

object EmitTransactionTestModule extends App {
  emitVerilog(new TransactionTestModule)
}
