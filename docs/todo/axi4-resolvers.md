# AXI4 Property Resolution and Compatibility

This document describes the AXI4 property model, resolver flow, and
system-wide compatibility checks. The broader tracking and resolution model is
described in [AXI4 Tracking and Resolution](axi4-tracking.md).

## Goals

The standard AXI4 property values are deliberately coarse:

- `MemoryMap`
- `BurstShape`
- `ThreadMode`
- `TrafficProfile`

Read and write properties remain separate. Master properties, i.e. the issued
traffic, describe transactions that a master may issue. Slave properties, i.e.
the accepted traffic, describe transactions that a slave accepts.

At the end of elaboration, tracking visits every registered AXI interface,
resolves every applicable property selected for checking, and checks the
master value against the slave value when both sides provide values.
Compatibility checking does not use a separate delegation graph.

A property belonging to a read or write access disabled by `axi4.Config` is
`Undefined`.
`BurstShape` is also `Undefined` for every AXI4-Lite interface because Lite has
no burst-shape signals; `bindMaster(...)` and `bindSlave(...)` apply both rules
eagerly. For checked properties, `Unresolved` and `Incomplete` are errors.
`DontCare` means that no compatibility check is needed at that particular
interface because the component's resolver policy preserves the check at other
interfaces. Since the checker visits every interface, no explicit delegation
graph is needed.

One-to-one modules do not originate `DontCare`: they forward an unchanged
property, calculate its forward or inverse transformation, retain an
authoritative local property, or return `Incomplete` when an applicable
transformation is not modeled. `DontCare` is reserved for synthetic
multi-endpoint aggregates that intentionally have no represented value.

`TrafficProfile` is not checked. Transparent boundaries may forward it;
transforming resolvers terminate traffic-profile requests with `incomplete()`
until its compatibility rules are defined.

## Conventions

On an individual AXI connection, the master endpoint is the upstream interface
(`m_axi`) and the slave endpoint is the downstream interface (`s_axi`).

For a module with `s_axi` and `m_axi` interfaces:

- `s_axi` is the slave interface of the module's upstream AXI connection.
- `m_axi` is the master interface of the module's downstream AXI connection.
- Master properties normally flow from `s_axi.properties` to
  `m_axi.properties`.
- Slave properties normally flow from `m_axi.properties` to
  `s_axi.properties`.
- Forwarding always remains within the same property family.
- Resolver bodies keep `import axi4.tracking._` local. They qualify catalogs as
  `properties.Master` / `properties.Slave` and values through `values`.
  Component APIs use the underlying domain type, such as
  `axi4.util.MemoryMap`, when it is meaningful outside property resolution.

All read and write rules below apply independently.

## Property value namespace

Property value types live separately from the master and slave key catalogs:

```scala
package chext.amba.axi4.tracking.values
```

This namespace contains:

```scala
BurstShape
ThreadMode
TrafficProfile
MemoryMap
```

`MemoryMap` keeps its existing implementation in `chext.amba.axi4.util`, but
is re-exported from the property-value namespace:

```scala
package chext.amba.axi4.tracking

package object values {
  type MemoryMap = chext.amba.axi4.util.MemoryMap

  val MemoryMap: chext.amba.axi4.util.MemoryMap.type =
    chext.amba.axi4.util.MemoryMap
}
```

The type alias preserves type identity, and the stable companion alias keeps
construction and nested members available through the same import:

```scala
import chext.amba.axi4.tracking.values._

val map: MemoryMap =
  MemoryMap(segments = Seq(MemoryMap.Segment(...)))
```

Code may also import `chext.amba.axi4.util.MemoryMap`. The property namespace
adds no wrapper or separate `AddressMap` type.

Keys and their match/select metadata live directly in:

```scala
import chext.amba.axi4.tracking.{properties => p, values => v}

p.MasterReadBurstShape
p.SlaveMemoryMap
```

## Property selectors

Resolver policies frequently care about the value type of a property rather
than one exact key. `p.Key[T]` carries explicit role, access, and value-type
metadata:

```scala
final case class Key[T](
    role: Role,
    access: Access,
    valueType: ValueType[T],
    val name: String,
    val description: String
)
```

Each classification is also a selector. `Manager.select` realizes and returns
the matching standard property cells:

```scala
val reads: Seq[p.Cell[_]] = interface.properties.select(p.Read)
val shapes: Seq[p.Cell[v.BurstShape]] =
  interface.properties.select(p.BurstShape)
```

`p.Key(role, access, valueType)` extracts the three classifications from a
property cell. A concrete resolver registers its interfaces and uses it with
the transparent `ResolveRequest` case class:

```scala
bindSlave(owner.s_axi)
bindMaster(owner.m_axi)

def resolve[T](request: ResolveRequest[T]): ResolveResult =
  request match {
    case ResolveRequest(_, p.Key(_, _, p.TrafficProfile)) =>
      request.incomplete()

    case ResolveRequest(_, p.Key(p.Slave, _, p.MemoryMap)) =>
      request.forwardTo(owner.m_axi)

    case ResolveRequest(_, p.Key(p.Master, _, p.BurstShape)) =>
      request.forwardTo(owner.s_axi)

    case ResolveRequest(_, p.Key(p.Master, _, p.ThreadMode)) =>
      request.calculate(p.ThreadMode, v.ThreadMode.SingleThread)

    case _ =>
      request.missingCase()
}
```

Default trace metadata is derived from the implementation class name. For
example, `CreditBuffer_Resolver` becomes `CreditBufferResolver` with kind
`credit-buffer`. Explicit `kind` or `resolver` overrides are reserved for names
that do not follow this convention. `request.missingCase()` uses that metadata
to produce the standard catch-all resolution failure.

The request pattern directly exposes the selected endpoint and original
cell:

```scala
case ResolveRequest(
      interface,
      cell @ p.Key(p.Slave, p.NoAccess, p.MemoryMap)
    )
    if interface eq owner.s_axi =>
  inspect(cell)
  request.forwardTo(owner.m_axi)
```

Stable values compose with normal pattern alternatives:

```scala
case ResolveRequest(
      _,
      p.Key(p.Master, _, p.BurstShape | p.ThreadMode)
    ) =>
  request.forwardTo(owner.s_axi)
```

Scala 2 does not refine the request's `T` from a stable value-type pattern.
Typed calculations and transformations after such a pattern therefore pass the
selected tag to `calculate` or `mapFrom`. Use an exact flat key when policy
truly differs between individual keys.

`ResolveRequest[T]` is exactly `ResolveRequest(tracked, cell)`, with a
convenience overload accepting a key. The request-oriented methods are provided
by an implicit `RequestOps[T]` class inherited from `Resolver`:
`calculate`, `incomplete`, `dontCare`, `undefined`, `failure`, `forwardTo`,
`mapFrom`, and `missingCase`. This keeps all resolver control operations in one
API and keeps retry construction internal to dependency operations.

`RequestOps` deliberately lives inside `Resolver` so it captures the enclosing
resolver for calculation ownership, trace metadata, and diagnostics. It does
not extend `AnyVal`: Scala value classes cannot be members of another class,
and this nested operation wrapper necessarily carries the enclosing
`Resolver` reference. Resolver ownership is represented by the public `Owner`
API in `tracking/Defs.scala`. The resolver exposes that exact module/component
owner implicitly to ordinary property assignment, so enforcement provenance
does not need a separate setter or call-site syntax.

## Standard keys

The flat standard catalog contains:

```scala
p.MasterReadBurstShape
p.MasterWriteBurstShape
p.MasterReadThreadMode
p.MasterWriteThreadMode
p.MasterReadTrafficProfile
p.MasterWriteTrafficProfile
p.SlaveMemoryMap
p.SlaveReadBurstShape
p.SlaveWriteBurstShape
p.SlaveReadThreadMode
p.SlaveWriteThreadMode
p.SlaveReadTrafficProfile
p.SlaveWriteTrafficProfile
```

