<!-- Generated from docs/synopsis.txt by scripts/generate_synopsis.py. Do not edit by hand. -->
# Chext Synopsis for GitHub

This is a working synopsis of the main Chext constructions, their intended use, and how they appear in the tracking/module graph. For project-wide naming guidance, see [naming.md](naming.md). For the underlying component model, see [tracking.md](tracking.md). For guidance on choosing `prefix(...)` and `uniquePrefix(...)`, see [prefixes.md](prefixes.md). For elastic-specific tracking and AXI4 DataView recovery, see [tracking-elastic.md](tracking-elastic.md). For detailed AXI4 component guides, see [axi4-full.md](axi4-full.md) and [axi4-lite.md](axi4-lite.md).

## Contents

- [Elastic Imports](#elastic-imports)
- [Elastic Interfaces and Operators](#elastic-interfaces-and-operators)
- [Elastic Components](#elastic-components)
- [Elastic Composite Components](#elastic-composite-components)
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

<a id="entry-elastic-interfaces-declare-elastic-interface"></a>

### `declareElasticInterface` [#](#entry-elastic-interfaces-declare-elastic-interface)

**Sources:** [Scala](../src/main/scala/chext/AnnotatedModule.scala)

**Intent and Usage:** Declare Elastic IO on an `AnnotatedModule`:
```scala
declareElasticInterface(source, "Task")
declareElasticInterface(sink, "Result")
```

**Details:** Interface. Adds hdlinfo metadata. If a module has Elastic interfaces but no Elastic components, manually initialize tracking in its module body with `chext.elastic.tracking.register()` so parent graph components can reference its child IO.

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

<a id="entry-elastic-components-e-stall-sink"></a>

### `e.StallSink` [#](#entry-elastic-components-e-stall-sink)

**Sources:** [Scala](../src/main/scala/chext/elastic/StallSink.scala)

**Intent and Usage:** Permanently backpressure a source:
```scala
val stallSink0 =
  new e.StallSink(source)
```

**Details:** Component; tpe: StallSink; namePrefix: stallSink. Port: `source`.

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

## Elastic Composite Components

Composite components may expose hierarchy-only Elastic ports with `addSource(..., boundary = true)` and `addSink(..., boundary = true)`. Boundary ports appear in the module graph without marking the interface or participating in deadlock monitors; operational ports remain on leaf descendants.

<a id="entry-elastic-composite-components-e-repeat"></a>

### `e.Repeat` [#](#entry-elastic-composite-components-e-repeat)

**Sources:** [Scala](../src/main/scala/chext/elastic/Repeat.scala); [Scala TB](../src/test/scala/chext/elastic/Count.tb.scala)

**Intent and Usage:** Repeat one input token multiple times:
```scala
val repeat0 = new e.Repeat(source, sink, wIndex = 8) {
  len { in => in.length }
  out { (in, index, first, last) => in }
}
```

**Details:** Component; tpe: Repeat; namePrefix: repeat. Boundary interfaces: source `source`, sink `sink`. Hierarchy: creates a child `Count` under `withComponent(this)`, with observed path shape `/repeat0` -> `/repeat0_count`.

---

<a id="entry-elastic-composite-components-e-fold"></a>

### `e.Fold` [#](#entry-elastic-composite-components-e-fold)

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

**Details:** Component; tpe: Fold; namePrefix: fold. Boundary sources: `source`, `sourceInit`, `sourceResult`; boundary sinks: `sink`, `sinkA`, `sinkB`. The latter three interfaces are user extension points for fold logic. Hierarchy: stage wiring under `stage0` and `stage1`; children include `Transducer` or `Transform`, `Join`, `Connect`, and buffers attached to the `Fold` component.

---

<a id="entry-elastic-composite-components-e-loop"></a>

### `e.Loop` [#](#entry-elastic-composite-components-e-loop)

**Sources:** [Scala](../src/main/scala/chext/elastic/Loop.scala)

**Intent and Usage:** Iterative elastic loop with user-visible body endpoints:
```scala
val loop0 = new e.Loop(sourceInit, sinkExit) {
  end { state => state.done }
  sinkCurrent :=> loopBody.source
  loopBody.sink :=> sourceNext
}
```

**Details:** Component; tpe: Loop; namePrefix: loop. Boundary sources: `sourceInit`, `sourceNext`; boundary sinks: `sinkExit`, `sinkCurrent`. `sinkCurrent` and `sourceNext` are the user-visible loop-body extension points. Hierarchy: creates `Stall`, `Connect`, `Fork`, `Demux`, `Merger`, and buffers attached to the `Loop` component.

---

<a id="entry-elastic-composite-components-e-scope"></a>

### `e.Scope` [#](#entry-elastic-composite-components-e-scope)

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

**Details:** Component; tpe: Scope; namePrefix: scope. Boundary sources: `sourceInit`, `sourceEnd`; boundary sinks: `sinkExit`, `sinkBegin`. `sinkBegin` and `sourceEnd` are the user-visible scope-body extension points. Hierarchy: creates a `Stall`, a `Connect`, and sink buffers attached to the `Scope` component.

---

<a id="entry-elastic-composite-components-e-random-stall"></a>

### `e.RandomStall` [#](#entry-elastic-composite-components-e-random-stall)

**Sources:** [Scala](../src/main/scala/chext/elastic/RandomStall.scala)

**Intent and Usage:** Test/diagnostic random backpressure:
```scala
val randomStall0 =
  new e.RandomStall(source, sink)
```

**Details:** Component; tpe: RandomStall; namePrefix: randomStall. Boundary interfaces: source `source`, sink `sink`. Hierarchy: creates `Stall` plus `SinkBuffer`; observed child paths include `/randomStall0_stall` and `/randomStall0_stall_sinkBuffer0_queue0`.

---

<a id="entry-elastic-composite-components-e-switch"></a>

### `e.Switch` [#](#entry-elastic-composite-components-e-switch)

**Sources:** [Scala](../src/main/scala/chext/elastic/Switch.scala); [Scala TB](../src/test/scala/chext/elastic/Switch.tb.scala)

**Intent and Usage:** Route tokens through user-defined branches:
```scala
val switch0 = new e.Switch(source, sink) {
  branch { in => in.select } { (branchSource, branchSink) =>
    branchSource :=> branchSink
  }
}
```

**Details:** Component; tpe: Switch; namePrefix: switch. Boundary interfaces include external source `source` and sink `sink`, plus `sink_<branch>` / `source_<branch>` extension pairs passed to each branch callback. Hierarchy: creates `Fork`, `Demux`, `Mux`, a selection queue, and the components instantiated by each branch.

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
  simCheckAddrWidth = chext.util.SimulationCheck.None
)
```

**Details:** Config. Configuration for `s_axi.connect(m_axi, cfg)`: controls tie-offs, warnings, and simulation checks for full AXI connections. `warnSideband` compares the effective widths of `LOCK`, `CACHE`, `PROT`, `QOS`, and `REGION` on the address channels, plus `ARUSER`, `RUSER`, `AWUSER`, `WUSER`, and `BUSER`. It does not add simulation-time sideband checks. Address-width simulation checks are disabled by default.

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
  simCheckAddrWidth = chext.util.SimulationCheck.None
)
```

**Details:** Config. Configuration for `s_axil.connect(m_axil, cfg)`: controls tie-offs, warnings, and address-width simulation checks for AXI4-Lite connections. Address-width simulation checks are disabled by default; select `Default`, `Printf`, or `Assert` to enable them.

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

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/Connect.scala); [Scala TB](../src/test/scala/chext/amba/axi4/full/Connect.test.scala)

**Intent and Usage:** Connect one full AXI interface pair:
```scala
s_axi :=> m_axi
```

**Details:** Creates `axi4f.Connect`; UniquePrefix: axi4fConnect; graph `tpe` is `Axi4f_Connect`. Exposes each available master/slave AXI channel as a boundary interface; channel-level child `Connect` components retain operational ownership. A private `Connect_Resolver(owner: Connect)(implicit sourceInfo: SourceInfo)` uses typed resolver bindings to register the endpoint roles and forward AXI tracking properties across the boundary.

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

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/Connect.scala); [Scala TB](../src/test/scala/chext/amba/axi4/lite/Connect.test.scala)

**Intent and Usage:** Connect one AXI4-Lite pair:
```scala
s_axil :=> m_axil
```

**Details:** Creates `axi4l.Connect`; UniquePrefix: axi4lConnect; graph `tpe` is `Axi4l_Connect`. Exposes each available master/slave AXI channel as a boundary interface; channel-level child `Connect` components retain operational ownership. A private `Connect_Resolver(owner: Connect)(implicit sourceInfo: SourceInfo)` uses typed resolver bindings to register the endpoint roles and forward AXI tracking properties across the boundary.

---

<a id="entry-axi-connects-lite-configured-connect"></a>

### Lite configured connect [#](#entry-axi-connects-lite-configured-connect)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/Connect.scala); [Scala TB](../src/test/scala/chext/amba/axi4/lite/Connect.test.scala)

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

AXI buffer naming follows the elastic convention: `SlaveBuffer`, `MasterBuffer`, `LeftBuffer`, and `RightBuffer` wrap their internal implementation in `uniquePrefix(name)` and are usually used anonymously inside a connection expression. `SlaveBuffered` and `MasterBuffered` return an interface directly and do not add an outer `uniquePrefix`, including for sequence overloads; bind them to a `val` when the buffered AXI side is reused. Every helper creates an inner tracked `Buffer` component (`axi4fBuffer` or `axi4lBuffer`) with a private `Buffer_Resolver(owner: Buffer)(implicit sourceInfo: SourceInfo)`, typed endpoint-family bindings, and channel-level elastic children.

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

**Details:** Function. Uses `uniquePrefix`; internally creates a tracked Component; tpe: Axi4f_Buffer; namePrefix: axi4fBuffer that owns the AXI property resolver and channel-level `e.SourceBuffer` / `e.SinkBuffer` children. Read channels use `arBuffer` / `rBuffer`; write channels use `awBuffer` / `wBuffer` / `bBuffer`; underlying graph nodes are elastic `Queue` and `Connect` components.

---

<a id="entry-axi-buffers-full-slave-master-buffered"></a>

### `axi4f.SlaveBuffered` / `axi4f.MasterBuffered` [#](#entry-axi-buffers-full-slave-master-buffered)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/Buffer.scala)

**Intent and Usage:** Create a buffered full AXI side for reuse as a named value:
```scala
val s_axi_buffered = axi4f.SlaveBuffered(s_axi, bufferCfg)
val m_axi_buffered = axi4f.MasterBuffered(m_axi, bufferCfg)
```

**Details:** Function. Same tracked `Axi4f_Buffer` component and channel-level buffering as `SlaveBuffer` / `MasterBuffer`, but the single-interface and sequence forms do not add an outer `uniquePrefix`; the Scala `val` name is the important handle. Sequence overloads add per-element index prefixes around the inner component, for example `slaveBuffered_0_axi4fBuffer0_arBuffer0_queue0`.

---

<a id="entry-axi-buffers-lite-slave-master-buffer"></a>

### `axi4l.SlaveBuffer` / `axi4l.MasterBuffer` [#](#entry-axi-buffers-lite-slave-master-buffer)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/Buffer.scala)

**Intent and Usage:** Buffer an AXI4-Lite side inline in a connection:
```scala
axi4l.SlaveBuffer(s_axil, bufferCfg) :=> m_axil
s_axil :=> axi4l.MasterBuffer(m_axil, bufferCfg)
```

**Details:** Function. Uses `uniquePrefix`; internally creates a tracked Component; tpe: Axi4l_Buffer; namePrefix: axi4lBuffer that owns the AXI property resolver and channel-level `e.SourceBuffer` / `e.SinkBuffer` children. AXI4-Lite read channels use `arBuffer` / `rBuffer`; write channels use `awBuffer` / `wBuffer` / `bBuffer`; underlying graph nodes are elastic `Queue` and `Connect` components.

---

<a id="entry-axi-buffers-lite-slave-master-buffered"></a>

### `axi4l.SlaveBuffered` / `axi4l.MasterBuffered` [#](#entry-axi-buffers-lite-slave-master-buffered)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/Buffer.scala)

**Intent and Usage:** Create a buffered AXI4-Lite side for reuse as a named value:
```scala
val s_axil_buffered = axi4l.SlaveBuffered(s_axil, bufferCfg)
val m_axil_buffered = axi4l.MasterBuffered(m_axil, bufferCfg)
```

**Details:** Function. Same tracked `Axi4l_Buffer` component and channel-level buffering as `SlaveBuffer` / `MasterBuffer`, but the single-interface and sequence forms do not add an outer `uniquePrefix`; the Scala `val` name is the important handle. Sequence overloads add per-element index prefixes around the inner component, for example `slaveBuffered_0_axi4lBuffer0_arBuffer0_queue0`.

---

## AXI4 Full Components

AXI4 full components are Chisel modules. They are not Chext components themselves; graph content comes from the elastic components they instantiate internally. Every module that declares tracked AXI ports creates a neighboring private `X_Resolver(owner: X)(implicit sourceInfo: SourceInfo)`. Resolvers use `bindCommon`, `bindMaster`, or `bindSlave`; each accepts one interface or a sequence, whose extractor also returns the matched interface. Common/master/slave classification comes from sealed property markers rather than separate role tags. Directional standard keys derive from `MasterReadProperty`, `MasterWriteProperty`, `SlaveReadProperty`, or `SlaveWriteProperty`; the catalogs expose read/write groups, shape groups, and Boolean extractors. `bindMaster` and `bindSlave` eagerly mark standard properties for directions disabled by `axi4.Config` as `Undefined`. Configuration-derived facts are eagerly enforced when a resolver is constructed; dependency forwarding and intentional `DontCare(message)` / unavailable `Incomplete` results remain lazy. Tracking generates the minimum information needed to catch composition errors; it does not try to fully annotate every interface. No compatibility checks run yet. Fan-in/fan-out behavior stays in owner-specific resolvers, not in generic tracking resolvers. For the detailed guide and examples for every full AXI component/helper, see [axi4-full.md](axi4-full.md).

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

**Details:** Module. Full AXI fan-out by address/routing selection. Its private `Demux_Resolver` binds the slave port and master-port vector, which eagerly marks disabled standard channel groups `Undefined`, forwards every enabled upstream master property unchanged to each output, and eagerly enforces `numOutstanding*` / `numIdsTracked*` only as local slave capabilities at the common input. This preserves separate facts for eventually checking the upstream master against the demux and against every downstream slave. It does not aggregate downstream slave capabilities; unnecessary synthetic input properties become `DontCare`. An arbitrary `decodeFn` cannot describe a combined address map, so `Slave.MemoryMap` remains `Incomplete`. Internally uses channel-level elastic wiring around `ar`, `r`, `aw`, `w`, and `b`.

---

<a id="entry-axi4-full-components-demux-mm-config"></a>

### `axi4f.components.DemuxMmConfig` [#](#entry-axi4-full-components-demux-mm-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/DemuxMm.scala)

**Intent and Usage:** Memory-map-driven fan-out configuration:
```scala
val cfg = axi4f.components.DemuxMmConfig(
  axiSlaveCfg = fullCfg,
  numMasters = 4,
  numIdsTrackedRead = 4,
  numIdsTrackedWrite = 4,
  numOutstandingRead = 16,
  numOutstandingWrite = 16,
  arbiterPolicy = e.Chooser.rr
)
```

**Details:** Config. Configuration for `DemuxMm`; address selection is supplied by generated elastic decoders rather than a Chisel decode function. AXI boundary buffering is left to the caller.

---

<a id="entry-axi4-full-components-demux-mm"></a>

### `axi4f.components.DemuxMm` [#](#entry-axi4-full-components-demux-mm)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/DemuxMm.scala); [Scala TB](../src/test/scala/chext/amba/axi4/full/components/DemuxMm.test.scala); [SysC TB](../sysc_tb/chext/amba/axi4/full/components/src/DemuxMm.tb.cpp)

**Intent and Usage:** Aggregate maps resolved at the master interfaces and generate address decoders:
```scala
val demux = Module(new axi4f.components.DemuxMm(cfg))
demux.m_axi :=> m_axi

// Slave properties propagate upstream through AXI Connect components.
m_axi.zip(childMaps).foreach { case (master, childMap) =>
  require(childMap.path.nonEmpty)
  master.slaveProps(Slave.MemoryMap) = childMap
}
// Place m_axi(2), then m_axi(0); reserve m_axi(1) for decode errors:
val memoryMap = demux.genDecoder(
  permutation = Some(Seq(2, 0)),
  errorSlave = Some(1),
  allocationScheme = MemoryMap.AllocationScheme.AlignedPacked
)
```

**Details:** Module. Resolves each mapped master interface's `Slave.MemoryMap`; AXI `Connect` and tracked `Buffer` component resolvers allow those properties to propagate through downstream connections and buffered links. `Resolver.resolve` returns a `Resolution` containing the terminal result and trace after recursively traversing dependencies depth-first, with the depth bounded by `maxStackSize`. Resolver candidates prefer the shallowest owner, with the most recently registered candidate winning at equal depth. It optionally reorders the maps with a complete permutation of non-error interfaces and calls `MemoryMap.aggregate` with `AlignedPacked`, `AlignedLargest`, or `Tight` allocation. `memoryMapPath` names a generated map when DemuxMm instances are composed hierarchically. Each decoder subtracts the selected child map's offset from forwarded `ARADDR` and `AWADDR`, so the next hierarchy level receives a local address. Maps and segments carry absolute slash-separated origins. Optional `captureResolutionTrace` records typed `interfaceFrom`, `interfaceTo`, `kind`, `resolver`, and `resolverPath` steps in map arguments. `errorSlave = Some(index)` reserves that `m_axi` interface for addresses outside all mapped segments, so it does not need a memory-map property. Without an error slave, misses select the first interface in address order. Every map has an explicit declared size; validation rejects out-of-bounds and overlapping children or segments. The map model contains no routing indices; decoder routing privately zips address-ordered children with the interface order. A private `DemuxMm_Resolver(owner: DemuxMm)(implicit sourceInfo: SourceInfo)` binds the slave interface and master-interface vector, which eagerly marks disabled standard channel groups `Undefined`, eagerly enforces the configured outstanding/ID limits as local input capabilities, publishes the generated slave memory map when `genDecoder()` runs, forwards every enabled upstream master fact from `s_axi` to every output, and deliberately does not aggregate downstream traffic capabilities. Decoder elastic IO remains declared for tracking but is private to the module. Unnamed or zero-sized child maps and layouts exceeding `wAddr` are rejected.

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

**Details:** Module. Full AXI fan-in. Its private `Mux_Resolver` uses a grouped binding for the slave-port vector and forwards the common downstream slave's capabilities to every input. It keeps upstream master facts at their individual inputs; requests for a synthetic aggregate master profile at `m_axi` return `DontCare`. Internally creates arbitration and channel-level elastic wiring for read and write paths.

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

**Details:** Module. ID-based full AXI fan-out. Its private `IdDemux_Resolver` forwards upstream master facts to each ID-selected output and leaves downstream slave capabilities separate instead of aggregating them. Internally expands to elastic channel components and rewrites ID bits for routing.

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

**Details:** Module. ID-aware full AXI fan-in. Its private `IdMux_Resolver` forwards the common downstream slave's capabilities to every input and returns `DontCare` for a synthetic aggregate output master profile. Internally uses ID-based response routing and channel-level elastic components.

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

**Details:** Module. Full AXI ID serialization. Its private `IdSerialize_Resolver` forwards the invariant memory map and leaves transformed traffic properties `Incomplete`. Internally limits outstanding work and routes response channels through elastic control logic.

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

**Details:** Module. Full AXI ID parallelization. Its private `IdParallelize_Resolver` forwards the invariant memory map and leaves transformed traffic properties `Incomplete`. Internally distributes requests and rejoins responses with elastic channel logic.

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

**Details:** Module. Full AXI width upscaler. Its private `Upscale_Resolver` forwards the invariant memory map and leaves transformed traffic properties `Incomplete`. Internally adapts read/write data channels and keeps address/control channels aligned.

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
  numOutstandingWrite = 32,
  simCheckBurst = chext.util.SimulationCheck.Default
)
```

**Details:** Config. Configuration for `Downscale`; master data width must be narrower than the slave data width. The input must have no transaction IDs (`wId == 0`) and every request must be single-beat (`ARLEN == 0`, `AWLEN == 0`). `simCheckBurst` controls optional simulation checks for the single-beat precondition.

---

<a id="entry-axi4-full-components-downscale"></a>

### `axi4f.components.Downscale` [#](#entry-axi4-full-components-downscale)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/Downscale.scala); [Scala TB](../src/test/scala/chext/amba/axi4/full/components/Downscale.tb.scala); [SysC TB](../sysc_tb/chext/amba/axi4/full/components/src/Downscale.tb.cpp)

**Intent and Usage:** Adapt a wider full AXI data bus to a narrower one:
```scala
val downscale0 = Module(new axi4f.components.Downscale(cfg))
```

**Details:** Module. Full AXI width downscaler for ID-free, single-beat input. Its private `Downscale_Resolver` forwards the invariant memory map and leaves transformed traffic properties `Incomplete`. It may create a narrow output burst; use `Unburst` before it for burst-capable input and after it for a single-beat downstream interface.

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

**Details:** Module. Full AXI burst decomposition. Its private `Unburst_Resolver` forwards the invariant memory map and leaves transformed traffic properties `Incomplete`. Internally generates per-beat addresses and coordinates response/data channels.

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

**Details:** Module. Full AXI data widening helper. Its private `Widen_Resolver` forwards the invariant memory map and leaves transformed traffic properties `Incomplete`. Internally coordinates address, strobe, data, and response handling through channel logic.

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

**Details:** Module. Full AXI credit-style response buffering around read/write channels. `CreditBuffer_Resolver` uses the shared `Master.ShapeProperty()` / `Slave.ShapeProperty()` catalog groups to forward invariant burst-shape facts, forwards `Slave.MemoryMap` separately, and marks credit-sensitive outstanding/thread properties `Incomplete` until their capacity equations are implemented.

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

**Details:** Module. Staged full AXI protocol conversion. It composes lower-level full AXI component modules and their internal elastic channel components. `ProtocolConverter_Resolver` forwards the invariant memory map and marks traffic properties `Incomplete` until the composed transformation equations are represented.

---

<a id="entry-axi4-full-components-lite-converter-config"></a>

### `axi4f.components.LiteConverterConfig` [#](#entry-axi4-full-components-lite-converter-config)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/LiteConverter.scala)

**Intent and Usage:** Configure conversion from AXI4-Full to AXI4-Lite:
```scala
val cfg = axi4f.components.LiteConverterConfig(
  axiSlaveCfg = fullCfg.copy(wId = 0, wData = 64),
  wDataMaster = 32,
  numOutstandingRead = 2,
  numOutstandingWrite = 2,
  simCheckNarrow = chext.util.SimulationCheck.Default,
  simCheckAligned = chext.util.SimulationCheck.Default
)
```

**Details:** Config. Configures inferred burst decomposition and width conversion. The Full interface must have `wId == 0`, and all user widths must be zero; `LiteConverter` does not instantiate `IdSerialize`. Outstanding capacities default to 2. `simCheckNarrow` verifies full-width `AxSIZE`, while `simCheckAligned` verifies naturally aligned `AxADDR`.

---

<a id="entry-axi4-full-components-lite-converter"></a>

### `axi4f.components.LiteConverter` [#](#entry-axi4-full-components-lite-converter)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/LiteConverter.scala); [Scala TB](../src/test/scala/chext/amba/axi4/full/components/LiteConverter.test.scala)

**Intent and Usage:** Convert an AXI4-Full slave to an AXI4-Lite master:
```scala
val liteConverter0 =
  Module(new axi4f.components.LiteConverter(cfg))
