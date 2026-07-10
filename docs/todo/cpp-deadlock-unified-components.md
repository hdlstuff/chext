# Porting the C++ Deadlock Detector to Unified Components

## Status and scope

Chext has removed the distinction between tracked `Component` and `Container` objects. A single
component model represents both leaf protocol operations and composite hierarchy nodes.

This note is the migration checklist for the C++ tracking and deadlock-detection code currently in
`/home/janberq/repos/jnbrq/arteris-interview/sysc_tb/bla`. It records the intended JSON contract,
the invariants enforced by Scala-side Elastic tracking, and the corresponding C++ changes.

The `*.hdlinfo.json` schema is unrelated and does not change. This migration applies to
`*.moduleGraph.json`.

## Unified model

Every tracked construction is a component with:

- `path`, `tpe`, `args`, and an optional `parent`;
- named Elastic `sources` and `sinks`;
- zero or more component `children`.

Elastic tracking enforces this invariant after deferred elaboration has completed:

```text
children.nonEmpty => sources.isEmpty && sinks.isEmpty
```

Consequently:

- a leaf component may own Elastic interfaces;
- a composite component owns children and does not also claim its children's boundary interfaces;
- an empty component with no interfaces and no children is allowed for configuration-dependent or
  diagnostic-only constructions;
- source-only and sink-only leaves remain valid and must participate in deadlock analysis.

The implication is intentionally one-way. A component with no interfaces is not necessarily a
composite because it may also have no children.

## JSON schema

The module graph has one `components` collection and no `containers` collection. Both directions
of component ancestry are retained deliberately: each component has `parent`, and each component
also has `children`.

```json
{
  "name": "ExampleTop",
  "path": "/",
  "sources": [],
  "sinks": [],
  "wires": [],
  "components": [
    {
      "path": "/loop0",
      "tpe": "Loop",
      "sources": [],
      "sinks": [],
      "children": [
        "/loop0_scope0",
        "/loop0_connect0"
      ],
      "parent": "",
      "args": {}
    },
    {
      "path": "/loop0_scope0",
      "tpe": "Scope",
      "sources": [],
      "sinks": [],
      "children": [
        "/loop0_scope0_stall0"
      ],
      "parent": "/loop0",
      "args": {}
    },
    {
      "path": "/loop0_scope0_stall0",
      "tpe": "Stall",
      "sources": [
        ["source", { "path": "/source", "desc": "IO" }]
      ],
      "sinks": [
        ["sink", { "path": "/loop0_current", "desc": "Wire" }]
      ],
      "children": [],
      "parent": "/loop0_scope0",
      "args": {}
    }
  ],
  "children": [],
  "args": {}
}
```

The two `children` fields have different scopes:

- `Module.children` contains child Chisel modules;
- `Component.children` contains child Chext components.

Paths remain absolute within the emitted/flattened graph. A top-level component currently uses the
empty string for `parent`, matching the existing module-graph convention.

## C++ JSON migration

In `common/include/chext_test/tracking/json.hpp`:

1. Add `std::vector<std::string> children` to `json::Component`.
2. Add `children` to `BOOST_DESCRIBE_STRUCT(Component, ...)`.
3. Remove `json::Container` and its Boost description.
4. Remove `containers` from `json::Module` and its Boost description.

The resulting component shape is:

```cpp
struct Component {
    std::string path;
    std::string tpe;
    std::vector<NamedInterfaceRef> sources;
    std::vector<NamedInterfaceRef> sinks;
    std::vector<std::string> children;
    std::string parent;
    boost::json::object args;
};
```

## C++ tracking-model migration

In `common/include/chext_test/tracking/tracking.hpp` and
`src/chext_test/tracking/tracking.cpp`:

1. Remove the separate runtime `Container` class.
2. Give `Component` a `Component* parent_` and `std::vector<Component*> children_`.
3. Replace `parentContainer()` with `parent()` and expose `children()`.
4. Remove `containerStore_`, `containers_`, `containerByPath_`, `findContainer()`, and the
   container-specific formatting code.
5. Keep every JSON component in `componentStore_` and `componentByPath_`, including components with
   no sources or sinks. They are required for ancestry, lookup, diagnostics, and visualization.
6. Build all component objects before resolving any links.
7. Resolve Elastic interface ownership from `sources` and `sinks` as before.
8. Resolve both `parent` and `children` references after every component exists.

Because ancestry is serialized in both directions, the loader must validate it rather than
silently choosing one representation. Reject:

- unknown parent or child paths;
- a child with multiple parents;
- disagreement between `parent` and a parent's `children` list;
- duplicate child paths;
- self-parenting;
- ancestry cycles;
- a component with nonempty `children` and nonempty `sources` or `sinks`.

After loading, every relationship must satisfy both:

```text
parent.children contains child
child.parent == parent
```

## Deadlock algorithm

The C++ deadlock graph must contain runtime monitor instances, not every tracking component.

The current algorithm already has the correct basic structure:

- SystemVerilog monitor registration looks up its tracking component by path and inserts a
  `Monitor` into `deadlock::Module::monitors_`;
- `deadlock::Module::checkCalled()` iterates `monitors_`, follows each monitored component's
  interface references, and maps the opposite endpoint through `componentToMonitor_`;
- `MonitorCycleFinder` starts from the registered monitors.

Therefore composite components need no synthetic deadlock vertex. They have no source/sink stall
signals, and their leaf descendants already own the actual interface dependencies. Adding a
composite vertex would add hierarchy to a protocol wait-for graph where it does not represent an
observable wait condition.

The required separation is:

```text
tracking and ancestry: retain every component
deadlock vertices:     retain components with registered runtime monitors
```

It is safe for deadlock-specific code to ignore a component when both `sources` and `sinks` are
empty. Do not apply that filter while parsing the tracking graph, because doing so removes parent
and child nodes. Also do not accidentally filter source-only or sink-only leaves; the predicate is
`sources.empty() && sinks.empty()`, not either collection being empty.

No structural change should be necessary in `deadlock::Module::checkCalled()` or
`MonitorCycleFinder`: both already operate on registered monitors. The important compatibility
requirement is that `tracking::Module::findComponent()` continues to find every unified component,
so `sv_register()` can associate an HDL monitor with the corresponding leaf.

Optionally, the C++ side may reject registration of a monitor for a component with both interface
lists empty. This is a diagnostic guard, not the mechanism used to select deadlock vertices.

## Porting and validation checklist

1. Update JSON structs and parsing for unified components.
2. Merge the C++ `Container` runtime object into `Component`.
3. Resolve and cross-check `parent` and `children`.
4. Preserve all components in `findComponent()`.
5. Leave deadlock traversal based on registered monitors.
6. Add loader tests for nested ancestry, conflicting ancestry, unknown paths, duplicate children,
   hierarchy cycles, and composite components with interfaces.
7. Add deadlock tests showing that:
   - inserting one or more portless ancestors does not change a detected leaf-level cycle;
   - a source-only or sink-only monitored component is not filtered;
   - portless ancestors appear in tracking/diagnostic output but not in the monitor cycle graph.
8. Regenerate the Arteris Interview `*.moduleGraph.json` fixtures using the unified Chext schema.
