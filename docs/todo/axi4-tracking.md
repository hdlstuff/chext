# AXI4 Tracking and Resolution

> **Status:** Implemented for burst shape, thread mode, and slave memory maps.
> `TrafficProfile` remains a placeholder and is not checked.

## Purpose

`chext.amba.axi4.tracking` detects incompatible AXI compositions during
elaboration. It models only the facts needed for compatibility checks:

- what traffic a master may issue;
- what traffic a slave may accept; and
- which address space a slave provides.

Resolvers propagate or transform those facts. They do not perform compatibility
checks or invent aggregate values merely to make a property complete.

For example, a demux forwards the master properties from `s_axi` to every
`m_axi` interface. It keeps the slave properties of those `m_axi` interfaces
separate instead of intersecting them into one synthetic property on `s_axi`.
The checker then compares each `m_axi` interface independently.

## Lifecycle

Tracking follows four steps:

1. Full and Lite interfaces are registered with their owning module.
2. Component resolvers bind the master or slave property family of each
   participating interface.
3. At root-module completion, `ModuleState` gathers all registered interfaces
   in the module tree and removes duplicate Scala objects by identity.
4. `Checker` resolves and validates the supported properties on
   every interface.

Child modules do not run the checker independently. Waiting for root completion
ensures that parent-side connections and resolver registrations are visible.

Interfaces declared through `AnnotatedModule` are registered automatically.
`.asFull` and `.asLite` register their DataView-created interfaces explicitly.
A raw interface and its DataView are still distinct Scala objects with distinct
property state, so create a view once at the root and reuse it. Further
conversions of the same raw interface emit warnings with the current and
previous call sites but do not fail elaboration.

## Property keys and values

Master and slave describe AXI protocol roles, not Chisel port directions:

- master properties, i.e. the issued traffic, describe transactions that a
  master may issue;
- slave properties, i.e. the accepted traffic, describe transactions that a
  slave accepts.

On an individual AXI connection, the master endpoint is the upstream interface
(`m_axi`) and the slave endpoint is the downstream interface (`s_axi`). For a
two-sided module, name its interfaces explicitly: its `s_axi` interface belongs
to the upstream connection, while its `m_axi` interface belongs to the
downstream connection.

Each `properties.Key[T]` carries:

- a `Role`: `Master`, `Slave`, or `None`;
- an `Access`: `Read`, `Write`, or `None`;
- a typed `ValueType[T]`; and
- its diagnostic name and description.

The standard keys are:

| Value type | Master keys | Slave keys |
|---|---|---|
| `BurstShape` | `MasterReadBurstShape`, `MasterWriteBurstShape` | `SlaveReadBurstShape`, `SlaveWriteBurstShape` |
| `ThreadMode` | `MasterReadThreadMode`, `MasterWriteThreadMode` | `SlaveReadThreadMode`, `SlaveWriteThreadMode` |
| `TrafficProfile` | `MasterReadTrafficProfile`, `MasterWriteTrafficProfile` | `SlaveReadTrafficProfile`, `SlaveWriteTrafficProfile` |
| `MemoryMap` | — | `SlaveMemoryMap` |

Keys and selector aliases live directly in `tracking.properties`; values live
in `tracking.values`. The conventional imports are:

```scala
import chext.amba.axi4.tracking.{properties => p, values => v}
```

`p.KnownKeys` is the complete standard-key catalog. `p.Master`, `p.Read`, and
`p.BurstShape` are selectors as well as stable pattern values. A manager can
select all matching cells:

```scala
val reads: Seq[p.Cell[_]] = interface.properties.select(p.Read)
val shapes: Seq[p.Cell[v.BurstShape]] =
  interface.properties.select(p.BurstShape)
```

Resolver binding uses the key metadata to mark disabled read/write groups
`Undefined`. AXI4-Lite burst-shape properties are also `Undefined`.

## Property storage and state

Each `Tracked` interface owns one `properties.Manager`, exposed as `properties`.
The key passed to the manager fixes the value type of the returned
`properties.Cell[T]`.

