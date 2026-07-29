# Chext: A Domain-specific Language for Safe and Agile Elastic Dataflow Accelerators

Chext is a Scala/Chisel-based DSL for building high-performance elastic
dataflow accelerators on FPGAs. It provides a thin, RTL-faithful abstraction so
you can write circuits that closely mirror your dataflow schematic while, in
practice, iterating faster and more safely.

- Deterministic, schematic-like construction of elastic circuits
- Rich, composable component library (elastic primitives, AXI, streaming, FP)
- Static checks plus simulation-time assertions that may catch protocol issues early
- Memory-forward components (bursting read/write, protocol adapters, interconnect)
- Interop-friendly with RTL and, if desired, HLS modules via elastic/AXI boundaries

> The paper PDF is included in this repository. You can also browse example
> designs here: https://github.com/hdlstuff/chext-examples

---

## Links

- Paper (PDF in this repo): “Chext: A Domain-specific Language for Safe and Agile Elastic Dataflow Accelerators”
- Examples: https://github.com/hdlstuff/chext-examples
- Documentation: [package synopsis](docs/synopsis.md), [Elastic API](docs/elastic.md), [tracking model](docs/tracking.md), [naming conventions](docs/naming.md), and [active TODOs](docs/todo/README.md)
- Contact: canberk.sonmez@epfl.ch

---

## Why Chext?

Chext is designed to keep you close to the architecture you would have drawn
anyway—forks, joins, mux/demux, queues, transducers, loops/scopes—while giving
you precise control over handshakes and memory traffic. It is, arguably, a good
fit when you want:

- Natural flow of data and control (ready/valid, first/last, index, user/meta)
- Tunable backpressure and buffering (queues, source/sink buffers, flow/pipeline)
- Safe state with elastic semantics (transducers, scopes, loops)
- Explicit, predictable memory behavior (bursting, response buffering, AXI mux/demux)

---

## Getting Started

### Prerequisites

- Scala 2.13.x
- SBT
- Java 8+ (JDK)
- Chisel 6.x

### Build configuration

Use the provided `build.sbt` (example below mirrors the attached config):

```scala
ThisBuild / scalaVersion := "2.13.18"
ThisBuild / version := "0.1.1"
ThisBuild / organization := "hdlstuff"

val chiselVersion = "7.6.0"
val circeVersion = "0.14.1"

lazy val root = (project in file("."))
  .settings(
    name := "chext_examples",
    libraryDependencies ++= Seq(
      "org.chipsalliance" %% "chisel" % chiselVersion,
      "hdlstuff" %% "hdlinfo" % "0.1.0",
      "hdlstuff" %% "chext" % "0.2.2",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-Ymacro-annotations"
    ),
    addCompilerPlugin(
      "org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full
    ),
    resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
    resolvers ++= Resolver.sonatypeOssRepos("releases")
  )
```

### Build, Test, and Emit Verilog

```bash
# Run example testbenches
sbt "runMain chext_examples.Example0_Tb"
sbt "runMain chext_examples.Example1_Tb"
sbt "runMain chext_examples.Example2_Tb"
sbt "runMain chext_examples.Example3_Tb"

# Emit Verilog for examples
sbt "runMain chext_examples.Example0_Emit"
sbt "runMain chext_examples.Example1_Emit"
sbt "runMain chext_examples.Example2_Emit"
sbt "runMain chext_examples.Example3_Emit"
```

---

## Examples (from `chext-examples`)

These snippets are directly aligned with
https://github.com/hdlstuff/chext-examples and can be used as-is.

### 1) Fork + Transform

```scala
class Example0 extends Module with chext.AnnotatedModule {
  val io = IO(new Bundle {
    val source = e.Source(UInt(32.W))
    val sink0  = e.Sink(UInt(32.W))
    val sink1  = e.Sink(UInt(32.W))
  })

  val fork0 = new e.Fork(e.SourceBuffer(io.source)) {
    fork(in + 9.U)  :=> e.SinkBuffer(io.sink0)
    fork(in + 87.U) :=> e.SinkBuffer(io.sink1)
  }

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(io.source, "Source")
  declareElasticInterface(io.sink0, "Sink0")
  declareElasticInterface(io.sink1, "Sink1")
}
```

### 2) Repeat + Mux (select-driven source choice)

```scala
class Example1 extends Module with chext.AnnotatedModule {
  val io = IO(new Bundle {
    val sourceSelect = e.Source(new Bundle {
      val sourceId = UInt(1.W)
      val count    = UInt(8.W)
    })
    val sourceA = e.Source(UInt(32.W))
    val sourceB = e.Source(UInt(32.W))
    val sink    = e.Sink(UInt(32.W))
  })

  val ewire0 = e.EWire(UInt(1.W))

  val repeat0 = new e.Repeat(io.sourceSelect, ewire0, 8) {
    len { in => in.count }
    out { (in, index, first, last) => in.sourceId }
  }

  e.Mux(Seq(io.sourceA, io.sourceB), io.sink, ewire0)

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(io.sourceA, "SourceData")
  declareElasticInterface(io.sourceB, "SourceData")
  declareElasticInterface(io.sourceSelect, "SourceSelect")
  declareElasticInterface(io.sink, "Sink")
}
```

