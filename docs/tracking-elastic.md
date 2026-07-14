# Elastic Tracking Mechanism

Chext elastic tracking records how ready/valid endpoints are declared, marked, connected, and
emitted into `*.moduleGraph.json`. The generic tracking model is described in
[tracking.md](tracking.md); this document focuses on the elastic layer and on the AXI4 DataView
recovery built on top of it.

## Elastic Tracking Layer

Elastic tracking is attached through `chext.elastic.tracking.Tag`. Components created from the
elastic package call `trackingState(t.Tag)` and then register their protocol ports with names such
as `source`, `sink`, `source_0`, `sink_1`, `sourceSelect`, or `sinkSelect`.

Elastic interfaces extend `Tracked`, which provides:

- source and sink markings through `markSource` and `markSink`;
- endpoint role checks through `DeclaredRole`;
- graph interface emission for root sources, root sinks, wires, and recoverable views;
- sanity checks for unused, repeatedly used, or wrongly used endpoints.

The generic graph layer does not know what an elastic source or sink means. It only stores
components, component ancestry, paths, child modules, and typed arguments. The elastic layer
supplies the protocol-specific interface declarations and component port references.

Components are also the only hierarchy node type. Elastic tracking checks after deferred
elaboration that a component with children has no operational source or sink interfaces. Composite
components may register hierarchy-only boundary ports for display, while their leaf descendants
own and mark the observable protocol ports.

## Declared Roles

An ordinary elastic IO endpoint derives its declared role from Chisel IO direction:

```scala
(DataInternals.isIO(this), DataMirror.directionOf(this.$valid)) match {
  case (true, ActualDirection.Output) => DeclaredRole.Sink
  case (true, ActualDirection.Input)  => DeclaredRole.Source
  case _                              => DeclaredRole.None
}
```

This means:

- root `e.Source(...)` IO is expected to be marked and used as a source;
- root `e.Sink(...)` IO is expected to be marked and used as a sink;
- internal wires and other non-IO objects are role-neutral, but still tracked for duplicate or
  missing use where possible.

