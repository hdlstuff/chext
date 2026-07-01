# AXI4 DataView Tracking Notes

This note records the changes made around Chisel `DataView` usage for raw AXI4 and AXI4
Stream interfaces.

## Background

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

AXI4 and AXI4 Stream raw interfaces now record view calls locally on the raw interface object.
There is no global registry for this. Each recorded call stores:

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

Before this change, module graph generation treated viewed elastic channels as neither IO nor
wire, which produced paths such as:

```text
/???/view.ar
```

and logged:

```text
Encountered an interface which is neither an IO or Wire.
Maybe you used a view? Please do not use views.
```

For recoverable views, module graph generation now resolves the raw interface and emits a stable
view path:

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
  AXI4 Stream passes `$view` because the whole stream is a single elastic endpoint. The raw source
  info is also stored so tracking warnings point at the user IO, not the internal view bundle.
- `directInterfaceRef(interface)` handles only real hardware objects. Local wires become
  `/wireName`, root IO becomes `/ioName`, and child IO becomes `/childInstance/ioName`. It
  intentionally returns `None` for a DataView-created interface because the view is not itself a
  wire or IO.
- `viewSource(interface)` looks up whether a tracked elastic interface came from a registered raw
  view. The returned metadata contains the raw source `Data`, an optional display suffix, and the
  raw source `SourceInfo`.
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
  diagnostics. For recovered views it uses the raw source interface location, so messages point at
  the user's `IO(...)` or `Wire(...)` declaration instead of the DataView mapping code. Ordinary
  interfaces keep their own source information.

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
- downstream wrong-role errors.

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
- viewed-channel misuse produces tracking diagnostics before relying only on FIRRTL errors.
