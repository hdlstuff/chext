# Chext Naming Conventions

This guide describes the preferred names for Scala and Chisel source, tracked graph objects,
Elastic interfaces, generated collections, testbenches, emit applications, and generated files.
It distinguishes conventions enforced by Chext from advisory conventions intended to keep source
and generated hardware readable.

For detailed guidance about Chisel `prefix(...)`, Chext `uniquePrefix(...)`, and tracked graph
paths, see [prefixes.md](prefixes.md). For the component and container model, see
[tracking.md](tracking.md).

## Enforcement Versus Style

Chext currently enforces only naming properties required for an unambiguous tracking graph:

- component and container paths should not be empty or duplicated;
- the latest prefix of a component or container must recognizably start with its declared
  `namePrefix`;
- helper-generated paths must remain unique within their module and logical scope.

Most other rules in this document are advisory. In particular, Elastic interfaces are graph edges
whose semantic purpose is often more important than whether they were constructed with `EWire`,
`Source`, or `Sink`.

## General Scala Naming

Use `PascalCase` for types and singleton objects that represent types, constructions, or named
applications:

```scala
class ReadResponseBuffer
case class ReadResponseBufferConfig(...)
object ReadResponseBufferConfig
object ReadResponseBuffer_Emit extends App
```

Use `lowerCamelCase` for methods, parameters, local values, and ordinary singleton helpers:

```scala
val sourceResult = e.EWire(genResult)
val axiCfg = axi4.Config(...)
def connectRead(...): Unit = ...
```

Prefer normal word capitalization for acronyms embedded in Scala identifiers:

```scala
transformAr
axiCfg
readId
```

Protocol-defined external names such as `s_axi`, `m_axi`, and channel names such as `ar` are
intentional exceptions.

## Short Names and Parameters

Use a `w` prefix for width parameters and values:

```scala
wData
wAddr
wId
wIndex
```

Use `n`, `i`, `j`, `x`, and similarly short names only where their scope makes their meaning
immediate. Single-letter values and `idx`/`index` are especially appropriate in monadic collection
expressions:

```scala
sources.map { x => ... }

sources.zipWithIndex.map { case (source, idx) =>
  ...
}

items.foreach { item =>
  ...
}
```

Outside a short lambda or similarly small mathematical scope, prefer a semantic name such as
`sourceIndex`, `portCount`, or `stageIndex`.

Common short domain names include:

- `cfg` for a configuration value;
- `gen` or `genData` for an unbound Chisel type generator;
- `n` for a local count when the meaning is obvious;
- `idx` or `index` inside `map`, `foreach`, `zipWithIndex`, and `Seq.tabulate`;
- `in` and `out` in the protected body of an Elastic construction.

## Configuration Types and Values

Name configuration types `FooConfig`, without an underscore:

```scala
case class MuxConfig(...)
case class ReadConfig(...)
case class BufferConfig(...)
```

Use a companion object with exactly the same name when factories or predefined configurations are
needed:

```scala
case class BufferConfig(req: Int, resp: Int)

object BufferConfig {
  def all(n: Int): BufferConfig = BufferConfig(n, n)
}
```

Name configuration values with a semantic lower-camel name ending in `Cfg` when useful:

```scala
val axiCfg = axi4.Config(...)
val bufferCfg = BufferConfig.all(2)
```

Prefer `FooConfig` and `fooCfg` in new Chext code. Spellings such as `Foo_Config` may exist in
applications or legacy code, but are not the preferred library convention.

## Modules and Module Instances

Use `PascalCase` for a Chisel module class:

```scala
class ReadResponseBuffer(cfg: ReadResponseBufferConfig) extends Module
```

Use the module family name as the beginning of each instance value. Follow it with a numeric,
uppercase semantic, or underscore-separated descriptor:

```scala
val readResponseBuffer0 = Module(new ReadResponseBuffer(cfg))
val readResponseBufferFast = Module(new ReadResponseBuffer(fastCfg))
val readResponseBuffer_write = Module(new ReadResponseBuffer(writeCfg))
```

