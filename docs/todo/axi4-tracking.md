# AXI4 Tracking Design

> **Status:** Active design notes. The initial tracking layer, master/slave resolver ownership,
> typed and iterable property storage, source-located resolver registration, depth-based resolver
> selection, dependency-driven DFS coordinator, transparent connection/buffer propagation, and
> memory-map resolution traces exist. Automatic resolver registration through hierarchy state,
> general property provenance, and canonical DataView identity are not implemented yet.

## Goal

`chext.amba.axi4.tracking` describes and validates AXI4 interface behavior. It should eventually
check address-space propagation, outstanding transaction limits, burst compatibility, and related
capabilities without making hardware generation depend on expensive hierarchy analysis.

Resolution and validation are separate phases. Resolution first infers as much metadata as
possible. After dependency resolution terminates, validation reports incomplete information,
rejected calculation attempts, and incompatible master/slave properties.

## Current implementation

The initial implementation provides:

- AXI4 `Tag`, `ModuleState`, and `ComponentState` scaffolding.
- `ResolveRequest`, targeting one `Tracked` interface and one realized property.
- `ResolveResult`: `Success`, dependency-bearing `Retry`, and request-bearing `Failure`.
- `Resolver.recursiveResolve`, which creates a disposable DFS context and performs dependency
  resolution with cycle and limit checks.
- `Resolver`, owned by either a Chisel `BaseModule` or unified Chext `Component`.
- `Tracked`, mixed into raw, full, and Lite AXI4 interface representations.
- `ForwardingResolver`, used by full and Lite AXI `Connect` and `Buffer` components to propagate
  slave properties upstream and master properties downstream.
- Absolute tracking paths for modules, components, and interfaces, plus typed `ResolutionStep`
  records for forwarded properties.
- Internal `(Role, Resolver, SourceInfo)` registration plus one cached selected resolver for each
  common/master/slave role.
- One interface-wide `PropertyManager`, exposed as `properties`. The compatibility accessors
  `commonProps`, `masterProps`, and `slaveProps` all return that same store.
- `properties.Common.Config`, enforced from the interface's `axi4.Config`.
- Standard key catalogs are organized in the `tracking.properties` subpackage as `Common`,
  `Master`, and `Slave`; the reusable property framework remains directly under `tracking`.
- Role-tagged property keys with descriptions and role-qualified diagnostic names.
- Per-property `PropertyState`: `Unresolved`, `Enforced`, `Calculated`, `Incomplete`, or
  `Undefined`.
- Insertion-ordered iteration over the properties instantiated in a manager.
- Iteration over all standard definitions through `properties.Common`, `properties.Master`, and
  `properties.Slave`.
- Internal retention of common/master/slave resolver registrations on each `Tracked`, including
  their call-site source information. The shallowest candidate is selected; equal-depth candidates
  retain registration order.
- Lazy depth-based resolver selection. A module-owned resolver has depth zero; each parent in the
  unified Chext `Component` hierarchy adds one.
- Hierarchical `MemoryMap` values with absolute origins and optional typed resolution traces.

The current selection deliberately does not validate ancestry. There is one generic `Component`
type; any component may own children through `withComponent(this)` and carries layer-specific
state. AXI4 resolver registration can therefore be added to AXI4 `ComponentState`, while
module-owned resolvers belong in AXI4 `ModuleState`.

Raw interfaces and their `.asFull`/`.asLite` DataViews currently have distinct `Tracked` Scala
objects. Actual resolution needs a canonical identity or explicit aliasing so every representation
of the same hardware interface shares property and resolver state.

## Common, master, and slave roles

Master and slave describe AXI protocol roles, not Chisel input/output direction:

- Master properties describe traffic that may be issued or generated.
- Slave properties describe traffic that may be accepted or supported.

Roles are runtime metadata on keys, not type parameters on keys, properties, or managers. One store
therefore contains common, master, and slave properties. The key namespace makes the intended role
explicit at the call site while allowing generic code to iterate and manipulate every property
uniformly.

