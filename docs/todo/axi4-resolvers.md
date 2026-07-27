# AXI4 Property Resolution and Compatibility

This document specifies the intended AXI4 property model, resolver flow, and
system-wide compatibility checks. The broader tracking and resolution model is
described in [AXI4 Tracking Design](axi4-tracking.md).

## Goals

The standard AXI4 property values are deliberately coarse:

- the existing `MemoryMap`;
- `BurstShape`;
- `ThreadMode`; and
- `TrafficProfile`.

Read and write properties remain separate. Master properties describe traffic
that may be generated, while slave properties describe traffic that may be
accepted.

At the end of elaboration, tracking visits every registered AXI interface,
resolves every applicable property selected for checking, and checks the
master value against the slave value when both sides provide values.
Compatibility checking does not use a separate delegation graph.

A property belonging to a direction disabled by `axi4.Config` is `Undefined`.
`BurstShape` is also `Undefined` for every AXI4-Lite interface because Lite has
no burst-shape signals; `bindMaster(...)` and `bindSlave(...)` apply both rules
eagerly. For the initially checked properties, `Unresolved` and `Incomplete`
are errors.
`DontCare` means that no compatibility check is needed at that particular
interface because the component's resolver policy preserves the check at other
interfaces. Since the checker visits every interface, no explicit delegation
graph is needed.

`TrafficProfile` is not part of the initial compatibility pass. Until its
semantics are implemented, resolvers terminate traffic-profile requests with
`incomplete()`.

## Conventions

For a module with `s_axi` and `m_axi` ports:

- `s_axi` is the upstream-facing slave port.
- `m_axi` is the downstream-facing master port.
- Master properties normally flow from `s_axi.masterProps` to
  `m_axi.masterProps`.
- Slave properties normally flow from `m_axi.slaveProps` to
  `s_axi.slaveProps`.
- Forwarding always remains within the same property family.
- Production AXI component files keep imports from `axi4.tracking`, including
  `properties` and `values`, inside their resolver classes. Component APIs use
  the underlying domain type, such as `axi4.util.MemoryMap`, when that type is
  independently meaningful outside property resolution.
- Resolver bodies use only `import axi4.tracking._` for tracking symbols.
  Property catalogs are qualified through `properties.Master` and
  `properties.Slave`; property values are qualified through
  `values.MemoryMap`, `values.BurstShape`, `values.ThreadMode`, and
  `values.TrafficProfile`.

All read and write rules below apply independently.

## Property value namespace

New property value types live separately from the master and slave key
catalogs:

```scala
package chext.amba.axi4.tracking.values
```

This namespace initially contains:

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

Existing code may continue to import `chext.amba.axi4.util.MemoryMap`. No
wrapper and no separate `AddressMap` type are introduced.

The keys remain in:

```scala
chext.amba.axi4.tracking.properties.Master
chext.amba.axi4.tracking.properties.Slave
```

## Value-type extractors

Resolver policies frequently care about the value type of a property rather
than a separate property-kind marker trait. `PropertyKey[T]` therefore retains
a runtime value-type token, initially implemented with `ClassTag[T]`:

```scala
abstract class PropertyKey[T: ClassTag](
    val name: String,
    val description: String
) {
  final val valueClass: Class[_] =
    implicitly[ClassTag[T]].runtimeClass
}
```

`PropertyValueType[T]` is a reusable extractor:

```scala
final class PropertyValueType[T: ClassTag] {
  private val expected = implicitly[ClassTag[T]].runtimeClass

  def accepts(key: PropertyKey[_]): Boolean =
    key.valueClass == expected

  def unapply(key: PropertyKey[T]): Boolean =
    accepts(key)
}

object PropertyValueType {
  def apply[T: ClassTag]: PropertyValueType[T] =
    new PropertyValueType[T]
}
```

`Resolver` provides stable `MemoryMap`, `BurstShape`, `ThreadMode`, and
`TrafficProfile` value-family extractors together with `Request`,
`MasterRequests`, and `SlaveRequests`. A concrete resolver registers its
interfaces and uses the inherited extractors in ordinary match statements:

```scala
bindSlave(owner.s_axi)
bindMaster(owner.m_axi)

def resolve[T](request: ResolveRequest[T]): ResolveResult =
  request match {
    case Request(TrafficProfile()) =>
      request.incomplete()

    case SlaveRequests(MemoryMap()) =>
      forwardTo(request, owner.m_axi)

    case MasterRequests(BurstShape()) =>
      forwardTo(request, owner.s_axi)

    case MasterRequests(ThreadMode()) =>
      request.incomplete()

    case _ =>
      missingCase(request)
}
```

