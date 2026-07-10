# `chext.elastic` Package

`chext.elastic` provides ready/valid building blocks for token-flow hardware. The
payload travels in `$bits`, producers assert `$valid`, and consumers apply
backpressure with `$ready`. The package also marks ports and components for
Chext's elastic tracking graph.

For graph and diagnostic details, see [tracking.md](tracking.md) and
[tracking-elastic.md](tracking-elastic.md). For a compact source-linked catalog,
see [synopsis.md](synopsis.md). AXI4 components are documented separately in
[axi4-full.md](axi4-full.md) and [axi4-lite.md](axi4-lite.md).

## Imports

Most examples use the package alias and the connection operator:

```scala
import chisel3._
import chisel3.util._
import chext.{elastic => e}
import e.ConnectOp._
```

## Interfaces

### `Interface`, `Source`, `Sink`, and `EWire`

`e.Interface(gen)` is the raw elastic record with `ready`, `valid`, and `bits`.
The direct accessors `ready`, `valid`, and `bits` print a warning; component
implementations use `$ready`, `$valid`, and `$bits` when they intentionally wire
the protocol.

`e.Source(gen)` is a producer endpoint from the module's perspective, and
`e.Sink(gen)` is a consumer endpoint. `e.EWire(gen)` creates an internal elastic
wire and applies `dontTouch`.

Useful helpers:

| Helper | Description |
|---|---|
| `e.Source.io(gen)` | `IO(e.Source(gen))`. |
| `e.Source.like(x)` / `e.Sink.like(x)` / `e.EWire.like(x)` | Clone the payload type of another elastic interface. |
| `e.Source.manyLike(n, x)` / `e.Sink.manyLike(n, x)` / `e.EWire.manyLike(n, x)` | Clone the payload type of another elastic interface multiple times. |
| `e.Source.ioLike(x)` | `IO(e.Source.like(x))`. |
| `e.Interface.many(n, gen)` / `e.Source.many(n, gen)` / `e.Sink.many(n, gen)` | Build a `chext.util.NamedVec` type of interfaces. |
| `e.EWire.many(n, gen)` | Build an internal `dontTouch` hardware wire containing a `chext.util.NamedVec` of interfaces. |
| `interface.fire` | True when `$ready && $valid`. |
| `interface.enq(value)` / `interface.noenq()` | Drive a producer side. |
| `interface.deq()` / `interface.nodeq()` | Drive a consumer side. |

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

### `Connect` and `ConnectOp`

`new e.Connect(source, sink)` connects the payload with Chisel's standard `:=`
connection (`sink.bits := source.bits`), so compatible aggregates use Chisel's
normal name-based field matching. It does not apply a user-defined transformation.
Sink valid follows source valid, source ready follows sink ready, and the component
supports `fire { ... }` hooks through the sink-side handshake.

`e.ConnectOp._` adds `source :=> sink`. For sequences, `sources :=> sinks`
creates one `Connect` per pair and requires equal sequence lengths. Sequence graph paths have an
outer `connectMany` scope, a numeric element scope, and an innermost `connect0` value prefix.

```scala
source :=> sink

val connect0 = new e.Connect(source, sink) {
  fire { accepted := true.B }
}

sources :=> sinks
```

## Buffering and Queues

### `SourceBuffer`, `SinkBuffer`, `LeftBuffer`, and `RightBuffer`

Buffer helpers insert a tracked `Queue` around an existing interface. All accept
`count`, `flow`, and `pipe`; the named `SourceBuffer` and `SinkBuffer` forms also
accept a `name` prefix. Sequence overloads buffer every element.

