# AXI4-Full Resolver and Shape-Property Flow

This document specifies the intended property flow for every resolver-owning
AXI4-Full component. The broader tracking and resolution model is described in
[AXI4 Tracking Design](axi4-tracking.md).

## Conventions

For a module with `s_axi` and `m_axi` ports:

- `s_axi` is the upstream-facing slave port.
- `m_axi` is the downstream-facing master port.
- Master properties describe traffic that is generated. They normally flow
  from `s_axi.masterProps` to `m_axi.masterProps`.
- Slave properties describe traffic that is accepted. They normally flow in
  the opposite direction, from `m_axi.slaveProps` to `s_axi.slaveProps`.
- Forwarding always stays within one property family. A master property is
  forwarded to the same master key, and likewise for a slave property.

All read and write rules below apply independently. A property belonging to a
direction disabled by `axi4.Config` is `Undefined`.

The rules describe shape-property resolution. Existing traffic-property
behavior is retained where it is called out explicitly. A residual `DontCare`
means that the component deliberately leaves an aggregate value unspecified;
it does not discard a property that is forwarded to a later compatibility
check.

## Properties

The standard shape properties are:

- maximum burst length in beats;
- whether narrow bursts may be generated or accepted;
- the set of burst types;
- the set of transfer sizes; and
- `ThreadMode`.

Outstanding-transaction and numeric thread-count properties remain traffic
properties and are outside the main scope of this work.

### Shape-property marker

Add a sealed `ShapeProperty` marker in the same style as `MasterProperty`,
`SlaveProperty`, `ReadProperty`, and `WriteProperty`. Define the four
shape-specific key bases (`MasterReadShapeProperty`, `MasterWriteShapeProperty`,
`SlaveReadShapeProperty`, and `SlaveWriteShapeProperty`) beside the sealed
marker, and make every standard shape key extend the applicable base. This
keeps extension of the sealed marker in its defining source file.

`ShapeProperty()` is the common extractor used by generic resolver policies;
the existing `Master.ShapeProperty()` and `Slave.ShapeProperty()` extractors may
remain as family-specific conveniences.

### ThreadMode

`ThreadMode` is one value type used by four directional property keys:

- `Master.ReadThreadMode` and `Master.WriteThreadMode`;
- `Slave.ReadThreadMode` and `Slave.WriteThreadMode`.

Its values describe simultaneously outstanding transactions:

- `SingleTransaction`: at most one transaction is outstanding. Its ID is not
  otherwise constrained.
- `SingleThread`: multiple transactions may be outstanding, but they all have
  the same ID. A new ID may be used after the outstanding set becomes empty.
- `UniqueThreads`: simultaneously outstanding transactions have distinct IDs.
  An ID may be reused after its earlier transaction completes.
- `Unconstrained`: neither sharing nor uniqueness is guaranteed.

Read and write modes are independent. For reads, a transaction is outstanding
from the AR handshake through the final R handshake. For writes, it is
outstanding from the AW handshake through the B handshake.

The compatibility rules are:

| Master mode | Compatible slave modes |
|---|---|
| `SingleTransaction` | any mode |
| `SingleThread` | `SingleThread`, `Unconstrained` |
| `UniqueThreads` | `UniqueThreads`, `Unconstrained` |
| `Unconstrained` | `Unconstrained` |

`SingleThread` and `UniqueThreads` are intentionally incomparable.

### Transfer-size notation

For a data width `W` in bits, define:

- `fullSize(W) = log2(W / 8)`;
- `validSizes(W) = { 0, ..., fullSize(W) }`.

When a resolver calculates `BurstSizes`, it also calculates `BurstNarrow`
relative to the destination interface: the narrow flag is true exactly when
the calculated size set contains a value below that interface's `fullSize`.

## AXI4-Full resolvers

### Buffer

`Buffer` does not change transaction shape or addressing:

- Master properties flow unchanged from the upstream `master` interface to the
  downstream `slave` interface.
- Slave properties, including `MemoryMap`, flow unchanged from `slave` to
  `master`.
- This remains a transparent policy for traffic and extension properties too;
  there is no `DontCare` or `Incomplete` fallback for a bound property.

### Connect

`Connect` is transparent after its configuration checks and optional connection
adaptations:

- Master properties flow unchanged from the connected master interface to the
  connected slave interface.
