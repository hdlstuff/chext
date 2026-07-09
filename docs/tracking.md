# Chext Tracking Model

Chext's tracking model is separate from Chisel's hardware module hierarchy. A Chisel `Module` is still the elaborated hardware boundary, but Chext adds a lighter protocol-level graph inside and across modules. This graph is what the generated `*.moduleGraph.json` files describe.

## Component Model

| Model object | Role | Important behavior |
|---|---|---|
| `BaseComponent` | Common base for graph objects. | Captures the current Chisel prefix as its graph path, registers with the current module's `ModuleInfo`, stores typed arguments, and optionally attaches to the current container. |
| `Component` | A leaf graph object. | Represents one protocol construction such as `Queue`, `Transducer`, `Fork`, `Join`, or `Connect`. It has no tracked children, but layer-specific state can attach ports and metadata to it. |
| `Container` | A hierarchical graph object. | Represents a logical construction made of other components, such as `Repeat`, `Fold`, `Loop`, `Scope`, or `RandomStall`. Components created inside `withContainer(this)` become its children. |
| `ModuleInfo` | Per-Chisel-module tracking record. | Owns the module's components, containers, child modules, unique-prefix counters, arguments, and completion hooks. |
| `HasPath` | Path-bearing mix-in used by graph objects. | Captures `path` / `pathStr` from the active Chisel prefix when the construction is created. |

The generic model deliberately does not know what an elastic source or sink is. Elastic-specific data is attached through a tracking tag/layer.

| Layer concept | Role |
|---|---|
| `chext.elastic.tracking.Tag` | Identifies the elastic tracking state attached to modules and components. |
| `component.trackingState(t.Tag)` | Returns the elastic component state for a component. Elastic components use this state to call `addSourcePort(...)` and `addSinkPort(...)`. |
| `Tracked` elastic interfaces | Elastic `Interface` extends `Tracked`, enabling `markSource`, `markSink`, role checks, and graph interface emission. |
| `DeclaredRole` | Records whether an interface is declared as `Source`, `Sink`, or role-neutral from the current module's perspective. |
| Elastic module graph state | Collects interface declarations, wires, view sources, sanity-check data, and component port references for graph generation. |

## Reading The Graph

A component's path is not derived from its `tpe`; it is derived from the Chisel prefix stack at construction time. The `tpe` says what the thing is, while the path says where it was elaborated. For example, a `Queue` created under `e.SourceBuffer(source)` may have `tpe = "Queue"` but a path containing `sourceBuffer0_queue0`.

The graph can be read at three levels.

| Level | Meaning |
|---|---|
| Chisel module hierarchy | Hardware instance boundaries, including child modules flattened into graph paths when requested. |
| Chext component/container hierarchy | Protocol-level constructions and logical groupings inside a module. |
| Tracking layer payload | Domain-specific facts, currently mostly elastic facts: source/sink interfaces, wires, component ports, and role/sanity diagnostics. |

In practice:

- Use `Component` when the construction is a single graph operation with named ports.
- Use `Container` when the construction is a reusable macro-structure that instantiates child components.
- Use ordinary Chisel `Module` when the construction is a hardware boundary. It may contain tracked Chext components, but it is not automatically a Chext `Component`.
- Use helper functions/objects such as `e.SourceBuffer`, `e.SinkBuffer`, `e.Zip`, and `:=>` operators when the API is meant to create underlying components anonymously or as part of a larger expression.

## Naming Model

| Concept | What it means | Naming / graph behavior |
|---|---|---|
| `tpe` | User-facing component/container type name. | Emitted as `"tpe"` in module graph JSON, for example `"Queue"`, `"Transducer"`, `"Repeat"`. |
| `namePrefix` | Default conceptual prefix for a construction. | Mostly advisory today. Actual graph path comes from Chisel prefix/variable names and helper prefixes; most classes still define it consistently. |
| `uniquePrefix(name) { ... }` | Generates a Chisel prefix unique in the current module for a base name. | Produces names like `connect0`, `connectMany0`, `sourceBuffer0`, `sinkBuffer0`; nested prefixes concatenate with underscores. `Buffered` helpers deliberately rely on the assigned Scala `val` name, and their sequence overloads add only numeric element prefixes. |
| `prefix("x") { ... }` | Chisel prefix scope. | Components created inside receive paths such as `/x_transform0`, `/read_ar_transform0`, etc. |
| Sequence helper prefixes | Helpers over `Seq` usually wrap each element in a unique `...Many` prefix and then `prefix(index.toString)`. | Example: `sources :=> sinks` creates `Connect` components under `/connectMany0_0_connect0`, `/connectMany0_1_connect0`, unless surrounding Scala names/prefixes alter the path. |
| Elastic graph ports | Elastic components call `addSourcePort` / `addSinkPort` through `trackingState(t.Tag)`. | Component entries include named ports such as `source`, `sink`, `source_0`, `sink_1`, `sourceSelect`, `sinkSelect`. |
| Interfaces | `e.Source`, `e.Sink`, `e.EWire`, and generated bridge wires are graph interfaces. | Emitted in module graph `sources`, `sinks`, or `wires` with type like `chext.elastic.Interface[UInt<16>]`. |