| Helper | Shape |
|---|---|
| `e.SourceBuffer(source, count)` / `e.LeftBuffer(source, count)` | `source -> Queue -> returned`. |
| `e.SinkBuffer(sink, count)` / `e.RightBuffer(sink, count)` | `returned -> Queue -> sink`. |
| `e.SourceBuffered(source, count)` | Same source-side queue without the `uniquePrefix` wrapper used by `SourceBuffer`; sequence overloads rely on the assigned `val` name plus per-element indexes. |
| `e.SinkBuffered(sink, count)` | Same sink-side queue without the `uniquePrefix` wrapper used by `SinkBuffer`; sequence overloads rely on the assigned `val` name plus per-element indexes. |

```scala
e.SourceBuffer(source, count = 2, flow = true) :=> sink
source :=> e.SinkBuffer(sink, count = 4, pipe = true)

val bufferedSources = e.SourceBuffer(sources, count = 2)
bufferedSources :=> sinks
```

### `Queue`

`Queue` is a tracked FIFO between a source and a sink. `count == 0` degenerates
to a combinational pass-through. For positive counts it uses either Chisel
memory, SyncReadMem, or the Chext Verilog memory blackboxes depending on
`useSyncReadMem` and the global `Queue.useVerilogMem` flag. `flow` allows
same-cycle pass-through when empty, and `pipe` allows enqueue when full if the
sink is ready.

`Queue` can also transform the payload while dequeuing via `out` or
`outExplicit`.

```scala
val queue0 = new e.Queue(source, sink, count = 8, pipe = true, flow = false) {
  out { in => in + 1.U }
}

val q = e.Queue(UInt(16.W), count = 4)
producer :=> q.source
q.sink :=> consumer

e.Queue.useVerilogMem(enabled = false)
```

## One-Input / One-Output Primitives

### `Transform`

`Transform` preserves the ready/valid handshake and lets the body define an
explicit combinational payload transformation by assigning the protected `out`
from the protected `in`. Unlike `Connect`, it does not connect the payload
automatically; the body must drive `out`.

```scala
val transform0 = new e.Transform(source, sink) {
  out := in + 1.U
}
```

### `Stall`

`Stall` forwards only while `cond` is false. When `cond` is true, it deasserts
both source ready and sink valid, holding the input token in place. The condition
must be called exactly once. Because elastic payloads are irrevocable, once the
stall condition becomes false for a token it should not become true again before
the transfer completes; add a sink buffer if that cannot be guaranteed.

```scala
val stall0 = new e.Stall(source, sink) {
  out := in
  cond { waitForCredit }
  fire { consumed := true.B }
}
```

### `Drop`

`Drop` discards tokens while `cond` is true. A dropped token is consumed from the
source without producing a sink token. When `cond` is false, the token is
forwarded normally. The condition must be called exactly once.

```scala
val drop0 = new e.Drop(source, sink) {
  out := in
  cond { in === 0.U }
}
```

### `Transducer`

`Transducer` is a stateful one-input/one-output controller. Define exactly one
`packet { ... }` block. Inside it, choose exactly one action whenever the source
is valid:

| Action | Source token | Sink token | State update timing |
|---|---|---|---|
| `stall { ... }` | Not consumed | Not produced | Immediate while source valid. |
| `accept { ... }` | Consumed when sink ready | Produced | Only when sink ready. |
| `consume { ... }` | Consumed | Not produced | Immediate while source valid. |
| `produce { ... }` | Not consumed | Produced | Only when sink ready. |

The implementation prints simulation warnings if more than one action or no
action is selected in a valid cycle. The default behavior when no action is
selected is a stall.

```scala
val transducer0 = new e.Transducer(source, sink) {
  val skipsLeft = RegInit(0.U(8.W))

  packet {
    out := in.data

    when(skipsLeft === 0.U) {
      accept { skipsLeft := in.skip }
    }.otherwise {
      consume { skipsLeft := skipsLeft - 1.U }
    }
  }
}
```

### `Wrap`

`Wrap` is an abstract wrapper for logic with a known latency. Implement
`delay`, assign `out` from the delayed computation, and optionally override
`queueLength`. For `delay == 0`, it behaves like a transform. For nonzero delay,
it tracks outstanding accepted inputs with a counter, shifts the source fire
signal by `delay`, and queues the delayed output before the sink.