- Slave properties, including `MemoryMap`, flow unchanged from the slave
  interface to the master interface.
- Traffic and extension properties retain the same transparent behavior.

### CreditBuffer

Credit management changes when requests may proceed but not their shape:

- Every master shape property, including both thread modes, flows unchanged
  from `s_axi` to `m_axi`.
- Every slave shape property flows unchanged from `m_axi` to `s_axi`.
- `Slave.MemoryMap` flows from `m_axi` to `s_axi`.
- Existing non-shape traffic requests remain `Incomplete`; their values must
  eventually account for the configured R, W, and B capacities.

### Demux

- Every master property on each `m_axi(i)` is forwarded from the same master
  property on `s_axi`. In particular, master shape and `ThreadMode` values are
  copied to every output before each output is checked against its connected
  downstream slave.
- No `Slave.ThreadMode` is enforced on `s_axi`. It remains covered by the
  existing slave-side `DontCare`, because the relevant constraint is checked
  after master-property propagation at the selected downstream path.
- `Slave.ReadOutstandingTransactions`, `Slave.WriteOutstandingTransactions`,
  `Slave.ReadThreads`, and `Slave.WriteThreads` retain their existing locally
  enforced tracker-capacity values.
- `Slave.MemoryMap` remains `Incomplete` because an address-function demux does
  not synthesize an aggregate map.
- All other slave properties remain `DontCare`; downstream capabilities stay
  separate rather than being aggregated at `s_axi`.

### DemuxMm

- Master properties, including `ThreadMode`, flow from `s_axi` to every
  `m_axi(i)` exactly as for `Demux`.
- No slave thread mode is enforced; non-memory-map slave shape properties stay
  `DontCare` and are checked downstream after propagation.
- `genDecoder()` resolves every downstream `Slave.MemoryMap`, aggregates the
  non-error-port maps in routing order, and enforces the resulting map on
  `s_axi`. Resolving it before `genDecoder()` is an error.
- The existing local outstanding-transaction and numeric-thread tracker
  capacities remain enforced. Other slave properties remain `DontCare`.

The future `uniqueThreads` optimization may remove the transaction tracker, but
must preserve this externally visible property flow and enforce whatever input
thread mode makes tracker removal safe.

### Downscale

Let `S = fullSize(s_axi.wData)`, `M = fullSize(m_axi.wData)`, where `S > M`.
For one accepted input transfer size `x`, the emitted shape is:

- output size `f(x) = min(x, M)`;
- output beats `e(x) = 2 ^ max(0, x - M)`;
- output burst type `INCR`.

The resolver therefore behaves as follows:

- Enforce `SingleThread` on the read/write slave thread modes of `s_axi` and on
  the read/write master thread modes of `m_axi`; both interfaces have `wId == 0`.
- Enforce slave `BurstBeats = 1` on `s_axi`.
- Enforce master `BurstTypes = { INCR }` on `m_axi`.
- Calculate master `BurstSizes` as `inputSizes.map(f)` and master `BurstBeats`
  as `max(inputSizes.map(e))`. Calculate master `BurstNarrow` from the mapped
  size set relative to `M`.
- The locally valid single-beat input burst types are `FIXED` and `INCR`;
  single-beat `WRAP` is not a legal AXI burst. Input size `x` is accepted only
  when the downstream slave accepts `f(x)` and at least `e(x)` beats. Slave
  `BurstSizes` is the set of all such `x` in `validSizes(s_axi.wData)`, and
  slave `BurstNarrow` is calculated from that set relative to `S`.
- `Slave.MemoryMap` flows unchanged from `m_axi` to `s_axi`.
- Residual traffic properties stay `Incomplete`.

### IdDemux

`IdDemux` selects `m_axi(i)` with the low `wIdSel` ID bits and removes those bits
from the downstream ID:

- Every master property flows from `s_axi` to every `m_axi(i)`.
- `ThreadMode` is preserved on each output. For a fixed output index, removing
  the selector bits is injective: two distinct input IDs routed to that output
  still have distinct remaining ID bits.
- All slave properties remain `DontCare`; the capabilities and memory maps of
  the downstream ports stay separate and are not aggregated.

### IdMux

`IdMux` prefixes each input ID with its input-port index:

- Every slave property, including `MemoryMap` and slave `ThreadMode`, flows from
  `m_axi` to every `s_axi(i)`.
