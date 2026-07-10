<!-- Generated from docs/synopsis.txt by scripts/generate_synopsis.py. Do not edit by hand. -->
# Chext Synopsis for GitHub

This is a working synopsis of the main Chext constructions, their intended use, and how they appear in the tracking/module graph. For the underlying component model, see [tracking.md](tracking.md). For elastic-specific tracking and AXI4 DataView recovery, see [tracking-elastic.md](tracking-elastic.md). For detailed AXI4 component guides, see [axi4-full.md](axi4-full.md) and [axi4-lite.md](axi4-lite.md).

## Contents

- [Elastic Imports](#elastic-imports)
- [Elastic Interfaces and Operators](#elastic-interfaces-and-operators)
- [Elastic Components](#elastic-components)
- [Elastic Containers](#elastic-containers)
- [AXI4 Imports](#axi4-imports)
- [AXI4 Interfaces and Connects](#axi4-interfaces-and-connects)
- [AXI4 Buffers](#axi4-buffers)
- [AXI4 Full Components](#axi4-full-components)
- [AXI4 Lite Components](#axi4-lite-components)
- [Memory](#memory)
- [Stream](#stream)
- [Load/Store](#loadstore)
- [Float](#float)

## Elastic Imports

Elastic examples assume:

```scala
import chext.{elastic => e}
import e.ConnectOp._
```

## Elastic Interfaces and Operators

<a id="entry-elastic-interfaces-e-interface"></a>

### `e.Interface(gen)` [#](#entry-elastic-interfaces-e-interface)

**Sources:** [Scala](../src/main/scala/chext/elastic/Interface.scala)

**Intent and Usage:** For Chisel types:
```scala
val genElastic = e.Interface(UInt(32.W))
```

**Details:** Interface. Not a component. When materialized as hardware and tracked, appears as `chext.elastic.Interface[...]`.

---

<a id="entry-elastic-interfaces-e-source"></a>

### `e.Source(gen)` [#](#entry-elastic-interfaces-e-source)

**Sources:** [Scala](../src/main/scala/chext/elastic/Interface.scala)

**Intent and Usage:** Producer endpoint from the module's perspective:
```scala
val source = IO(e.Source(UInt(32.W)))
```

**Details:** Interface. Root IO appears under graph `sources`.

---

<a id="entry-elastic-interfaces-e-sink"></a>

### `e.Sink(gen)` [#](#entry-elastic-interfaces-e-sink)

**Sources:** [Scala](../src/main/scala/chext/elastic/Interface.scala)

**Intent and Usage:** Consumer endpoint from the module's perspective:
```scala
val sink = IO(e.Sink(UInt(32.W)))
```

**Details:** Interface. Root IO appears under graph `sinks`.

---

<a id="entry-elastic-interfaces-e-ewire"></a>

### `e.EWire(gen)` [#](#entry-elastic-interfaces-e-ewire)

**Sources:** [Scala](../src/main/scala/chext/elastic/Interface.scala)

**Intent and Usage:** Internal elastic wire:
```scala
val ewire0 = e.EWire(UInt(32.W))
```

**Details:** Interface. Usually emitted under graph `wires`; path follows the Chisel/Scala name, for example `/ewire0`.

---

<a id="entry-elastic-interfaces-e-source-like-e-sink-like-e-ewire-like-e-source-manylike-e-sink-manylike-e-ewire-manylike"></a>

### `e.Source.like`, `e.Sink.like`, `e.EWire.like`, `e.Source.manyLike`, `e.Sink.manyLike`, `e.EWire.manyLike` [#](#entry-elastic-interfaces-e-source-like-e-sink-like-e-ewire-like-e-source-manylike-e-sink-manylike-e-ewire-manylike)

**Sources:** [Scala](../src/main/scala/chext/elastic/Interface.scala)

**Intent and Usage:** Preserve the payload type of an existing elastic interface:
```scala
val ewire0 = e.EWire.like(source)
val wires = e.EWire.manyLike(4, source)
```

**Details:** Interface. Creates typed elastic interfaces from existing hardware. `e.EWire.like` and `e.EWire.manyLike` return internal wires.

---

<a id="entry-elastic-interfaces-e-source-many-e-sink-many-e-interface-many-e-ewire-many"></a>

### `e.Source.many`, `e.Sink.many`, `e.Interface.many`, `e.EWire.many` [#](#entry-elastic-interfaces-e-source-many-e-sink-many-e-interface-many-e-ewire-many)

**Sources:** [Scala](../src/main/scala/chext/elastic/Interface.scala)

**Intent and Usage:** Similar to the single-interface constructors, but returns `chext.util.NamedVec[e.Interface[T]]`. `e.EWire.many` returns an internal `Wire` of that shape:
```scala
val sources = IO(e.Source.many(4, UInt(32.W)))
val wires = e.EWire.many(4, UInt(32.W))
```

**Details:** Interface. `NamedVec` naming strictly uses underscores, for example `/sources_0`, `/sources_1`.

---

<a id="entry-elastic-interfaces-source-connect-sink"></a>

### `source :=> sink` [#](#entry-elastic-interfaces-source-connect-sink)

**Sources:** [Scala](../src/main/scala/chext/elastic/Connect.scala)

**Intent and Usage:** Connect elastic interfaces:
```scala
source :=> sink
```

**Details:** Creates `Connect`; payload uses Chisel's standard `:=` connection and name-based aggregate field matching; no user-defined transformation is applied. Component; UniquePrefix: connect.

---

<a id="entry-elastic-interfaces-sources-connect-sinks"></a>

### `sources :=> sinks` [#](#entry-elastic-interfaces-sources-connect-sinks)

**Sources:** [Scala](../src/main/scala/chext/elastic/Connect.scala)

**Intent and Usage:** Connect matching sequences:
```scala
sources :=> sinks
```

**Details:** Creates one `Connect` per pair; Component; UniquePrefix: connectMany with per-index prefixes and an innermost `connect0` value prefix, for example `connectMany0_0_connect0`.

---

<a id="entry-elastic-interfaces-e-zip"></a>

### `e.Zip(...)` [#](#entry-elastic-interfaces-e-zip)

**Sources:** [Scala](../src/main/scala/chext/elastic/Zip.scala)

**Intent and Usage:** Join sources into a bundle-valued elastic interface:
```scala
val zipped = e.Zip(sourceA, sourceB)
```

**Details:** Function. Not its own graph node; internally creates join-style wiring and a result wire.

---

## Elastic Components

Many elastic components support `fire { ... }`, a protected hook that runs when the component's sink interface fires. The `Stall` row shows the pattern.

Buffer naming convention: `SourceBuffer`, `SinkBuffer`, `LeftBuffer`, and `RightBuffer` wrap their internal implementation in `uniquePrefix(name)` and are usually used inline in a connection expression. `SourceBuffered` and `SinkBuffered` return a buffered interface directly and do not add an internal `uniquePrefix`, including for sequence overloads; bind them to a `val` when the buffered interface is the thing you want to keep using.

<a id="entry-elastic-components-e-connect"></a>

### `e.Connect` [#](#entry-elastic-components-e-connect)

**Sources:** [Scala](../src/main/scala/chext/elastic/Connect.scala)

**Intent and Usage:** Standard Chisel payload connection with no user-defined transformation. Prefer `source :=> sink` unless a named value is useful:
```scala
val connect0 = new e.Connect(source, sink)
```

**Details:** Uses `sink.bits := source.bits`, including Chisel's name-based matching for compatible aggregates. Component; tpe: Connect; namePrefix: connect Ports: `source`, `sink`.

---

<a id="entry-elastic-components-e-transform"></a>

### `e.Transform` [#](#entry-elastic-components-e-transform)

**Sources:** [Scala](../src/main/scala/chext/elastic/Transform.scala)

**Intent and Usage:** Explicit combinational payload transformation:
```scala
val transform0 = new e.Transform(source, sink) {
  out := f(in)
}
```

**Details:** Forwards ready/valid but does not connect the payload automatically; the body must assign protected `out` from protected `in`. Component; tpe: Transform; namePrefix: transform Ports: `source`, `sink`.

---

<a id="entry-elastic-components-e-stall"></a>

### `e.Stall` [#](#entry-elastic-components-e-stall)

**Sources:** [Scala](../src/main/scala/chext/elastic/Stall.scala)

**Intent and Usage:** Conditionally hold a token; supports `fire`:
```scala
val stall0 = new e.Stall(source, sink) {
  out := in
  cond { shouldStall }
  fire { didPass := true.B }
}
```

**Details:** Component; tpe: Stall; namePrefix: stall. Ports: `source`, `sink`.

---

<a id="entry-elastic-components-e-drop"></a>

### `e.Drop` [#](#entry-elastic-components-e-drop)

**Sources:** [Scala](../src/main/scala/chext/elastic/Drop.scala)

**Intent and Usage:** Conditionally drop a token:
```scala
val drop0 = new e.Drop(source, sink) {
  out := in
  cond { shouldDrop }
}
```

**Details:** Component; tpe: Drop; namePrefix: drop. Ports: `source`, `sink`.

---

<a id="entry-elastic-components-e-transducer"></a>

### `e.Transducer` [#](#entry-elastic-components-e-transducer)

**Sources:** [Scala](../src/main/scala/chext/elastic/Transducer.scala); [Scala TB](../src/test/scala/chext/elastic/Transducer.tb.scala); [SysC TB](../sysc_tb/chext/elastic/src/Transducer.tb.cpp)

**Intent and Usage:** Stateful packet-level control:
```scala
val transducer0 = new e.Transducer(source, sink) {
  packet {
    when(done) {
      consume { state := 0.U }
    }.otherwise {
      accept { out := in }
    }
  }
}
```

**Details:** Component; tpe: Transducer; namePrefix: transducer. Ports: `source`, `sink`. Actions: `stall`, `accept`, `consume`, `produce`.

---

<a id="entry-elastic-components-e-queue"></a>

### `e.Queue` [#](#entry-elastic-components-e-queue)

**Sources:** [Scala](../src/main/scala/chext/elastic/Queue.scala); [SysC TB](../sysc_tb/chext/elastic/src/Queue.tb.cpp)

**Intent and Usage:** Explicit queue between existing endpoints:
```scala
val queue0 =
  new e.Queue(source, sink, count = 2)
```

**Details:** Component; tpe: Queue; namePrefix: queue. Ports: `source`, `sink`.

---

<a id="entry-elastic-components-e-source-buffer"></a>

### `e.SourceBuffer` [#](#entry-elastic-components-e-source-buffer)

**Sources:** [Scala](../src/main/scala/chext/elastic/Buffer.scala); [Scala TB](../src/test/scala/chext/elastic/BufferedNaming.tb.scala)

**Intent and Usage:** Insert a queue after a source, usually inline in a connection:
```scala
e.SourceBuffer(source, count = 2) :=> sink
```

**Details:** Function. Creates a returned interface wrapped in UniquePrefix: sourceBuffer. Hierarchy: source -> `Queue` -> returned interface. Underlying graph node is tpe: Queue, often with a path like `/sourceBuffer0_queue0`.

---

<a id="entry-elastic-components-e-sink-buffer"></a>

### `e.SinkBuffer` [#](#entry-elastic-components-e-sink-buffer)

**Sources:** [Scala](../src/main/scala/chext/elastic/Buffer.scala); [Scala TB](../src/test/scala/chext/elastic/BufferedNaming.tb.scala)

**Intent and Usage:** Insert a queue before a sink, usually inline in a connection:
```scala
source :=> e.SinkBuffer(sink, count = 2)
```

**Details:** Function. Creates a returned interface wrapped in UniquePrefix: sinkBuffer. Hierarchy: returned interface -> `Queue` -> sink. Underlying graph node is tpe: Queue, often with a path like `/sinkBuffer0_queue0`.

---

<a id="entry-elastic-components-e-left-buffer-e-right-buffer"></a>

### `e.LeftBuffer` / `e.RightBuffer` [#](#entry-elastic-components-e-left-buffer-e-right-buffer)

**Sources:** [Scala](../src/main/scala/chext/elastic/Buffer.scala); [Scala TB](../src/test/scala/chext/elastic/BufferedNaming.tb.scala)

**Intent and Usage:** Generic aliases for source-side / sink-side buffering:
```scala
e.LeftBuffer(source) :=> sink
source :=> e.RightBuffer(sink)
```

**Details:** Function. Same hierarchy as `SourceBuffer` / `SinkBuffer`; default prefixes are `leftBuffer` / `rightBuffer`.

---

<a id="entry-elastic-components-e-source-buffered"></a>

### `e.SourceBuffered` [#](#entry-elastic-components-e-source-buffered)

**Sources:** [Scala](../src/main/scala/chext/elastic/Buffer.scala); [Scala TB](../src/test/scala/chext/elastic/BufferedNaming.tb.scala)

**Intent and Usage:** Return a buffered source-side interface for reuse as a named value:
```scala
val sourceBuffered0 =
  e.SourceBuffered(source, count = 2)
```

**Details:** Function. Creates a `Queue` but does not wrap the single-interface or sequence form in `uniquePrefix`; the Scala `val` name is the important handle. Sequence overloads add only per-element index prefixes, for example `sourceBuffered_0_queue0`.

---

<a id="entry-elastic-components-e-sink-buffered"></a>

### `e.SinkBuffered` [#](#entry-elastic-components-e-sink-buffered)

**Sources:** [Scala](../src/main/scala/chext/elastic/Buffer.scala); [Scala TB](../src/test/scala/chext/elastic/BufferedNaming.tb.scala)

**Intent and Usage:** Return a buffered sink-side interface for reuse as a named value:
```scala
val sinkBuffered0 =
  e.SinkBuffered(sink, count = 2)
```

**Details:** Function. Creates a `Queue` but does not wrap the single-interface or sequence form in `uniquePrefix`; the Scala `val` name is the important handle. Sequence overloads add only per-element index prefixes, for example `sinkBuffered_0_queue0`.

---

<a id="entry-elastic-components-e-null-sink"></a>

### `e.NullSink` [#](#entry-elastic-components-e-null-sink)

**Sources:** [Scala](../src/main/scala/chext/elastic/NullSink.scala); [Scala TB](../src/test/scala/chext/elastic/Null.tb.scala)

**Intent and Usage:** Consume/drop all tokens from a source:
```scala
val nullSink0 =
  new e.NullSink(source)
```

**Details:** Component; tpe: NullSink; namePrefix: nullSink. Port: `source`.

---

<a id="entry-elastic-components-e-null-source"></a>

### `e.NullSource` [#](#entry-elastic-components-e-null-source)

**Sources:** [Scala](../src/main/scala/chext/elastic/NullSource.scala); [Scala TB](../src/test/scala/chext/elastic/Null.tb.scala)

**Intent and Usage:** Drive a sink with no valid tokens:
```scala
val nullSource0 =
  new e.NullSource(sink)
```

**Details:** Component; tpe: NullSource; namePrefix: nullSource. Port: `sink`.

---

<a id="entry-elastic-components-e-fork"></a>

### `e.Fork` [#](#entry-elastic-components-e-fork)

**Sources:** [Scala](../src/main/scala/chext/elastic/Fork.scala)

**Intent and Usage:** Branch one source to several sinks:
```scala
val fork0 = new e.Fork(source) {
  fork(in.lo) :=> sinkLo
  fork(in.hi) :=> sinkHi
}
```

**Details:** Component; tpe: Fork; namePrefix: fork. Ports: `source`, `sink_0`, `sink_1`, ... The current implementation is eager, but the construction does not have to be conceptually limited to eager implementations.

---

<a id="entry-elastic-components-e-join"></a>

### `e.Join` [#](#entry-elastic-components-e-join)

**Sources:** [Scala](../src/main/scala/chext/elastic/Join.scala)

**Intent and Usage:** Join several sources into one sink:
```scala
val join0 = new e.Join(sink) {
  out := join(sourceA) + join(sourceB)
}
```

**Details:** Component; tpe: Join; namePrefix: join. Ports: `source_0`, `source_1`, ..., `sink`.

---

<a id="entry-elastic-components-e-merger"></a>

### `e.Merger` [#](#entry-elastic-components-e-merger)

**Sources:** [Scala](../src/main/scala/chext/elastic/Merger.scala)

**Intent and Usage:** Merge multiple sources into one sink:
```scala
val merger0 =
  new e.Merger(Seq(source0, source1), sink)
```

**Details:** Component; tpe: Merger; namePrefix: merger. Ports: `source_i`, `sink`. Requires a strong external guarantee that at most one input is active at a given time.

---

<a id="entry-elastic-components-e-mux"></a>

### `e.Mux` [#](#entry-elastic-components-e-mux)

**Sources:** [Scala](../src/main/scala/chext/elastic/Mux.scala)

**Intent and Usage:** Select one source by an elastic select stream:
```scala
val mux0 =
  new e.Mux(sources, sink, sourceSelect)
```

**Details:** Component; tpe: Mux; namePrefix: mux. Ports: `source_i`, `sourceSelect`, `sink`.

---

<a id="entry-elastic-components-e-demux"></a>

### `e.Demux` [#](#entry-elastic-components-e-demux)

**Sources:** [Scala](../src/main/scala/chext/elastic/Demux.scala)

**Intent and Usage:** Route one source to selected sink:
```scala
val demux0 =
  new e.Demux(source, sinks, sourceSelect)
```

**Details:** Component; tpe: Demux; namePrefix: demux. Ports: `source`, `sourceSelect`, `sink_i`.

---

<a id="entry-elastic-components-e-arbiter"></a>

### `e.Arbiter` [#](#entry-elastic-components-e-arbiter)

**Sources:** [Scala](../src/main/scala/chext/elastic/Arbiter.scala); [Scala TB](../src/test/scala/chext/elastic/Arbiter.tb.scala); [SysC TB](../sysc_tb/chext/elastic/src/Arbiter.tb.cpp)

**Intent and Usage:** Select among sources and emit the selected index:
```scala
val arbiter0 =
  new e.Arbiter(sources, sink, sinkSelect, e.Chooser.rr)
```

**Details:** Component; tpe: Arbiter; namePrefix: arbiter. Ports: `source_i`, `sink`, `sinkSelect`.

---

<a id="entry-elastic-components-e-arbiter-ns"></a>

### `e.ArbiterNs` [#](#entry-elastic-components-e-arbiter-ns)

**Sources:** [Scala](../src/main/scala/chext/elastic/ArbiterNs.scala)

**Intent and Usage:** Select among sources without a selected-index stream:
```scala
val arbiter0 =
  new e.ArbiterNs(sources, sink, e.Chooser.rr)
```

**Details:** Component; tpe: ArbiterNs; namePrefix: arbiter. Ports: `source_i`, `sink`. The no-select implementation shares the `arbiter` naming family.

---

<a id="entry-elastic-components-e-demux-ns"></a>

### `e.DemuxNs` [#](#entry-elastic-components-e-demux-ns)

**Sources:** [Scala](../src/main/scala/chext/elastic/DemuxNs.scala)

**Intent and Usage:** Route by a protected select function:
```scala
val demux0 = new e.DemuxNs(source, sinks) {
  select { in => in.index }
}
```

**Details:** Component; tpe: DemuxNs; namePrefix: demux. Ports: `source`, `sink_i`. The no-select implementation shares the `demux` naming family.

---

<a id="entry-elastic-components-e-count"></a>

### `e.Count` [#](#entry-elastic-components-e-count)

**Sources:** [Scala](../src/main/scala/chext/elastic/Count.scala); [Scala TB](../src/test/scala/chext/elastic/Count.tb.scala)

**Intent and Usage:** Stateful repeat/count primitive:
```scala
val count0 = new e.Count(source, sink, UInt(8.W)) {
  init { in => 0.U }
  cond { (in, state) => state =/= in.limit }
  next { (in, state) => state + 1.U }
  out { (in, state, first, last) => in }
}
```

**Details:** Component; tpe: Count; namePrefix: count. Ports: `source`, `sink`.

---

<a id="entry-elastic-components-self-alias-form"></a>

### Self-alias form [#](#entry-elastic-components-self-alias-form)

**Sources:** [Scala](../src/main/scala/chext/elastic/Count.scala); [Scala TB](../src/test/scala/chext/elastic/Count.tb.scala)

**Intent and Usage:** Useful when nesting components and you want a stable name for the outer anonymous instance:
```scala
val count0 = new e.Count(source, sink, UInt(8.W)) { count0 =>
  count0.init { in => 0.U }
}
```

**Details:** Scala construction style only; graph behavior is unchanged.

---

<a id="entry-elastic-components-e-once"></a>

### `e.Once` [#](#entry-elastic-components-e-once)

**Sources:** [Scala](../src/main/scala/chext/elastic/Once.scala)

**Intent and Usage:** One-shot source, explicit component form:
```scala
val sinkOnce = e.EWire(UInt(8.W))
val once0 = new e.Once(sinkOnce) {
  out := value
}
```

**Details:** Component; tpe: Once; namePrefix: once. Port: `sink`.

---

<a id="entry-elastic-components-e-once-2"></a>

### `e.Once(value)` [#](#entry-elastic-components-e-once-2)

**Sources:** [Scala](../src/main/scala/chext/elastic/Once.scala)

**Intent and Usage:** Functional one-shot form:
```scala
val sourceOnce = e.Once(value)
```

**Details:** Function. Creates an internal `EWire` and a child `Once` component named `once0` by local val.

---

<a id="entry-elastic-components-e-counter"></a>

### `e.Counter` [#](#entry-elastic-components-e-counter)

**Sources:** [Scala](../src/main/scala/chext/elastic/Counter.scala)

**Intent and Usage:** Explicit component form:
```scala
val sinkCounter = e.EWire(UInt(8.W))
val counter0 = new e.Counter(sinkCounter)
```

**Details:** Component; tpe: Counter; namePrefix: counter. Port: `sink`.

---

<a id="entry-elastic-components-e-counter-e-counter-from-width"></a>

### `e.Counter(...)` / `e.Counter.fromWidth(...)` [#](#entry-elastic-components-e-counter-e-counter-from-width)

**Sources:** [Scala](../src/main/scala/chext/elastic/Counter.scala)

**Intent and Usage:** Functional source form:
```scala
val sourceCounter =
  e.Counter(maxValueExclusive = 16)
```

**Details:** Function. Creates an internal `EWire` and a child `Counter` component named `counter0` by local val.

---

<a id="entry-elastic-components-e-wrap"></a>

### `e.Wrap` [#](#entry-elastic-components-e-wrap)

**Sources:** [Scala](../src/main/scala/chext/elastic/Wrap.scala)

**Intent and Usage:** Wrap delayed logic:
```scala
val wrap0 = new e.Wrap(source, sink) {
  protected def delay = 2
  out := f(in)
}
```

**Details:** Component; tpe: Wrap; namePrefix: wrap. Nonzero delay internally creates `Counter`, `Queue`, and `Connect`.

---

## Elastic Containers

<a id="entry-elastic-containers-e-repeat"></a>

### `e.Repeat` [#](#entry-elastic-containers-e-repeat)

**Sources:** [Scala](../src/main/scala/chext/elastic/Repeat.scala); [Scala TB](../src/test/scala/chext/elastic/Count.tb.scala)

**Intent and Usage:** Repeat one input token multiple times:
```scala
val repeat0 = new e.Repeat(source, sink, wIndex = 8) {
  len { in => in.length }
  out { (in, index, first, last) => in }
}
```

**Details:** Container; tpe: Repeat; namePrefix: repeat. Hierarchy: creates a child `Count` under `withContainer(this)`, with observed path shape `/repeat0` -> `/repeat0_count`.

---

<a id="entry-elastic-containers-e-fold"></a>

### `e.Fold` [#](#entry-elastic-containers-e-fold)

**Sources:** [Scala](../src/main/scala/chext/elastic/Fold.scala); [Scala TB](../src/test/scala/chext/elastic/Fold.tb.scala); [SysC TB](../sysc_tb/chext/elastic/src/Fold.tb.cpp)

**Intent and Usage:** Reduce a stream using user-provided fold logic:
```scala
val fold0 = new e.Fold(source, sourceInit, sink) {
  operand { in => in.data }
  last { in => in.last }
  val join0 = new e.Join(sourceResult) {
    out := join(sinkA) + join(sinkB)
  }
}
```

**Details:** Container; tpe: Fold; namePrefix: fold. User-facing internal wires: `sinkA`, `sinkB`, `sourceResult`. Hierarchy: stage wiring under `stage0` and `stage1`; children include `Transducer` or `Transform`, `Join`, `Connect`, and buffers attached to the `Fold` container.

---

<a id="entry-elastic-containers-e-loop"></a>

### `e.Loop` [#](#entry-elastic-containers-e-loop)

**Sources:** [Scala](../src/main/scala/chext/elastic/Loop.scala)

**Intent and Usage:** Iterative elastic loop with user-visible body endpoints:
```scala
val loop0 = new e.Loop(sourceInit, sinkExit) {
  end { state => state.done }
  sinkCurrent :=> loopBody.source
  loopBody.sink :=> sourceNext
}
```

**Details:** Container; tpe: Loop; namePrefix: loop. User-facing internal wires: `sinkCurrent`, `sourceNext`. Hierarchy: creates `Stall`, `Connect`, `Fork`, `Demux`, `Merger`, and buffers attached to the `Loop` container.

---

<a id="entry-elastic-containers-e-scope"></a>

### `e.Scope` [#](#entry-elastic-containers-e-scope)

**Sources:** [Scala](../src/main/scala/chext/elastic/Scope.scala)

**Intent and Usage:** Begin/end scoped elastic region:
```scala
val scope0 = new e.Scope(sourceInit, sinkExit) {
  init { in => started := true.B }
  exit { out => finished := true.B }
  sinkBegin :=> body.source
  body.sink :=> sourceEnd
}
```

**Details:** Container; tpe: Scope; namePrefix: scope. User-facing internal wires: `sinkBegin`, `sourceEnd`. Hierarchy: creates a `Stall`, a `Connect`, and sink buffers attached to the `Scope` container.

---

<a id="entry-elastic-containers-e-random-stall"></a>

### `e.RandomStall` [#](#entry-elastic-containers-e-random-stall)

**Sources:** [Scala](../src/main/scala/chext/elastic/RandomStall.scala)

**Intent and Usage:** Test/diagnostic random backpressure:
```scala
val randomStall0 =
  new e.RandomStall(source, sink)
```

**Details:** Container; tpe: RandomStall; namePrefix: randomStall. Hierarchy: creates `Stall` plus `SinkBuffer`; observed child paths include `/randomStall0_stall` and `/randomStall0_stall_sinkBuffer0_queue0`.

---

## AXI4 Imports

AXI examples assume:

```scala
import chext.amba.axi4
import axi4.{full => axi4f, lite => axi4l}
import axi4.ConnectOp._
import chext.{elastic => e}
```

## AXI4 Interfaces and Connects

<a id="entry-axi-connects-axi4-config"></a>

### `axi4.Config` [#](#entry-axi-connects-axi4-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/Interface.scala)

**Intent and Usage:** Configuration:
```scala
val axiCfg = axi4.Config(
  wId = 4,    // ID width; 0 is common for lite
  wAddr = 32, // byte address width
  wData = 64, // data bus width
  read = true,
  write = true,
  lite = false,
  hasLock = true,
  hasCache = true,
  hasProt = true,
  hasQos = true,
  hasRegion = true,
  axi3Compat = false,
  wUserAR = 0,
  wUserR = 0,
  wUserAW = 0,
  wUserW = 0,
  wUserB = 0
)
```

**Details:** Config. Configuration for raw, full, and lite AXI interfaces. `lite = true` selects AXI4-Lite behavior; full AXI keeps `lite = false`.

---

<a id="entry-axi-connects-full-connect-config"></a>

### `axi4f.ConnectConfig` [#](#entry-axi-connects-full-connect-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/Connect.scala)

**Intent and Usage:** Configured full AXI connect diagnostics:
```scala
val cfg = axi4f.ConnectConfig(
  tieOffMaster = true,
  tieOffSlave = true,
  warnReadWriteMismatch = true,
  warnAxi3Compat = true,
  warnIdWidth = true,
  warnAddrWidth = true,
  warnSideband = true,
  // SimulationCheck: Default, None, Printf, Assert.
  // Default resolves to the process-wide policy.
  simCheckAxi3Compat = chext.util.SimulationCheck.Default,
  simCheckIdWidth = chext.util.SimulationCheck.Default,
  simCheckAddrWidth = chext.util.SimulationCheck.Default
)
```

**Details:** Config. Configuration for `s_axi.connect(m_axi, cfg)`: controls tie-offs, warnings, and simulation checks for full AXI connections.

---

<a id="entry-axi-connects-lite-connect-config"></a>

### `axi4l.ConnectConfig` [#](#entry-axi-connects-lite-connect-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/Connect.scala)

**Intent and Usage:** Configured AXI4-Lite connect diagnostics:
```scala
val cfg = axi4l.ConnectConfig(
  tieOffMaster = true,
  tieOffSlave = true,
  warnReadWriteMismatch = true,
  warnAddrWidth = true,
  // SimulationCheck: Default, None, Printf, Assert.
  // Default resolves to the process-wide policy.
  simCheckAddrWidth = chext.util.SimulationCheck.Default
)
```

**Details:** Config. Configuration for `s_axil.connect(m_axil, cfg)`: controls tie-offs, warnings, and address-width simulation checks for AXI4-Lite connections.

---

<a id="entry-axi-connects-full-slave-master"></a>

### `axi4f.Slave` / `axi4f.Master` [#](#entry-axi-connects-full-slave-master)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/Interface.scala)

**Intent and Usage:** Full AXI IO:
```scala
val s_axi = IO(axi4f.Slave(axiCfg))
val m_axi = IO(axi4f.Master(axiCfg))
```

**Details:** Interface. Channel-level elastic interfaces: `ar`, `r`, `aw`, `w`, `b`.

---

<a id="entry-axi-connects-lite-slave-master"></a>

### `axi4l.Slave` / `axi4l.Master` [#](#entry-axi-connects-lite-slave-master)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/Interface.scala)

**Intent and Usage:** AXI4-Lite IO:
```scala
val s_axil = IO(axi4l.Slave(axiCfg))
val m_axil = IO(axi4l.Master(axiCfg))
```

**Details:** Interface. Channel-level elastic interfaces: `ar`, `r`, `aw`, `w`, `b`.

---

<a id="entry-axi-connects-full-single-connect"></a>

### Full single connect [#](#entry-axi-connects-full-single-connect)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/Connect.scala); [Scala TB](../src/test/scala/chext/amba/axi4/full/Connect.tb.scala)

**Intent and Usage:** Connect one full AXI interface pair:
```scala
s_axi :=> m_axi
```

**Details:** Creates `axi4f.Connect`; UniquePrefix: axi4fConnect; graph `tpe` is `Axi4f_Connect`.

---

<a id="entry-axi-connects-full-configured-connect"></a>

### Full configured connect [#](#entry-axi-connects-full-configured-connect)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/Connect.scala)

**Intent and Usage:** Same, with diagnostics/tie-off config:
```scala
s_axi.connect(m_axi, axi4f.ConnectConfig())
```

**Details:** Same component and prefix as full single connect.

---

<a id="entry-axi-connects-full-many-connect"></a>

### Full many connect [#](#entry-axi-connects-full-many-connect)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/Connect.scala)

**Intent and Usage:** Connect matching full AXI sequences:
```scala
s_axi_N :=> m_axi_N
```

**Details:** Creates one full connect per pair; UniquePrefix: axi4fConnectMany with per-index prefixes and an innermost `axi4fConnect0` value prefix, for example `axi4fConnectMany0_0_axi4fConnect0`.

---

<a id="entry-axi-connects-lite-single-connect"></a>

### Lite single connect [#](#entry-axi-connects-lite-single-connect)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/Connect.scala); [Scala TB](../src/test/scala/chext/amba/axi4/lite/Connect.tb.scala)

**Intent and Usage:** Connect one AXI4-Lite pair:
```scala
s_axil :=> m_axil
```

**Details:** Creates `axi4l.Connect`; UniquePrefix: axi4lConnect; graph `tpe` is `Axi4l_Connect`.

---

<a id="entry-axi-connects-lite-configured-connect"></a>

### Lite configured connect [#](#entry-axi-connects-lite-configured-connect)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/Connect.scala); [Scala TB](../src/test/scala/chext/amba/axi4/lite/Connect.tb.scala)

**Intent and Usage:** Same, with diagnostics/tie-off config:
```scala
s_axil.connect(m_axil, axi4l.ConnectConfig())
```

**Details:** Same component and prefix as lite single connect.

---

<a id="entry-axi-connects-lite-many-connect"></a>

### Lite many connect [#](#entry-axi-connects-lite-many-connect)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/Connect.scala)

**Intent and Usage:** Connect matching AXI4-Lite sequences:
```scala
s_axil_N :=> m_axil_N
```

**Details:** Creates one lite connect per pair; UniquePrefix: axi4lConnectMany with per-index prefixes and an innermost `axi4lConnect0` value prefix, for example `axi4lConnectMany0_0_axi4lConnect0`.

---

<a id="entry-axi-connects-raw-single-connect"></a>

### Raw single connect [#](#entry-axi-connects-raw-single-connect)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/Connect.scala)

**Intent and Usage:** Connect raw AXI interfaces and dispatch to full/lite based on config:
```scala
s_axi_raw :=> m_axi_raw
```

**Details:** Dispatch helper; creates full or lite connect components depending on interface config.

---

<a id="entry-axi-connects-raw-many-connect"></a>

### Raw many connect [#](#entry-axi-connects-raw-many-connect)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/Connect.scala)

**Intent and Usage:** Connect matching raw AXI sequences:
```scala
s_axi_raw_N :=> m_axi_raw_N
```

**Details:** Dispatch helper; UniquePrefix: axi4ConnectMany. Each element receives an innermost `axi4fConnect0` or `axi4lConnect0` value prefix according to its interface configuration.

---

## AXI4 Buffers

AXI buffer naming follows the elastic convention: `SlaveBuffer`, `MasterBuffer`, `LeftBuffer`, and `RightBuffer` wrap their internal implementation in `uniquePrefix(name)` and are usually used anonymously inside a connection expression. `SlaveBuffered` and `MasterBuffered` return an interface directly and do not add an internal `uniquePrefix`, including for sequence overloads; bind them to a `val` when the buffered AXI side is reused.

<a id="entry-axi-buffers-axi4-buffer-config"></a>

### `axi4.BufferConfig` [#](#entry-axi-buffers-axi4-buffer-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/Buffer.scala)

**Intent and Usage:** Per-channel buffer depths:
```scala
val bufferCfg = axi4.BufferConfig(
  aw = 2, // write address
  w = 2,  // write data
  b = 2,  // write response
  ar = 2, // read address
  r = 2   // read data
)
```

**Details:** Config. Configuration shared by full and lite AXI buffer helpers. `axi4.BufferConfig.all(2)` applies the same depth to all five channels.

---

<a id="entry-axi-buffers-full-slave-master-buffer"></a>

### `axi4f.SlaveBuffer` / `axi4f.MasterBuffer` [#](#entry-axi-buffers-full-slave-master-buffer)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/Buffer.scala)

**Intent and Usage:** Buffer a full AXI side inline in a connection:
```scala
axi4f.SlaveBuffer(s_axi, bufferCfg) :=> m_axi
s_axi :=> axi4f.MasterBuffer(m_axi, bufferCfg)
```

**Details:** Function. Uses `uniquePrefix`; internally creates channel-level `e.SourceBuffer` / `e.SinkBuffer`. Read channels use `arBuffer` / `rBuffer`; write channels use `awBuffer` / `wBuffer` / `bBuffer`; underlying graph nodes are elastic `Queue` and `Connect` components.

---

<a id="entry-axi-buffers-full-slave-master-buffered"></a>

### `axi4f.SlaveBuffered` / `axi4f.MasterBuffered` [#](#entry-axi-buffers-full-slave-master-buffered)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/Buffer.scala)

**Intent and Usage:** Create a buffered full AXI side for reuse as a named value:
```scala
val s_axi_buffered = axi4f.SlaveBuffered(s_axi, bufferCfg)
val m_axi_buffered = axi4f.MasterBuffered(m_axi, bufferCfg)
```

**Details:** Function. Same channel-level buffering as `SlaveBuffer` / `MasterBuffer`, but the single-interface and sequence forms do not add a `uniquePrefix`; the Scala `val` name is the important handle. Sequence overloads add only per-element index prefixes, for example `slaveBuffered_0_arBuffer0_queue0`.

---

<a id="entry-axi-buffers-lite-slave-master-buffer"></a>

### `axi4l.SlaveBuffer` / `axi4l.MasterBuffer` [#](#entry-axi-buffers-lite-slave-master-buffer)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/Buffer.scala)

**Intent and Usage:** Buffer an AXI4-Lite side inline in a connection:
```scala
axi4l.SlaveBuffer(s_axil, bufferCfg) :=> m_axil
s_axil :=> axi4l.MasterBuffer(m_axil, bufferCfg)
```

**Details:** Function. Uses `uniquePrefix`; internally creates channel-level `e.SourceBuffer` / `e.SinkBuffer`. AXI4-Lite read channels use `arBuffer` / `rBuffer`; write channels use `awBuffer` / `wBuffer` / `bBuffer`; underlying graph nodes are elastic `Queue` and `Connect` components.

---

<a id="entry-axi-buffers-lite-slave-master-buffered"></a>

### `axi4l.SlaveBuffered` / `axi4l.MasterBuffered` [#](#entry-axi-buffers-lite-slave-master-buffered)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/Buffer.scala)

**Intent and Usage:** Create a buffered AXI4-Lite side for reuse as a named value:
```scala
val s_axil_buffered = axi4l.SlaveBuffered(s_axil, bufferCfg)
val m_axil_buffered = axi4l.MasterBuffered(m_axil, bufferCfg)
```

**Details:** Function. Same channel-level buffering as `SlaveBuffer` / `MasterBuffer`, but the single-interface and sequence forms do not add a `uniquePrefix`; the Scala `val` name is the important handle. Sequence overloads add only per-element index prefixes, for example `slaveBuffered_0_arBuffer0_queue0`.

---

## AXI4 Full Components

AXI4 full components are Chisel modules. They are not Chext components themselves; graph content comes from the elastic components they instantiate internally. For the detailed guide and examples for every full AXI component/helper, see [axi4-full.md](axi4-full.md).

<a id="entry-axi4-full-components-demux-config"></a>

### `axi4f.components.DemuxConfig` [#](#entry-axi4-full-components-demux-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/Demux.scala)

**Intent and Usage:** Fan-out configuration:
```scala
val cfg = axi4f.components.DemuxConfig(
  axiSlaveCfg = fullCfg,
  numMasters = 4, // output ports
  decodeFn = addr => addr(13, 12),
  numIdsTrackedRead = 4,
  numIdsTrackedWrite = 4,
  numOutstandingRead = 16,
  numOutstandingWrite = 16,
  capacityPortQueueW = 8,
  slaveBuffers = axi4.BufferConfig.all(2),
  masterBuffers = axi4.BufferConfig.all(0),
  arbiterPolicy = e.Chooser.rr
)
```

**Details:** Config. Configuration for `Demux`; `decodeFn` maps each address to an output port.

---

<a id="entry-axi4-full-components-demux"></a>

### `axi4f.components.Demux` [#](#entry-axi4-full-components-demux)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/Demux.scala); [Scala TB](../src/test/scala/chext/amba/axi4/full/components/Interconnect.tb.scala); [SysC TB](../sysc_tb/chext/amba/axi4/full/components/src/Interconnect.tb.cpp)

**Intent and Usage:** Fan one full AXI slave-side port out to multiple master-side ports:
```scala
val demux0 = Module(new axi4f.components.Demux(cfg))
```

**Details:** Module. Full AXI fan-out by address/routing selection. Internally uses channel-level elastic wiring around `ar`, `r`, `aw`, `w`, and `b`.

---

<a id="entry-axi4-full-components-mux-config"></a>

### `axi4f.components.MuxConfig` [#](#entry-axi4-full-components-mux-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/Mux.scala)

**Intent and Usage:** Fan-in configuration:
```scala
val cfg = axi4f.components.MuxConfig(
  axiSlaveCfg = fullCfg,
  numSlaves = 4, // input ports
  slaveBuffers = axi4.BufferConfig.all(0),
  masterBuffers = axi4.BufferConfig.all(2),
  arbiterPolicy = e.Chooser.rr
)
```

**Details:** Config. Configuration for `Mux`; output IDs are widened to include the selected input port.

---

<a id="entry-axi4-full-components-mux"></a>

### `axi4f.components.Mux` [#](#entry-axi4-full-components-mux)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/Mux.scala); [Scala TB](../src/test/scala/chext/amba/axi4/full/components/Interconnect.tb.scala); [SysC TB](../sysc_tb/chext/amba/axi4/full/components/src/Interconnect.tb.cpp)

**Intent and Usage:** Merge multiple full AXI slave-side ports into one master-side port:
```scala
val mux0 = Module(new axi4f.components.Mux(cfg))
```

**Details:** Module. Full AXI fan-in. Internally creates arbitration and channel-level elastic wiring for read and write paths.

---

<a id="entry-axi4-full-components-id-demux-config"></a>

### `axi4f.components.IdDemuxConfig` [#](#entry-axi4-full-components-id-demux-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/IdDemux.scala)

**Intent and Usage:** ID fan-out configuration:
```scala
val cfg = axi4f.components.IdDemuxConfig(
  axiSlaveCfg = fullCfg,
  wIdSel = 2, // routes by 2 ID bits
  capacityPortQueueW = 8,
  arbiterPolicy = e.Chooser.rr
)
```

**Details:** Config. Configuration for `IdDemux`; `wIdSel` determines the number of output ports as `1 << wIdSel`.

---

<a id="entry-axi4-full-components-id-demux"></a>

### `axi4f.components.IdDemux` [#](#entry-axi4-full-components-id-demux)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/IdDemux.scala)

**Intent and Usage:** Route full AXI traffic by transaction ID:
```scala
val idDemux0 = Module(new axi4f.components.IdDemux(cfg))
```

**Details:** Module. ID-based full AXI fan-out. Internally tracks transactions and expands to elastic channel components.

---

<a id="entry-axi4-full-components-id-mux-config"></a>

### `axi4f.components.IdMuxConfig` [#](#entry-axi4-full-components-id-mux-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/IdMux.scala)

**Intent and Usage:** ID fan-in configuration:
```scala
val cfg = axi4f.components.IdMuxConfig(
  axiSlaveCfg = fullCfg,
  wIdSel = 2, // input port bits in ID
  arbiterPolicy = e.Chooser.rr
)
```

**Details:** Config. Configuration for `IdMux`; response routing uses ID bits to recover the input port.

---

<a id="entry-axi4-full-components-id-mux"></a>

### `axi4f.components.IdMux` [#](#entry-axi4-full-components-id-mux)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/IdMux.scala)

**Intent and Usage:** Merge full AXI traffic while preserving ID-based response routing:
```scala
val idMux0 = Module(new axi4f.components.IdMux(cfg))
```

**Details:** Module. ID-aware full AXI fan-in. Internally uses transaction tracking and channel-level elastic components.

---

<a id="entry-axi4-full-components-id-serialize-config"></a>

### `axi4f.components.IdSerializeConfig` [#](#entry-axi4-full-components-id-serialize-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/IdSerialize.scala)

**Intent and Usage:** ID serialization configuration:
```scala
val cfg = axi4f.components.IdSerializeConfig(
  axiSlaveCfg = fullCfg,
  numOutstandingRead = 4,
  numOutstandingWrite = 4,
  wIdSelect = 0
)
```

**Details:** Config. Configuration for `IdSerialize`; output ID width becomes zero.

---

<a id="entry-axi4-full-components-id-serialize"></a>

### `axi4f.components.IdSerialize` [#](#entry-axi4-full-components-id-serialize)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/IdSerialize.scala); [Scala TB](../src/test/scala/chext/amba/axi4/full/components/IdSerialize.tb.scala); [SysC TB](../sysc_tb/chext/amba/axi4/full/components/src/IdSerialize.tb.cpp)

**Intent and Usage:** Serialize transactions by ID:
```scala
val idSerialize0 =
  Module(new axi4f.components.IdSerialize(cfg))
```

**Details:** Module. Full AXI ID serialization. Internally limits outstanding work and routes response channels through elastic control logic.

---

<a id="entry-axi4-full-components-id-parallelize-config"></a>

### `axi4f.components.IdParallelizeConfig` [#](#entry-axi4-full-components-id-parallelize-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/IdParallelize.scala)

**Intent and Usage:** ID parallelization configuration:
```scala
val cfg = axi4f.components.IdParallelizeConfig(
  axiSlaveCfg = fullCfg.copy(wId = 0),
  wIdMaster = 3,
  wBufferIndex = 10,
  readUseSyncMem = true,
  writeUseSyncMem = true
)
```

**Details:** Config. Configuration for `IdParallelize`; creates a wider-ID master side from a zero-ID slave side.

---

<a id="entry-axi4-full-components-id-parallelize"></a>

### `axi4f.components.IdParallelize` [#](#entry-axi4-full-components-id-parallelize)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/IdParallelize.scala); [Scala TB](../src/test/scala/chext/amba/axi4/full/components/IdParallelize.tb.scala); [SysC TB](../sysc_tb/chext/amba/axi4/full/components/src/IdParallelize.tb.cpp)

**Intent and Usage:** Parallelize transactions by ID:
```scala
val idParallelize0 =
  Module(new axi4f.components.IdParallelize(cfg))
```

**Details:** Module. Full AXI ID parallelization. Internally distributes requests and rejoins responses with elastic channel logic.

---

<a id="entry-axi4-full-components-upscale-config"></a>

### `axi4f.components.UpscaleConfig` [#](#entry-axi4-full-components-upscale-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/Upscale.scala)

**Intent and Usage:** Width upscale configuration:
```scala
val cfg = axi4f.components.UpscaleConfig(
  axiSlaveCfg = fullCfg.copy(wId = 0, wData = 32),
  wDataMaster = 64,
  numOutstandingRead = 32,
  numOutstandingWrite = 32
)
```

**Details:** Config. Configuration for `Upscale`; master data width must be wider than the slave data width.

---

<a id="entry-axi4-full-components-upscale"></a>

### `axi4f.components.Upscale` [#](#entry-axi4-full-components-upscale)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/Upscale.scala); [Scala TB](../src/test/scala/chext/amba/axi4/full/components/Upscale.tb.scala); [SysC TB](../sysc_tb/chext/amba/axi4/full/components/src/Upscale.tb.cpp)

**Intent and Usage:** Adapt a narrower full AXI data bus to a wider one:
```scala
val upscale0 = Module(new axi4f.components.Upscale(cfg))
```

**Details:** Module. Full AXI width upscaler. Internally adapts read/write data channels and keeps address/control channels aligned.

---

<a id="entry-axi4-full-components-downscale-config"></a>

### `axi4f.components.DownscaleConfig` [#](#entry-axi4-full-components-downscale-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/Downscale.scala)

**Intent and Usage:** Width downscale configuration:
```scala
val cfg = axi4f.components.DownscaleConfig(
  axiSlaveCfg = fullCfg.copy(wId = 0, wData = 64),
  wDataMaster = 32,
  numOutstandingRead = 32,
  numOutstandingWrite = 32
)
```

**Details:** Config. Configuration for `Downscale`; master data width must be narrower than the slave data width.

---

<a id="entry-axi4-full-components-downscale"></a>

### `axi4f.components.Downscale` [#](#entry-axi4-full-components-downscale)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/Downscale.scala); [Scala TB](../src/test/scala/chext/amba/axi4/full/components/Downscale.tb.scala); [SysC TB](../sysc_tb/chext/amba/axi4/full/components/src/Downscale.tb.cpp)

**Intent and Usage:** Adapt a wider full AXI data bus to a narrower one:
```scala
val downscale0 = Module(new axi4f.components.Downscale(cfg))
```

**Details:** Module. Full AXI width downscaler. Internally splits wider beats and coordinates read/write data flow through elastic logic.

---

<a id="entry-axi4-full-components-unburst-config"></a>

### `axi4f.components.UnburstConfig` [#](#entry-axi4-full-components-unburst-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/Unburst.scala)

**Intent and Usage:** Burst decomposition configuration:
```scala
val cfg = axi4f.components.UnburstConfig(
  axiCfg = fullCfg.copy(wId = 0),
  numOutstandingRead = 32,
  numOutstandingWrite = 32
)
```

**Details:** Config. Configuration for `Unburst`; input and output AXI configs are the same, but bursts are decomposed internally.

---

<a id="entry-axi4-full-components-unburst"></a>

### `axi4f.components.Unburst` [#](#entry-axi4-full-components-unburst)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/Unburst.scala); [Scala TB](../src/test/scala/chext/amba/axi4/full/components/Unburst.tb.scala); [SysC TB](../sysc_tb/chext/amba/axi4/full/components/src/Unburst.tb.cpp)

**Intent and Usage:** Convert burst transactions into single-beat transactions:
```scala
val unburst0 = Module(new axi4f.components.Unburst(cfg))
```

**Details:** Module. Full AXI burst decomposition. Internally generates per-beat addresses and coordinates response/data channels.

---

<a id="entry-axi4-full-components-widen-config"></a>

### `axi4f.components.WidenConfig` [#](#entry-axi4-full-components-widen-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/Widen.scala)

**Intent and Usage:** Narrow-transfer widening configuration:
```scala
val cfg = axi4f.components.WidenConfig(
  axiCfg = fullCfg.copy(wId = 0),
  numOutstandingRead = 32,
  numOutstandingWrite = 32
)
```

**Details:** Config. Configuration for `Widen`; converts narrow transfers to full-sized transfers on the same AXI data width.

---

<a id="entry-axi4-full-components-widen"></a>

### `axi4f.components.Widen` [#](#entry-axi4-full-components-widen)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/Widen.scala); [Scala TB](../src/test/scala/chext/amba/axi4/full/components/Widen.tb.scala); [SysC TB](../sysc_tb/chext/amba/axi4/full/components/src/Widen.tb.cpp)

**Intent and Usage:** Bridge full AXI width differences with widening behavior:
```scala
val widen0 = Module(new axi4f.components.Widen(cfg))
```

**Details:** Module. Full AXI data widening helper. Internally coordinates address, strobe, data, and response handling through channel logic.

---

<a id="entry-axi4-full-components-credit-buffer-config"></a>

### `axi4f.components.CreditBufferConfig` [#](#entry-axi4-full-components-credit-buffer-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/CreditBuffer.scala)

**Intent and Usage:** Response-aware full AXI buffering configuration:
```scala
val cfg = axi4f.components.CreditBufferConfig(
  axiCfg = fullCfg,
  rBuffer = 32, // read response credits
  wBuffer = 8,  // write payload credits
  bBuffer = 32  // write response credits
)
```

**Details:** Config. Configuration for full AXI `CreditBuffer`; address channels wait until local response/payload buffering has capacity.

---

<a id="entry-axi4-full-components-credit-buffer"></a>

### `axi4f.components.CreditBuffer` [#](#entry-axi4-full-components-credit-buffer)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/CreditBuffer.scala)

**Intent and Usage:** Add response-aware buffering for full AXI:
```scala
val creditBuffer0 =
  Module(new axi4f.components.CreditBuffer(cfg))
```

**Details:** Module. Full AXI credit-style response buffering around read/write channels.

---

<a id="entry-axi4-full-components-protocol-converter-config"></a>

### `axi4f.components.ProtocolConverterConfig` [#](#entry-axi4-full-components-protocol-converter-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/ProtocolConverter.scala)

**Intent and Usage:** Staged protocol conversion configuration:
```scala
val cfg = axi4f.components.ProtocolConverterConfig(
  axiSlaveCfg = fullCfg.copy(wId = 4, wData = 32),
  axiMasterCfg = fullCfg.copy(wId = 2, wData = 64),
  slaveNeverBursts = false
)
```

**Details:** Config. Configuration for `ProtocolConverter`; internally selects ID, burst, and width conversion stages.

---

<a id="entry-axi4-full-components-protocol-converter"></a>

### `axi4f.components.ProtocolConverter` [#](#entry-axi4-full-components-protocol-converter)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/ProtocolConverter.scala)

**Intent and Usage:** Convert between supported full AXI protocol shapes:
```scala
val protocolConverter0 =
  Module(new axi4f.components.ProtocolConverter(cfg))
```

**Details:** Module. Staged full AXI protocol conversion. It composes lower-level full AXI component modules and their internal elastic channel components.

---

## AXI4 Lite Components

AXI4-Lite components are Chisel modules. They are not Chext components themselves; graph content comes from the internal channel-level elastic components. For the detailed guide and examples for every Lite AXI component/helper, see [axi4-lite.md](axi4-lite.md).

<a id="entry-axi4-lite-components-demux-config"></a>

### `axi4l.components.DemuxConfig` [#](#entry-axi4-lite-components-demux-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/components/Demux.scala)

**Intent and Usage:** AXI4-Lite fan-out configuration:
```scala
val cfg = axi4l.components.DemuxConfig(
  axiSlaveCfg = liteCfg,
  numMasters = 4, // output ports
  decodeFn = addr => addr(13, 12),
  capacityPortQueueR = 8,
  capacityPortQueueW = 8,
  capacityPortQueueB = 8,
  slaveBuffers = axi4.BufferConfig.all(2),
  masterBuffers = axi4.BufferConfig.all(0)
)
```

**Details:** Config. Configuration for AXI4-Lite `Demux`; `decodeFn` maps each address to an output port.

---

<a id="entry-axi4-lite-components-demux"></a>

### `axi4l.components.Demux` [#](#entry-axi4-lite-components-demux)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/components/Demux.scala)

**Intent and Usage:** Fan one AXI4-Lite slave-side port out to multiple master-side ports:
```scala
val demux0 = Module(new axi4l.components.Demux(cfg))
```

**Details:** Module. AXI4-Lite fan-out for address/data/control channels.

---

<a id="entry-axi4-lite-components-mux-config"></a>

### `axi4l.components.MuxConfig` [#](#entry-axi4-lite-components-mux-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/components/Mux.scala)

**Intent and Usage:** AXI4-Lite fan-in configuration:
```scala
val cfg = axi4l.components.MuxConfig(
  axiSlaveCfg = liteCfg,
  numSlaves = 4, // input ports
  capacityPortQueueR = 8,
  capacityPortQueueW = 8,
  capacityPortQueueB = 8,
  slaveBuffers = axi4.BufferConfig.all(0),
  masterBuffers = axi4.BufferConfig.all(2),
  arbiterPolicy = e.Chooser.rr
)
```

**Details:** Config. Configuration for AXI4-Lite `Mux`; queue capacities tune per-channel port tracking.

---

<a id="entry-axi4-lite-components-mux"></a>

### `axi4l.components.Mux` [#](#entry-axi4-lite-components-mux)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/components/Mux.scala)

**Intent and Usage:** Merge multiple AXI4-Lite slave-side ports into one master-side port:
```scala
val mux0 = Module(new axi4l.components.Mux(cfg))
```

**Details:** Module. AXI4-Lite fan-in with channel-level elastic arbitration and routing.

---

<a id="entry-axi4-lite-components-credit-buffer-config"></a>

### `axi4l.components.CreditBufferConfig` [#](#entry-axi4-lite-components-credit-buffer-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/components/CreditBuffer.scala)

**Intent and Usage:** Response-aware buffering configuration:
```scala
val cfg = axi4l.components.CreditBufferConfig(
  axiCfg = liteCfg,
  rBuffer = 8, // read response credits
  wBuffer = 8, // write payload credits
  bBuffer = 8  // write response credits
)
```

**Details:** Config. Configuration for `CreditBuffer`; address channels are delayed until local response/payload buffering has capacity.

---

<a id="entry-axi4-lite-components-credit-buffer"></a>

### `axi4l.components.CreditBuffer` [#](#entry-axi4-lite-components-credit-buffer)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/components/CreditBuffer.scala)

**Intent and Usage:** Add response-aware buffering for AXI4-Lite:
```scala
val creditBuffer0 =
  Module(new axi4l.components.CreditBuffer(cfg))
```

**Details:** Module. AXI4-Lite response buffering with credit-style control around read/write channels.

---

<a id="entry-axi4-lite-components-mem-controller"></a>

### `axi4l.components.MemController` [#](#entry-axi4-lite-components-mem-controller)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/components/MemController.scala)

**Intent and Usage:** Expose a memory-like backend as AXI4-Lite:
```scala
val memController0 =
  Module(new axi4l.components.MemController(
    log2numElements = 10, // 1024 words
    axiCfg = liteCfg,
    debugEnabled = false
  ))
```

**Details:** Module. AXI4-Lite memory controller. Internally bridges AXI4-Lite channels to memory-style request and response paths.

---

<a id="entry-axi4-lite-components-sync-read-mem-controller"></a>

### `axi4l.components.SyncReadMemController` [#](#entry-axi4-lite-components-sync-read-mem-controller)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/components/SyncReadMemController.scala)

**Intent and Usage:** Expose a synchronous-read memory backend as AXI4-Lite:
```scala
val syncReadMemController0 =
  Module(new axi4l.components.SyncReadMemController(
    log2numElements = 10, // 1024 words
    axiCfg = liteCfg,
    debugEnabled = false
  ))
```

**Details:** Module. AXI4-Lite controller variant for synchronous-read memories.

---

<a id="entry-axi4-lite-components-register-block"></a>

### `axi4l.components.RegisterBlock` [#](#entry-axi4-lite-components-register-block)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/components/RegisterBlock.scala)

**Intent and Usage:** Build an AXI4-Lite register block:
```scala
val regs = new axi4l.components.RegisterBlock(
  wAddr = 32, // AXI4-Lite address width
  wData = 32, // AXI4-Lite data width
  wMask = 12  // register address-space mask
)
val s_axil = IO(axi4l.Slave(regs.cfgAxi))
s_axil :=> regs.s_axil
```

**Details:** Module. AXI4-Lite register block module with generated read/write register access behavior.

---

## Memory

Memory interfaces are bundle-level protocols whose request/response fields are elastic interfaces. RAM wrappers are Chisel modules; their graph visibility mostly comes from the elastic ports and components they instantiate or connect.

Memory buffer naming follows the same convention as elastic and AXI: `SlaveBuffer` / `MasterBuffer` are the named-prefix inline forms, while `SlaveBuffered` / `MasterBuffered` are for binding the returned interface to a `val`. `SlaveBuffered` / `MasterBuffered` do not add an internal `uniquePrefix`, including for sequence overloads.

<a id="entry-memory-read-interface-memory-write-interface"></a>

### `memory.ReadInterface`, `memory.WriteInterface` [#](#entry-memory-read-interface-memory-write-interface)

**Sources:** [Scala](../src/main/scala/chext/memory/Interfaces.scala)

**Intent and Usage:** Memory read/write protocol bundles.

**Details:** Request/response bundle interfaces for memory-like ports. Their `req`/`resp` fields are elastic interfaces and can appear in elastic graph layers.

---

<a id="entry-memory-connect-op-master-connect-slave"></a>

### `memory.ConnectOp._` / `master :=> slave` [#](#entry-memory-connect-op-master-connect-slave)

**Sources:** [Scala](../src/main/scala/chext/memory/Connect.scala)

**Intent and Usage:** Connect memory interfaces or sequences.

**Details:** Creates elastic `Connect` components on request/response channels; sequence forms use prefixes `memoryReadConnectMany` or `memoryWriteConnectMany`.

---

<a id="entry-memory-buffer-config"></a>

### `memory.BufferConfig` [#](#entry-memory-buffer-config)

**Sources:** [Scala](../src/main/scala/chext/memory/Buffer.scala)

**Intent and Usage:** Memory interface buffer depths:
```scala
val bufferCfg = memory.BufferConfig(
  req = 2,  // request-channel queue depth
  resp = 2  // response-channel queue depth
)
```

**Details:** Config. Configuration for memory `SlaveBuffer`, `MasterBuffer`, `SlaveBuffered`, and `MasterBuffered`.

---

<a id="entry-memory-slave-buffer-memory-master-buffer"></a>

### `memory.SlaveBuffer`, `memory.MasterBuffer` [#](#entry-memory-slave-buffer-memory-master-buffer)

**Sources:** [Scala](../src/main/scala/chext/memory/Buffer.scala); [Scala TB](../src/test/scala/chext/memory/Buffer.tb.scala)

**Intent and Usage:** Buffer memory interfaces inline:
```scala
memory.SlaveBuffer(readMaster, bufferCfg) :=> readSlave
writeMaster :=> memory.MasterBuffer(writeSlave, bufferCfg)
```

**Details:** Function. Uses `uniquePrefix`; internally creates elastic `SourceBuffer` and `SinkBuffer` on `req`/`resp` channels.

---

<a id="entry-memory-slave-buffered-memory-master-buffered"></a>

### `memory.SlaveBuffered`, `memory.MasterBuffered` [#](#entry-memory-slave-buffered-memory-master-buffered)

**Sources:** [Scala](../src/main/scala/chext/memory/Buffer.scala); [Scala TB](../src/test/scala/chext/memory/Buffer.tb.scala)

**Intent and Usage:** Create a buffered memory interface for reuse as a named value:
```scala
val bufferCfg = memory.BufferConfig.all(2) // req and resp depth
val readBuffered = memory.SlaveBuffered(readMaster, bufferCfg)
val writeBuffered = memory.MasterBuffered(writeSlave, bufferCfg)
```

**Details:** Function. Same request/response buffering as `SlaveBuffer` / `MasterBuffer`, but the single-interface and sequence forms do not add a `uniquePrefix`; the Scala `val` name is the important handle. Sequence overloads add only per-element index prefixes, for example `slaveBufferedRead_0_reqBuffer0_queue0`.

---

<a id="entry-memory-raw-mem-config-memory-port-config"></a>

### `memory.RawMemConfig` / `memory.PortConfig` [#](#entry-memory-raw-mem-config-memory-port-config)

**Sources:** [Scala](../src/main/scala/chext/memory/RawMem.scala); [Scala](../src/main/scala/chext/memory/RAM.scala)

**Intent and Usage:** RAM wrapper configuration:
```scala
val rawCfg = memory.RawMemConfig(
  wAddr = 10,       // 1024 elements
  wData = 32,       // data width
  latencyRead = 2,
  latencyWrite = 1
)
val portCfg = memory.PortConfig(
  numOutstandingRead = 8,
  numOutstandingWrite = 8,
  arbiterFunc = memory.ReadWriteArbiter.defaultFunc
)
```

**Details:** Config. `RawMemConfig` describes the target memory shape and latency. `PortConfig` sets elastic buffering around RAM read/write ports.

---

<a id="entry-memory-single-port-ram-simple-dual-port-ram-true-dual-port-ram"></a>

### `memory.SinglePortRAM`, `SimpleDualPortRAM`, `TrueDualPortRAM` [#](#entry-memory-single-port-ram-simple-dual-port-ram-true-dual-port-ram)

**Sources:** [Scala](../src/main/scala/chext/memory/RAM.scala)

**Intent and Usage:** Memory wrappers around raw memory targets:
```scala
val ram0 =
  Module(new memory.TrueDualPortRAM(rawCfg, portCfg, portCfg))
```

**Details:** Module. RAM wrappers with elastic read/write ports and raw memory bridges. Not tracking components; elastic ports participate when connected.

---

## Stream

Stream modules create AXI-facing work/result frontends. They are Chisel modules; graph visibility comes from their task/data/result elastic channels and internal elastic control components.

<a id="entry-stream-chunk-config"></a>

### `stream.ChunkConfig` [#](#entry-stream-chunk-config)

**Sources:** [Scala](../src/main/scala/chext/stream/Chunk.scala)

**Intent and Usage:** Chunking configuration:
```scala
val cfg = stream.ChunkConfig(
  wAddress = axiCfg.wAddr,
  wLength = 32,        // request length width
  wData = axiCfg.wData,
  maxBurstLength = 256,
  genUser = UInt(0.W)
)
```

**Details:** Config. Configuration for `stream.Chunk`; controls address width, length width, data width, and maximum emitted burst length.

---

<a id="entry-stream-chunk"></a>

### `stream.Chunk` [#](#entry-stream-chunk)

**Sources:** [Scala](../src/main/scala/chext/stream/Chunk.scala)

**Intent and Usage:** Split a variable-length stream request into AXI-sized chunks:
```scala
val chunk0 = Module(new stream.Chunk(cfg))
```

**Details:** Module. Chunks stream requests/results by repeatedly emitting per-beat work. Internally creates an elastic `Count`.

---

<a id="entry-stream-read-config-stream-write-config"></a>

### `stream.ReadConfig` / `stream.WriteConfig` [#](#entry-stream-read-config-stream-write-config)

**Sources:** [Scala](../src/main/scala/chext/stream/Read.scala); [Scala](../src/main/scala/chext/stream/Write.scala)

**Intent and Usage:** AXI stream frontend configuration:
```scala
val readCfg = stream.ReadConfig(
  axiCfg = axiCfg.copy(read = true, write = false),
  genUser = UInt(0.W),
  resultMode = stream.ReadResultMode.LastAlwaysInvalid,
  maxBurstLength = 256,
  wLength = 32,
  numOutstandingTasks = 8
)
val writeCfg = stream.WriteConfig(
  axiCfg = axiCfg.copy(read = false, write = true),
  genUser = UInt(0.W),
  resultMode = stream.WriteResultMode.KeepAll,
  maxBurstLength = 256,
  wLength = 32,
  numOutstandingTasks = 8
)
```

**Details:** Config. `ReadConfig` controls read result mode and outstanding task buffering. `WriteConfig` controls write result mode and outstanding task buffering.

---

<a id="entry-stream-read-stream-write"></a>

### `stream.Read`, `stream.Write` [#](#entry-stream-read-stream-write)

**Sources:** [Scala](../src/main/scala/chext/stream/Read.scala); [Scala](../src/main/scala/chext/stream/Write.scala); [Scala TB](../src/test/scala/chext/stream/Read.tb.scala); [Scala TB](../src/test/scala/chext/stream/Write.tb.scala); [SysC TB](../sysc_tb/chext/stream/src/Read.tb.cpp); [SysC TB](../sysc_tb/chext/stream/src/Write.tb.cpp)

**Intent and Usage:** AXI stream read/write frontends:
```scala
val read0 = Module(new stream.Read(readCfg))
val write0 = Module(new stream.Write(writeCfg))
```

**Details:** Module. AXI stream read/write engines. They create task/data/result elastic channels and internally use `Drop`, `Fork`, `Transform`, `Repeat`, `Join`, `Mux`, buffers, and null channel components.

---

## Load/Store

Load/store modules are single-beat AXI full frontends. They expose elastic task/result interfaces and a full AXI master port, then build the necessary AXI channel traffic internally.

<a id="entry-load-store-ldstr-load-config-ldstr-store-config"></a>

### `ldstr.LoadConfig` / `ldstr.StoreConfig` [#](#entry-load-store-ldstr-load-config-ldstr-store-config)

**Sources:** [Scala](../src/main/scala/chext/ldstr/Load.scala); [Scala](../src/main/scala/chext/ldstr/Store.scala)

**Intent and Usage:** Single-beat load/store frontend configuration:
```scala
val loadCfg = ldstr.LoadConfig(
  axiCfg = axiCfg.copy(read = true, write = false),
  genUser = UInt(0.W),
  numOutstandingTasks = 8
)
val storeCfg = ldstr.StoreConfig(
  axiCfg = axiCfg.copy(read = false, write = true),
  genUser = UInt(0.W),
  numOutstandingTasks = 8
)
```

**Details:** Config. Configuration for `ldstr.Load` and `ldstr.Store`; `numOutstandingTasks` sizes the buffering around AXI read/write response paths.

---

<a id="entry-load-store-ldstr-load-ldstr-store"></a>

### `ldstr.Load`, `ldstr.Store` [#](#entry-load-store-ldstr-load-ldstr-store)

**Sources:** [Scala](../src/main/scala/chext/ldstr/Load.scala); [Scala](../src/main/scala/chext/ldstr/Store.scala)

**Intent and Usage:** Load/store AXI frontends:
```scala
val load0 = Module(new ldstr.Load(loadCfg))
val store0 = Module(new ldstr.Store(storeCfg))
```

**Details:** Module. Load/store frontends for AXI memory access. They internally use elastic `Fork`, `Transform`, `SinkBuffer`, `Join`, and null channel components around AXI `ar/r` or `aw/w/b` paths.

---

## Float

Floating-point elastic modules wrap floating-point datapaths with elastic source/sink interfaces. Their graph participation comes from the wrapper queues, joins, and surrounding elastic wiring.

<a id="entry-float-floating-point"></a>

### `float.FloatingPoint` [#](#entry-float-floating-point)

**Sources:** [Scala](../src/main/scala/chext/float/FloatingPoint.scala)

**Intent and Usage:** Floating-point format configuration:
```scala
val fp = float.FloatingPoint.ieeeFp32
val custom = float.FloatingPoint(
  wExponent = 8,
  wMantissa = 23
)
```

**Details:** Config. Bundle/config object for floating-point datapaths. Common helpers include `ieeeFp16`, `ieeeFp32`, `ieeeFp64`, `fp18`, and `bfloat16`.

---

<a id="entry-float-elastic-add-float-elastic-multiply"></a>

### `float.ElasticAdd`, `float.ElasticMultiply` [#](#entry-float-elastic-add-float-elastic-multiply)

**Sources:** [Scala](../src/main/scala/chext/float/Elastic.scala); [Scala TB](../src/test/scala/chext/float/Elastic.tb.scala); [SysC TB](../sysc_tb/chext/float/src/ElasticTop.tb.cpp)

**Intent and Usage:** Elastic floating-point operators:
```scala
val add0 =
  Module(new float.ElasticAdd(fp, combinational = false))
val mul0 =
  Module(new float.ElasticMultiply(fp, combinational = false))
```

**Details:** Module. Elastic wrappers around floating-point add/multiply datapaths. They expose elastic input/output interfaces; graph participation comes through surrounding elastic wiring and transforms.

---