The default trace metadata is also derived from the implementation class name:
`CreditBuffer_Resolver` becomes resolver `CreditBufferResolver` with kind
`credit-buffer`. Explicit `kind` or `resolver` overrides are reserved for names
that intentionally do not follow this convention. `missingCase(request)` uses
that metadata to produce the standard catch-all resolution failure.

For a policy that needs the selected endpoint, append `.withInterface`:

```scala
case SlaveRequests.withInterface(interface, MemoryMap())
    if interface eq owner.s_axi =>
  forwardTo(request, owner.m_axi)
```

The typed `unapply` signature preserves the request value type, so the same
family pattern works for forwarding, transformations, terminal states, and
calculation:

```scala
case MasterRequests(ThreadMode()) =>
  request.calculate(values.ThreadMode.Unconstrained, this)
  ResolveResult.Success()
```

Use an exact singleton key only when policy truly differs between individual
keys rather than between value families.

No `AddressMapProperty`, `BurstShapeProperty`, `ThreadModeProperty`, or
`TrafficProperty` marker traits, and no corresponding four-way specialized key
bases, are needed.

## Standard keys

The master catalog contains:

```scala
Master.ReadBurstShape
Master.WriteBurstShape
Master.ReadThreadMode
Master.WriteThreadMode
Master.ReadTrafficProfile
Master.WriteTrafficProfile
```

The slave catalog contains:

```scala
Slave.MemoryMap
Slave.ReadBurstShape
Slave.WriteBurstShape
Slave.ReadThreadMode
Slave.WriteThreadMode
Slave.ReadTrafficProfile
Slave.WriteTrafficProfile
```

`Master.readProperties`, `Master.writeProperties`, `Slave.readProperties`, and
`Slave.writeProperties` are derived from their `all` catalogs using the
existing `ReadProperty` and `WriteProperty` extractors. This keeps disabled
direction handling synchronized automatically.

## Mutable value API

Aggregate values support field-oriented construction and adjustment. A caller
may enforce a complete value:

```scala
interface.masterProps(Master.ReadBurstShape) = shape
```

It may also update an individual field directly:

```scala
interface.masterProps(Master.ReadBurstShape).burstTypes = Set(
  axi4.BurstType.FIXED.litValue.toInt,
  axi4.BurstType.INCR.litValue.toInt
)
```

Type-specific operations on `Property[BurstShape]` and
`Property[TrafficProfile]` provide this syntax. They perform a controlled
property mutation rather than exposing an untracked mutable value:

- the first field update creates the type's empty/default value and changes
  the property to `Enforced`;
- later field updates mutate the enforced value;
- updating a calculated value changes it to `Enforced` and clears its
  resolution trace;
- updating `Undefined`, `Incomplete`, or `DontCare` replaces that terminal
  state with an enforced value;
- final compatibility checking validates that the resulting value is
  internally consistent.

The mutable fields of the value itself are not public. Mutation goes through
the typed `Property` operations so lifecycle state and traces remain correct.
The type-specific operations can live in the value companion objects, putting
them in implicit scope for `Property[BurstShape]` and
`Property[TrafficProfile]`.

Re-enforcing an equal complete value is idempotent. Re-enforcing a different
complete value is an error; contradictory authoritative declarations must not
be silently ignored.

`MemoryMap` remains immutable. Incrementally constructed maps use an explicit
builder or an owner such as `RegisterBlock`, and only the completed immutable
map is published as a property.

## BurstShape

`BurstShape` contains the burst-related values:

```scala
final class BurstShape private[tracking] (
    private var burstBeats_ : Int,
    private var burstNarrow_ : Boolean,
    private var burstTypes_ : Set[Int],
    private var burstSizes_ : Set[Int]
) {
  def burstBeats: Int
  def burstNarrow: Boolean
  def burstTypes: Set[Int]
  def burstSizes: Set[Int]
}
```

The fields mean:

- `burstBeats`: maximum burst length in beats;
- `burstNarrow`: whether narrow bursts may be generated or accepted;
- `burstTypes`: AXI burst type encodings that may be generated or accepted;
- `burstSizes`: AXI transfer-size encodings that may be generated or accepted.