```scala
val wrap0 = new e.Wrap(source, sink) {
  protected def delay: Int = 2
  out := RegNext(RegNext(in + 3.U))
}
```

### `Fire`

`Fire` is a private mixin used by components that expose `fire { ... }` hooks.
The hook body runs when the component's sink-side interface fires. User code does
not instantiate `Fire` directly; use it through components such as `Connect`,
`Queue`, `Transform`, `Stall`, `Drop`, `Transducer`, `Once`, `Const`, `Fold`,
`Repeat`, `Arbiter`, and `Wrap`. Some final source/sink classes also mix in
`Fire` internally, but user code cannot add anonymous-subclass hook bodies to
final classes.

```scala
val queue0 = new e.Queue(source, sink, count = 4) {
  fire { packets := packets + 1.U }
}
```

## Sources and Sinks

### `NullSink`

`NullSink` consumes every token from a source by permanently asserting ready. It
is tracked with a single `source` port.

```scala
val nullSink0 = new e.NullSink(unusedSource)
```

### `NullSource`

`NullSource` drives a sink with no valid tokens by calling `noenq`.

```scala
val nullSource0 = new e.NullSource(idleSink)
```

### `Once`

`Once` emits one token and then holds valid low forever. The class form lets the
body assign `out`; the functional forms return the generated source interface.

```scala
val once0 = new e.Once(sink) {
  out := 7.U
}

val onceSource = e.Once(7.U(8.W))
val explicitOnce = e.Once.explicit(new Bundle {
  val data = UInt(8.W)
}) { out => out.data := 7.U }
```

### `Counter`

`Counter` is an always-valid wrapping UInt source. The class form writes into an
existing sink. `Counter(maxValueExclusive, start)` returns a source whose width
is `log2Ceil(maxValueExclusive)`. `Counter.fromWidth(width, start)` returns a
source that wraps over the full UInt width.

```scala
val counter0 = new e.Counter(sink, maxValueExclusive = 16, start = 4)

val small = e.Counter(maxValueExclusive = 10)
val fullWidth = e.Counter.fromWidth(width = 8, start = 3)
```

### `Const` and `Constant`

`Const` is an always-valid source. `Const(value)` requires a hardware value and
returns an elastic interface. `Const.explicit(gen) { out => ... }` constructs a
constant-like payload with imperative assignments. `Constant(value)` is the older
helper that creates an `EWire`, enqueues the value, and returns it without a
tracked component.

```scala
val c0 = e.Const(42.U(8.W))

val c1 = e.Const.explicit(new Bundle {
  val opcode = UInt(4.W)
  val data = UInt(32.W)
}) { out =>
  out.opcode := 1.U
  out.data := 42.U
}

val legacy = e.Constant(0.U(8.W))
```

## Forking and Joining

### `Fork`

`Fork` creates multiple sink interfaces from one source. In eager mode, the
default, the source token may be accepted by different sinks over multiple
cycles; the original source is consumed only after every forked sink has accepted
the token. In lazy mode, all sinks must be ready in the same cycle.

Inside the body, call `fork()` to forward the full input payload or
`fork(expr)` to produce a branch with a derived payload.

```scala
val fork0 = new e.Fork(source, eager = true) {
  fork(in(31, 16)) :=> sinkHi
  fork(in(15, 0)) :=> sinkLo
  fork() :=> sinkFull
}
```

### `Join`

`Join` waits until every joined source is valid, asserts the output valid, and
fires all sources together when the sink is ready. The body assigns `out` using
values returned from `join(source)`.

```scala
val join0 = new e.Join(sink) {
  out := join(sourceA) + join(sourceB)
}
```

### `Zip`

`Zip` is a helper built on `joinImpl`. It joins one to eight interfaces and
returns an elastic interface carrying a `WireBundleN` of the input payloads. It
is useful when the desired output is a tuple-like bundle.