`p.KnownKeys` contains these keys. Role, access, and value type selectors replace
separate family catalogs and marker extractors.

## Immutable value API

Aggregate values are immutable case classes. A caller enforces a complete
value:

```scala
interface.properties(p.MasterReadBurstShape) = shape
```

To adjust an existing value, copy it and enforce the complete replacement:

```scala
val current = interface.properties(p.MasterReadBurstShape).get
interface.properties(p.MasterReadBurstShape) =
  current.copy(
    types = Seq(
      axi4.BurstType.Encoding.FIXED,
      axi4.BurstType.Encoding.INCR
    )
  )
```

Enforcing a replacement for a calculated value changes it to `Enforced` and
clears its resolution trace. Enforcement may likewise replace `Undefined`,
`Incomplete`, or `DontCare`. Final compatibility checking validates the
complete value.

Re-enforcing an equal complete value is idempotent. Re-enforcing a different
complete value is an error; contradictory authoritative declarations must not
be silently ignored.

`MemoryMap` is immutable. Incrementally constructed maps use an explicit
builder or an owner such as `RegisterBlock`, and only the completed immutable
map is published as a property.

## BurstShape

`BurstShape` contains the burst-related values:

```scala
final case class BurstShape private (
    maxBeats: Int,
    types: Seq[Int],
    sizes: Seq[Int],
    aligned: Boolean
)
```

The fields mean:

- `maxBeats`: maximum burst length in beats;
- `types`: AXI burst-type encodings that may be issued or accepted;
- `sizes`: AXI transfer-size encodings that may be issued or accepted;
- `aligned`: whether every transaction is naturally aligned, meaning
  `AxADDR % (1 << AxSIZE) == 0`.

The Boolean is role-dependent. As a master property, `aligned = false` means
the master may issue unaligned transactions. As a slave property,
`aligned = false` means the slave accepts unaligned transactions; it does not
require them to be unaligned. A slave property with `aligned = true` accepts
only naturally aligned transactions.

Burst-type and transfer-size encodings use `Int`. `axi4.BurstType.Encoding`
provides the shared `FIXED`, `INCR`, and `WRAP` integer constants; resolver
code does not convert Chisel literals.

An empty shape has `maxBeats = 0`, empty `types` and `sizes`
sequences, and `aligned = false`. Non-empty shapes must have positive
`maxBeats` and non-empty `types` and `sizes`. Validation also
checks protocol burst limits and transfer sizes against the interface
configuration. Sequences are normalized to sorted, distinct values.

The companion object exposes the protocol-mode maxima:

```text
fullSize(W)             = log2(W / 8)
supportedTypesFor(cfg)  = protocol-supported burst types
supportedSizesFor(cfg)  = protocol- and width-supported transfer sizes
maxBeatsFor(cfg)        = maximum protocol beat count
```

Compatibility is:

```text
master.maxBeats       <= slave.maxBeats
master.types     subsetOf slave.types
master.sizes  subsetOf slave.sizes
!slave.aligned || master.aligned
```

The natural-alignment comparison is vacuously satisfied by an empty master
shape. Compatibility returns every mismatch rather than stopping at the first
one.

## ThreadMode

`ThreadMode` is separate from `BurstShape`:

```scala
sealed trait ThreadMode

object ThreadMode {
  case object SingleTransaction extends ThreadMode
  case object SingleThread extends ThreadMode
  case object UniqueThreads extends ThreadMode
  case object Unconstrained extends ThreadMode
}
```

The values describe simultaneously outstanding transactions:

- `SingleTransaction`: at most one transaction is outstanding. Its ID is not
  otherwise constrained.
- `SingleThread`: multiple transactions may be outstanding, but they all have
  the same ID.
- `UniqueThreads`: simultaneously outstanding transactions have distinct IDs.
- `Unconstrained`: neither sharing nor uniqueness is guaranteed.