Initially, encodings remain `Int` to avoid coupling this property overhaul to a
separate AXI enumeration refactor.

An empty shape has zero beats, `burstNarrow = false`, and empty type and size
sets. Non-empty shapes must have positive `burstBeats` and non-empty type and
size sets. Validation also checks protocol burst limits and transfer sizes
against the interface configuration.

For a data width `W` in bits:

```text
fullSize(W)   = log2(W / 8)
validSizes(W) = { 0, ..., fullSize(W) }
```

`burstNarrow` must agree with `burstSizes` relative to the interface:

```text
burstNarrow == burstSizes.exists(_ < fullSize(W))
```

Compatibility is:

```text
master.burstBeats  <= slave.burstBeats
master.burstNarrow implies slave.burstNarrow
master.burstTypes  subsetOf slave.burstTypes
master.burstSizes  subsetOf slave.burstSizes
```

Compatibility returns every mismatch rather than stopping at the first one.

## ThreadMode

`ThreadMode` remains separate from `BurstShape`:

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

`TrafficProfile` keeps concurrency and latency together for now:

```scala
final class TrafficProfile private[tracking] (
    private var outstandingTransactions_ : Int,
    private var threads_ : Int,
    private var latencyCycles_ : Option[Double]
) {
  def outstandingTransactions: Int
  def threads: Int
  def latencyCycles: Option[Double]
}
```

The same value type is used by the read/write master and slave keys. Master
counts describe traffic that may be generated; slave counts describe traffic
that may be accepted.

The latency field is simply named `latencyCycles`: being part of
`TrafficProfile` already makes it clear that it is an expected traffic
characteristic.

For the first implementation, traffic profiles are placeholders. Resolver
branches recognize the value type and return:

```scala
case Request(TrafficProfile()) =>
  request.incomplete()
```

The exhaustive compatibility pass does not request or check `TrafficProfile`
yet. Its count compatibility, latency meaning, and component aggregation rules
will be defined together when traffic checking is implemented.

## MemoryMap

`Slave.MemoryMap` continues to use the existing immutable recursive
`chext.amba.axi4.util.MemoryMap`, publicly re-exported as
`chext.amba.axi4.tracking.values.MemoryMap`.

Maps retain:

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

For each enabled AXI4-Full read direction it:

1. resolves `Master.ReadBurstShape` and `Slave.ReadBurstShape`;
2. checks burst compatibility when both resolutions contain values;
3. resolves `Master.ReadThreadMode` and `Slave.ReadThreadMode`;
4. checks thread compatibility when both resolutions contain values.

For AXI4-Lite it skips the burst-shape steps and checks only the enabled
read/write thread modes. It performs the corresponding operations for writes,
and resolves and validates `Slave.MemoryMap` once for every interface.

Resolved values are cached by the existing property state, so later visits to
the same dependency are inexpensive.

`DontCare` suppresses only the local comparison for that property. The checker
still visits all other interfaces, so the existing mux/demux policies place the
comparison at the interfaces where the unaggregated facts remain available.
There is no compatibility-check delegation.

`TrafficProfile` is excluded from this initial pass; direct traffic requests
resolve to `Incomplete`.

## AXI4-Full resolvers

### Buffer and Connect

For the initially checked properties, both are transparent:

- master `BurstShape` and `ThreadMode` flow upstream to downstream;
- slave `BurstShape`, `ThreadMode`, and `MemoryMap` flow downstream to
  upstream;
- `TrafficProfile` requests return `Incomplete` for now.

### CreditBuffer

- Master and slave `BurstShape` values flow unchanged.
- Master and slave `ThreadMode` values flow unchanged.
- `Slave.MemoryMap` flows unchanged.
- Every `TrafficProfile` request returns `Incomplete`.

### Demux

- Every master `BurstShape` and `ThreadMode` on each `m_axi(i)` is forwarded
  from the same property on `s_axi`.
- No slave `BurstShape` or `ThreadMode` is aggregated on `s_axi`; these
  properties remain `DontCare`. The relevant compatibility checks run on the
  selected downstream paths after master-property propagation.
- `Slave.MemoryMap` remains `Incomplete` because an arbitrary address decode
  function does not synthesize an aggregate map.
- Every `TrafficProfile` request returns `Incomplete`.

### DemuxMm