A realized property has one of these states:

| State | Meaning |
|---|---|
| `Unresolved` | Resolver work may still produce a terminal state. |
| `Enforced(value)` | Component or user code supplied an authoritative value. |
| `Calculated(value, resolver)` | A resolver derived the value. |
| `DontCare(message)` | This boundary does not need a synthetic value. |
| `Incomplete` | The property applies, but available information is insufficient. |
| `Undefined` | The property does not apply to this interface or topology. |

Only `Unresolved` is nonterminal. `valueOption` returns a value only for
`Enforced` and `Calculated`.

Assignment enforces a value:

```scala
interface.properties(p.MasterReadThreadMode) =
  v.ThreadMode.SingleThread
```

Re-enforcing an equal value is idempotent. Re-enforcing a different value
fails. Enforcement may replace a calculated or valueless state and clears its
resolution trace. The cell retains the Chisel `SourceInfo` supplied at
enforcement, the originating interface, and a public `Owner` describing the
resolver's module or component. Ordinary assignment syntax receives the exact
resolver owner implicitly; enforcement outside a resolver falls back to
Chisel's current module when one exists. Calculated forwarding preserves this
authoritative origin.

Aggregate property values are immutable. Adjust a value with `copy` and enforce
the complete replacement:

```scala
val current = interface.properties(p.MasterReadBurstShape).get
interface.properties(p.MasterReadBurstShape) =
  current.copy(maxBeats = 16)
```

Enforcing a replacement for a calculated or valueless state changes it to
`Enforced` and clears its resolution trace.

## Resolver registration and matching

A resolver belongs to a Chext `Component` or Chisel `BaseModule`. It binds each
participating interface without storing local binding objects:

```scala
private final class Example_Resolver(owner: Example)(implicit
    sourceInfo: SourceInfo
) extends axi4.tracking.Resolver(owner) {
  import axi4.tracking._
  import axi4.tracking.{properties => p}

  bindSlave(owner.s_axi)
  bindMaster(owner.m_axi)

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case ResolveRequest(_, p.Key(_, _, p.TrafficProfile)) =>
        request.incomplete()

      case ResolveRequest(_, p.Key(p.Slave, _, p.MemoryMap)) =>
        request.forwardTo(owner.m_axi)

      case ResolveRequest(
            _,
            p.Key(p.Master, _, p.BurstShape | p.ThreadMode)
          ) =>
        request.forwardTo(owner.s_axi)

      case _ =>
        request.missingCase()
    }
}
```

`p.Key(role, access, valueType)` extracts all three classifications from
the original cell. `_` leaves a classification unrestricted, and
normal pattern alternatives combine alternatives. Bind the original cell when
it is useful:

```scala
case ResolveRequest(
      interface,
      cell @ p.Key(p.Slave, p.NoAccess, p.MemoryMap)
    )
    if interface eq owner.s_axi =>
  inspect(cell)
  request.forwardTo(owner.m_axi)
```

`bindMaster` and `bindSlave` accept one interface or a sequence. Resolver
selection prefers the candidate owned by the shallowest hierarchy node. The
most recently registered candidate wins at equal depth.

Resolver trace metadata defaults from the implementation class name. For
example, `CreditBuffer_Resolver` becomes `CreditBufferResolver` with kind
`credit-buffer`. Override `resolver` or `kind` only when the public name should
differ.

## Resolution protocol

`ResolveRequest[T]` is the transparent case class
`ResolveRequest(tracked, cell)`. Its generated extractor provides direct
pattern matching, and `ResolveRequest(tracked, key)` remains a convenience
constructor that realizes the cell through the interface's manager.

Every concrete `Resolver` inherits an implicit `RequestOps[T]` class. This
adds the complete resolver policy API directly to a request without putting
mutation primitives on `ResolveRequest` itself:

```scala
request.calculate(value)
request.incomplete()
request.dontCare(reason)
request.undefined()
request.failure(message)
request.forwardTo(target)
request.mapFrom(target)(transform)
request.calculate(p.ThreadMode, value)
request.mapFrom(target, p.BurstShape)(transform)
request.missingCase()
```

Scala 2 does not refine the request's `T` from a stable value-type pattern such
as `p.Key(_, _, p.ThreadMode)`. The overloads taking a `ValueType[T]`
preserve static value typing for calculations and transformations after such a
match.

`RequestOps` is deliberately nested in `Resolver`: it captures the enclosing
resolver for calculation ownership, trace metadata, and diagnostics, so call
sites do not pass `this`. It cannot extend `AnyVal` because Scala value classes
cannot be members of another class; a nested instance also necessarily carries
the enclosing `Resolver` reference.

A resolver completes one step by returning:

```scala
sealed trait ResolveResult

object ResolveResult {
  final case class Success() extends ResolveResult
  final case class Retry(requests: Seq[ResolveRequest[_]]) extends ResolveResult
  final case class Failure(
      message: String,
      request: ResolveRequest[_]
  ) extends ResolveResult
}
```

Request operations provide the terminal transitions:

- `calculate(value)` stores a derived value;
- `dontCare(message)` marks the boundary intentionally irrelevant;
- `incomplete()` records insufficient information;
- `undefined()` records inapplicability;
- `failure(message)` returns a request-specific error.

One-to-one AXI module resolvers should forward or transform applicable
properties rather than use `dontCare`; applicable transformations that are not
yet modeled are `Incomplete`. `DontCare` is reserved for boundaries such as
synthetic multi-endpoint aggregates where no single value is represented.

`request.forwardTo(target)` implements transparent propagation. It creates a
dependency for the same key, asks the coordinator to retry while that
dependency is unresolved, copies a resolved value, and propagates valueless
terminal states. `request.mapFrom(target)(transform)` follows the same protocol
but transforms a resolved value. Retry construction remains internal to these
operations.

`request.missingCase()` is the standard catch-all failure. Its message includes
the derived resolver name, kind, and requested property.

## Dependency coordinator

`Resolver.resolve(initial)` creates a fresh recursive context for one top-level
request:

1. Return success if the property is already terminal.
2. Select the interface's resolver for the key's family.
3. Call that resolver with the request.
4. Accept `Success` only if the property became terminal.
5. Return `Failure` unchanged.
6. For `Retry(dependencies :+ request)`, resolve each dependency in order and
   then process the original request again.

The coordinator:

- traverses dependencies depth-first;
- detects cycles on the active request path;
- rejects a repeated retry that made no progress;
- validates the retry continuation marker; and
- enforces `maxStackSize`.

Shared dependencies that are already terminal succeed immediately. A fresh
context is created for every top-level call, so active paths and retry history
do not leak between resolutions.

Calculated values retain ordered `ResolutionStep` records. Each step contains
the source interface, target interface, relationship kind, resolver name, and
resolver-owner path. Enforced and valueless states have no calculated trace.

## Compatibility checks

The checker processes each enabled Full read and write access independently:

1. resolve the master and slave `BurstShape`;
2. when either local burst property is `Enforced`, validate both values and
   compare the master and slave burst shapes;
3. resolve the master and slave `ThreadMode`; and
4. when either local thread property is `Enforced`, validate both values and
   compare the master and slave thread modes.

AXI4-Lite skips burst checks and checks only `ThreadMode`. Every interface also
resolves and validates `p.SlaveMemoryMap`. `TrafficProfile` is not requested.

Calculated-vs-calculated pairs are omitted because they describe an inferred
intermediate boundary and duplicate a check at an authoritative enforced
boundary.

`DontCare` skips only the local comparison. The checker still visits every
other registered interface, where a component's resolver policy preserves the
relevant facts. `Incomplete`, unexpected `Undefined`, unresolved properties,
resolution failures, invalid values, and incompatibilities are diagnostics.
`p.SlaveMemoryMap` may be `Undefined` for endpoints that intentionally provide
no successful address space.

