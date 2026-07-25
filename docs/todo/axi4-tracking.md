# AXI4 Tracking Design

> **Status:** Active design notes. The initial tracking layer, master/slave resolver ownership,
> typed and iterable property storage, source-located resolver registration, depth-based resolver
> selection, dependency-driven DFS coordinator, transparent connection/buffer propagation, and
> memory-map resolution traces exist. Automatic resolver registration through hierarchy state,
> general property provenance, and canonical DataView identity are not implemented yet.

## Goal

`chext.amba.axi4.tracking` exists to catch AXI composition errors. It is deliberately not a
complete behavioral annotation of every interface. Resolvers should generate the minimum facts
needed by a planned validation rule, propagate requirements or capabilities to the boundary where
that rule will use them, and expose real limits imposed by component parameters. They should not
invent aggregate interface profiles merely for completeness.

Resolution and validation are separate phases. The current work only generates and resolves
properties. Compatibility checks and their diagnostics will be designed after property generation
is in place; no resolver should perform those checks itself.

For example, a demux forwards an upstream master's burst behavior to each master-side output, where
it can eventually be compared with that output's connected slave. It does not intersect the
capabilities of all downstream slaves into one synthetic input capability. Conversely, a mux
forwards its one downstream slave's capabilities to each slave-side input, but it does not union or
sum the distinct upstream masters into a synthetic output traffic profile. Queue and tracker
parameters may still be published when they are genuine component-imposed limits.

A demux keeps those two sources of information distinct. Its slave properties at the common input
describe limits imposed by the demux internals. Its master properties at every output forward the
original upstream master's requirements, including outstanding transactions and threads. The
internal limits must not replace those forwarded requirements: later validation needs the former
to compare the upstream master with the demux and the latter to compare that same master with every
downstream slave.

## Current implementation

The initial implementation provides:

- AXI4 `Tag`, `ModuleState`, and `ComponentState` scaffolding.
- `ResolveRequest`, targeting one `Tracked` interface and one realized property.
- `ResolveResult`: `Success`, dependency-bearing `Retry`, and request-bearing `Failure`.
- `Resolver.resolve`, which creates a disposable DFS context and returns both the terminal result
  and trace after recursive dependency resolution with cycle checks and depth limits.
- `Resolver`, owned by either a Chisel `BaseModule` or unified Chext `Component`.
- `Tracked`, mixed into raw, full, and Lite AXI4 interface representations.
- Private `X_Resolver(owner: X)(implicit sourceInfo: SourceInfo)` implementations beside every
  Full or Lite component/module that declares a tracked AXI interface. `Resolver.bindCommon`,
  `bindMaster`, and `bindSlave` register either one interface or a sequence of interfaces and
  return a typed request matcher, keeping registration and dispatch consistent without
  endpoint-identity or role-tag tests in concrete resolvers. Behavioral resolvers are not defined
  in the generic `tracking` package. Full and Lite AXI `Connect` and `Buffer` resolvers propagate
  unchanged properties; component-specific resolvers own terminal values and transformations.
  Fan-in/fan-out resolvers preserve the individual boundaries needed by later checks instead of
  synthesizing aggregate traffic profiles.
- Absolute tracking paths for modules, components, and interfaces, plus typed `ResolutionStep`
  records for forwarded properties.
- Separate internal common/master/slave `(Resolver, SourceInfo)` registrations plus one cached
  selected resolver for each property family.
- One interface-wide `PropertyManager`, exposed as `properties`. The compatibility accessors
  `commonProps`, `masterProps`, and `slaveProps` all return that same store.
- `properties.Common.Config`, enforced from the interface's `axi4.Config`.
- Standard key catalogs are organized in the `tracking.properties` subpackage as `Common`,
  `Master`, and `Slave`; the reusable property framework remains directly under `tracking`.