```scala
val zipped = e.Zip(sourceA, sourceB, sourceC)
val useZip = new e.Transform(zipped, sink) {
  out := in._1 + in._2 + in._3
}
```

## Routing and Arbitration

### `Merger`

`Merger` OR-merges several sources into one sink under the external guarantee
that at most one source is valid in a cycle. It asserts in hardware if multiple
sources are valid. All sources see the sink ready, and the sink payload comes
from the valid source selected by `PriorityEncoder`.

```scala
val merger0 = e.Merger(Seq(sourceA, sourceB, sourceC), sink)
```

### `Mux`

`Mux` uses an elastic select stream to choose one input source. The selected
source is consumed when the output sink fires. The select stream is consumed
only when `last(selectedPayload)` is true, which lets one select token cover a
multi-token sequence. `last` defaults to `true.B`. Optional `out` and
`outExplicit` hooks transform the selected payload.

```scala
val mux0 = new e.Mux(sources, sink, sourceSelect) {
  last { in => in.last }
  out { in => in.data }
}

val mux1 = e.Mux(sources, sink, sourceSelect, isLastFn = _.last)
```

### `Demux`

`Demux` uses an elastic select stream to route one source into one of many
sinks. The select stream is consumed only when `last(sourcePayload)` is true.
`last` defaults to `true.B`. Optional `out` and `outExplicit` hooks transform the
payload written to the selected sink.

```scala
val demux0 = new e.Demux(source, sinks, sourceSelect) {
  last { in => in.last }
  out { in => in.data }
}

val demux1 = e.Demux(source, sinks, sourceSelect, isLastFn = _.last)
```

### `Arbiter`

`Arbiter` chooses one valid source with a `Chooser`, forwards the payload to the
sink, and emits the chosen source index on `sinkSelect`. Both the data sink and
the select sink must accept before a new arbitration decision is released.
Optional `out` and `outExplicit` hooks transform the selected payload.

```scala
val select = e.EWire(UInt(log2Ceil(sources.length).W))

val arbiter0 = new e.Arbiter(sources, sink, select, e.Chooser.rr) {
  out { in => in }
}
```

### `ArbiterNs`

`ArbiterNs` is the no-select variant of `Arbiter`. It chooses one valid source
with a `Chooser` and forwards only the payload.

```scala
val arbiterNs0 = new e.ArbiterNs(sources, sink, e.Chooser.priority)
```

The `Arbiter.apply` helper creates `Arbiter` when `select` is provided and
`ArbiterNs` otherwise:

```scala
e.Arbiter(sources, sink, e.Chooser.rr, Some(select))
e.Arbiter(sources, sink, e.Chooser.rr)
```

### `DemuxNs`

`DemuxNs` computes the destination sink directly from the current input token
instead of consuming a select stream. `select` must be called exactly once.
Optional `out` and `outExplicit` hooks transform the payload.

```scala
val demuxNs0 = new e.DemuxNs(source, sinks) {
  select { in => in.route }
  out { in => in.data }
}
```

### `Chooser`

`Chooser` is the arbitration policy interface used by `Arbiter` and
`ArbiterNs`. It receives the vector of source valid bits and a ready hint, and
returns the chosen index. The package includes round-robin and priority
policies; both lock their current choice while the chosen source is valid but
the downstream path is not ready.

```scala
val rr = e.Chooser.rr
val priority = e.Chooser.priority
val arbiter0 = new e.ArbiterNs(sources, sink, rr)
```

## Counting, Repetition, and Reduction

### `Count`

`Count` turns each input token into zero or more output tokens using an internal
state. It requires all four hooks:

| Hook | Role |
|---|---|
| `init { in => state }` / `initExplicit` | Initial state for a new input token. |
| `cond { (in, state) => Bool }` | Whether the current state should produce output. |
| `next { (in, state) => stateNext }` / `nextExplicit` | State transition after an emitted token. |
| `out { (in, state, first, last) => value }` / `outExplicit` | Output payload and first/last flags. |

