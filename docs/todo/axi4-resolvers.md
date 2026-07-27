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
eagerly. For checked properties, `Unresolved` and `Incomplete` are errors.
`DontCare` means that no compatibility check is needed at that particular
interface because the component's resolver policy preserves the check at other
interfaces. Since the checker visits every interface, no explicit delegation
graph is needed.

`TrafficProfile` is not checked. Resolvers terminate traffic-profile requests
with `incomplete()` until its compatibility rules are defined.

## Conventions

For a module with `s_axi` and `m_axi` ports:

- `s_axi` is the upstream-facing slave port.
- `m_axi` is the downstream-facing master port.
- Master properties normally flow from `s_axi.masterProps` to
  `m_axi.masterProps`.
- Slave properties normally flow from `m_axi.slaveProps` to
  `s_axi.slaveProps`.
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

Keys live in:

```scala
chext.amba.axi4.tracking.properties.Master
chext.amba.axi4.tracking.properties.Slave
```

## Value-type extractors

Resolver policies frequently care about the value type of a property rather
than a separate property-kind marker trait. `PropertyKey[T]` therefore retains
a runtime value-type token using `ClassTag[T]`:

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

Default trace metadata is derived from the implementation class name. For
example, `CreditBuffer_Resolver` becomes `CreditBufferResolver` with kind
`credit-buffer`. Explicit `kind` or `resolver` overrides are reserved for names
that do not follow this convention. `missingCase(request)` uses that metadata
to produce the standard catch-all resolution failure.

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
  request.calculate(values.ThreadMode.SingleThread, this)
  ResolveResult.Success()
```

Use an exact singleton key only when policy truly differs between individual
keys rather than between value families.

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
interface.masterProps(Master.ReadBurstShape).tpe = Seq(
  axi4.BurstType.Encoding.FIXED,
  axi4.BurstType.Encoding.INCR
)
```

Type-specific operations on `Property[BurstShape]` and
`Property[TrafficProfile]` provide this syntax. They perform a controlled
property mutation rather than exposing an untracked mutable value:

- the first field update creates the type's empty/default value and changes
  the property to `Enforced`;
- later field updates copy, modify, and replace the enforced value;
- updating a calculated value changes it to `Enforced` and clears its
  resolution trace;
- updating `Undefined`, `Incomplete`, or `DontCare` replaces that terminal
  state with an enforced value;
- final compatibility checking validates that the resulting value is
  internally consistent.

The mutable fields of the value itself are not public. Mutation goes through
the typed `Property` operations so lifecycle state and traces remain correct.
The type-specific operations live in the value companion objects, putting them
in implicit scope for `Property[BurstShape]` and
`Property[TrafficProfile]`.

Re-enforcing an equal complete value is idempotent. Re-enforcing a different
complete value is an error; contradictory authoritative declarations must not
be silently ignored.

`MemoryMap` is immutable. Incrementally constructed maps use an explicit
builder or an owner such as `RegisterBlock`, and only the completed immutable
map is published as a property.

## BurstShape

`BurstShape` contains the burst-related values:

```scala
final class BurstShape private[tracking] (
    private var len_ : Int,
    private var tpe_ : Seq[Int],
    private var size_ : Seq[Int],
    private var align_ : Int
) {
  def len: Int
  def tpe: Seq[Int]
  def size: Seq[Int]
  def align: Int
}
```

The fields mean:

- `len`: maximum burst length in beats;
- `tpe`: AXI burst-type encodings that may be generated or accepted;
- `size`: AXI transfer-size encodings that may be generated or accepted;
- `align`: base-2 logarithm of the minimum byte alignment of every
  transaction's starting address. For example, `3` means 8-byte alignment.
  A master guarantees this alignment; a slave requires it.

Burst-type, transfer-size, and alignment encodings use `Int`.
`axi4.BurstType.Encoding` provides the shared `FIXED`, `INCR`, and `WRAP`
integer constants; resolver code does not convert Chisel literals.

An empty shape has `len = 0`, empty `tpe` and `size` sequences, and
`align = 0`. Non-empty shapes must have positive `len`, non-empty `tpe` and
`size`, and `0 <= align <= wAddr`. Validation also checks protocol burst limits
and transfer sizes against the interface configuration. Sequences are
normalized to sorted, distinct values.

For a data width `W` in bits:

```text
fullSize(W)   = log2(W / 8)
validSizes(W) = { 0, ..., fullSize(W) }
```

Compatibility is:

```text
master.len   <= slave.len
master.tpe   subsetOf slave.tpe
master.size  subsetOf slave.size
master.align >= slave.align
```

The alignment comparison is vacuously satisfied by an empty master shape.
Compatibility returns every mismatch rather than stopping at the first one.

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