- With one input, master `ThreadMode` is forwarded from that input. With
  multiple inputs, master `ThreadMode` is conservatively `Unconstrained`:
  transactions within one input may share an ID while transactions from other
  inputs receive different prefixed IDs.
- Other master properties retain the existing `DontCare` result because the
  resolver does not yet aggregate the separate upstream traffic descriptions.

### IdParallelize

- Enforce slave `ThreadMode = SingleThread` on `s_axi`, whose ID width is zero.
- Enforce master `ThreadMode = UniqueThreads` on `m_axi`. A fresh ID is assigned
  to every accepted transaction and is not reused while transactions remain
  outstanding.
- All other master shape properties flow unchanged from `s_axi` to `m_axi`.
- All other slave shape properties and `MemoryMap` flow unchanged from `m_axi`
  to `s_axi`.
- Residual traffic properties stay `Incomplete`.

### IdSerialize

- Enforce slave `ThreadMode = Unconstrained` on `s_axi`; the component accepts
  arbitrary input IDs and records them for response restoration.
- Enforce master `ThreadMode = SingleThread` on `m_axi`; every downstream
  transaction uses ID zero.
- All other master shape properties flow unchanged from `s_axi` to `m_axi`.
- All other slave shape properties and `MemoryMap` flow unchanged from `m_axi`
  to `s_axi`.
- Residual traffic properties stay `Incomplete`.

### LiteConverter

The Full-side interface is ID-free and accepts only naturally aligned,
full-width transfers. Its shape is authoritative rather than forwarded:

- On `s_axi`, enforce `ThreadMode = SingleThread`, protocol maximum
  `BurstBeats`, `BurstNarrow = false`, `BurstTypes = { FIXED, INCR, WRAP }`, and
  `BurstSizes = { fullSize(s_axi.wData) }`.
- On `m_axil`, enforce `ThreadMode = SingleThread`, `BurstBeats = 1`,
  `BurstNarrow = false`, `BurstTypes = { INCR }`, and
  `BurstSizes = { fullSize(m_axil.wData) }`.
- Keep the existing configured outstanding-transaction and numeric-thread
  values on both sides.
- `Slave.MemoryMap` and any other non-enforced slave property flow from
  `m_axil` to `s_axi`; non-enforced master properties flow from `s_axi` to
  `m_axil`.

### Mux

- Every slave property, including `MemoryMap` and slave `ThreadMode`, flows from
  `m_axi` to every `s_axi(i)`.
- With one input, master `ThreadMode` is forwarded from that input. With
  multiple inputs, enforce master `ThreadMode = Unconstrained`; combining
  streams can produce repeated IDs within one input and different IDs between
  inputs.
- Other master properties retain the existing `DontCare` result. This is
  deliberate until union/max aggregation of the separate input shape
  properties is implemented.

### ProtocolConverter

The externally visible result is the composition of the selected internal
stages, not an independent approximation in the outer resolver:

- In passthrough mode, master shape flows unchanged from `s_axi` to `m_axi`,
  and slave shape plus `MemoryMap` flows unchanged from `m_axi` to `s_axi`.
- Otherwise, master properties are composed in this order:
  `IdDemux`, optional `IdSerialize`, optional `Upscale`, optional first
  `Unburst`, optional `Downscale`, optional second `Unburst`, and `IdMux`.
- Slave properties are composed through the same stages in reverse order.
- Each stage applies the rules in its section above or below. For example, a
  selected `Unburst` forces one-beat INCR output, while a selected ID serializer
  forces `SingleThread` output.
- `Slave.MemoryMap` ultimately flows from external `m_axi` to external `s_axi`.
- Traffic properties for which a selected stage has no implemented rule remain
  `Incomplete`; the outer resolver must not replace a resolvable stage result
  with a blanket `Incomplete`.

### ConstantSlave

- For each enabled direction, enforce `ThreadMode = SingleTransaction`, because
  the implementation services at most one transaction at a time.
- Retain the existing authoritative shape: protocol maximum `BurstBeats`,
  `BurstNarrow = true`, all three burst types, and every size in
  `validSizes(s_axi.wData)`.