```scala
trait Tracked {
  def cfg: axi4.Config

  val properties: PropertyManager

  def addResolver(role: Role, resolver: Resolver)(implicit
      sourceInfo: SourceInfo
  ): this.type
}
```

A resolver can participate in either role:

```scala
val resolver = new Resolver(this) {
  def resolve[T](request: tracking.ResolveRequest[T]): tracking.ResolveResult = {
    // request.role tells whether the current key is common, master, or slave.
  }
}

s_axi.addResolver(MasterTag, resolver)
m_axi.addResolver(SlaveTag, resolver)
```

Roles without registrations use a future structural/default resolution strategy. A module or
component can therefore specialize selected interfaces without claiming every interface in its
hierarchy.

Hierarchy ownership remains useful for resolver precedence, lifetime, and diagnostics. Resolution
should operate on registered identities and resolvers rather than recursively finalizing one module
at a time. Parent connections may not exist when a child module body completes.

## Property keys and managers

A property key contains a value type, a runtime role tag, a local name, and a description. Role is
deliberately not encoded in the Scala type:

```scala
sealed trait Role { def name: String }
case object CommonTag extends Role
case object MasterTag extends Role
case object SlaveTag extends Role

abstract class PropertyKey[T](
    val name: String,
    val role: Role,
    val description: String
)

final class Property[T](val key: PropertyKey[T]) {
  def valueOption: Option[T]
  def get: T
  def getOrElse[B >: T](default: => B): B
  def update(value: T): Unit
  def enforce(value: T): this.type
}

final class PropertyManager extends Iterable[Property[_]] {
  def apply[T](key: PropertyKey[T]): Property[T]
  def update[T](key: PropertyKey[T], value: T): Unit
}
```

Names need only be unique within a role, so master and slave keys intentionally share concise names
such as `read_burstBeats`. Diagnostics and serialized output use qualified names such as
`master.read_burstBeats` and `slave.read_burstBeats`. Duplicate definitions within one role fail
immediately.

`Property` is the mutable realization of a `PropertyKey`. Managers use insertion-ordered storage,
and iteration returns only realized properties:

```scala
for (property <- interface.properties) {
  println(s"${property.key.qualifiedName}: ${property.valueOption}")
}
```

The `properties.Common`, `properties.Master`, and `properties.Slave` namespaces are themselves
iterable when a caller needs to inspect all definitions, including keys that have not been realized
in a particular interface store.

The unavoidable cast for heterogeneous storage is localized inside `PropertyManager.apply`.

`get` and `getOrElse` follow `Option` conventions. `enforce(value)` is authoritative: the first
enforced value remains in effect, later enforcement is harmless, and calculation can never replace
it. If enforcement occurs before any enforced value, it supersedes the previous non-enforced state.
Resolver-specific operations live on `ResolveRequest` rather than expanding the basic
`Property` API.

The key namespace provides explicit, compact role selection. Scala application-update syntax
enforces a value:

```scala
import chext.amba.axi4.tracking.properties.{Master, Slave}

interface.properties(Master.ReadOutstandingTransactions) = 8
interface.properties(Master.ReadBurstBeats) = 16
interface.properties(Slave.ReadOutstandingTransactions) = 12
interface.properties(Slave.ReadBurstBeats) = 256
```

External libraries may add keys and corresponding extension methods without modifying `Tracked`.

`axi4.Config` is a common property shared by both roles:

```scala
import chext.amba.axi4.tracking.properties.Common

val cfg = interface.properties(Common.Config).get
```

`Tracked.properties` initializes `common.config` from the interface's `cfg`. The store is lazy so
the abstract interface configuration is fully constructed before it is enforced.

## Initial standard properties

The current standard keys are deliberately role-specific:

| Master property | Slave property | Compatibility |
|---|---|---|
| `read_outstandingTransactions` | `read_outstandingTransactions` | master <= slave |
| `write_outstandingTransactions` | `write_outstandingTransactions` | master <= slave |
| `read_threads` | `read_threads` | master <= slave and protocol ID capacity |
| `write_threads` | `write_threads` | master <= slave and protocol ID capacity |
| `read_burstBeats` | `read_burstBeats` | master <= slave |
| `write_burstBeats` | `write_burstBeats` | master <= slave |
| `read_burstNarrow` | `read_burstNarrow` | master implies slave |
| `write_burstNarrow` | `write_burstNarrow` | master implies slave |
| `read_burstTypes` | `read_burstTypes` | master subset of slave |
| `write_burstTypes` | `write_burstTypes` | master subset of slave |
| `read_burstSizes` | `read_burstSizes` | master subset of slave |
| `write_burstSizes` | `write_burstSizes` | master subset of slave |

Burst lengths are expressed in beats, not bytes or encoded `AxLEN`. They are at most 16 in
AXI3-compatible mode and at most 256 in AXI4-Full mode. A one-beat maximum represents non-bursting
behavior without a redundant Boolean.

Addressing will also be split rather than represented by one paired key:

```scala
object Master {
  case object AddressSpace
      extends MasterPropertyKey[properties.AddressSpace](
        "addressSpace",
        "Address space accessed by the master."
      )
}

object Slave {
  case object MemoryMap
      extends SlavePropertyKey[chext.amba.axi4.util.MemoryMap](
        "memoryMap",
        "Memory map provided by the slave."
      )
}
```

`MemoryMap` is an immutable recursive structure. Each node has a logical component `path`, an offset
relative to its immediate parent, an explicit size, and child maps and segments. Maps and segments
also support an absolute slash-separated `origin` and `Map[String, TypedObject]` arguments compatible
with elastic tracking. A segment also has a component
`path`, plus a base address relative to its containing map and a positive size. Addresses
and sizes use `BigInt`; offsets and base addresses are non-negative. The absolute address of a
segment is the sum of its base address and every map offset on its path from the root. `flattenOnce`
promotes direct-child segments and grandchildren while accumulating their relative offsets and
slash-delimited paths; `flatten` repeats the operation until no children remain. `aggregate`
allocates maps in caller-supplied order with aligned-packed, largest-aligned, or tight placement. Routing
indices are decoder metadata rather than part of the memory-map model. Hardware decoder
construction rejects overlapping or out-of-range segments.

```scala
import chext.amba.axi4.util.MemoryMap

val memoryMap = MemoryMap(
  offset = 0x1000,
  size = 0x400,
  children = Seq(
    MemoryMap(
      path = Seq("peripheral"),
      offset = 0x200,
      size = 0x100,
      segments = Seq(MemoryMap.Segment(Seq("control"), 0x10, 0x20))
    )
  ),
  segments = Seq(MemoryMap.Segment(Seq("status"), 0x80, 0x10))
)
```

The concrete immutable `AddressSpace` model is still to be designed.

## Property resolution state

Every realized property is either unresolved or in one of four terminal resolved states:

```scala
sealed trait PropertyState[+T]

object PropertyState {
  case object Unresolved extends PropertyState[Nothing]

  sealed trait Resolved[+T] extends PropertyState[T]
  final case class Enforced[T](value: T) extends Resolved[T]
  final case class Calculated[T](value: T, resolver: Resolver) extends Resolved[T]
  case object Incomplete extends Resolved[Nothing]
  case object Undefined extends Resolved[Nothing]
}
```

- `Enforced(value)` means the user supplied the value. Ordinary property assignment uses this
  state.
- `Calculated(value, resolver)` means a resolver produced the value.
- `Incomplete` means resolution terminated without a value because of a missing or inconsistent
  design relationship. It is resolved for scheduling purposes but is a diagnostic condition.
- `Undefined` means the property does not apply to the interface configuration. For example, write
  properties are undefined on a read-only interface.
- `Unresolved` means another resolution attempt may make progress. `request.retry(dependencies)`
  leaves the property in this state and schedules its dependencies before the original request.