AXI4-Lite has one implicit ID, so only `SingleTransaction` and `SingleThread`
are valid there. The checker validates both resolved master and slave modes
before comparing them; `UniqueThreads` or `Unconstrained` on a Lite interface
is an elaboration error whose diagnostic includes the resolution trace.

For reads, a transaction is outstanding from the AR handshake through the
final R handshake. For writes, it is outstanding from the AW handshake through
the B handshake.

The compatibility rules are:

| Master mode | Compatible slave modes |
|---|---|
| `SingleTransaction` | any mode |
| `SingleThread` | `SingleThread`, `Unconstrained` |
| `UniqueThreads` | `UniqueThreads`, `Unconstrained` |
| `Unconstrained` | `Unconstrained` |

`SingleThread` and `UniqueThreads` are incomparable.

## TrafficProfile

`TrafficProfile` groups concurrency and latency:

```scala
final case class TrafficProfile(
    outstandingTransactions: Int = 0,
    threads: Int = 0,
    latencyCycles: Option[Double] = None
)
```

The same value type is used by the read/write master and slave keys. Master
counts describe issued traffic; slave counts describe accepted traffic.

Traffic profiles are placeholders. Resolver branches recognize the value type
and return:

```scala
case ResolveRequest(_, p.Key(_, _, p.TrafficProfile)) =>
  request.incomplete()
```

The compatibility pass does not request `TrafficProfile`. Its count, latency,
and component-aggregation rules remain undefined.

## MemoryMap

`p.SlaveMemoryMap` uses the immutable recursive
`chext.amba.axi4.util.MemoryMap`, publicly re-exported as
`chext.amba.axi4.tracking.values.MemoryMap`.

Maps contain:

- component paths, offsets, and explicit sizes;
- child maps and segments;
- absolute origins;
- typed arguments and optional resolution traces;
- overlap and bounds validation;
- `AlignedPacked`, `AlignedLargest`, and `Tight` aggregation.

Every resolved map is checked to fit within the interface address width.
`ErrorSlave` and components that intentionally provide no successful address
space may use `Undefined`.

An arbitrary address-function demux cannot derive a correct aggregate
`MemoryMap`; it remains `Incomplete` unless the user explicitly supplies one.
Consequently, a design containing such a demux does not pass strict memory-map
checking.

## System-wide checking

AXI tracking registers every Full and Lite `Tracked` interface by object
identity. Registration includes vector elements and DataView-created
interfaces.

After the root module has completed construction and all parent connections
and resolver registrations exist, the checker visits every registered
interface.

For each enabled AXI4-Full read access, the checker:

1. resolves `p.MasterReadBurstShape` and `p.SlaveReadBurstShape`;
2. checks burst validity and compatibility when both resolutions contain
   values and at least one local property is `Enforced`;
3. resolves `p.MasterReadThreadMode` and `p.SlaveReadThreadMode`;
4. checks thread validity and compatibility when both resolutions contain
   values and at least one local property is `Enforced`.

The checker performs the same steps for enabled writes. For AXI4-Lite, it skips
burst shape and checks only the enabled read and write thread modes. It also
resolves and validates `p.SlaveMemoryMap` once per interface.

Resolved values are cached by the existing property state, so later visits to
the same dependency are inexpensive.

`DontCare` suppresses only the local comparison for that property. The checker
still visits all other interfaces, so mux/demux policies place the
comparison at the interfaces where the unaggregated facts remain available.
Calculated-vs-calculated boundaries are also omitted because they only repeat
constraints inferred from authoritative boundaries. There is no
compatibility-check delegation.

`TrafficProfile` is excluded from the compatibility pass. Direct requests may
be forwarded, marked `DontCare`, or resolve to `Incomplete` according to the
component's transaction transformation.

## AXI4-Full resolvers

### Buffer and Connect

Both transparently transport checked facts so component-owned wire interfaces
reach a registered compatibility-check boundary:

- master `BurstShape` and `ThreadMode` follow the request path;
- slave `BurstShape`, `ThreadMode`, and `MemoryMap` follow the response path;
- `TrafficProfile` requests return `Incomplete`.

