# `chext.elastic` Package

`chext.elastic` provides ready/valid building blocks for expressing token-flow hardware. The
package follows the usual Chisel `Decoupled` contract: a producer may assert `valid` independent
of `ready`, and consumers apply backpressure through `ready`. Chext components add a tracking
layer on top of those interfaces so module graph generation can describe which components consume,
produce, split, join, buffer, or route each elastic endpoint.

For graph and diagnostic details, see [tracking.md](tracking.md) and
[tracking-elastic.md](tracking-elastic.md). For a compact source-linked catalog, see
[synopsis.md](synopsis.md).

## Imports

Most examples use the short package alias:

```scala
import chisel3._
import chisel3.util._
import chext.{elastic => e}
import e.ConnectOp._
```

## Interfaces

| Construction | Purpose |
|---|---|
| `e.Interface(gen)` | Raw elastic interface with `$valid`, `$ready`, and `$bits`. |
| `e.Source(gen)` | Producer endpoint from the current module's perspective. Root IO is tracked as a graph source. |
| `e.Sink(gen)` | Consumer endpoint from the current module's perspective. Root IO is tracked as a graph sink. |
| `e.EWire(gen)` | Internal elastic wire. Tracked as a graph wire when materialized. |
| `e.Source.like(x)`, `e.Sink.like(x)`, `e.EWire.like(x)` | Create an endpoint with the same payload type as another interface. |
| `e.Source.many(n, gen)`, `e.Sink.many(n, gen)`, `e.Interface.many(n, gen)` | Create a `chext.util.NamedVec` of elastic interfaces. |

```scala
class EndpointExample extends Module {
  val source = IO(e.Source(UInt(32.W)))
  val sink = IO(e.Sink(UInt(32.W)))
  val temp = e.EWire.like(source)

  source :=> temp
  temp :=> sink
}
```

## Connections

`source :=> sink` creates a tracked `Connect` component. The source appears on the left of the
dataflow description and the sink on the right of the operator expression:

```scala
source :=> sink
```

Sequence connections create one `Connect` per pair:

```scala
sources :=> sinks
```

Use `new e.Connect(source, sink)` when a named component value is useful, for example to attach a
`fire { ... }` hook.

## Buffering

Buffers are queues inserted around existing endpoints. They are commonly used to break long
backpressure paths, add elasticity between slow and fast regions, or remove a combinational path
from an interface's `ready` to the same interface's `valid`.

| Construction | Shape |
|---|---|
| `e.SourceBuffer(source, count)` | Returns an interface after a queue: `source -> Queue -> returned`. |
| `e.SinkBuffer(sink, count)` | Returns an interface before a queue: `returned -> Queue -> sink`. |
| `e.LeftBuffer(source, count)` | Generic source-side alias. |
| `e.RightBuffer(sink, count)` | Generic sink-side alias. |
| `e.SourceBuffered(source, count)` | Buffered source-side interface intended for reuse as a named value. |
| `e.SinkBuffered(sink, count)` | Buffered sink-side interface intended for reuse as a named value. |

```scala
e.SourceBuffer(source, count = 2) :=> sink
source :=> e.SinkBuffer(sink, count = 2)

val bufferedSource = e.SourceBuffered(source, count = 4)
bufferedSource :=> sink
```

`SourceBuffer.irrevocable`, `SourceBuffer.decoupled`, `SinkBuffer.irrevocable`, and
`SinkBuffer.decoupled` select the returned interface flavor explicitly.

## Primitive Components

| Component | Use |
|---|---|
| `e.Connect` | Pass tokens through unchanged. |
| `e.Transform` | Combinationally transform payload bits while preserving handshake behavior. |
| `e.Stall` | Hold a token while a condition is true. Supports `fire { ... }`. |
| `e.Drop` | Consume a token without forwarding it when a condition is true. |
| `e.Transducer` | Stateful token or packet control with `stall`, `accept`, `consume`, and `produce` actions. |
| `e.Queue` | Explicit tracked queue between an existing source and sink. |
| `e.Wrap` | Wrap delayed logic; nonzero delay builds internal counter, queue, and connect stages. |

```scala
val transform0 = new e.Transform(source, sink) {
  out := in + 1.U
}

val stall0 = new e.Stall(source, sink) {
  out := in
  cond { waitForSideCondition }
  fire { accepted := true.B }
}

val drop0 = new e.Drop(source, sink) {
  out := in
  cond { shouldDiscard }
}
```

`Transducer` is the most flexible primitive when output behavior depends on local state:

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

## Sources and Sinks

| Component | Use |
|---|---|
| `e.NullSink(source)` | Consume all tokens from a source. |
| `e.NullSource(sink)` | Drive a sink with no valid tokens. |
| `e.Once(sink)` | Component form of a one-shot source. |
| `e.Once(value)` | Functional form returning a source that emits one value. |
| `e.Counter(sink, ...)` | Component form of a wrapping counter source. |
| `e.Counter(...)` / `e.Counter.fromWidth(...)` | Functional forms returning a counter source. |
| `e.Const(value)` / `e.Const.explicit(gen)(...)` | Constant-producing source helpers. |
| `e.Constant(value)` | Older helper that creates an `EWire` and enqueues a hardware value. |

```scala
val once = e.Once(value)
val counter = e.Counter(maxValueExclusive = 16)
val constant = e.Const(42.U(8.W))

once :=> sink0
counter :=> sink1
constant :=> sink2
```

## Forking, Joining, and Zipping

`Fork` branches one input token to multiple outputs. The implementation is eager: the input token
is consumed only when all selected forks can accept their outputs.

