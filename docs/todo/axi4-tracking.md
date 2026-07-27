# AXI4 Tracking and Resolution

> **Status:** Implemented for burst shape, thread mode, and slave memory maps.
> `TrafficProfile` remains a placeholder and is not checked.

## Purpose

`chext.amba.axi4.tracking` detects incompatible AXI compositions during
elaboration. It models only the facts needed for compatibility checks:

- what traffic a master may generate;
- what traffic a slave may accept; and
- which address space a slave provides.

Resolvers propagate or transform those facts. They do not perform compatibility
checks or invent aggregate values merely to make a property complete.

For example, a demux forwards the upstream master's requirements to every
output. It keeps the downstream slave capabilities separate instead of
intersecting them into one synthetic input capability. The checker then compares
each output independently.

## Lifecycle

Tracking follows four steps:

1. Full and Lite interfaces are registered with their owning module.
2. Component resolvers bind the master or slave property family of each
   participating interface.
3. At root-module completion, `ModuleState` gathers all registered interfaces
   in the module tree and removes duplicate Scala objects by identity.
4. `CompatibilityChecker` resolves and validates the supported properties on
   every interface.

Child modules do not run the checker independently. Waiting for root completion
ensures that parent-side connections and resolver registrations are visible.

Interfaces declared through `AnnotatedModule` are registered automatically.
`.asFull` and `.asLite` register their DataView-created interfaces explicitly.
A raw interface and its DataView are still distinct Scala objects with distinct
property state, so create a view once at the root and reuse it.

## Property keys and values

Master and slave describe AXI protocol roles, not Chisel port directions:

- master properties describe traffic that may be generated;
- slave properties describe traffic that may be accepted.

Each `PropertyKey[T]` carries:

- a family marker (`MasterProperty` or `SlaveProperty`);
- an optional access marker (`ReadProperty` or `WriteProperty`);
- a family-qualified diagnostic name; and
- a runtime value-class token used by `PropertyValueType[T]`.

The standard keys are:

| Value type | Master keys | Slave keys |
|---|---|---|
| `BurstShape` | `ReadBurstShape`, `WriteBurstShape` | `ReadBurstShape`, `WriteBurstShape` |
| `ThreadMode` | `ReadThreadMode`, `WriteThreadMode` | `ReadThreadMode`, `WriteThreadMode` |
| `TrafficProfile` | `ReadTrafficProfile`, `WriteTrafficProfile` | `ReadTrafficProfile`, `WriteTrafficProfile` |
| `MemoryMap` | — | `MemoryMap` |

Keys live in `tracking.properties.Master` and
`tracking.properties.Slave`. Values live in `tracking.values`;
`values.MemoryMap` re-exports the existing `axi4.util.MemoryMap` type and
companion.

`Master.readProperties`, `Master.writeProperties`, and the corresponding slave
collections are derived from the complete catalogs. Resolver binding uses these
collections to mark properties on disabled channel groups `Undefined`.
AXI4-Lite burst-shape properties are also `Undefined`.

## Property storage and state

Each `Tracked` interface owns one `PropertyManager`. `masterProps` and
`slaveProps` are family-oriented views of that same manager.

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
interface.masterProps(properties.Master.ReadThreadMode) =
  values.ThreadMode.SingleThread
```

Re-enforcing an equal value is idempotent. Re-enforcing a different value
fails. Enforcement may replace a calculated or valueless state and clears its
resolution trace.

`BurstShape` and `TrafficProfile` also provide controlled field updates:

```scala
interface.masterProps(properties.Master.ReadBurstShape).len = 16
```

The update copies any existing aggregate value, applies the mutation, and
stores the result as `Enforced`. This prevents one shared value instance from
being mutated through the wrong property.

## Resolver registration and matching

A resolver belongs to a Chext `Component` or Chisel `BaseModule`. It binds each
participating interface without storing local binding objects:

```scala
private final class Example_Resolver(owner: Example)(implicit
    sourceInfo: SourceInfo
) extends axi4.tracking.Resolver(owner) {
  import axi4.tracking._

  bindSlave(owner.s_axi)
  bindMaster(owner.m_axi)

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case Request(TrafficProfile()) =>
        request.incomplete()

      case SlaveRequests(MemoryMap()) =>
        forwardTo(request, owner.m_axi)

      case MasterRequests(BurstShape() | ThreadMode()) =>
        forwardTo(request, owner.s_axi)

      case _ =>
        missingCase(request)
    }
}
```

The inherited extractors have distinct roles:

- `Request(ValueType())` matches a value family without selecting a
  master/slave family.
- `MasterRequests(ValueType())` matches master requests on interfaces bound
  with `bindMaster`.
- `SlaveRequests(ValueType())` matches slave requests on interfaces bound with
  `bindSlave`.
- `.withInterface(interface, ValueType())` also extracts the selected
  interface.

Use an identity guard when a branch applies to one exact endpoint:

```scala
case SlaveRequests.withInterface(interface, MemoryMap())
    if interface eq owner.s_axi =>
  forwardTo(request, owner.m_axi)