### BurstShape

`BurstShape` is an immutable case class containing:

- `maxBeats`, the maximum burst length in beats;
- `types`, the supported AXI burst-type encodings;
- `sizes`, the supported transfer-size encodings; and
- `aligned`, whether every transaction is naturally aligned:
  `AxADDR % (1 << AxSIZE) == 0`.

A nonempty shape must have positive `maxBeats` and nonempty `types` and
`sizes` sequences. Validation checks protocol limits and interface
transfer sizes.

`BurstShape.normalize` produces the canonical representation.
`BurstShape.all(cfg)` produces the most permissive valid shape for an
interface configuration. `checkConfig` returns all configuration errors, and
`checkCompatible` returns all master-to-slave compatibility errors. Both
checks use `CheckResult.Success` or `CheckResult.Error(errors: Seq[String])`.

As a master property, `aligned = false` means the master may issue unaligned
transactions. As a slave property, `aligned = false` means the slave accepts
unaligned transactions; it does not mean transactions must be unaligned. A
slave property with `aligned = true` accepts only naturally aligned
transactions. The `types` and
`sizes` sequences are normalized to sorted, distinct values.

A master shape is compatible with a slave shape when:

```text
master.maxBeats       <= slave.maxBeats
master.types     subsetOf slave.types
master.sizes  subsetOf slave.sizes
!slave.aligned || master.aligned
```

An empty master shape has no transfers and therefore satisfies any alignment
requirement. All mismatches are reported together.

### ThreadMode

`ThreadMode` describes the IDs of simultaneously outstanding transactions:

- `SingleTransaction`: at most one transaction is outstanding.
- `SingleThread`: outstanding transactions share one ID.
- `UniqueThreads`: outstanding transactions have distinct IDs.
- `Unconstrained`: neither relationship is guaranteed.

Compatibility is:

| Master mode | Compatible slave modes |
|---|---|
| `SingleTransaction` | any mode |
| `SingleThread` | `SingleThread`, `Unconstrained` |
| `UniqueThreads` | `UniqueThreads`, `Unconstrained` |
| `Unconstrained` | `Unconstrained` |

AXI4-Lite has one implicit ID. Only `SingleTransaction` and `SingleThread` are
valid on a Lite interface; the checker rejects `UniqueThreads` and
`Unconstrained` on either side and includes resolution provenance.

`ThreadMode.values` lists every mode. `supportedModesFor(cfg)` limits that list
for AXI4-Lite, and `all(cfg)` returns the most permissive valid mode:
`Unconstrained` for Full AXI or `SingleThread` for AXI4-Lite. `normalize` is an
identity operation because thread modes have no redundant representation.
As for `BurstShape`, `checkConfig` and `checkCompatible` return `CheckResult`.

### MemoryMap

`MemoryMap` is an immutable hierarchy of maps and segments with offsets,
explicit sizes, component paths, absolute origins, and typed arguments.
Validation rejects invalid bounds, overlaps, and layouts that exceed the
interface address width.

`DemuxMm` resolves the maps at its `m_axi` interfaces, aggregates them in
routing order, and publishes the result after `genDecoder()`. A demux driven by an arbitrary
address function cannot derive a correct aggregate map and leaves the property
`Incomplete`.

## Component policies

Transparent connections and buffers forward unchanged values. Transforming,
fan-in, fan-out, and terminal components define owner-specific policies beside
their implementations. See
[AXI4 Property Resolution and Compatibility](axi4-resolvers.md) for the policy
of each component.

## Known limitations

- `TrafficProfile` has storage and mutation APIs, but no compatibility rules.
  Resolver requests terminate as `Incomplete`, and the checker does not request
  it.
- Raw interfaces and DataView-created interfaces do not share canonical
  property identity.
- Rejected calculation attempts do not retain separate source provenance.
- Resolver selection uses hierarchy depth, not a validated ancestry
  relationship, and does not fall back to a lower-priority candidate after a
  selected resolver fails.