`valueOption` is defined only for `Enforced` and `Calculated`. Repeating an identical enforcement
is harmless because the first enforced value remains authoritative. Calculation never changes a
terminal state. Instead it returns a reason describing why assignment succeeded or was rejected:

```scala
sealed trait CalculateResult

object CalculateResult {
  case object Success extends CalculateResult

  sealed trait Rejected extends CalculateResult
  case object AlreadyEnforced extends Rejected
  case object AlreadyCalculated extends Rejected
  case object Incomplete extends Rejected
  case object Undefined extends Rejected
}
```

`AlreadyEnforced`, `AlreadyCalculated`, `Incomplete`, and `Undefined` leave the property unchanged.
This makes calculation non-throwing and lets the coordinator distinguish the reason that no
assignment occurred. A later assignment journal may additionally retain source locations and
attempt order for diagnostics.

## Participation and completeness

Users should not need a normal `markResolved` call. Completeness is derived from active property
obligations and must not mean that every extensible property key or both roles have a value.

A property becomes active when it is explicitly assigned, claimed as a resolver output or
dependency, or requested by an applicable compatibility check or resolution profile. An untouched
master or slave role is inactive, not incomplete.

Participation remains separate from state: an unrealized or unrequested key need not become a
property at all. Once active, it follows the state machine above.

Applicability depends on `axi4.Config`. Write properties, for example, are not applicable to a
read-only interface. Optional extension keys do not block standard AXI resolution unless they join
an active profile.

Resolution state is property-local. Interfaces do not cache common, master, slave, or aggregate
resolve-state flags; any summary needed for reporting is derived by iterating the active properties.

## Property propagation

Propagation is also role-specific:

- Master facts flow from an upstream master through connections and transformations toward slaves.
- Slave capabilities flow from a downstream slave back through connections and transformations
  toward masters.
- Master facts never become slave facts through copying. Compatibility validation is the explicit
  operation that compares corresponding role-tagged keys from the common store.

A direct connection and an ordinary buffer normally propagate both roles transparently. A width
converter, unburster, mux, demux, ID serializer, or protocol converter supplies transformation
rules for the properties it changes.

Full and Lite AXI `Connect` and tracked `Buffer` components register transparent resolvers.
Slave-role properties flow upstream from the connected slave endpoint to the master endpoint,
while master-role properties flow downstream from master to slave. Resolution retains terminal
`Undefined` and `Incomplete` states as well as concrete property values. Each forwarding step can
record the absolute source and target interface paths, relationship kind, resolver name, and
resolver-owner path.

Important component behavior includes:

- `Unburst`: downstream master burst beats become one; upstream slave acceptance may remain
  broader, subject to buffering and outstanding capacity.
- `Upscale`/`Downscale`: transform burst sizes, burst beats, narrow-transfer behavior, and possibly
  outstanding requirements.
- `Mux`/`Demux`: combine or partition address maps and conservatively combine capacity limits.
- `IdSerialize`/`IdParallelize`: transform thread and outstanding behavior.
- `CreditBuffer`: may cap accepted outstanding transactions by configured credits.
- Terminations: provide terminal slave capabilities or terminal master behavior.

One resolver should represent one architectural relationship, not one property. Its individual
properties still make progress independently.

`ResolveRequest[T]` is a concrete final resolver-facing class. It targets one `Tracked` interface
and one realized property while preserving the property value through its ordinary type parameter.
It exposes the interface/configuration, common store, property/key/role metadata, state, and current value.
Requests compare by interface and property object identity, which makes independently constructed
requests for the same target equal for dependency tracking.

```scala
sealed trait ResolveResult

object ResolveResult {
  final case class Success() extends ResolveResult
  final case class Retry(requests: Seq[ResolveRequest[_]]) extends ResolveResult
  final case class Failure(message: String, request: ResolveRequest[_]) extends ResolveResult
}
```

A resolver takes a request directly:

```scala
def resolve[T](request: ResolveRequest[T]): ResolveResult = {
  if (dependency.isResolved) {
    request.calculate(MyProperties.Key, calculatedValue, this)
    ResolveResult.Success()
  } else {
    request.retry(Seq(dependency))
  }
}
```

