# Prefixing Tracked Constructions

Chext components capture the active Chisel prefix when they are constructed.
That prefix becomes their path in the module graph. The graph type
(`tpe`) describes what was constructed; it does not provide the graph object's
identity.

This page explains explicit prefix scopes. For the broader project conventions,
including semantic names, indexed instances, short local names, interfaces,
testbenches, emitters, and configuration objects, see
[Naming Conventions](naming.md).

In ordinary code, a descriptive Scala `val` is usually enough:

```scala
val transform0 = new e.Transform(source, sink) {
  out := in
}
```

The component receives a path ending in `transform0`, which is unique in its
scope and agrees with `Transform.namePrefix`. Add an explicit prefix when a
construction cannot get an equally clear and unique path from such a binding.

The relevant imports are:

```scala
import chisel3.experimental.{SourceInfo, prefix}
import chext.tracking.uniquePrefix
```

## Choosing Between the Prefix Helpers

| Situation | Recommended form |
|---|---|
| One fixed semantic region exists in the surrounding scope | `prefix("read") { ... }` |
| Repeated elements already have stable, distinct names or indexes | `prefix(s"lane$index") { ... }` |
| A reusable helper may be invoked more than once in a module | `uniquePrefix("helperName") { ... }` |
| A tracked construction is created inline without a caller-side `val` | `uniquePrefix("constructionName") { ... }` |
| A helper builds a sequence of anonymous constructions | An outer `uniquePrefix("...Many")`, then an indexed `prefix(...)` per element |

`prefix(name)` uses exactly the supplied scope name. `uniquePrefix(name)` asks
Chext for a name unique within the current module and surrounding prefix, such
as `connect0`, `connect1`, or `sourceBuffer0`.

## When `prefix(...)` Is Highly Recommended

### Separate fixed semantic regions

Use `prefix(...)` when one module contains distinct regions that deliberately
reuse short local names. Read/write paths and implementation phases are common
examples:

```scala
prefix("read") {
  val transform0 = new e.Transform(readSource, readSink) {
    out := in
  }
}

prefix("write") {
  val transform0 = new e.Transform(writeSource, writeSink) {
    out := in
  }
}
```

The resulting paths include `read_transform0` and `write_transform0`. This is a
good fit only when each named region occurs at most once under its parent.

### Identify repeated elements by a stable name or index

Use distinct fixed prefixes when a loop or collection already provides the
identity of each element. The prefix keeps each construction readable even
though its local `val` name is repeated:

```scala
sources.zip(sinks).zipWithIndex.foreach { case ((source, sink), index) =>
  prefix(s"lane$index") {
    val transform0 = new e.Transform(source, sink) {
      out := in
    }
  }
}
```

This produces paths such as `lane0_transform0` and `lane1_transform0`. Prefer a
semantic key over an index when the collection
already has one and that key is a valid, stable Chisel prefix.

### Name caller-selected instances

A helper may accept a caller-supplied name and use `prefix(name)` when the name
is part of the API contract:

```scala
def buildChannel(
    name: String,
    source: e.Interface[UInt],
    sink: e.Interface[UInt]
)(implicit sourceInfo: SourceInfo): Unit =
  prefix(name) {
    val transform0 = new e.Transform(source, sink) {
      out := in
    }
  }
```

The caller must supply a distinct name for every invocation under the same
parent. If the API cannot reasonably enforce that requirement, use
`uniquePrefix(...)` instead.

## When `uniquePrefix(...)` Is Highly Recommended

### Implement reusable construction helpers

A helper's local Scala names repeat every time the helper is called. Give the
helper a unique semantic scope so that callers do not have to coordinate names:

```scala
def connectWithTransform(
    source: e.Interface[UInt],
    sink: e.Interface[UInt]
)(implicit sourceInfo: SourceInfo): Unit =
  uniquePrefix("connectWithTransform") {
    val transform0 = new e.Transform(source, sink) {
      out := in
    }
  }
```

Repeated calls receive scopes such as `connectWithTransform0` and
`connectWithTransform1`. This recommendation also applies to operators and
convenience APIs that construct tracked objects internally.