The important property is that a reader can recognize the instance family at the start of the
name. Avoid unrelated names that hide the instantiated construction:

```scala
val helper = Module(new ReadResponseBuffer(cfg)) // discouraged
```

Use a concise semantic family name when the concrete module type is an implementation detail:

```scala
val stageRead = Module(new ReadStage(cfg))
val cacheData = Module(new Cache(cfg))
```

The semantic name should still start with the chosen family (`stage`, `cache`, and so on), followed
by an uppercase or underscore-separated descriptor.

## Programmatically Generated Collections: `foo_N`

Use a trailing `_N` to show that a Scala value contains multiple instances of the same conceptual
family:

```scala
val stage_N = Seq.tabulate(numStages) { idx =>
  Module(new Stage(stageCfg_N(idx)))
}

val source_N = IO(e.Source.many(numPorts, gen))
val sink_N = IO(e.Sink.many(numPorts, gen))
```

This convention applies to collections of modules, components, interfaces, and related values. The
singular family remains visible at the beginning, and `_N` distinguishes the collection from one
instance such as `stage0` or `sourceTask`.

Give generated elements stable names or prefixes. With tracked components, combine the collection
name with a family-compatible element prefix:

```scala
val transform_N = Seq.tabulate(numTransforms) { idx =>
  prefix(s"transform_$idx") {
    new e.Transform(source_N(idx), sink_N(idx)) {
      out := in
    }
  }
}
```

Names such as `transform_0` and `transform_1` satisfy the `transform` family convention. If the
component is instead bound to a repeated local value, retain both scopes:

```scala
uniquePrefix("transformMany") {
  sources.zip(sinks).zipWithIndex.map { case ((source, sink), idx) =>
    prefix(idx.toString) {
      val transform0 = new e.Transform(source, sink) {
        out := in
      }
      transform0
    }
  }
}
```

This produces deterministic paths shaped like `transformMany0_0_transform0`.

Prefer a stable semantic key over an integer when the generated collection already has one:

```scala
prefix("transformRead") { ... }
prefix("transformWrite") { ... }
```

## Components and Containers

Every tracked component and container declares a conceptual `namePrefix`. Begin its most recent
prefix with that family name:

```scala
val queue0 = new e.Queue(source, sink, 2)

val transformAr0 = new e.Transform(sourceAr, sinkAr) {
  out := in
}

val connectAwRespOut = new e.Connect(sourceAw, sinkAw)
```

The family prefix may be followed by:

- the end of the name: `queue`;
- a digit: `queue0`;
- an uppercase descriptor: `queueRd`, `transformAr`;
- an underscore: `queue_0_1`, `queue_lane0`.

Lowercase continuation without a boundary does not identify the family clearly:

```text
queueing0   // not a Queue-family name
reconnect0  // not a Connect-family name
```

Implementation variants share a conceptual family where appropriate. For example, `Arbiter` and
`ArbiterNs` use the `arbiter` naming family; `Demux` and `DemuxNs` use `demux`.

When a component or container is declared under other scopes, only the latest prefix must identify
its family:

```scala
prefix("read") {
  val transform0 = new e.Transform(source, sink) {
    out := in
  }
}
```

The graph path is shaped like `read_transform0`, while the latest prefix remains `transform0`.

## Prefixes and Helper-Generated Constructions

Use an ordinary Scala `val` when it provides a clear component-family name. Use `prefix(...)` to
name one fixed semantic region or one stable generated element. Use `uniquePrefix(...)` when a
helper can be invoked repeatedly or creates a construction inline.

Sequence helpers normally use three levels:

1. a unique operation scope such as `connectMany0`;
2. an element scope such as `0`;
3. an innermost component-family value such as `connect0`.

For example:

```text
connectMany0_0_connect0
axi4fConnectMany0_1_axi4fConnect0
```

See [prefixes.md](prefixes.md) for the complete prefixing rules and examples.