`request.retry(dependencies)` constructs `Retry(dependencies :+ request)`, guaranteeing that the
original request is at the back. The coordinator validates this invariant even for manually built
`Retry` values. `request.failure(message)` attaches the current request to a failure. Applicability
handling uses `request.undefined()`, while an unrecoverable design relationship can use
`request.incomplete()`.

Each `Tracked` internally retains registrations for all three roles in one ordered sequence. Each
registration records `(Role, Resolver, SourceInfo)`; no public accessor exposes that implementation
detail. Adding a candidate updates the corresponding cached `commonResolver`, `masterResolver`, or
`slaveResolver` only when the new resolver is shallower. Equal-depth registration keeps the
existing selection. The recursive coordinator uses a package-private role-based lookup. Resolver
hierarchy depth is a `lazy val`: component parentage is fixed by the time the resolver is used, so
repeated selection does not rescan the hierarchy.

## Depth-first dependency resolution

`Resolver.recursiveResolve(initial)` creates a disposable internal context and performs depth-first
resolution:

1. Return success immediately if the requested property is already terminal.
2. Select the best retained resolver for the request's runtime role.
3. Call `resolver.resolve(request)`.
4. On `Success`, require that the property actually became terminal.
5. On `Failure`, return the message and failing request unchanged.
6. On `Retry(dependencies :+ request)`, resolve each dependency in order and then retry the original
   request.

The original request is logically behind its dependencies on the stack, so a dependency's own
retry requests are processed before siblings and the parent request. Shared dependencies that are
already resolved return success immediately.

The coordinator tracks the active DFS path. Encountering the same interface/property request on
that active path returns a cycle `Failure`; it does not reject a request merely because another
completed or sibling branch used the same dependency. `maxStackSize` limits dependency depth.
Within one active request, returning the identical `Retry` sequence again after its dependencies
have resolved produces a no-progress `Failure`; this avoids an arbitrary attempt-count limit.

After requested properties have been resolved, later validation can mark non-applicable properties
`Undefined`, required relationships with no solution `Incomplete`, and run activated master/slave
compatibility checks.

Resolution should run after the root hierarchy is complete. Finalizing child modules independently
would report false incompleteness because their parent-side connections may not yet exist.

## Final diagnostics

Validation should cover relationships such as:

```text
master outstanding reads issued    <= slave outstanding reads accepted
master outstanding writes issued   <= slave outstanding writes accepted
master generated burst types       subset of slave accepted burst types
master generated read burst beats  <= slave accepted read burst beats
master generated write burst beats <= slave accepted write burst beats
master address space               covered/mapped by slave memory map
```

Diagnostics should distinguish incomplete resolution, rejected calculations, capability
incompatibility, and resolver ownership conflicts. An attempt journal should identify every
responsible resolver or source location.

## Next implementation slices

1. Canonicalize raw/full/Lite DataView tracking identity.
2. Generalize provenance-aware contributions and attempt tracing beyond the forwarding steps
   currently retained on calculated properties.
3. Register component-owned resolvers in AXI4 `ComponentState` and module-owned resolvers in AXI4
   `ModuleState`.
4. Define per-property participation independently of the implemented resolution state.
5. Implement applicability selection and property transformations for non-transparent components.
6. Add resolver fallback policy and final diagnostics to the DFS coordinator.
7. Design the minimal immutable `AddressSpace` model before adding the addressing keys.

## Open questions

- Should resolver precedence validate true ancestry rather than hierarchy depth alone?
- How should the disposable recursive-resolution context expose dependency/attempt traces and
  provenance?
- When should resolver selection fall back from the shallowest candidate to another retained
  candidate?
- Which standard profile activates properties automatically on ordinary connections?
- Which values have safe defaults, and which must remain unknown?
- Should role-level resolve state remain stored or become a view derived from active properties?
- What address model supports mux/demux translation without becoming a full IP-XACT model?