```

`bindMaster` and `bindSlave` accept one interface or a sequence. Resolver
selection prefers the candidate owned by the shallowest hierarchy node. The
most recently registered candidate wins at equal depth.

Resolver trace metadata defaults from the implementation class name. For
example, `CreditBuffer_Resolver` becomes `CreditBufferResolver` with kind
`credit-buffer`. Override `resolver` or `kind` only when the public name should
differ.

## Resolution protocol

`ResolveRequest[T]` identifies one realized property on one interface. Requests
compare by interface and property identity, so independently constructed
requests for the same property compare equal.

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

Request helpers provide the terminal transitions:

- `calculate(value, resolver)` stores a derived value;
- `dontCare(message)` marks the boundary intentionally irrelevant;
- `incomplete()` records insufficient information;
- `undefined()` records inapplicability;
- `failure(message)` returns a request-specific error; and
- `retry(dependencies)` asks the coordinator to resolve dependencies and then
  process the original request again.

`forwardTo(request, target)` implements transparent propagation. It retargets
the same key, retries while the dependency is unresolved, copies a resolved
value, and propagates valueless terminal states. `mapFrom` follows the same
protocol but transforms a resolved value.

`missingCase(request)` is the standard catch-all failure. Its message includes
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

The checker processes each enabled Full read and write direction independently:

1. resolve and validate the master and slave `BurstShape`;
2. compare their burst capabilities;
3. resolve and validate the master and slave `ThreadMode`; and
4. compare their thread guarantees.

AXI4-Lite skips burst checks and checks only `ThreadMode`. Every interface also
resolves and validates `Slave.MemoryMap`. `TrafficProfile` is not requested.

`DontCare` skips only the local comparison. The checker still visits every
other registered interface, where a component's resolver policy preserves the
relevant facts. `Incomplete`, unexpected `Undefined`, unresolved properties,
resolution failures, invalid values, and incompatibilities are diagnostics.
`Slave.MemoryMap` may be `Undefined` for endpoints that intentionally provide
no successful address space.

### BurstShape

`BurstShape` contains:

- `len`, the maximum burst length in beats;
- `tpe`, the supported AXI burst-type encodings;
- `size`, the supported transfer-size encodings; and
- `align`, the base-2 logarithm of the minimum byte alignment of every
  transaction's starting address (`3` means 8-byte alignment).

A nonempty shape must have positive `len` and nonempty `tpe` and `size`
sequences. Validation checks protocol limits and interface transfer sizes. It
also requires `0 <= align <= wAddr`.

On a master property, `align` is a guarantee. On a slave property, it is a
requirement. The `tpe` and `size` sequences are normalized to sorted, distinct
values.

A master shape is compatible with a slave shape when:

```text
master.len   <= slave.len
master.tpe   subsetOf slave.tpe
master.size  subsetOf slave.size
master.align >= slave.align
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

| Master mode | Accepted slave modes |
|---|---|
| `SingleTransaction` | any mode |
| `SingleThread` | `SingleThread`, `Unconstrained` |
| `UniqueThreads` | `UniqueThreads`, `Unconstrained` |
| `Unconstrained` | `Unconstrained` |

AXI4-Lite has one implicit ID. Only `SingleTransaction` and `SingleThread` are
valid on a Lite interface; the checker rejects `UniqueThreads` and
`Unconstrained` on either side and includes resolution provenance.

### MemoryMap

`MemoryMap` is an immutable hierarchy of maps and segments with offsets,
explicit sizes, component paths, absolute origins, and typed arguments.
Validation rejects invalid bounds, overlaps, and layouts that exceed the
interface address width.

`DemuxMm` resolves downstream maps, aggregates them in routing order, and
publishes the result after `genDecoder()`. A demux driven by an arbitrary
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
- Calculated forwarding steps have provenance; enforced values and rejected
  calculation attempts do not yet retain general source provenance.
- Resolver selection uses hierarchy depth, not a validated ancestry
  relationship, and does not fall back to a lower-priority candidate after a
  selected resolver fails.