Traffic profiles are placeholders. Resolver branches recognize the value type
and return:

```scala
case Request(TrafficProfile()) =>
  request.incomplete()
```

The compatibility pass does not request `TrafficProfile`. Its count, latency,
and component-aggregation rules remain undefined.

## MemoryMap

`Slave.MemoryMap` uses the immutable recursive
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

For each enabled AXI4-Full read direction, the checker:

1. resolves `Master.ReadBurstShape` and `Slave.ReadBurstShape`;
2. checks burst compatibility when both resolutions contain values;
3. resolves `Master.ReadThreadMode` and `Slave.ReadThreadMode`;
4. checks thread compatibility when both resolutions contain values.

The checker performs the same steps for enabled writes. For AXI4-Lite, it skips
burst shape and checks only the enabled read and write thread modes. It also
resolves and validates `Slave.MemoryMap` once per interface.

Resolved values are cached by the existing property state, so later visits to
the same dependency are inexpensive.

`DontCare` suppresses only the local comparison for that property. The checker
still visits all other interfaces, so mux/demux policies place the
comparison at the interfaces where the unaggregated facts remain available.
There is no compatibility-check delegation.

`TrafficProfile` is excluded; direct traffic-profile requests resolve to
`Incomplete`.

## AXI4-Full resolvers

### Buffer and Connect

For checked properties, both are transparent:

- master `BurstShape` and `ThreadMode` flow upstream to downstream;
- slave `BurstShape`, `ThreadMode`, and `MemoryMap` flow downstream to
  upstream;
- `TrafficProfile` requests return `Incomplete`.

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
- With multiple inputs, master `ThreadMode` is calculated as `Unconstrained`;
  combining streams can produce repeated IDs within one input and different
  IDs between inputs.
- Master `BurstShape` is `DontCare`. Its compatibility
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
- Master `BurstShape` is `DontCare` because separate
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
- Calculate the master shape with type `{ INCR }`, sizes
  `inputSizes.map(f)`, and beats `max(inputSizes.map(e))`.
- Calculate the accepted input shape with one beat and the sizes supported by
  downstream acceptance of `f(x)` and `e(x)`.
- Preserve the input alignment guarantee forward and the downstream alignment
  requirement backward because the transaction's starting address is
  unchanged.
- `Slave.MemoryMap` flows unchanged.
- Every `TrafficProfile` request returns `Incomplete`.

### Unburst

- Enforce `SingleThread` on both sides.
- Calculate a one-beat `{ INCR }` burst shape on `m_axi`.
- Master sizes flow from `s_axi`.
- For a multi-beat `INCR` or `WRAP` input, the output alignment guarantee is
  the lesser of the input start alignment and the smallest transfer size.
  `FIXED` and one-beat inputs preserve the input alignment.
- Accepted input shapes are calculated from downstream acceptance of one-beat
  INCR transfers. Multi-beat input sizes smaller than the downstream alignment
  requirement are excluded.
- `Slave.MemoryMap` flows unchanged.
- Every `TrafficProfile` request returns `Incomplete`.

### Upscale

Let `S = fullSize(s_axi.wData)` and `M = fullSize(m_axi.wData)`, where `S < M`.

- Enforce `SingleThread` on both sides.
- Master `len`, `tpe`, and `size` flow unchanged.
- Slave `len` and `tpe` flow unchanged.
- Intersect accepted sizes with `validSizes(s_axi.wData)`.
- Preserve alignment forward and backward because start addresses are
  unchanged.
- `Slave.MemoryMap` flows unchanged.
- Every `TrafficProfile` request returns `Incomplete`.

### Widen

Let `F = fullSize(axiCfg.wData)`.

- Master and slave `ThreadMode` flow unchanged.
- Calculate the `m_axi` shape with size `{ F }` and the input `tpe` minus
  `FIXED`.
- Calculate output beats with the worst-case alignment function.
- Calculate accepted input types, sizes, and beats from downstream acceptance.
- Preserve alignment forward and backward because start addresses are
  unchanged.
- `Slave.MemoryMap` flows unchanged.
- Every `TrafficProfile` request returns `Incomplete`.

### LiteConverter

The Full-side input is ID-free and accepts naturally aligned, full-width
transfers.

- Enforce `SingleThread` on both sides.
- Enforce the authoritative Full input burst shape, including natural
  full-width alignment; the Lite output burst properties remain `Undefined`.
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
`Incomplete`. `TrafficProfile` is the deliberate exception.

### ConstantSlave

For each enabled direction:

- enforce `ThreadMode = SingleTransaction`;
- on Full AXI, enforce protocol-maximum `len`, all legal burst types, every
  valid transfer size, and byte alignment; Lite burst shapes remain `Undefined`.

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

## Test coverage

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