If `cond(initState)` is false, the input is consumed and no output is produced.

```scala
val count0 = new e.Count(source, sink, UInt(8.W)) {
  init { in => 0.U }
  cond { (in, state) => state =/= in.limit }
  next { (in, state) => state + 1.U }
  out { (in, state, first, last) => state }
}
```

### `Repeat`

`Repeat` is a container implemented with `Count`. `len` returns how many outputs
to produce for each input token, and `out` builds each repeated payload from the
input, repeat index, and first/last flags.

```scala
val repeat0 = new e.Repeat(source, sink, wIndex = 8) {
  len { in => in.count }
  out { (in, index, first, last) => in.data + index }
}
```

### `Fold`

`Fold` reduces a token group into one result. It consumes a stream `source`, an
initial accumulator source `sourceInit`, and emits the group result on `sink`.
`operand` maps each input token into the reduction type. `last` is required and
marks the end of a group. `first` is optional; if omitted, `Fold` derives first
with an internal `Transducer`. `zero` is optional and skips zero-valued tokens.

The user supplies the actual reduction graph by connecting the protected
interfaces:

| Protected interface | Meaning |
|---|---|
| `sinkA` | Newest operand. |
| `sinkB` | Accumulator or initial value. |
| `sourceResult` | Result of combining `sinkA` and `sinkB`. |

```scala
val fold0 = new e.Fold(source, e.Const(0.S(32.W)), sink) {
  operand { in => in.data }
  zero { in => in.zero }
  last { in => in.last }

  val add0 = new e.Join(sourceResult) {
    out := join(sinkA) + join(sinkB)
  }
}
```

## Containers and Higher-Level Flow

### `Loop`

`Loop` creates an iterative region. `sourceInit` starts an iteration and
`sinkExit` receives states for which `end(state)` is true. States for which
`end` is false are emitted on `sinkCurrent`; the loop body feeds the next state
back through `sourceNext`.

```scala
val loop0 = new e.Loop(sourceInit, sinkExit) {
  end { state => state.done }

  val body = new e.Transform(sinkCurrent, sourceNext) {
    out := in.next
  }
}
```

### `Scope`

`Scope` gates an elastic region so only one scoped transaction is active at a
time. It exposes `sinkBegin` and `sourceEnd` for the body. `init` runs when the
begin token fires, and `exit` runs when the end token fires.

```scala
val scope0 = new e.Scope(sourceInit, sinkExit) {
  init { in => started := true.B }
  exit { out => finished := true.B }

  sinkBegin :=> body.source
  body.sink :=> sourceEnd
}
```

### `Switch`

`Switch` routes each source token into the first branch whose condition is true,
runs a branch-local subgraph, and muxes branch results back to the sink. It
queues branch indices so multi-token branches can return multiple outputs before
the next branch result is selected. `last` defaults to `true.B`; override it when
a branch can emit a sequence. `numOutstanding` controls the index queue depth and
defaults to the number of branches.

```scala
val switch0 = new e.Switch(source, sink) {
  last { out => out.last }

  namedBranch("literal") { _.opcode === 0.U } { (source, sink) =>
    val transform0 = new e.Transform(source, sink) {
      out.data := in.data
      out.last := true.B
    }
  }

  branch { _.opcode === 1.U } { (source, sink) =>
    val repeat0 = new e.Repeat(source, sink, 8) {
      len { _.count }
      out { (in, index, first, last) =>
        val result = Wire(chiselTypeOf(sink.$bits))
        result.data := in.data + index
        result.last := last
        result
      }
    }
  }
}
```

### `RandomStall`

`RandomStall` is a test/diagnostic container that inserts a `Stall` with an
LFSR-derived condition and a `SinkBuffer`. `threshold == 0` always stalls;
`threshold == 2**lfsrBits` never stalls. `lfsrBits` must be at least 4.