- Retain the existing outstanding and numeric-thread capacity of one.
- `ZeroSlave`, and a constant slave returning `OKAY` or `EXOKAY`, provide a
  whole-address-space `MemoryMap`. `ErrorSlave` (`SLVERR` or `DECERR`) leaves
  `MemoryMap` `Undefined`.
- Any remaining slave property is `Undefined`; there is no downstream flow.

### StallSlave

- Retain zero outstanding capacity, zero numeric threads, zero burst beats,
  `BurstNarrow = false`, and empty burst-type and transfer-size sets.
- Use slave `ThreadMode = Unconstrained`: ID shape adds no restriction because
  the slave accepts no transaction; the zero traffic capacity is represented
  by the traffic properties.
- `MemoryMap` and any remaining property are `Undefined`.

### IdleMaster

- Retain zero outstanding transactions, zero numeric threads, zero burst beats,
  `BurstNarrow = false`, and empty burst-type and transfer-size sets.
- Use master `ThreadMode = SingleTransaction`. An idle stream satisfies the
  strongest master guarantee and is therefore compatible with every slave
  thread mode.
- Any remaining master property is `Undefined`; there is no upstream source.

### Unburst

- Enforce slave `ThreadMode = SingleThread` on `s_axi` and master
  `ThreadMode = SingleThread` on `m_axi`; the component is ID-free and may have
  several same-ID transactions outstanding.
- On `m_axi`, enforce `BurstBeats = 1` and `BurstTypes = { INCR }`.
- Master `BurstSizes` flows from `s_axi` to `m_axi`; master `BurstNarrow` is
  unchanged because the data width and transfer size do not change.
- If the downstream slave accepts one-beat INCR transfers, `s_axi` may accept
  the protocol maximum burst length and all legal input burst types. Accepted
  slave transfer sizes are the downstream accepted sizes, and slave narrow
  support follows those sizes. If one-beat INCR is not accepted downstream,
  the corresponding accepted input sets are empty.
- `Slave.MemoryMap` flows from `m_axi` to `s_axi`.
- Residual traffic properties stay `Incomplete`.

### Upscale

Let `S = fullSize(s_axi.wData)` and `M = fullSize(m_axi.wData)`, where `S < M`.
The address-channel fields are otherwise unchanged:

- Enforce `SingleThread` on both sides because both interfaces have `wId == 0`.
- Master `BurstBeats`, `BurstTypes`, and `BurstSizes` flow unchanged from
  `s_axi` to `m_axi`.
- Calculate master `BurstNarrow` relative to `M`. Every nonempty valid output
  size set is narrow because its largest possible value is `S < M`.
- In the slave direction, burst length and type flow unchanged from `m_axi` to
  `s_axi`. Accepted sizes are downstream accepted sizes intersected with
  `validSizes(s_axi.wData)`; because every emitted transfer is narrow on
  `m_axi`, the downstream slave must support narrow transfers for this set to
  be usable. Calculate slave `BurstNarrow` from the accepted set relative to
  `S`.
- `Slave.MemoryMap` flows from `m_axi` to `s_axi`.
- Residual traffic properties stay `Incomplete`.

### Widen

Let `F = fullSize(axiCfg.wData)`. `Widen` maps one input transaction to one
output transaction, preserves its ID, and makes every output beat full-width:

- Master and slave `ThreadMode` flow unchanged through the component.
- On `m_axi`, enforce `BurstSizes = { F }` and `BurstNarrow = false`.
- Master burst types flow from `s_axi` to `m_axi`, subject to the local rule that
  `FIXED` is unsupported.
- For an input burst of at most `N` beats and size `x`, the worst-case number of
  output beats over all legal starting alignments is
  `g(N, x) = ceil((N * 2^x + (2^F - 2^x)) / 2^F)`. Master `BurstBeats` is the
  maximum `g(N, x)` over the input master size set.
- In the slave direction, accepted burst types are the downstream accepted
  types intersected with `{ INCR, WRAP }`. If the downstream accepts size `F`,
  the component can accept input sizes in `validSizes(axiCfg.wData)`; otherwise
  the accepted size set is empty. Calculate slave narrow support from that set.
- Slave `BurstBeats` is the largest input maximum `N` for which `g(N, x)` does
  not exceed the downstream slave maximum for every accepted input size `x`.
- `Slave.MemoryMap` flows from `m_axi` to `s_axi`.
- Residual traffic properties stay `Incomplete`.
