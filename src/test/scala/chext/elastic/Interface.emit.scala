package chext.elastic

import chisel3._

object InterfaceApp extends App {
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