```scala
val noisy = new e.RandomStall(source, sink, lfsrBits = 8, threshold = 128)
```

### `ResponseBuffer`

`ResponseBuffer` is a `Module` for request/response resources. It exposes
external `sourceReq`/`sinkResp` and resource-side `sinkReq`/`sourceResp`.
Requests are stalled when the number of outstanding responses reaches
`numEntries`; responses pass through a source buffer and decrement the
outstanding counter when consumed.

```scala
val rb = Module(new e.ResponseBuffer(
  genReq = UInt(32.W),
  genResp = UInt(32.W),
  numEntries = 8
))

clientReq :=> rb.sourceReq
rb.sinkReq :=> resourceReq
resourceResp :=> rb.sourceResp
rb.sinkResp :=> clientResp
```

### `ShareConfig`, `ShareD`, and `ShareNd`

`ShareConfig` describes a sharing module with `2**log2n` clients and
`2**log2k` resources. `log2n >= log2k` is required.

`ShareD` deterministically groups clients per resource and uses counters to mux
requests and demux responses in a fixed order. `ShareNd` arbitrates requests
with round-robin arbiters and routes responses back using a buffered select
stream. When `respQueueLength > 0`, `ShareNd` inserts a `ResponseBuffer` per
client before arbitration.

Subclasses implement `instantiate(index, source, sink)` for one shared resource.

```scala
class SharedAdder(cfg: e.ShareConfig[UInt, UInt]) extends e.ShareNd(cfg) {
  protected def instantiate(
      index: Int,
      source: e.Interface[UInt],
      sink: e.Interface[UInt]
  ): Unit = {
    val transform0 = new e.Transform(source, sink) {
      out := in + index.U
    }
  }
}

val shared = Module(new SharedAdder(
  e.ShareConfig(UInt(32.W), UInt(32.W), log2n = 3, log2k = 1)
))
```

## Experimental Helper

### `Transaction`

`Transaction` is an experimental helper and is not a complete component. Its
`peek`, `poke`, and `hasData` helpers mark interfaces as graph sources or sinks,
but `pop`, `push`, and `block` are unimplemented (`???`). Use the concrete
elastic components above for production designs.

```scala
val tx = new e.Transaction
import tx._

when(source.hasData) {
  val data = source.peek
  sink.poke := data
  // tx.pop(), tx.push(), and tx.block are not implemented.
}
```

## Tracking Summary

Most concrete elastic constructions are tracked as either `Component` or
`Container` entries in the module graph:

| Graph type | Typical constructors | Ports |
|---|---|---|
| `Connect` | `source :=> sink`, `new e.Connect` | `source`, `sink` |
| `Transform`, `Stall`, `Drop`, `Transducer`, `Queue`, `Wrap` | Primitive one-in/one-out components | `source`, `sink` |
| `NullSink` | `new e.NullSink(source)` | `source` |
| `NullSource`, `Once`, `Counter`, `Const` | Source-producing components | `sink` |
| `Fork` | `new e.Fork(source)` | `source`, `sink_0`, `sink_1`, ... |
| `Join` | `new e.Join(sink)` | `source_0`, `source_1`, ..., `sink` |
| `Merger`, `Mux`, `Demux`, `Arbiter`, `ArbiterNs`, `DemuxNs` | Routing components | Indexed source/sink/select ports |
| `Repeat`, `Fold`, `Loop`, `Scope`, `Switch`, `RandomStall` | Containers | Child components appear under the container |
| `ResponseBuffer`, `ShareD`, `ShareNd` | Chisel modules with declared elastic IO | Module graph includes the declared interfaces and contained components |

Actual graph paths come from Chisel naming, `prefix(...)`, and Chext
`uniquePrefix(...)`; the component `tpe` describes what was built, while the path
describes where it was elaborated.
