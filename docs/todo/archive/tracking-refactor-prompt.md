# Tracking Refactor Prompt

> **Status:** Archived. The generic hierarchy and Elastic tracking state have
> been separated and typed state lookup is implemented. The proposed
> `layers/elastic/...` graph layout is not the current graph contract. This
> prompt is retained as design history, not as active work. See the current
> [tracking model](../../tracking.md) and
> [Elastic tracking model](../../tracking-elastic.md).

We are working in the Scala/Chisel repo `chext`.

## Goal

Refactor the tracking system so Elastic-specific tracking concepts move out of `chext.tracking` and into `chext.tracking.elastic`, while keeping the API ergonomic for the closed-world use case where the important tracking domains are Elastic and AXI.

## High-Level Story

Originally `chext.tracking` contained hierarchy tracking plus Elastic-specific ideas: `Tracked`, source/sink ports, wires, views, sanity checks, `RigidComponent`, `DeclaredRole`, and utility functions.

## Current Desired Architecture

`chext.tracking` owns generic hierarchy:

- `BaseComponent`
- `Component`
- `Container`
- `ModuleInfo`
- `Graph`
- path handling
- container hierarchy
- module hierarchy
- module graph generation

`chext.elastic.tracking` owns Elastic-specific tracking, we call this a Layer:

- `Tracked`
- Elastic source/sink registration
- view registration
- Elastic interface sanity checks
- Elastic graph payloads, which is an entry under `layers`.
- `DeclaredRole`
- `ViewSource`
- Elastic module-side tracking state

Later on, we will have `chext.amba.axi4.tracking`.

Make sure that for each tracking layer, it is first registered as the following:

```scala
// a unique type
trait Layer {
  type ComponentState // assoc with Component
  type ModuleState    // assoc with Module
  type GraphState     // assoc with Graph
}

class ElasticLayer {
  // defines the types
}

implicit val elasticLayerHelper = registerTag[Tag](): LayerHelper[chext.elastic.tracking.Tag]
```

Within a `Component`, access the tracking state through `.trackingState[Tag]`,
which looks for a `LayerHelper` and uses it to return the correct type.

The `LayerHelper` should allow cheap state lookup given `ModuleInfo`,
`Component`, or `Graph`, perhaps by retaining an index for direct lookup.

Assume each layer is independent, so there is no need to worry about multiple
implicits.

Calls such as `addSourcePort` should become
`trackingState[t.Tag].addSourcePort`, with
`chext.elastic.tracking` imported as `t`.

The desired endpoint was for `chext.tracking` to manage only hierarchy and
paths, and for `chext.elastic.tracking` to generate the Elastic graph payload.
The prompt also proposed moving graph entries from `/{sources,sinks,wires}` to
`layers/elastic/{sources,sinks,wires}`.