## Elastic Interfaces and Wires

Elastic interface names are semantic graph-edge names. They are not required to begin with the API
that constructed them.

Use `ewire...` when an internal Elastic wire is generic or temporary:

```scala
val ewire0 = e.EWire(gen)
val ewireSelect = e.EWire(UInt(wSelect.W))
```

Use `source...` or `sink...` when the interface's role is the useful fact:

```scala
val sourceNext = e.EWire(genState)
val sinkCurrent = e.EWire(genState)
val sourceTask = IO(e.Source(genTask))
val sinkResult = IO(e.Sink(genResult))
```

Use a semantic data, protocol, or stage name when it communicates more:

```scala
val fp32_sourceInA = e.EWire(Float32())
val demuxInput = e.EWire(genData)
val taskAr = e.EWire(genTask)
val idLast = e.EWire(new IdLastBundle(wId))
val stage0Result = e.EWire(genResult)
```

These are all valid styles. Chext should diagnose interface connectivity, ownership, and graph-path
problems, but should not reject a meaningful interface name because it lacks `ewire`, `source`, or
`sink` at the beginning.

Use `wire...` for ordinary Chisel wires when that distinction is useful:

```scala
val wireValid = Wire(Bool())
val ewireData = e.EWire(genData)
```

This is advisory; a more semantic ordinary-wire name may be preferable.

## Interface Aggregates and Protocol Channels

Names of aggregate children inherit context from their enclosing `Bundle`, `Record`, `Vec`, or
`NamedVec`. Short field names are appropriate when the aggregate supplies the missing meaning:

```scala
class MemoryPort extends Bundle {
  val req = e.Sink(genRequest)
  val resp = e.Source(genResponse)
}
```

AXI channel names follow the protocol:

```text
ar, r, aw, w, b
```

Do not mechanically expand these to names such as `sinkWriteAddress` inside an AXI aggregate.

Use established external interface names where interoperability matters:

```text
s_axi
m_axi
s_axil
m_axil
s_axis
m_axis
```

Here `s` and `m` are protocol roles, and the underscore spelling intentionally follows common HDL
practice rather than ordinary Scala lower-camel style.

For `Source.many`, `Sink.many`, and `EWire.many`, prefer a plural or `_N` name for the aggregate:

```scala
val sources = IO(e.Source.many(numPorts, gen))
val sinks = IO(e.Sink.many(numPorts, gen))
val ewire_N = e.EWire.many(numPorts, gen)
```

Element names may be numeric or use a stable custom `NamedVec.Naming` policy.

## Buffers: `Buffer` and `Buffered`

`SourceBuffer`, `SinkBuffer`, `LeftBuffer`, and `RightBuffer` add an internal unique prefix. They
are suitable for inline use:

```scala
source :=> e.SinkBuffer(sink)
```

`SourceBuffered` and `SinkBuffered` return the buffered interface without adding a unique helper
scope. Bind a reusable result to a descriptive value:

```scala
val sourceBuffered = e.SourceBuffered(source)
val sinkBuffered = e.SinkBuffered(sink)
```

Use `_N` for sequence results:

```scala
val sourceBuffered_N = e.SourceBuffered(source_N, 2)
val sinkBuffered_N = e.SinkBuffered(sink_N, 2)
```

The same distinction applies to corresponding AXI and memory buffer helpers.

## Registers and State

Use `r` followed by an uppercase semantic name for a register when the register/wire distinction is
important in a dense datapath:

```scala
val rState = RegInit(false.B)
val rIndex = RegInit(0.U(wIndex.W))
```

A direct semantic name is also appropriate when the construction already makes storage obvious:

```scala
val outstanding = RegInit(0.U)
```

Avoid encoding implementation details mechanically when doing so makes the name less readable.

## Scala Testbenches

Place Scala testbench generators in test scope and use the suffix `.tb.scala`:

```text
src/test/scala/chext/elastic/Fold.tb.scala
```