```scala
val fork0 = new e.Fork(source) {
  fork(in(31, 16)) :=> sinkHi
  fork(in(15, 0)) :=> sinkLo
  fork() :=> sinkFull
}
```

`Join` waits for several sources and produces one output token:

```scala
val join0 = new e.Join(sink) {
  out := join(sourceA) + join(sourceB)
}
```

`e.Zip(...)` is a helper for joining sources into a bundle-valued elastic interface. It is not its
own graph node; the graph records the join-style wiring it creates.

```scala
val zipped = e.Zip(sourceA, sourceB)
```

## Selection and Routing

| Component | Use |
|---|---|
| `e.Merger` | Merge several sources under the external guarantee that at most one is active. |
| `e.Mux` | Select one source using an elastic select stream. |
| `e.Demux` | Route one source to one selected sink using an elastic select stream. |
| `e.Arbiter` | Choose among sources and emit the selected index. |
| `e.ArbiterNs` | Choose among sources without emitting the selected index. |
| `e.DemuxNs` | Route using a protected `select { ... }` function instead of a select stream. |

```scala
val mux0 = new e.Mux(sources, sink, sourceSelect)
val demux0 = new e.Demux(source, sinks, sourceSelect)
val arbiter0 = e.Arbiter(sources, sink, e.Chooser.rr, Some(sinkSelect))
val arbiterNs0 = new e.ArbiterNs(sources, sink, e.Chooser.rr)

val demuxNs0 = new e.DemuxNs(source, sinks) {
  select { in => in.index }
}
```

`Chooser` supplies arbitration policies. The commonly used round-robin chooser is `e.Chooser.rr`.

## Counting, Repetition, and Stateful Flow

`Count` is a low-level repeat/count primitive. It computes initial state from an input token,
continues while `cond` is true, updates state with `next`, and builds each output using `out`.

```scala
val count0 = new e.Count(source, sink, UInt(8.W)) {
  init { in => 0.U }
  cond { (in, state) => state =/= in.limit }
  next { (in, state) => state + 1.U }
  out { (in, state, first, last) => in }
}
```

`Repeat` is a container built on top of `Count`:

```scala
val repeat0 = new e.Repeat(source, sink, wIndex = 8) {
  len { in => in.length }
  out { (in, index, first, last) => in }
}
```

`Fold` reduces a stream using user-provided fold logic:

```scala
val fold0 = new e.Fold(source, sourceInit, sink) {
  operand { in => in.data }
  last { in => in.last }
  val join0 = new e.Join(sourceResult) {
    out := join(sinkA) + join(sinkB)
  }
}
```

## Higher-Level Containers

| Container / module | Use |
|---|---|
| `e.Loop` | Iterative elastic loop with user-visible `sinkCurrent` and `sourceNext` body endpoints. |
| `e.Scope` | Begin/end scoped region with `init` and `exit` hooks. |
| `e.Switch` | Route tokens into the first matching branch, run branch-local subgraphs, then recombine outputs. |
| `e.RandomStall` | Test or diagnostic wrapper that injects random backpressure. |
| `e.ResponseBuffer` | Module that stalls requests when too many responses are outstanding. |
| `e.ShareD` / `e.ShareNd` | Modules for deterministic or non-deterministic sharing of fewer resources among more requesters. |

```scala
val loop0 = new e.Loop(sourceInit, sinkExit) {
  end { state => state.done }
  sinkCurrent :=> loopBody.source
  loopBody.sink :=> sourceNext
}

val scope0 = new e.Scope(sourceInit, sinkExit) {
  init { in => started := true.B }
  exit { out => finished := true.B }
  sinkBegin :=> body.source
  body.sink :=> sourceEnd
}
```

`Switch` is useful when the selected branch owns a subgraph rather than a single output expression:

```scala
val switch0 = new e.Switch(source, sink) {
  last { _.last }

  namedBranch("add") { _.opcode === 0.U } { (source, sink) =>
    val transform0 = new e.Transform(source, sink) {
      out := in
    }
  }

  namedBranch("repeat") { _.opcode === 1.U } { (source, sink) =>
    val repeat0 = new e.Repeat(source, sink, 8) {
      len { _.count }
      out { (in, index, first, last) => in }
    }
  }
}
```

## Component Tracking Summary

Most concrete elastic constructions are tracked as either `Component` or `Container` entries in the
module graph:

| Graph type | Typical constructors | Ports |
|---|---|---|
| `Connect` | `source :=> sink`, `new e.Connect` | `source`, `sink` |
| `Transform`, `Stall`, `Drop`, `Transducer`, `Queue`, `Wrap` | Primitive one-in/one-out components | `source`, `sink` |
| `NullSink` | `new e.NullSink(source)` | `source` |
| `NullSource`, `Once`, `Counter`, `Const` | Source-producing components | `sink` |
| `Fork` | `new e.Fork(source)` | `source`, `sink_0`, `sink_1`, ... |
| `Join` | `new e.Join(sink)` | `source_0`, `source_1`, ..., `sink` |
| `Merger`, `Mux`, `Demux`, `Arbiter`, `ArbiterNs`, `DemuxNs` | Routing components | Component-specific indexed source/sink/select ports |
| `Repeat`, `Fold`, `Loop`, `Scope`, `Switch`, `RandomStall` | Containers that own child components | Child components appear under the container |

Actual graph paths come from Chisel naming, `prefix(...)`, and Chext `uniquePrefix(...)`; the
component `tpe` describes what was built, while the path describes where it was elaborated.
