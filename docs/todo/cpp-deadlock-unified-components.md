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
- named Elastic `sources` and `sinks`, whose interface references carry a `boundary` flag;
- zero or more component `children`.

Elastic tracking enforces this invariant after deferred elaboration has completed:

```text
children.nonEmpty => every source and sink reference has boundary == true
```

Consequently:

- a leaf component may own operational Elastic interfaces;
- a composite component may expose hierarchy-only boundary interfaces but does not claim
  operational ownership of them;
- an empty component with no interfaces and no children is allowed for configuration-dependent or
  diagnostic-only constructions;
- source-only and sink-only leaves remain valid and must participate in deadlock analysis.

An operational reference has `boundary == false`. Registering it marks the Elastic interface and
makes it available to a Scala deadlock monitor. A boundary reference has `boundary == true`; it is
serialized for hierarchy and visualization without marking the interface or entering a deadlock
monitor. Multiple nested composites may therefore refer to the same interface while the innermost
leaf retains operational ownership.

On the Scala side, each registration is represented by `ComponentInterface`, which contains the
component-local `name`, referenced Elastic `interface`, and `boundary` flag. There is no separate
`operationalSources` or `operationalSinks` collection. A deadlock monitor reads the component's
registered `sources` and `sinks` directly and, during deferred elaboration, rejects the component if
any registered interface is a boundary reference.

The children implication is intentionally one-way. A component with no interfaces is not
necessarily a composite because it may also have no children.

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
      "sources": [
        ["source", { "path": "/source", "desc": "IO", "boundary": true }]
      ],
      "sinks": [
        ["sink", { "path": "/sink", "desc": "IO", "boundary": true }]
      ],
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
        ["source", { "path": "/source", "desc": "IO", "boundary": false }]
      ],
      "sinks": [
        ["sink", { "path": "/loop0_current", "desc": "Wire", "boundary": false }]
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

Chext currently emits boundary references for the Elastic composites `Repeat`, `Fold`, `Loop`,
`Scope`, `Switch`, and `RandomStall`. AXI4 Full and AXI4-Lite `Connect` components expose each
available master/slave channel as a boundary reference. Their leaf descendants still provide the
non-boundary references used for deadlock resolution.

Composite-owned wires used as user extension points are serialized as boundary references too.
This includes the Scope, Loop, and Fold body interfaces and the per-branch input/output pairs in
Switch. C++ consumers must therefore allow boundary references to wires as well as module IO.

Paths remain absolute within the emitted/flattened graph. A top-level component currently uses the
empty string for `parent`, matching the existing module-graph convention.

## C++ JSON migration

In `common/include/chext_test/tracking/json.hpp`:

1. Add `bool boundary` to `json::InterfaceRef` and its Boost description.
2. Add `std::vector<std::string> children` to `json::Component`.
3. Add `children` to `BOOST_DESCRIBE_STRUCT(Component, ...)`.
4. Remove `json::Container` and its Boost description.
5. Remove `containers` from `json::Module` and its Boost description.

The resulting reference shape is:

```cpp
struct InterfaceRef {
    std::string path;
    std::string desc;
    bool boundary;
};
```

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
7. Retain every source and sink reference on its component, including boundary references.
8. Resolve operational Elastic interface ownership only from references with `boundary == false`.
   An interface may have multiple boundary references but at most one operational endpoint in each
   direction.
9. Resolve both `parent` and `children` references after every component exists.

Because ancestry is serialized in both directions, the loader must validate it rather than
silently choosing one representation. Reject:

- unknown parent or child paths;
- a child with multiple parents;
- disagreement between `parent` and a parent's `children` list;
- duplicate child paths;
- self-parenting;
- ancestry cycles;
- a component with nonempty `children` and any source or sink reference whose `boundary` flag is
  false;
- more than one operational source or sink endpoint for an interface.

After loading, every relationship must satisfy both:

```text
parent.children contains child
child.parent == parent
```

## Deadlock algorithm

The C++ tracking graph must retain every component and every boundary reference. The deadlock graph
must still contain runtime monitor instances, not every tracking component.

The current algorithm already has the correct basic structure:

- SystemVerilog monitor registration looks up its tracking component by path and inserts a
  `Monitor` into `deadlock::Module::monitors_`;
- `deadlock::Module::checkCalled()` iterates `monitors_`, follows each monitored component's
  operational interface references, resolves the opposite operational leaf endpoint, and maps it
  through `componentToMonitor_`;
- `MonitorCycleFinder` starts from the registered monitors.

Therefore composite components need no synthetic deadlock vertex. Their boundary ports have no
stall signals, and their leaf descendants already own the actual interface dependencies. Adding a
composite vertex would add hierarchy to a protocol wait-for graph where it does not represent an
observable wait condition.

The required separation is:

```text
tracking and ancestry:       retain every component and every port reference
operational endpoint owner:  resolve the non-boundary leaf reference
deadlock vertices:           retain components with registered runtime monitors
```

Do not filter components while parsing the tracking graph, because doing so removes ancestry and
display information. When following an interface for deadlock analysis, ignore boundary references
and select its operational endpoint. That endpoint must be a leaf (`children.empty()`), but monitor
lookup remains decisive: a leaf without a registered runtime monitor contributes no deadlock
neighbor. Source-only and sink-only monitored leaves remain valid.

`MonitorCycleFinder` needs no structural change because it already operates on registered monitors.
`deadlock::Module::checkCalled()` must use the operational endpoint resolver rather than treating a
container boundary reference as interface ownership. The important compatibility requirement is
that `tracking::Module::findComponent()` continues to find every unified component, so
`sv_register()` can associate an HDL monitor with the corresponding leaf.

Optionally, the C++ side may reject registration of a monitor for a component with no operational
interface references. This is a diagnostic guard, not the mechanism used to select deadlock
vertices.

## Porting and validation checklist

1. Update JSON structs and parsing for unified components.
2. Merge the C++ `Container` runtime object into `Component`.
3. Resolve and cross-check `parent` and `children`.
4. Retain boundary references while assigning interface ownership only to operational references.
5. Preserve all components in `findComponent()`.
6. Resolve deadlock neighbors to operational leaves and keep traversal based on registered
   monitors.
7. Add loader tests for nested ancestry, conflicting ancestry, unknown paths, duplicate children,
   hierarchy cycles, operational ports on composite components, nested boundary references, and
   duplicate operational endpoints.
8. Add deadlock tests showing that:
   - inserting one or more portless ancestors does not change a detected leaf-level cycle;
   - inserting one or more boundary-port ancestors does not change a detected leaf-level cycle;
   - a source-only or sink-only monitored component is not filtered;
   - boundary ancestors appear in tracking/diagnostic output but not in the monitor cycle graph.
9. Regenerate the Arteris Interview `*.moduleGraph.json` fixtures using the unified Chext schema.
