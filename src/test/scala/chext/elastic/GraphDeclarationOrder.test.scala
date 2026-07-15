package chext.elastic

import chisel3._

import chext.{elastic => e}
import chext.elastic.ConnectOp._
import chext.util.{ElaborationTest, NamedVec}

import java.nio.file.Path

private class GraphDeclarationOrderTestTop extends Module with chext.AnnotatedModule {
  val sources = IO(e.Source.many(11, UInt(8.W), NamedVec.ZeroExtended()))
  val sinks = IO(e.Sink.many(11, UInt(8.W), NamedVec.ZeroExtended()))

  sources :=> sinks

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(sources, "In")
  declareElasticInterface(sinks, "Out")
}

object GraphDeclarationOrder_Test extends App with ElaborationTest {
  val expectedSources = (0 until 11).map { index => f"/sources_$index%02d" }
  val expectedSinks = (0 until 11).map { index => f"/sinks_$index%02d" }

  suite(
    name = "graph-declaration-order",
    outputDir = Path.of("output", "graph_declaration_order")
  )

  test(
    name = "zero_extended_named_vec",
    gen = () => new GraphDeclarationOrderTestTop,
    checks = Seq(
      ModuleGraph.check("source declaration order") { graph =>
        val actual = graph.sources.map(_.path)
        Option.unless(actual == expectedSources)(s"actual sources: $actual")
      },
      ModuleGraph.check("sink declaration order") { graph =>
        val actual = graph.sinks.map(_.path)
        Option.unless(actual == expectedSinks)(s"actual sinks: $actual")
      },
      ModuleGraph.check("multi-digit component names") { graph =>
        Option.unless(
          graph.components(9).path.contains("_9_") &&
            graph.components(10).path.contains("_10_")
        )(s"actual components: ${graph.components.map(_.path)}")
      }
    )
  )

  runTests()
}