- Master `BurstShape` and `ThreadMode` flow from `s_axi` to every `m_axi(i)`
  exactly as for `Demux`.
- Slave `BurstShape` and `ThreadMode` on `s_axi` remain `DontCare`; downstream
  capabilities remain separate and are checked at the outputs.
- `genDecoder()` resolves every downstream `Slave.MemoryMap`, aggregates the
  non-error-port maps in routing order, and enforces the result on `s_axi`.
  Resolving it before `genDecoder()` is an error.
- Every `TrafficProfile` request returns `Incomplete`.

### Mux

- Slave `BurstShape`, `ThreadMode`, and `MemoryMap` flow from `m_axi` to every
  `s_axi(i)`.
- With one input, master `ThreadMode` is forwarded from that input.
- With multiple inputs, master `ThreadMode` is enforced as `Unconstrained`;
  combining streams can produce repeated IDs within one input and different
  IDs between inputs.
- Master `BurstShape` retains the existing `DontCare` result. Its compatibility
  is checked independently at every input after the downstream slave shape is
  propagated backward.
- Every `TrafficProfile` request returns `Incomplete`.

### IdDemux

`IdDemux` selects `m_axi(i)` with the low `wIdSel` ID bits and removes those
bits from the downstream ID.

- Every master `BurstShape` flows from `s_axi` to every output.
- Master `ThreadMode` is preserved on each output because selector-bit removal
  is injective for a fixed output.
- All slave `BurstShape`, `ThreadMode`, and `MemoryMap` properties remain
  `DontCare`; downstream capabilities and maps stay separate rather than being
  aggregated.
- Every `TrafficProfile` request returns `Incomplete`.

### IdMux

`IdMux` prefixes each input ID with its input-port index.

- Slave `BurstShape`, `ThreadMode`, and `MemoryMap` flow from `m_axi` to every
  input.
- With one input, master `ThreadMode` is forwarded from that input.
- With multiple inputs, master `ThreadMode` is conservatively `Unconstrained`:
  transactions within one input may share an ID while transactions from other
  inputs receive different prefixed IDs.
- Master `BurstShape` retains the existing `DontCare` result because separate
  upstream burst shapes are not aggregated.
- Every `TrafficProfile` request returns `Incomplete`.

### IdParallelize

- Enforce slave `ThreadMode = SingleThread` on `s_axi`.
- Enforce master `ThreadMode = UniqueThreads` on `m_axi`.
- Master and slave `BurstShape` flow unchanged.
- `Slave.MemoryMap` flows unchanged.
- Every `TrafficProfile` request returns `Incomplete`.

### IdSerialize

- Enforce slave `ThreadMode = Unconstrained` on `s_axi`.
- Enforce master `ThreadMode = SingleThread` on `m_axi`.
- Master and slave `BurstShape` flow unchanged.
- `Slave.MemoryMap` flows unchanged.
- Every `TrafficProfile` request returns `Incomplete`.

### Downscale

Let `S = fullSize(s_axi.wData)` and `M = fullSize(m_axi.wData)`, where `S > M`.
For one accepted input size `x`:

```text
output size  f(x) = min(x, M)
output beats e(x) = 2 ^ max(0, x - M)
output type       = INCR
```

- Enforce `SingleThread` on both sides.
- Enforce slave `BurstShape.burstBeats = 1` on `s_axi`.
- Enforce master burst types `{ INCR }` on `m_axi`.
- Calculate master sizes as `inputSizes.map(f)` and master beats as
  `max(inputSizes.map(e))`.
- Calculate accepted input sizes from downstream acceptance of `f(x)` and
  `e(x)`.
- Calculate both narrow flags from the resulting size sets.
- `Slave.MemoryMap` flows unchanged.
- Every `TrafficProfile` request returns `Incomplete`.

### Unburst

- Enforce `SingleThread` on both sides.
- On `m_axi`, enforce a one-beat `{ INCR }` burst shape.
- Master sizes flow from `s_axi`; narrow support is unchanged.
- Accepted input shapes are calculated from downstream acceptance of one-beat
  INCR transfers.
- `Slave.MemoryMap` flows unchanged.
- Every `TrafficProfile` request returns `Incomplete`.

### Upscale

Let `S = fullSize(s_axi.wData)` and `M = fullSize(m_axi.wData)`, where `S < M`.