- `Master.shapeProperties` and `Slave.shapeProperties` collect the burst shape keys, with nested
  `ShapeProperty()` Boolean extractors for resolver patterns. `Slave.MemoryMap` remains separate
  because address topology is not traffic shape.
- `Master.readProperties` / `writeProperties` and the corresponding `Slave` collections enumerate
  the standard access-specific keys. `bindMaster` and `bindSlave` eagerly mark a disabled
  interface direction `Undefined` from those catalogs.
- Sealed common/master/slave and read/write key markers, descriptions, and family-qualified
  diagnostic names.
- Four family/access-specific standard key bases and composable `CommonProperty()` /
  `MasterProperty()` / `SlaveProperty()` / `ReadProperty()` / `WriteProperty()` Boolean
  extractors.
- Per-property `PropertyState`: `Unresolved`, `Enforced`, `Calculated`, `DontCare(message)`,
  `Incomplete`, or `Undefined`.
- Insertion-ordered iteration over the properties instantiated in a manager.
- Iteration over all standard definitions through `properties.Common`, `properties.Master`, and
  `properties.Slave`.
- Internal retention of common/master/slave resolver registrations on each `Tracked`, including
  their call-site source information. The shallowest candidate is selected; the most recently
  registered candidate wins at equal depth.
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

## Common, master, and slave property families

Master and slave describe AXI protocol roles, not Chisel input/output direction:

- Master properties describe traffic that may be issued or generated.
- Slave properties describe traffic that may be accepted or supported.

The family is represented by a sealed marker inherited from the key's specialized base class; it
is not a separate runtime tag or constructor argument. One store therefore contains common,
master, and slave properties. The key namespace makes the intended family explicit in diagnostics,
while generic code can still iterate and manipulate every property uniformly.

```scala
trait Tracked {
  def cfg: axi4.Config

  val properties: PropertyManager

  def addCommonResolver(resolver: Resolver)(implicit sourceInfo: SourceInfo): this.type
  def addMasterResolver(resolver: Resolver)(implicit sourceInfo: SourceInfo): this.type
  def addSlaveResolver(resolver: Resolver)(implicit sourceInfo: SourceInfo): this.type
}
```

A concrete resolver accepts its registration source implicitly and binds each participating
interface/property-family pair. The sequence overload returns the matched interface together with
the typed property key:

```scala
private final class X_Resolver(owner: X)(implicit sourceInfo: SourceInfo)
    extends Resolver(owner) {
  private val SlaveRequests = bindSlave(owner.s_axi)
  private val MasterRequests = bindMaster(owner.m_axi.toSeq)

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case SlaveRequests(Slave.MemoryMap) => resolveMemoryMap(request)
      case MasterRequests(_, _)           => forwardTo(request, owner.s_axi)
      case SlaveRequests(_) =>
        request.dontCare("downstream slave capabilities remain separate")
      case _                              => request.failure("unsupported request")
    }
}
```

Property families without registrations use a future structural/default resolution strategy. A module or
component can therefore specialize selected interfaces without claiming every interface in its
hierarchy.

Hierarchy ownership remains useful for resolver precedence, lifetime, and diagnostics. Resolution
should operate on registered identities and resolvers rather than recursively finalizing one module
at a time. Parent connections may not exist when a child module body completes.

## Property keys and managers

A property key contains a value type, a local name, a description, and one family marker inherited
from its base class. There is no `Role` value and no `CommonTag`, `MasterTag`, or `SlaveTag`:

```scala
abstract class PropertyKey[T](
    val name: String,
    val description: String
)

sealed trait CommonProperty
object CommonProperty {
  def unapply(key: PropertyKey[_]): Boolean
}

sealed trait MasterProperty
object MasterProperty {
  def unapply(key: PropertyKey[_]): Boolean
}

sealed trait SlaveProperty
object SlaveProperty {
  def unapply(key: PropertyKey[_]): Boolean
}

sealed trait ReadProperty
object ReadProperty {
  def unapply(key: PropertyKey[_]): Boolean
}

sealed trait WriteProperty
object WriteProperty {
  def unapply(key: PropertyKey[_]): Boolean
}

abstract class CommonPropertyKey[T](name: String, description: String)
abstract class MasterPropertyKey[T](name: String, description: String)
abstract class SlavePropertyKey[T](name: String, description: String)
abstract class MasterReadProperty[T](name: String, description: String)
abstract class MasterWriteProperty[T](name: String, description: String)
abstract class SlaveReadProperty[T](name: String, description: String)
abstract class SlaveWriteProperty[T](name: String, description: String)

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
  def markUndefined(key: PropertyKey[_]): this.type
  def markUndefined(keys: Iterable[PropertyKey[_]]): this.type
}
```

Every directional standard key derives from one of the four family/access-specific bases.
`ReadProperty()` and `WriteProperty()` are Boolean extractors over those sealed markers. They
remain available for extension-key resolver policies without changing a binding's typed key
extractor. Standard keys also appear in explicit read/write catalog groups:

```scala
Master.readProperties
Master.writeProperties
Slave.readProperties
Slave.writeProperties

interface.slaveProps.markUndefined(Slave.readProperties)
```

Interface-wide keys such as `Common.Config` and `Slave.MemoryMap` implement neither marker. The
marker bases prevent standard keys from acquiring independent or contradictory read/write Boolean
fields. Resolver bindings use the catalog groups to eagerly classify every standard property in a
direction disabled by `axi4.Config`.

Names need only be unique within a family, so master and slave keys intentionally share concise names
such as `read_burstBeats`. Diagnostics and serialized output use qualified names such as
`master.read_burstBeats` and `slave.read_burstBeats`. Duplicate definitions within one family fail
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
Static applicability can be declared through `PropertyManager.markUndefined`; dependency-driven
terminal operations live on `ResolveRequest`.

The key namespace provides explicit, compact family context. Scala application-update syntax
enforces a value:

```scala
import chext.amba.axi4.tracking.properties.{Master, Slave}

interface.properties(Master.ReadOutstandingTransactions) = 8
interface.properties(Master.ReadBurstBeats) = 16
interface.properties(Slave.ReadOutstandingTransactions) = 12
interface.properties(Slave.ReadBurstBeats) = 256
```

External libraries may add keys by extending the appropriate family base, plus corresponding
extension methods, without modifying `Tracked`.

`axi4.Config` is a common property shared by both directions:

```scala
import chext.amba.axi4.tracking.properties.Common

val cfg = interface.properties(Common.Config).get
```

`Tracked.properties` initializes `common.config` from the interface's `cfg`. The store is lazy so
the abstract interface configuration is fully constructed before it is enforced.

## Initial standard properties

The current standard keys are deliberately family-specific:

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

Every realized property is either unresolved or in one of five terminal resolved states:

```scala
sealed trait PropertyState[+T]

object PropertyState {
  case object Unresolved extends PropertyState[Nothing]

  sealed trait Resolved[+T] extends PropertyState[T]
  final case class Enforced[T](value: T) extends Resolved[T]
  final case class Calculated[T](value: T, resolver: Resolver) extends Resolved[T]
  final case class DontCare(message: String) extends Resolved[Nothing]
  case object Incomplete extends Resolved[Nothing]
  case object Undefined extends Resolved[Nothing]
}
```

- `Enforced(value)` means component or user code supplied an authoritative value. Ordinary
  property assignment uses this state.
- `Calculated(value, resolver)` means a resolver produced the value.
- `DontCare(message)` means the resolver intentionally does not need a synthetic value at that
  boundary for the tracking model. The nonempty message records why this is deliberate.