### CreditBuffer

- Master `BurstShape` and `ThreadMode` flow unchanged from `s_axi` to `m_axi`.
- `p.SlaveMemoryMap` flows unchanged from `m_axi` to `s_axi`.
- With read buffering enabled, slave `ReadBurstShape.maxBeats` is capped by the
  response-buffer capacity; its types and sizes cover every protocol-supported
  value, and `aligned = false` accepts unaligned transactions.
- With write-payload buffering enabled, slave `WriteBurstShape.maxBeats` is
  capped by the payload-buffer capacity. This guarantees that the complete W
  burst is buffered locally before the AW transfer is allowed to proceed,
  without depending on W progress at `m_axi`.
- Without the corresponding buffer, slave `BurstShape` flows unchanged
  from `m_axi` to `s_axi`. Slave `ThreadMode` always follows the same path.
- Slave and master `TrafficProfile` remain `Incomplete`.

### Demux

- Every master `BurstShape` and `ThreadMode` on each `m_axi(i)` is forwarded
  from the same property on `s_axi`.
- No slave `BurstShape` or `ThreadMode` is aggregated on `s_axi`; these
  properties remain `DontCare`. The relevant compatibility checks run on the
  selected `m_axi` interfaces after master-property propagation.
- `p.SlaveMemoryMap` remains `Incomplete` because an arbitrary address decode
  function does not synthesize an aggregate map.
- Every `TrafficProfile` request returns `Incomplete`.

### DemuxMm

- Master `BurstShape` and `ThreadMode` flow from `s_axi` to every `m_axi(i)`
  exactly as for `Demux`.
- Slave `BurstShape` and `ThreadMode` on `s_axi` remain `DontCare`; the slave
  properties of the `m_axi` interfaces remain separate and are checked there.
- `genDecoder()` resolves every `m_axi` `p.SlaveMemoryMap`, aggregates the
  non-error-port maps in routing order, and enforces the result on `s_axi`.
  Resolving it before `genDecoder()` is an error.
- Every `TrafficProfile` request returns `Incomplete`.

### Mux

- Slave `BurstShape`, `ThreadMode`, and `MemoryMap` flow from `m_axi` to every
  `s_axi(i)`.
- With one `s_axi` interface, master `ThreadMode` is forwarded from that
  interface.
- With multiple `s_axi` interfaces, master `ThreadMode` is calculated as
  `Unconstrained`; combining streams can produce repeated IDs within one
  `s_axi` interface and different IDs between the interfaces.
- Master `BurstShape` is `DontCare`. Its compatibility
  is checked independently at every `s_axi` interface after the slave shape
  from `m_axi` is propagated to them.
- Every `TrafficProfile` request returns `Incomplete`.

### IdDemux

`IdDemux` selects `m_axi(i)` with the low `wIdSel` ID bits and removes those
bits from the ID issued at `m_axi`.

- Every master `BurstShape` flows from `s_axi` to every `m_axi` interface.
- Master `ThreadMode` is preserved while the `m_axi` interfaces retain ID bits.
  When selection removes every ID bit, `SingleTransaction` is preserved and
  every other `s_axi` mode becomes `SingleThread`.
- Slave `BurstShape` remains `DontCare`; the slave properties of the `m_axi`
  interfaces remain separate and are checked there.
- Slave `ThreadMode` intersects the slave properties from the `m_axi`
  interfaces and, when selection removes every ID bit, applies the inverse of
  the `m_axi` thread transformation.
- Slave and master `TrafficProfile` remain `Incomplete`.
- `p.SlaveMemoryMap` remains `Incomplete`.

### IdMux

`IdMux` prefixes each ID from an `s_axi` interface with that interface's index.

- Slave `BurstShape`, `ThreadMode`, and `MemoryMap` flow from `m_axi` to every
  `s_axi` interface.
