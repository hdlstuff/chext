package chext.elastic

import chisel3._
import circt.stage.ChiselStage

import chext.{elastic => e}
import chext.elastic.ConnectOp._
import chext.util.NamedVec

private class GraphDeclarationOrder_Tbtop extends Module with chext.AnnotatedModule {
  val sources = IO(e.Source.many(11, UInt(8.W), NamedVec.ZeroExtended()))
  val sinks = IO(e.Sink.many(11, UInt(8.W), NamedVec.ZeroExtended()))

  sources :=> sinks

  declareElasticInterface(sources, "In")
  declareElasticInterface(sinks, "Out")
}

object GraphDeclarationOrder_Tb extends App {
  var graph = Option.empty[e.tracking.Graph.Module]

  ChiselStage.emitSystemVerilog(
    {
      val module = new GraphDeclarationOrder_Tbtop
      chext.tracking.onComplete(module) {
        graph = e.tracking.moduleGraphOption(module)
      }
      module
    },
    args = Array("--no-source-info"),
    firtoolOpts = Array(
      "-disable-all-randomization",
      "-strip-debug-info",
      "-default-layer-specialization=enable"
    )
  )

  val expectedSources = (0 until 11).map { index => f"/sources_$index%02d" }
  val expectedSinks = (0 until 11).map { index => f"/sinks_$index%02d" }

  scala.Predef.assert(
    graph.get.sources.map(_.path) == expectedSources,
    graph.get.sources.map(_.path)
  )
  scala.Predef.assert(
    graph.get.sinks.map(_.path) == expectedSinks,
    graph.get.sinks.map(_.path)
  )
  scala.Predef.assert(graph.get.components(9).path.contains("_9_"))
  scala.Predef.assert(graph.get.components(10).path.contains("_10_"))
}