Viewed endpoints can carry an enforced role. See [Enforced Roles For Viewed Endpoints](#enforced-roles-for-viewed-endpoints).

## Interface Classification

During graph generation, elastic tracking classifies interfaces into:

| Class | Meaning |
|---|---|
| `sources` | Elastic endpoints that produce tokens from the current module's perspective. |
| `sinks` | Elastic endpoints that consume tokens from the current module's perspective. |
| `wires` | Internal elastic interfaces that are neither root source nor root sink. |
| viewed interfaces | Elastic DataView objects registered back to their raw hardware source. |

If an interface cannot be resolved as IO, wire, or registered view, Chext keeps the diagnostic
fallback path:

```text
/???/...
```

and reports that the interface is neither IO nor wire. Recoverable AXI4 and AXI4 Stream views
avoid this fallback by registering their raw source.

## Source and Sink Marking

Each component marks the interfaces it uses. For example:

- `Connect` marks one source and one sink;
- `Fork` marks its input as a source and each branch output as a sink;
- `Join` marks each joined input as a source and its output as a sink;
- `Mux`, `Demux`, and `Arbiter` also mark select streams where applicable;
- source-like components such as `Counter`, `Once`, and `Const` mark their output sink port.

Tracking diagnostics can then report:

- an endpoint that was declared but never marked;
- a source used as a source more than once;
- a sink used as a sink more than once;
- a source/sink role mismatch;
- an interface whose path cannot be recovered.

## Component Interface State

Elastic component state is stored on each tracked component under `chext.elastic.tracking.Tag`.
When a component registers an interface with `addSource(...)` or `addSink(...)`, it assigns that
interface a local port name. The registration therefore associates two separate names:

- the local port name describes the interface's role within that component, such as `source`,
  `sink`, `source_0`, or `sinkSelect`;
- the interface reference points to the actual Chisel object—an IO, wire, child IO, or registered
  view—which has its own graph path.

For example, a component at `/fork0` might register its input using the local name `source`, while
the referenced IO object has the path `/input`. The component's JSON entry records both values:

```json
[
  "source",
  { "path": "/input", "desc": "IO" }
]
```

The local name is meaningful inside the component type; it does not rename the interface object.
Interface references use the same path-resolution code as top-level interface declarations, so a
component port and the corresponding entry in `sources`, `sinks`, or `wires` identify the same
object.

Each `ComponentInterface` reference also has a `boundary` flag. It records the component-local
name, the Elastic interface, and whether the reference is hierarchy-only. The default,
`boundary = false`, is operational: registration marks the interface and makes it available to the
component's deadlock monitor. Passing `boundary = true` records and serializes the reference without calling
`markSource()` or `markSink()`. In its deferred block, `deadlock.Monitor` verifies that every port
registered on its component is non-boundary; a component exposing boundary ports therefore cannot
own a deadlock monitor. This lets a composite component expose its logical boundary while its leaf
descendants retain operational ownership.

For example:

```scala
val elasticState = trackingState(chext.elastic.tracking.Tag)
elasticState.addSource("source", source, boundary = true)
elasticState.addSink("sink", sink, boundary = true)
```

The corresponding reference carries the flag in JSON:

```json
["source", { "path": "/input", "desc": "IO", "boundary": true }]
```

A component with children may contain only boundary ports. Ordinary operational ports remain
valid only on components with no children.

The built-in Elastic composites `Repeat`, `Fold`, `Loop`, `Scope`, `Switch`, and `RandomStall`
register their external interfaces as boundary references. AXI4 Full and AXI4-Lite `Connect`
components likewise expose every available master/slave channel as a boundary reference, while
their channel-level Elastic children retain operational ownership.

User extension points are part of the logical boundary as well. `Scope` exposes `sinkBegin` and
`sourceEnd`; `Loop` exposes `sinkCurrent` and `sourceNext`; `Fold` exposes `sinkA`, `sinkB`, and
`sourceResult`; and `Switch` exposes a `sink_<branch>` / `source_<branch>` pair for every branch
callback. The source/sink classification is from the composite's perspective: an interface driven
by the composite is a boundary sink, while an interface consumed by it is a boundary source.

For example, a `Fork` may emit ports like:

```text
source -> /source
sink_0 -> /sinkLo
sink_1 -> /sinkHi
```

The component path itself is separate from the interface path. Component paths come from Chisel
prefixing and Chext `uniquePrefix(...)`; interface paths come from the hardware object or from
view recovery.

## JSON Ancestry

Elastic module-graph JSON has one `components` collection. Every component entry contains both
`parent` and `children`, in addition to `sources` and `sinks`. Top-level components use an empty
`parent`, and leaf components use an empty `children` list. Keeping both directions makes the
serialized hierarchy convenient to traverse and lets consumers validate that the two
representations agree.

The module-level `children` field remains separate: it contains child Chisel modules, while a
component's `children` contains child Chext components.

Module-graph collections preserve declaration and construction order. Interfaces retain their port
declaration or first component-use order, components retain call order, and child modules retain
instantiation order. Consequently, constructions created by a loop appear as `loop0` through
`loop10` rather than being lexically reordered with `loop10` before `loop9`.

## View Tracking Background

Chext exposes raw AXI4 interfaces as ordinary Chisel bundles, and then provides ergonomic
conversions such as `.asLite` and `.asFull` to view those raw bundles as Chext elastic
interfaces.

A Chisel `.viewAs` call does not create new hardware. It can, however, create a new Scala-side
wrapper object. This matters for Chext because elastic interfaces carry tracking state such as
source/sink markings and endpoint identity. Repeated views of the same raw interface can therefore
refer to the same hardware while producing distinct Chext-side objects.

The intended safe pattern is:

1. Call `.asLite` or `.asFull` exactly once.
2. Do this at the root module.
3. Do this on root IO.
4. Reuse the resulting elastic interface.

## View Warnings

AXI4 and AXI4 Stream raw interfaces record view calls locally on the raw interface object. There is
no global registry for this. Each recorded call stores:

- the conversion name, such as `asLite` or `asFull`;
- the `SourceInfo` of the call;
- the current module;
- whether the call was made on root IO.

Warnings are emitted through the same tracking logger style used elsewhere in Chext.

The warnings cover:

- calling `.asLite` or `.asFull` outside the root module;
- calling `.asLite` or `.asFull` at the root but not on root IO;
- calling the conversion multiple times on the same raw interface.

When the call is outside the root module, Chext does not also warn about root IO. That keeps the
message focused: the non-root-module use is already the important unsafe pattern.

The warnings deliberately do not try to diagnose whether the view type is compatible. If a raw
interface is viewed as the wrong AXI flavor, downstream AXI/connect logic is responsible for
catching that semantic error.

## Registering View Sources

The cast sites explicitly register each view with tracking. After `.asLite` or `.asFull` creates
the elastic view object, it records the relationship:

```text
viewed elastic interface -> raw AXI interface
```

The mapping is kept inside the current module's `ModuleInfo`. Each tracked child of the viewed
elastic interface is mapped to the raw source data.

This avoids walking Chisel's internal aggregate view bindings. Chext knows a channel such as
`view.ar` is backed by `s_axi` because `.asFull` registered that fact when the view was created.

## Module Graph Naming

Before view recovery, module graph generation treated viewed elastic channels as neither IO nor
wire, which produced paths such as:

```text
/???/view.ar
```

and logged:

```text
Encountered an interface which is neither an IO or Wire.
Maybe you used a view? Please do not use views.
```

For recoverable views, module graph generation resolves the raw interface and emits a stable view
path:

```text
/s_axi$view.ar
/m_axi$view.r
/leaf/s_axi$view.b
```

The `$view` marker is appended to the raw interface path rather than modeled as a child path
segment. This means `/s_axi$view.ar` should be read as "the `ar` channel of a view of `s_axi`",
not as a real hardware child named `view`.

If view recovery fails, the old `/???/...` fallback and diagnostic remain in place.

The path recovery logic lives in `ModuleInfo`, next to the registered view map. Module graph
generation and tracking diagnostics both consult the same local helpers.

## ModuleInfo Helpers

`ModuleInfo` keeps the view recovery logic local to the module that created the view. The helpers
separate ordinary interface naming from view-backed interface naming:

- `registerView(view, source, suffix, sourceInfo)` is called by `.asLite` and `.asFull`. It records
  each tracked child of the elastic view, such as `ar` or `r`, and maps that child back to the raw
  source interface. The suffix is optional; AXI4 lets tracking infer the channel suffix, while
  AXI4 Stream passes `$view` because the whole stream is a single elastic endpoint. The stored
  source info is the `.asLite` or `.asFull` call site, because that is where the viewed endpoint is
  introduced.
- `directInterfaceRef(interface)` handles only real hardware objects. Local wires become
  `/wireName`, root IO becomes `/ioName`, and child IO becomes `/childInstance/ioName`. It
  intentionally returns `None` for a DataView-created interface because the view is not itself a
  wire or IO.
- `viewSource(interface)` looks up whether a tracked elastic interface came from a registered raw
  view. The returned metadata contains the raw source `Data`, an optional display suffix, and the
  view-conversion call-site `SourceInfo`.
- `viewSuffix(interface)` derives the visible view suffix from the viewed channel name when the
  registration did not pass an explicit suffix. AXI4 channels have useful child names, so this
  produces strings such as `$view.ar`, `$view.r`, and `$view.b`. AXI4 Stream registers `$view`
  explicitly because its entire raw interface maps to one elastic endpoint.
- `viewInterfaceRef(interface)` combines the raw source reference and the view suffix. If the raw
  source is `/s_axi` and the viewed child is `ar`, the result is `/s_axi$view.ar` with a
  `View(...)` description. For AXI4 Stream, the explicit suffix produces `/axis$view`. If the raw
  source cannot be resolved as hardware, this helper returns `None` and the old unknown-interface
  fallback remains responsible for reporting the problem.
- `interfaceRef(interface)` is the single lookup used by module graph generation and tracking
  diagnostics. It first tries `directInterfaceRef`; if that fails, it tries `viewInterfaceRef`.
- `interfaceDisplayOwner(interface)` chooses the module to print in tracking diagnostics. For
  recovered views it reports the raw source interface owner, so warnings point back to the user IO
  rather than an internal Chisel view object.
- `interfaceDisplaySourceInfo(interface)` chooses the source location to print in tracking
  diagnostics. For recovered views it uses the `.asLite` or `.asFull` call site, so messages point
  at the code that introduced the viewed endpoint. Ordinary interfaces keep their own source
  information.

The graph construction fallback is still present. If an interface is neither direct hardware nor a
registered view, Chext keeps the old `/???/...` path and emits the old diagnostic.

## Tracking Sanity For Views

Tracking sanity checks originally looked mainly at module IO and wires. Viewed elastic channels
are Scala-side view objects, so misuse of a view could be noticed late by FIRRTL instead of being
reported clearly by Chext tracking.

To improve this, `.asLite` and `.asFull` inform the current `ModuleInfo` when they create a view.
The module records all tracked children of the viewed elastic interface, not only channels that
later happen to be used by components.

This lets tracking report misuse such as:

- a viewed channel that was never marked;
- a viewed channel used as a source more than once;
- a viewed channel used as a sink more than once.

The tracking report now uses the recovered view path as well. For example:

```text
Interface '/s_axi$view.r' is defined by 'chext.amba.Axi4ViewMissingChannelTop...'
Interface '/s_axi$view.ar' is defined by 'chext.amba.Axi4ViewDuplicatedChannelTop...'
```

instead of internal Chisel view names such as `_$$View$$_.s_view_view_1.r` or
`_$$View$$_.view_view_7`.

## Enforced Roles For Viewed Endpoints

Path recovery alone is not enough for viewed interfaces. A normal `chext.elastic.Interface`
computes its declared role from actual IO direction. That works for real elastic IO, but it
deliberately returns `None` for wires and other non-IO objects. DataView-backed elastic channels
fall into the awkward middle: they are not themselves ordinary IO objects, but they may represent
channels of a raw AXI IO port. If Chext asks only the view child for its `declaredRole`, the
checker loses the protocol role and wrong-role mistakes can fall through to Chisel/FIRRTL
direction errors.

Chext handles this by letting `Tracked` carry an optional enforced role:

```text
Tracked.enforceRole(tracked, role)
```

Internally the tracked endpoint stores this in `enforcedRole_`. The mark-time role check uses:

```text
enforcedRole_.getOrElse(declaredRole)
```

An enforced role is checked even when the viewed object has unusual DataView ownership. Without an
enforced role, the old behavior remains: ordinary declared-role mismatches are checked only when
the marking module owns the interface, and interfaces whose declared role is `None` are treated as
role-neutral.

The enforced role is assigned immediately after `.viewAs`, before the view is registered with
`ModuleInfo`. This is intentional:

- `Tracked` owns source/sink marking state, so it is the natural place to store the effective role
  used by marking checks.
- `ModuleInfo` owns graph and diagnostic path recovery, so it should not also carry role policy.
- The AXI protocol cast code is the only layer that knows which viewed channels are request
  channels and which are response channels.

The cast code enforces roles only when the raw source is IO. This mirrors normal
`elastic.Interface.declaredRole`: if the raw source is a wire or other non-IO object, the view is
registered for path recovery and sanity coverage, but no source/sink role is forced. Non-IO views
still participate in "never marked", duplicated source, and duplicated sink sanity checks; they
just do not gain a declared endpoint role.

For raw AXI4 views, the cast code derives one request role and one response role per view:

- raw slave IO viewed from its owning module:
  - request channels `ar`, `aw`, and `w` are `Source`;
  - response channels `r` and `b` are `Sink`.
- raw master IO viewed from its owning module:
  - request channels are `Sink`;
  - response channels are `Source`.
- child IO is viewed from the parent side, so the role is inverted relative to the child module's
  owner-side role.

The request role is computed once for a view and the response role is its inverse. Full AXI4 and
AXI4-Lite then stamp only the channels enabled by the raw configuration. For example, a read-only
configuration stamps only `ar` and `r`; a write-only configuration stamps only `aw`, `w`, and `b`.

For AXI4 Stream views there is only one elastic endpoint. A raw stream slave IO viewed from its
owning module is enforced as `Source`, a raw stream master IO is enforced as `Sink`, and child IO
is inverted when viewed from the parent side.

This changes the failure mode for wrong-role viewed endpoints. For example,
`axi4_view_wrong_role` now fails during Chext tracking when `m_axi$view.ar` is marked as a source
even though the viewed master request channel is a sink from the module's perspective. Previously
that case reached Chisel's lower-level write-direction check.

## Tests

`TrackingDiagnostics_Tb` is the merged diagnostic driver under `src/test/scala/chext/amba`.
It covers default tracking behavior and view-specific behavior in one run.

The default tracking cases include:

- plain elastic source/sink connections;
- null endpoints;
- unused root IO and unused wires;
- repeated source and repeated sink use;
- wrong endpoint roles;
- internal elastic wires;
- fork/join components;
- nested modules and duplicated child-module IO use;
- native AXI4 full elastic interfaces without raw DataView conversion.

Path-related tracking warnings are tested separately by `tracking_path_warning`, which deliberately
creates two components under the same explicit prefix. The other diagnostic cases reject
`tracking/pathChecks` warnings so path noise does not hide the source/sink or view behavior being
checked.

The view-specific cases include:

- AXI4 and AXI4 Stream root IO viewed once;
- repeated AXI4 and AXI4 Stream views;
- child-module views;
- root-module views of non-IO wires;
- nested raw AXI4 ports viewed and connected;
- viewed channels tied off, unused, used twice, or connected multiple times;
- AXI4 Stream views passed through, tied off, unused, consumed twice, and driven twice;
- wrong-role viewed endpoints caught early through enforced tracking roles.

The driver writes one report per test case:

```text
output/tracking_diagnostics/elastic_connect.txt
output/tracking_diagnostics/axi4_view_missing_channel.txt
...
```

Each report includes the test name, description, emitted SystemVerilog, captured log, errors, and
flattened module graph. Each section starts with a `>` category header and ends with an
80-character `=` separator.

The tests assert that:

- recoverable views use `/raw$view.channel` paths;
- the old `/???/...` fallback does not appear for recoverable views;
- bad view-call patterns warn through Chext logging;
- viewed-channel misuse produces tracking diagnostics before relying only on FIRRTL errors;
- viewed IO endpoints have enforced protocol roles, while viewed non-IO endpoints remain
  role-neutral.