- With one `s_axi` interface, master `ThreadMode` is forwarded from it.
- With multiple `s_axi` interfaces, master `ThreadMode` is conservatively
  `Unconstrained`: transactions within one interface may share an ID while
  transactions from other interfaces receive different prefixed IDs.
- Master `BurstShape` is `DontCare` because master burst shapes from separate
  `s_axi` interfaces are not aggregated.
- Every `TrafficProfile` request returns `Incomplete`.

### IdParallelize

- Enforce slave `ThreadMode = SingleThread` on `s_axi`.
- Limit slave `ReadBurstShape.maxBeats` to the response-buffer capacity (or the
  protocol maximum, whichever is smaller), while accepting every supported
  type and size with `aligned = false`.
- Enforce master `ThreadMode = UniqueThreads` on `m_axi`.
- Master `BurstShape` flows unchanged from `s_axi` to `m_axi`; slave write
  `BurstShape` flows unchanged from `m_axi` to `s_axi`.
- Slave and master `TrafficProfile` remain `Incomplete`.
- `p.SlaveMemoryMap` flows unchanged.

### IdSerialize

- Master `ThreadMode` preserves `SingleTransaction`; all other modes at `s_axi`
  become `SingleThread` at `m_axi`.
- Master `BurstShape` flows unchanged.
- Slave `BurstShape` flows unchanged from `m_axi` to `s_axi`, preserving
  the `m_axi` burst restrictions across ID serialization.
- Slave `ThreadMode` is the inverse of the serialization transform:
  `SingleThread` or `Unconstrained` at `m_axi` permits `Unconstrained` at
  `s_axi`, while `SingleTransaction` or `UniqueThreads` at `m_axi` permits only
  one transaction at `s_axi`.
- Slave and master `TrafficProfile` remain `Incomplete`.
- `p.SlaveMemoryMap` flows unchanged.

### Downscale

Let `S = fullSize(s_axi.wData)` and `M = fullSize(m_axi.wData)`, where `S > M`.
For one transfer-size encoding `x` issued at `s_axi`:

```text
m_axi size  f(x) = min(x, M)
m_axi beats e(x) = 2 ^ max(0, x - M)
m_axi type       = INCR
```

- Enforce `SingleThread` at `s_axi`.
- Publish a one-beat slave shape with every supported type and size and
  `aligned = false`.
- Calculate the master shape with types `{ INCR }`, protocol-maximum
  `maxBeats`, and transfer sizes obtained by applying `f` to the `s_axi` sizes.
- Preserve the `s_axi` natural-alignment guarantee at `m_axi`.
- Forward the master thread mode from `s_axi` unchanged.
- `p.SlaveMemoryMap` flows unchanged.
- Slave and master `TrafficProfile` remain `Incomplete`.

### Unburst

- Enforce `SingleThread` on both sides.
- Calculate a one-beat `{ INCR }` burst shape on `m_axi`.
- Master sizes flow from `s_axi`.
- Preserve the `s_axi` natural-alignment guarantee.
- Derive slave `BurstShape` from the one-beat `{ INCR }` property at `m_axi`:
  filter sizes accepted at `m_axi`, retain every burst type supported
  at `s_axi`, and propagate the `m_axi` alignment requirement.
- Slave and master `TrafficProfile` remain `Incomplete`.
- `p.SlaveMemoryMap` flows unchanged.

### Upscale

Let `S = fullSize(s_axi.wData)` and `M = fullSize(m_axi.wData)`, where `S < M`.

- Enforce `SingleThread` at `s_axi`.
- Forward master `BurstShape` and `ThreadMode` unchanged, preserving stronger
  modes such as `SingleTransaction`.
- Derive slave `BurstShape` from the property at `m_axi`, filtering types and
  sizes to the narrower `s_axi` interface.
- Slave and master `TrafficProfile` remain `Incomplete`.
- `p.SlaveMemoryMap` flows unchanged.

### Widen

Let `F = fullSize(axiCfg.wData)`.

- Publish a protocol-maximum `s_axi` burst shape with all supported sizes and all
  types except `FIXED`.