### 3) Transducer (stateful stream compaction/aggregation)

```scala
class Example2_In(wData: Int = 8) extends Bundle {
  val data = UInt(wData.W)
  val last = Bool()
}

class Example2_Out extends Bundle {
  val data   = UInt(8.W)
  val length = UInt(32.W)
  val last   = Bool()
}

class Example2 extends Module with chext.AnnotatedModule {
  val genIn  = new Example2_In(8)
  val genOut = new Example2_Out

  val source = IO(e.Source(genIn))
  val sink   = IO(e.Sink(genOut))

  val transducer0 = new e.Transducer(source, sink) {
    val rLastDataValid  = RegInit(false.B)
    val rLastDataLength = RegInit(0.U(32.W))
    val rLastData       = RegInit(0.U(8.W))

    packet {
      out.data   := rLastData
      out.length := rLastDataLength
      out.last   := in.last

      when(rLastDataValid) {
        when(in.data === rLastData && !in.last) {
          consume { rLastDataLength := rLastDataLength + 1.U }
        }.otherwise {
          accept {
            rLastData       := in.data
            rLastDataLength := 0.U
            rLastDataValid  := !in.last
          }
        }
      }.otherwise {
        when(in.last) {
          accept {}
        }.otherwise {
          consume {
            rLastData      := in.data
            rLastDataValid := true.B
          }
        }
      }
    }
  }

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(source, "Source")
  declareElasticInterface(sink, "Sink")
}
```

### 4) AXI Read–Accumulate–Store (bursting + fold)

```scala
import chext.amba.axi4
import axi4.Ops._
import chext.{ldstr => ldst}
import chext.stream

class Example3_Task extends Bundle {
  val ptrArray  = UInt(64.W) // uint32_t[uLength]
  val uLength   = UInt(64.W)
  val ptrResult = UInt(64.W)
}

class Example3_Result extends Bundle {}

case class Example3_Config() {
  val genTask   = new Example3_Task
  val genResult = new Example3_Result

  val axiCfg   = axi4.Config(wId = 6, wAddr = 64, wData = 64)
  val axiRdCfg = axi4.Config(wAddr = 64, wData = 32, write = false)
  val axiStCfg = axi4.Config(wAddr = 64, wData = 32, read = false)
}

class Example3_Basic(cfg: Example3_Config) extends Module {
  import cfg._

  val sourceTask = IO(e.Source(genTask))
  val sinkResult = IO(e.Sink(genResult))

  val m_axi_rd = IO(axi4.full.Master(axiRdCfg))
  val m_axi_st = IO(axi4.full.Master(axiStCfg))

  val rd = Module(new stream.Read(stream.ReadConfig(
    axiRdCfg,
    resultMode = stream.ReadResultMode.LastSometimesInvalid
  )))
  rd.m_axi :=> m_axi_rd

  val st = Module(new ldst.Store(ldst.StoreConfig(axiStCfg)))
  st.m_axi :=> m_axi_st

  val fork0 = new e.Fork(sourceTask) {
    val t0 = new e.Transform(e.SourceBuffer(fork(), 8), rd.sourceTask) {
      out.address := in.ptrArray
      out.length  := in.uLength
      out.user    := 0.U
    }

    val fold0 =
      new e.Fold(rd.sinkResult, e.Const(0.U, "zero"), st.sourceData) {
        operand { in => in.data }
        last    { in => in.last }
        zero    { in => !in.valid }

        // optional: join partials if you split accumulation paths
        // val join0 = new e.Join(e.SinkBuffer(sourceResult)) { ... }
      }

    val t1 = new e.Transform(e.SourceBuffer(fork(), 8), st.sourceTask) {
      out.address := in.ptrResult
      out.user    := 0.U
    }
  }

  val t2 = new e.Transform(st.sinkResult, sinkResult) {}
}
```

---

## Design Philosophy

- Data-is-control: tokens carry both payload and control (e.g., first/last, index,
  user/meta). Many components let you map which fields drive signaling.
- Safe handshakes: ready/valid compliance is respected; violations are, at least
  during simulation, surfaced via assertions.
- Stateful when appropriate: transducers, loops, and scopes let you write precise
  behaviors without contorting a sequential description.
- Memory-aware: AXI4 helpers (full/lite/stream), width converters, ID/addr mux/demux,
  response/write buffers, and bursting read/write components make throughput
  work more straightforward.
- Composability: elastic components compose like a schematic; Scala/Chisel
  parametric generators help factor patterns.

### Typical Patterns

- Fork/Join: replicate or synchronize token flows
- Queues/Buffers: break combinational paths; tune throughput/latency
- Mux/Demux/Arbiter: route based on select/destination or scheduling policy
- Fold/Reduce: reduction with variable-latency operators
- Scope: single-token “critical region” for shared state or hazards
- Loop: for/while-like iteration over elastic state
- Transducer: single-action-per-cycle state machine coupled to handshakes

---

## Citing Chext

If you use Chext in academic work, please contact Canberk, who will provide you
with the necessary information.
