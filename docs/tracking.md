# Chext Tracking Model

Chext's tracking model is separate from Chisel's hardware module hierarchy. A Chisel `Module` is still the elaborated hardware boundary, but Chext adds a lighter protocol-level graph inside and across modules. This graph is what the generated `*.moduleGraph.json` files describe.

## Component Model

| Model object | Role | Important behavior |
|---|---|---|
| `Component` | Unified protocol graph object. | Captures the current Chisel prefix as its graph path, stores typed arguments and layer-specific state, and may own child components created inside `withComponent(this)`. |
| `ModuleInfo` | Per-Chisel-module tracking record. | Owns the module's components, child modules, active component-parent stack, unique-prefix counters, arguments, and completion hooks. |
| `HasPath` | Path-bearing mix-in used by graph objects. | Captures `path` / `pathStr` from the active Chisel prefix when the construction is created. |

The generic model deliberately does not know what an elastic source or sink is. Elastic-specific data is attached through a tracking tag/layer.

| Layer concept | Role |
|---|---|
| `chext.elastic.tracking.Tag` | Identifies the elastic tracking state attached to modules and components. |
| `component.trackingState(t.Tag)` | Returns the elastic component state for a component. Elastic components use this state to call `addSource(...)` and `addSink(...)`. |
| `Tracked` elastic interfaces | Elastic `Interface` extends `Tracked`, enabling `markSource`, `markSink`, role checks, and graph interface emission. |
| `DeclaredRole` | Records whether an interface is declared as `Source`, `Sink`, or role-neutral from the current module's perspective. |
| Elastic module graph state | Collects interface declarations, wires, view sources, sanity-check data, and component port references for graph generation. |

## Module Lifecycle and Registry Cleanup

The tracking manager keeps an identity-based registry from each active Chisel module to its
`ModuleInfo`. Registering a module also ensures that its ancestors are registered, links its
`ModuleInfo` into its parent's child map, and installs a callback for the end of the module body.

At module-body completion, `ModuleInfo` performs its work in this order:

1. Run generic path and naming checks.
2. Call `onComplete()` for every registered tracking layer.
3. Run user-provided completion callbacks.
4. Remove the module's direct children from the manager registry.
5. If the module is the root, remove the module itself from the registry.

Unregistering a module only removes the global registry entry. It does not clear the corresponding
`ModuleInfo`, its child map, components, or layer-specific states. The parent therefore retains the
constructed tracking tree until the parent itself becomes unreachable.

For example, consider `Root -> A -> B`:

- When leaf module `B` completes, it remains registered because its parent still needs it.
- When `A` completes, `B` is removed from the global registry, while `A` remains registered.
- When `Root` completes, root-level tracking passes can still traverse `Root -> A -> B` through the
  retained child maps. Only after those passes and completion callbacks finish are `A` and `Root`
  removed from the registry.

This distinction lets a tracking layer wait for root completion without losing descendant metadata.
Such a layer should traverse the retained `ModuleInfo` tree and keep references to the original
module states. Re-registering an already-removed descendant during root completion could instead
create a fresh `ModuleInfo` with fresh layer state.

AXI tracking uses this lifecycle for its exhaustive check. Immediately before invoking `Checker`,
the root AXI `ModuleState` recursively assigns module-instantiation source information across the
retained tree, scanning each tracked parent's emitted instance commands once.

## Reading The Graph

A component's path is not derived from its `tpe`; it is derived from the Chisel prefix stack at construction time. The `tpe` says what the thing is, while the path says where it was elaborated. For example, a `Queue` created under `e.SourceBuffer(source)` may have `tpe = "Queue"` but a path containing `sourceBuffer0_queue0`.

The graph can be read at three levels.

| Level | Meaning |
|---|---|
| Chisel module hierarchy | Hardware instance boundaries, including child modules flattened into graph paths when requested. |
| Chext component hierarchy | Protocol-level constructions and logical groupings inside a module. |
| Tracking layer payload | Domain-specific facts, currently mostly elastic facts: source/sink interfaces, wires, component ports, and role/sanity diagnostics. |

In practice:

- Use `Component` for both individual graph operations and reusable macro-structures.
- Wrap child construction in `withComponent(this)` when a component represents a logical grouping.
- Elastic components with children may expose hierarchy-only boundary source or sink ports, but
  must not register operational ports; their leaf descendants own the actual protocol behavior.
- Use ordinary Chisel `Module` when the construction is a hardware boundary. It may contain tracked Chext components, but it is not automatically a Chext `Component`.
- Use helper functions/objects such as `e.SourceBuffer`, `e.SinkBuffer`, `e.Zip`, and `:=>` operators when the API is meant to create underlying components anonymously or as part of a larger expression.

## Naming Model

For the overall project vocabulary and naming shapes, see
[Naming Conventions](naming.md). For practical guidance on when explicit naming
scopes are necessary, including recommended patterns for reusable and inline
helpers, see [Prefixing Tracked Constructions](prefixes.md).

| Concept | What it means | Naming / graph behavior |
|---|---|---|
| `tpe` | User-facing component type name. | Emitted as `"tpe"` in module graph JSON, for example `"Queue"`, `"Transducer"`, `"Repeat"`. |
| `namePrefix` | Conceptual family prefix for a construction. | At tracking completion, the latest Chisel prefix must start with `namePrefix`, followed by the end of the name, a digit, an uppercase letter, or an underscore. A mismatch emits a `tracking/namePrefixChecks` warning. Actual graph paths still come from Chisel prefix/variable names and helper prefixes. |
| `uniquePrefix(name) { ... }` | Generates a Chisel prefix unique in the current module for a base name. | Produces names like `connect0`, `elasticConnectMany0`, `sourceBuffer0`, `sinkBuffer0`; nested prefixes concatenate with underscores. `Buffered` helpers deliberately rely on the assigned Scala `val` name, and their sequence overloads add only numeric element prefixes. |
| `prefix("x") { ... }` | Chisel prefix scope. | Components created inside receive paths such as `/x_transform0`, `/read_ar_transform0`, etc. |
| Sequence helper prefixes | Helpers over `Seq` usually wrap each element in a unique domain-specific `...Many` prefix and then `prefix(index.toString)`. | Example: `sources :=> sinks` creates `Connect` components under `/connectMany0_0_connect0`, `/connectMany0_1_connect0`, unless surrounding Scala names/prefixes alter the path. |
| Elastic graph ports | Elastic components call `addSource` / `addSink` through `trackingState(t.Tag)`. | Component entries include named ports such as `source`, `sink`, `source_0`, `sink_1`, `sourceSelect`, `sinkSelect`. Passing `boundary = true` records a hierarchy-only reference without marking the interface as an operational endpoint. |
| Interfaces | `e.Source`, `e.Sink`, `e.EWire`, and generated bridge wires are graph interfaces. | Emitted in module graph `sources`, `sinks`, or `wires` with type like `chext.elastic.Interface[UInt<16>]`. |