- Slave and master `ThreadMode` flow unchanged.
- Slave and master `TrafficProfile` remain `Incomplete`.
- Calculate the `m_axi` shape with transfer sizes `{ F }` and the `s_axi`
  `types` minus `FIXED`.
- Calculate `m_axi` beats for the worst-case starting offset.
- Set `m_axi.aligned = false`: natural alignment to a narrow `s_axi` size does
  not imply natural alignment to the widened full-width size.
- `p.SlaveMemoryMap` flows unchanged.

### LiteConverter

The Full `s_axi` interface is ID-free and accepts naturally aligned, full-width
transfers.

- Enforce `SingleThread` on both sides.
- Enforce the authoritative Full `s_axi` burst shape, including natural
  full-width alignment; the Lite `m_axil` burst properties remain `Undefined`.
- Publish the protocol-implied one-beat shape on the internal Full interface at
  the manual Full-to-Lite bridge.
- Forward the bridge's slave thread and traffic properties from the Lite
  `m_axil` interface. The external slave traffic profile remains `Incomplete`.
- Forward `p.SlaveMemoryMap` from Lite to Full.
- Master `TrafficProfile` remains `Incomplete`.

### ProtocolConverter

The externally visible values are the composition of the selected internal
stages:

```text
IdDemux
optional IdSerialize
optional Upscale
optional first Unburst
optional Downscale
optional second Unburst
IdMux
```

Master burst/thread properties compose from external `s_axi` to external
`m_axi`, and slave properties resolve from external `m_axi` to external
`s_axi` through the same stages. An internal `IdDemux` leaves the
external slave burst aggregate `DontCare` while preserving checks at its
`m_axi` interfaces; slave thread mode still uses its represented aggregate and
inverse ID-removal rule. `p.SlaveMemoryMap` flows directly from external
`m_axi` to external `s_axi`. A passthrough converter also forwards the master
traffic profile from `s_axi` to `m_axi` and the slave traffic profile from
`m_axi` to `s_axi`; a transforming converter leaves them `Incomplete`.

### ConstantSlave

For each enabled read or write access:

- enforce `ThreadMode = SingleTransaction`;
- on Full AXI, enforce protocol-maximum `maxBeats`, all legal burst types, every
  valid transfer size, and `aligned = false`; Lite burst shapes remain
  `Undefined`.

Successful constant slaves publish a whole-address-space `MemoryMap`.
`ErrorSlave` leaves it `Undefined`.
Every `TrafficProfile` request returns `Incomplete`.

### StallSlave

For each enabled read or write access:

- on Full AXI, enforce an empty slave `BurstShape` and
  `ThreadMode = Unconstrained`;
- on AXI4-Lite, leave burst shape `Undefined` and enforce
  `ThreadMode = SingleThread`.

`MemoryMap` is `Undefined`.
Every `TrafficProfile` request returns `Incomplete`.

### IdleMaster

For each enabled read or write access:

- on Full AXI, enforce an empty master `BurstShape`; Lite burst shapes remain
  `Undefined`;
- enforce `ThreadMode = SingleTransaction`.

The master issues no transactions.
Every `TrafficProfile` request returns `Incomplete`.

## Test coverage

Unit tests cover:

- value-type extractor matches and mismatches;
- direct field mutation and property lifecycle transitions;
- all burst compatibility dimensions;
- the complete 4-by-4 thread compatibility table;
- mux and ID-mux burst `DontCare` plus the existing `ThreadMode` rules;
- demux master propagation plus slave-property `DontCare`, and ID-demux thread
  aggregation;
- one-to-one resolver propagation, inverse transformations, and the absence of
  enabled `DontCare` results;
- every registered interface being visited;
- missing, incomplete, undefined, and incompatible property diagnostics;
- resolution traces and enforcement-owner context for both sides of a failed
  compatibility check;
- module and component owners, lazy module-instantiation `SourceInfo`, and the
  early-lookup failure contract.