- Enforce `SingleThread` on both sides.
- Master beats, types, and sizes flow unchanged.
- Recalculate master narrow support relative to `M`.
- Slave beats and types flow unchanged.
- Intersect accepted sizes with `validSizes(s_axi.wData)` and recalculate narrow
  support relative to `S`.
- `Slave.MemoryMap` flows unchanged.
- Every `TrafficProfile` request returns `Incomplete`.

### Widen

Let `F = fullSize(axiCfg.wData)`.

- Master and slave `ThreadMode` flow unchanged.
- On `m_axi`, enforce sizes `{ F }` and `burstNarrow = false`.
- Master burst types flow subject to removal of `FIXED`.
- Calculate output beats using the existing worst-case alignment function.
- Calculate accepted input types, sizes, and beats from downstream acceptance.
- `Slave.MemoryMap` flows unchanged.
- Every `TrafficProfile` request returns `Incomplete`.

### LiteConverter

The Full-side input is ID-free and accepts naturally aligned, full-width
transfers.

- Enforce `SingleThread` on both sides.
- Enforce the authoritative Full input burst shape; the Lite output burst
  properties remain `Undefined`.
- Publish the protocol-implied one-beat shape on the internal Full interface at
  the manual Full-to-Lite bridge.
- Forward `Slave.MemoryMap` from Lite to Full.
- Compose any non-enforced aggregate property through the internal stages.
- Every `TrafficProfile` request returns `Incomplete`.

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

Master properties compose in that order and slave properties in reverse.
`Slave.MemoryMap` ultimately flows from external `m_axi` to external `s_axi`.
The outer resolver must not replace a resolvable stage value with a blanket
`Incomplete`, except that `TrafficProfile` is deliberately `Incomplete` in the
initial implementation.

### ConstantSlave

For each enabled direction:

- enforce `ThreadMode = SingleTransaction`;
- on Full AXI, enforce protocol-maximum beats, narrow support, all legal burst
  types, and every valid transfer size; Lite burst shapes remain `Undefined`.

Successful constant slaves publish a whole-address-space `MemoryMap`.
`ErrorSlave` leaves it `Undefined`.
Every `TrafficProfile` request returns `Incomplete`.

### StallSlave

For each enabled direction:

- on Full AXI, enforce an empty slave `BurstShape` and
  `ThreadMode = Unconstrained`;
- on AXI4-Lite, leave burst shape `Undefined` and enforce
  `ThreadMode = SingleThread`.

`MemoryMap` is `Undefined`.
Every `TrafficProfile` request returns `Incomplete`.

### IdleMaster

For each enabled direction:

- on Full AXI, enforce an empty master `BurstShape`; Lite burst shapes remain
  `Undefined`;
- enforce `ThreadMode = SingleTransaction`.

There is no upstream source.
Every `TrafficProfile` request returns `Incomplete`.

## Implementation plan

Breaking changes are acceptable; no compatibility layer for the scalar keys is
required.

1. Add the value namespace; re-export the existing `MemoryMap`; and implement
   `BurstShape`, `ThreadMode`, and `TrafficProfile`, including validation and
   typed property field operations.
2. Add the runtime value type token and `PropertyValueType[T]` extractor to
   `PropertyKey`.
3. Replace scalar burst, outstanding, and thread-count keys with the aggregate
   standard keys in one change.
4. Change conflicting re-enforcement to an error.
5. Migrate transparent connections and buffers.
6. Migrate terminals and authoritative endpoints.
7. Migrate mux/demux and ID mux/demux while preserving their existing
   propagation and `DontCare` policies.
8. Migrate width, burst, and ID transformers.
9. Migrate `LiteConverter` and `ProtocolConverter`.
10. Register all Full and Lite interfaces and add the root-level exhaustive
    resolver/checker pass.
11. Enable strict burst-shape, thread-mode, and memory-map checking.
12. Leave all traffic profiles `Incomplete` and defer traffic compatibility
    until its semantics and component equations are defined.

Unit tests cover:

- value-type extractor matches and mismatches;
- direct field mutation and property lifecycle transitions;
- all burst compatibility dimensions;
- the complete 4-by-4 thread compatibility table;
- mux and ID-mux burst `DontCare` plus the existing `ThreadMode` rules;
- demux and ID-demux master propagation plus slave-side `DontCare`;
- every registered interface being visited;
- missing, incomplete, undefined, and incompatible property diagnostics;
- resolution traces for both sides of a failed compatibility check.