- `Incomplete` means resolution terminated without a value because of a missing or inconsistent
  design relationship that would be useful to model. It is resolved for scheduling purposes but
  is a diagnostic condition.
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
  final case class DontCare(message: String) extends Rejected
  case object Incomplete extends Rejected
  case object Undefined extends Rejected
}
```

The rejected results leave the property unchanged. This makes calculation non-throwing and lets
the coordinator distinguish an intentional `DontCare` from missing information and the other
reasons that no assignment occurred. A later assignment journal may additionally retain source
locations and attempt order for diagnostics.

## Participation and completeness

Users should not need a normal `markResolved` call. Completeness is derived from active property
obligations and must not mean that every extensible property key or both directional families have
a value.

A property becomes active when it is explicitly assigned, claimed as a resolver output or
dependency, or requested by an applicable compatibility check or resolution profile. An untouched
master or slave family is inactive, not incomplete.

Participation remains separate from state: an unrealized or unrequested key need not become a
property at all. Once active, it follows the state machine above.

Applicability depends on `axi4.Config`. Write properties, for example, are not applicable to a
read-only interface. Optional extension keys do not block standard AXI resolution unless they join
an active profile.

Resolution state is property-local. Interfaces do not cache common, master, slave, or aggregate
resolve-state flags; any summary needed for reporting is derived by iterating the active properties.

## Property propagation

Propagation is also property-family-specific:

- Master facts flow from an upstream master through connections and transformations toward slaves.
- Slave capabilities flow from a downstream slave back through connections and transformations
  toward masters.
- Master facts never become slave facts through copying. Compatibility validation is the explicit
  operation that compares corresponding master- and slave-property keys from the common store.

A direct connection and an ordinary buffer normally propagate both roles transparently. A width
converter, unburster, mux, demux, ID serializer, or protocol converter supplies transformation
rules for the properties it changes.

Full and Lite AXI `Connect` and tracked `Buffer` components register transparent resolvers.
Slave properties flow upstream from the connected slave endpoint to the master endpoint, while
master properties flow downstream from master to slave. Resolution retains terminal
`DontCare`, `Undefined`, and `Incomplete` states as well as concrete property values. Each
forwarding step can record the absolute source and target interface paths, relationship kind,
resolver name, and resolver-owner path. `DemuxMm` optionally stores those steps on resolved child
memory maps.

Implemented component behavior includes:

- `Mux`/`IdMux`: forward the common downstream slave's capabilities to every input. A master
  property requested at the common output is `DontCare`, because the distinct upstream masters
  remain available at the individual inputs and are not unioned or summed.
- `Demux`/`IdDemux`/`DemuxMm`: forward upstream master behavior to every output. They do not
  intersect downstream slave capabilities. Ordinary `Demux` and `DemuxMm` publish their configured
  outstanding-transaction and tracked-ID limits only as local slave capabilities at the common
  input as eager `Enforced` values; output master properties always preserve the upstream values.
  Binding eagerly classifies disabled standard channel groups as `Undefined`. Arbitrary decode
  functions leave the combined memory map incomplete, while `DemuxMm` publishes the map built by
  `genDecoder()`.
- Lite `Demux` publishes its response-routing queue capacities as local slave-side
  outstanding-transaction limits eagerly, forwards all upstream master properties to every output,
  and otherwise follows the same non-aggregating fan-out and eager disabled-channel policy.
- `CreditBuffer`: matches the shared `Master.ShapeProperty()` / `Slave.ShapeProperty()` catalog
  groups and forwards those unchanged burst-shape facts. It forwards `Slave.MemoryMap` separately,
  while credit-sensitive outstanding/thread facts remain incomplete until their exact capacity
  equations are encoded.
- `Unburst`, width converters, ID converters, and `ProtocolConverter`: currently forward only the
  invariant memory map and explicitly mark unimplemented traffic transformations incomplete rather
  than treating the boundary as transparent.
- Constant/stall/idle terminations and Lite memory controllers provide their terminal properties
  directly in their owner-specific resolvers.

One resolver should represent one architectural relationship, not one property. Its individual
properties still make progress independently.

`ResolveRequest[T]` is a concrete final resolver-facing class. It targets one `Tracked` interface
and one realized property while preserving the property value through its ordinary type parameter.
It exposes the interface/configuration, common store, property/key metadata, state, and current value.
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

A resolver takes a request directly. A binding extracts `PropertyKey[T]`, so matching a singleton
key refines the request's value type:

```scala
private val SlaveRequests = bindSlave(owner.s_axi)