s_axi :=> liteConverter0.s_axi
liteConverter0.m_axil :=> registerBlock.s_axil
```

**Details:** Module. ID-free Full-to-Lite conversion with generated and accepted traffic properties. A private `LiteConverter_Resolver(owner: LiteConverter)(implicit sourceInfo: SourceInfo)` uses typed endpoint-family bindings, assigns the interface traffic properties, and resolves `Slave.MemoryMap` from `m_axil` back to `s_axi`. Input unbursting always precedes inferred width conversion. Upscaling steers data and write strobes without adding beats; downscaling is followed by a second unburst stage.

---

<a id="entry-axi4-full-components-constant-slaves"></a>

### `axi4f.components.ConstantSlave` / `ZeroSlave` / `ErrorSlave` [#](#entry-axi4-full-components-constant-slaves)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/Termination.scala)

**Intent and Usage:** Terminate a full AXI master with constant responses:
```scala
val constantSlave = Module(new axi4f.components.ConstantSlave(
  axiCfg = fullCfg,
  readData = "h1234".U,
  response = axi4.ResponseFlag.OKAY
))
val zeroSlave = Module(new axi4f.components.ZeroSlave(fullCfg))
val errorSlave0 = Module(new axi4f.components.ErrorSlave(fullCfg))
val errorSlave1 = Module(new axi4f.components.ErrorSlave(
  fullCfg,
  axi4.ResponseFlag.SLVERR
))
```

**Details:** Module. `ConstantSlave` applies `SlaveBuffered` to its complete interface, preserves IDs and read burst length, discards writes, and returns constant read data and responses. Its owner-specific resolver publishes terminal slave traffic capabilities and a full-address-space memory map only for successful responses. `ZeroSlave` derives from it with zero/`OKAY`; `ErrorSlave` derives from it with zero and a selectable `SLVERR` or `DECERR` response (default `DECERR`). Internal two-entry channel buffers, transducers, and joins are tracked elastic components.

---

<a id="entry-axi4-full-components-stall-slave-idle-master"></a>

### `axi4f.components.StallSlave` / `IdleMaster` [#](#entry-axi4-full-components-stall-slave-idle-master)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/full/components/Termination.scala)

**Intent and Usage:** Attach explicit inactive full AXI endpoints:
```scala
val stallSlave = Module(new axi4f.components.StallSlave(fullCfg))
val idleMaster = Module(new axi4f.components.IdleMaster(fullCfg))
```

**Details:** Module. `StallSlave` permanently backpressures requests and produces no responses; `StallSlave_Resolver` publishes zero accepted traffic. `IdleMaster` issues no requests and consumes any responses; `IdleMaster_Resolver` publishes zero generated traffic. Both expose their behavior through tracked elastic termination components.

---

## AXI4 Lite Components

AXI4-Lite components are Chisel modules. They are not Chext components themselves; graph content comes from the internal channel-level elastic components. Every module that declares tracked AXI ports owns a private `X_Resolver(owner: X)(implicit sourceInfo: SourceInfo)` beside the construction; resolvers use the role-free `bindCommon`, `bindMaster`, and `bindSlave` single-interface or grouped matchers. Directional standard keys use the same family/access-specific bases, catalog groups, and extractors as Full AXI. Bindings eagerly mark disabled standard directions `Undefined`, while owner-configuration facts are eagerly enforced. Tracking generates only the information needed to catch composition errors: intentional non-properties resolve as `DontCare(message)`, useful but unavailable facts resolve as `Incomplete`, and compatibility checks are deferred to a later phase. No component-specific resolver is defined under `tracking`. For the detailed guide and examples for every Lite AXI component/helper, see [axi4-lite.md](axi4-lite.md).

<a id="entry-axi4-lite-components-constant-slaves"></a>

### `axi4l.components.ConstantSlave` / `ZeroSlave` / `ErrorSlave` [#](#entry-axi4-lite-components-constant-slaves)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/components/Termination.scala)

**Intent and Usage:** Terminate an AXI4-Lite master with constant responses:
```scala
val constantSlave = Module(new axi4l.components.ConstantSlave(
  axiCfg = liteCfg,
  readData = "h1234".U,
  response = axi4.ResponseFlag.OKAY
))
val zeroSlave = Module(new axi4l.components.ZeroSlave(liteCfg))
val errorSlave0 = Module(new axi4l.components.ErrorSlave(liteCfg))
val errorSlave1 = Module(new axi4l.components.ErrorSlave(
  liteCfg,
  axi4.ResponseFlag.SLVERR
))
```

**Details:** Module. `ConstantSlave` applies `SlaveBuffered` to its complete interface, accepts AW and W in either order, discards writes, and returns constant read data and responses. Its owner-specific resolver publishes terminal Lite slave capabilities and a full-address-space memory map only for successful responses. `ZeroSlave` derives from it with zero/`OKAY`; `ErrorSlave` derives from it with zero and a selectable `SLVERR` or `DECERR` response (default `DECERR`). Internal two-entry channel buffers, transforms, and joins are tracked elastic components.

---

<a id="entry-axi4-lite-components-stall-slave-idle-master"></a>

### `axi4l.components.StallSlave` / `IdleMaster` [#](#entry-axi4-lite-components-stall-slave-idle-master)

**Sources:** [Scala](../src/main/scala/chext/amba/axi4/lite/components/Termination.scala)

**Intent and Usage:** Attach explicit inactive AXI4-Lite endpoints:
```scala
val stallSlave = Module(new axi4l.components.StallSlave(liteCfg))
val idleMaster = Module(new axi4l.components.IdleMaster(liteCfg))
```

**Details:** Module. `StallSlave` permanently backpressures requests and produces no responses, and its resolver publishes zero slave traffic. `IdleMaster` issues no requests and consumes any responses, and its resolver publishes zero master traffic. Both expose their behavior through tracked elastic termination components.

---

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

**Details:** Module. AXI4-Lite fan-out for address/data/control channels. Its private `Demux_Resolver` bindings eagerly classify disabled standard channel groups as `Undefined`, forwards every enabled upstream master property from `s_axil` unchanged to each output, eagerly enforces response-routing queue capacities only as local slave-side outstanding limits, and does not aggregate downstream slave capabilities. Unnecessary synthetic input properties return `DontCare`; an arbitrary decode function leaves the combined memory map `Incomplete`.

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

**Details:** Module. AXI4-Lite fan-in with channel-level elastic arbitration and routing. Its private `Mux_Resolver` binds the complete input vector and forwards slave properties from `m_axil` to each input. Upstream master facts remain separate, so a synthetic aggregate master property at `m_axil` returns `DontCare`.

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

**Details:** Module. AXI4-Lite response buffering with credit-style control around read/write channels. Its owner-specific resolver uses the shared `Master.ShapeProperty()` / `Slave.ShapeProperty()` catalog groups for invariant burst shape, forwards `Slave.MemoryMap` separately, and marks credit-sensitive capacity facts `Incomplete`.

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

**Details:** Module. AXI4-Lite memory controller. `MemController_Resolver` publishes the memory size and fixed Lite slave traffic properties beside the module. Internally bridges AXI4-Lite channels to memory-style request and response paths.

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

**Details:** Module. AXI4-Lite controller variant for synchronous-read memories. `SyncReadMemController_Resolver` publishes the memory size and fixed Lite slave traffic properties beside the module.

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

val control = RegInit(0.U(32.W))
val status = WireDefault(0.U(32.W))
regs.reg(control, desc = "control")
regs.reg(status, write = false, desc = "status")
val memoryMap = regs.complete()
assert(regs.memoryMapOption.contains(memoryMap))
assert(regs.memoryMap == memoryMap)
```

**Details:** Component. AXI4-Lite register block with generated Elastic read/write behavior. `memoryMapOption` is `None` until `complete()` is called exactly once; `memoryMap` requires the completed value. A private `RegisterBlock_Resolver(owner: RegisterBlock)(implicit sourceInfo: SourceInfo)` uses a typed slave binding and eagerly assigns the fixed AXI slave properties. `complete()` eagerly publishes `Slave.MemoryMap`; a resolution request made before completion fails.

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