Name the generated hardware wrapper `Foo_Tbtop`:

```scala
class Fold_Tbtop extends Module with chext.AnnotatedModule
```

Name the runner `Foo_Tb`:

```scala
object Fold_Tb extends chext.TestBench {
  emit(new Fold_Tbtop)
}
```

Use a semantic descriptor before the suffix for variants:

```scala
class ReadDropEmpty_Tbtop extends Module
object ReadDropEmpty_Tb extends chext.TestBench
```

Alternatively, one runner may emit several variants with distinct `desiredName` values when that
better matches the SystemC testbench. Do not hand-author `_1`, `_2`, and similar suffixes merely to
imitate names assigned by an emission tool. Generated numeric suffixes describe artifact-name
collisions, not source-level semantic identity.

Diagnostic applications that do not use the ordinary `chext.TestBench` flow may use a descriptor:

```scala
object TrackingDiagnostics_Tb extends App
object ConnectDiag_Tb extends App
```

## Emit Applications

Place ad hoc or documentation-oriented emit applications in test scope and use `.emit.scala`:

```text
src/test/scala/chext/amba/axi4/Interface.emit.scala
```

Name a public emit application `Foo_Emit`:

```scala
object ProtocolConverter_Emit extends App
object RAM_Emit extends App
```

If the application needs a wrapper module, give the wrapper a normal type name such as
`FullInterfaceEmitDevice`; the `_Emit` suffix is reserved for the runnable application.

Keep an emit application in main scope only when it is intentionally part of the supported library
or tooling API.

## SystemC Testbenches

Name a SystemC testbench source `Foo.tb.cpp` and keep it under the corresponding `sysc_tb` package
directory:

```text
sysc_tb/chext/elastic/src/Fold.tb.cpp
```

Include the generated wrapper matching the Scala `Tbtop` name and call the DUT value `dut` when one
device is tested:

```cpp
#include <Fold_Tbtop.hpp>

Fold_Tbtop dut;
```

Use `dut0`, `dut1`, or semantic family-prefixed DUT names when several devices are present.
Functions such as `resetDUT()` or `resetDUTs()` may retain the established C++ capitalization.

## Generated HDL and Metadata

Generated file and top-level module names follow the Scala module's `desiredName`:

```text
Foo_Tbtop.sv
Foo_Tbtop.hdlinfo.json
Foo_Tbtop.moduleGraph.json
```

If several differently configured modules share one base desired name, the emission flow may append
numeric suffixes such as `_1` and `_2`. C++ includes must match the actual emitted names:

```cpp
#include <Downscale_Tbtop_1.hpp>
#include <Downscale_Tbtop_2.hpp>
```

Prefer assigning semantic desired names when the variants have stable meanings. Accept generated
numeric suffixes when they simply enumerate configurations owned by one test runner.

## File and Package Names

Use lowercase package directories matching Scala package structure. Name a primary production
source file after its main public type:

```text
src/main/scala/chext/elastic/Queue.scala
src/main/scala/chext/amba/axi4/full/components/Mux.scala
```

Use the established multi-part suffixes for test and emit sources:

```text
Queue.tb.scala
Components.emit.scala
```

A file containing several closely related constructions may use their shared family name rather
than one individual type.

## Review Checklist

When adding a construction or test, check that:

- a module or tracked construction begins with a recognizable family name;
- semantic descriptors follow that family with an uppercase letter or underscore;
- generated collections use a clear plural or `_N` name;
- each generated element has a stable semantic or numeric prefix;
- configuration types use `FooConfig` and values use names such as `fooCfg`;
- Elastic interfaces communicate role, payload, protocol purpose, or stage without artificial
  vocabulary enforcement;
- protocol aggregates retain conventional channel and external interface names;
- testbench wrappers use `Foo_Tbtop` and runners use `Foo_Tb`;
- emit applications use `.emit.scala` and `Foo_Emit`;
- SystemC sources and generated artifacts match the emitted `Tbtop` desired name.