def resolve[T](request: ResolveRequest[T]): ResolveResult =
  request match {
    case SlaveRequests(MyProperties.Key) =>
      request.calculate(calculatedValue, this)
      ResolveResult.Success()
    case SlaveRequests(_) =>
      forwardTo(request, owner.m_axi)
    case _ =>
      request.failure("unsupported request")
}
```

`request.retry(dependencies)` constructs `Retry(dependencies :+ request)`, guaranteeing that the
original request is at the back. The coordinator validates this invariant even for manually built
`Retry` values. `request.failure(message)` attaches the current request to a failure. Applicability
handling uses `request.undefined()`, a useful property that cannot currently be derived uses
`request.incomplete()`, and an intentionally unnecessary synthetic property uses
`request.dontCare(message)`.

Each `Tracked` internally retains separate common/master/slave registration sequences. Each
registration records `(Resolver, SourceInfo)`; no public accessor exposes that implementation
detail. Concrete resolvers normally register through `bindCommon`, `bindMaster`, or `bindSlave`;
`Binding` handles one interface, while `Bindings` handles a sequence and returns the matched
interface with its typed key. Both extractors encapsulate interface identity and property-family
matching. Internally they receive the named `accepts` predicate from the appropriate marker
companion; resolver call sites never pass `.unapply`. The resolver's implicit constructor
`SourceInfo` is passed through to every binding registration. Adding a candidate updates the
corresponding cached `commonResolver`, `masterResolver`, or `slaveResolver` when the new resolver
is shallower or is the latest candidate at the same depth. The recursive coordinator selects the
cache directly from the request key's sealed family base. Resolver hierarchy depth is a `lazy val`:
component parentage is fixed by the time the resolver is used, so repeated selection does not
rescan the hierarchy.

## Depth-first dependency resolution

`Resolver.resolve(initial)` creates a disposable internal context and returns a `Resolution`
containing the terminal `result` and the initial request's `steps` after depth-first resolution
using JVM recursion:

1. Return success immediately if the requested property is already terminal.
2. Select the best retained resolver for the request key's property family.
3. Call `resolver.resolve(request)`.
4. On `Success`, require that the property actually became terminal.
5. On `Failure`, return the message and failing request unchanged.
6. On `Retry(dependencies :+ request)`, resolve each dependency in order and then retry the original
   request.

The original request remains active while its dependencies are resolved recursively, so a
dependency's own retry requests are processed before siblings and the parent request. Shared
dependencies that are already resolved return success immediately.

The coordinator tracks the active DFS path. Encountering the same interface/property request on
that active path returns a cycle `Failure`; it does not reject a request merely because another
completed or sibling branch used the same dependency. `maxStackSize` limits dependency depth.
Within one active request, returning the identical `Retry` sequence again after its dependencies
have resolved produces a no-progress `Failure`; this avoids an arbitrary attempt-count limit.

After requested properties have been resolved, later validation can ignore intentional
`DontCare` states, diagnose required relationships that ended `Incomplete`, and run activated
master/slave compatibility checks. That validation pass is not implemented yet.

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
- How should the disposable recursive resolution context expose dependency/attempt traces and
  provenance?
- When should resolver selection fall back from the shallowest candidate to another retained
  candidate?
- Which standard profile activates properties automatically on ordinary connections?
- Which values have safe defaults, and which must remain unknown?
- Should property-family-level resolve state remain stored or become a view derived from active
  properties?
- What address model supports mux/demux translation without becoming a full IP-XACT model?