### Construct something inline

An inline construction has no caller-side `val` from which Chisel can derive a
useful name. The helper that owns the inline construction should provide a
unique prefix:

```scala
source :=> e.SinkBuffer(sink)
```

Chext's `SourceBuffer`, `SinkBuffer`, `LeftBuffer`, and `RightBuffer` follow this
rule. They wrap their implementation in `uniquePrefix(...)` because they are
commonly used inside larger expressions. In contrast, the `SourceBuffered` and
`SinkBuffered` forms are intended to be retained under a descriptive Scala
binding:

```scala
val bufferedSource = e.SourceBuffered(source)
```

### Build a repeatable sequence helper

When an API constructs a whole sequence anonymously, use one unique outer scope
for the operation and a fixed element scope inside it:

```scala
uniquePrefix("transformMany") {
  sources.zip(sinks).zipWithIndex.map { case ((source, sink), index) =>
    prefix(index.toString) {
      val transform0 = new e.Transform(source, sink) {
        out := in
      }
    }
  }
}
```

This separates repeated calls to the sequence helper while keeping element
paths deterministic within each call.

## Common Mistakes

Do not use the same fixed prefix for a helper that can elaborate twice under the
same parent:

```scala
def buildPath(...) = prefix("path") { ... }

buildPath(...)
buildPath(...) // tracked objects can now have duplicate paths
```

Duplicate or empty paths cause Chext tracking warnings and make the generated
graph ambiguous. Use `uniquePrefix("path")`, or make a distinct caller-supplied
name mandatory.

An outer grouping prefix also does not replace a component-family name. The
latest prefix captured by a component must recognizably start with its
`namePrefix`. For example, this anonymous component can be reported as a naming
mismatch:

```scala
prefix("read") {
  new e.Transform(source, sink) {
    out := in
  }
}
```

Keep the component under an appropriately named `val`:

```scala
prefix("read") {
  val transform0 = new e.Transform(source, sink) {
    out := in
  }
}
```

or give the anonymous construction its own family-compatible unique scope:

```scala
prefix("read") {
  uniquePrefix("transform") {
    new e.Transform(source, sink) {
      out := in
    }
  }
}
```

Finally, do not add explicit prefixes mechanically. A directly constructed
component or container with a clear Scala `val`, such as `fold0`, `fork0`, or
`transform0`, normally needs no additional scope. Extra prefixes make graph
paths longer without adding identity.

## Preserve `SourceInfo` in Reusable Helpers

Reusable construction helpers should normally accept an implicit
`chisel3.experimental.SourceInfo`. Chisel creates it at the public helper call
site. Each helper layer then receives and forwards the same implicit value until
a Chext component or interface consumes it:

```scala
import chisel3._
import chisel3.experimental.SourceInfo

import chext.{elastic => e}
import chext.tracking.uniquePrefix

object ConnectWithTransform {
  def apply[T <: Data](
      source: e.Interface[T],
      sink: e.Interface[T]
  )(implicit sourceInfo: SourceInfo): Unit =
    uniquePrefix("connectWithTransform") {
      impl(source, sink) // sourceInfo is forwarded implicitly
    }

  private def impl[T <: Data](
      source: e.Interface[T],
      sink: e.Interface[T]
  )(implicit sourceInfo: SourceInfo): Unit = {
    // e.Transform receives sourceInfo implicitly too.
    val transform0 = new e.Transform(source, sink) {
      out := in
    }
  }
}

// Chisel supplies SourceInfo for this line; callers do not pass it explicitly.
ConnectWithTransform(source, sink)
```

With this pattern, diagnostics for `transform0` refer to the user's invocation
of `ConnectWithTransform(source, sink)`, rather than to the construction line
inside the helper. If another intermediate helper is introduced, give that
method an implicit `SourceInfo` parameter as well; normal Scala implicit
resolution passes the in-scope value onward.

Do not construct a `SourceInfo` manually or replace it with a constant or
unlocatable value. Custom component classes also store their own source
location; follow the constructor pattern used by existing Chext components when
implementing one.
