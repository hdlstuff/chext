# AXI4-Full Components

This page documents the `chext.amba.axi4.full` interfaces and the modules in
`chext.amba.axi4.full.components`. AXI4-Full ports are represented as elastic channel interfaces:
`ar`, `r`, `aw`, `w`, and `b`. The modules here are Chisel modules, not Chext tracking components
themselves; graph visibility comes from their annotated AXI ports and the elastic components they
instantiate internally.

## Imports

```scala
import chisel3._
import chext.amba.axi4
import axi4.{full => axi4f}
import axi4.Ops._
```

## Configuration and Interfaces

`axi4.Config` describes both full and lite AXI. For AXI4-Full, keep `lite = false`; enable or
disable read/write channel groups with `read` and `write`; use `wId`, `wAddr`, and `wData` for the
main widths. `wData` must be a power of two and at least 8.

```scala
val cfg = axi4.Config(
  wId = 4,
  wAddr = 32,
  wData = 128,
  read = true,
  write = true,
  lite = false
)

val s_axi = IO(axi4f.Slave(cfg))
val m_axi = IO(axi4f.Master(cfg))
```

Full channel payloads are:

| Channel | Payload |
|---|---|
| `ar` | `ReadAddressChannel`: `id`, `addr`, `len`, `size`, `burst`, optional sidebands, `user`. |
| `r` | `ReadDataChannel`: `id`, `data`, `resp`, `last`, `user`. |
| `aw` | `WriteAddressChannel`: same address shape as `ar`, with AW user width. |
| `w` | `WriteDataChannel`: `data`, `strb`, `last`, `user`. |
| `b` | `WriteResponseChannel`: `id`, `resp`, `user`. |

Raw standard-named AXI ports can be created with `axi4.Slave(cfg)` / `axi4.Master(cfg)` and viewed
as full interfaces via `axi4.Casts` or `axi4.Ops`.

## Connections and Buffers

`master :=> slave` creates an `axi4f.Connect` container. Strict `:=>` requires compatible fields
and ties off missing read/write channel groups. Use `.connect(slave, ConnectConfig(...))` when
intentional width or channel differences should emit warnings or simulation checks instead of
strict failures.

```scala
s_axi :=> m_axi

s_axi.connect(
  m_axi,
  axi4f.ConnectConfig(
    warnIdWidth = true,
    warnAddrWidth = true,
    warnSideband = true,
    tieOffMaster = true,
    tieOffSlave = true,
    // Disabled by default; select Default, Printf, or Assert to enable it.
    simCheckAddrWidth = chext.util.SimulationCheck.None
  )
)
```

`warnSideband` compares the effective widths of the address-channel `LOCK`, `CACHE`, `PROT`,
`QOS`, and `REGION` fields and all five user fields: `ARUSER`, `RUSER`, `AWUSER`, `WUSER`, and
`BUSER`. The address-channel checks apply to both `AR` and `AW`. This option controls diagnostic
warnings only; configured connections still reject unsupported differences in sideband-presence
flags and user widths. There are no simulation-time upper-bit checks for sideband fields.

Address-width truncation can be checked with `simCheckAddrWidth`. Its default is
`SimulationCheck.None`, so it emits no checking hardware unless explicitly set to `Default`,
`Printf`, or `Assert`. When enabled, it checks the upper `ARADDR` and `AWADDR` bits while the
corresponding channel is valid.

`axi4.BufferConfig` controls per-channel queue depths: `aw`, `w`, `b`, `ar`, and `r`.

```scala
val buf = axi4.BufferConfig(aw = 2, w = 8, b = 2, ar = 2, r = 8)

axi4f.SlaveBuffer(s_axi, buf) :=> m_axi
s_axi :=> axi4f.MasterBuffer(m_axi, buf)

val sBuffered = axi4f.SlaveBuffered(s_axi, buf)
val mBuffered = axi4f.MasterBuffered(m_axi, buf)
sBuffered :=> mBuffered
```

`SlaveBuffer` and `MasterBuffer` add a `uniquePrefix` for inline use. `SlaveBuffered` and
`MasterBuffered` rely on the assigned Scala `val` name instead; their sequence overloads add only
per-element index prefixes.

At the channel level, use `SinkBuffer(..., name = ...)` when inserting a buffer inline and use
`SinkBuffered(...)` when assigning the result to a named `val`. The latter uses that value for
naming and defaults to a two-entry queue, so no explicit `name` or depth is needed:

```scala
val rBuffered = elastic.SinkBuffered(s_axi.r)
```

## Constant-response slaves

`ConstantSlave` accepts AXI transactions, discards writes, and returns a configured constant for
every read beat. It first applies `SlaveBuffered` to the complete interface, then retains request IDs
and generates the requested number of read burst beats. Its implementation is visible in the Chext
graph as two-entry channel buffers, joins, and transducers.

```scala
val constantSlave = Module(
  new axi4f.components.ConstantSlave(
    axiCfg = cfg,
    readData = "h1234".U,
    response = axi4.ResponseFlag.OKAY
  )
)

s_axi :=> constantSlave.s_axi
```

`ZeroSlave` and `ErrorSlave` derive from `ConstantSlave`. `ZeroSlave` returns zero data and `OKAY`;
`ErrorSlave` returns zero data and defaults to `DECERR`; pass `ResponseFlag.SLVERR` when the mapped
slave exists but cannot complete the transaction.

```scala
val zeroSlave = Module(new axi4f.components.ZeroSlave(cfg))
val errorSlave0 = Module(new axi4f.components.ErrorSlave(cfg))
val errorSlave1 = Module(
  new axi4f.components.ErrorSlave(cfg, axi4.ResponseFlag.SLVERR)
)
```

`StallSlave` accepts no requests and produces no responses. `IdleMaster` issues no requests and
keeps its response channels ready. Both use tracked elastic termination components internally.

```scala
val stallSlave = Module(new axi4f.components.StallSlave(cfg))
val idleMaster = Module(new axi4f.components.IdleMaster(cfg))

s_axi :=> stallSlave.s_axi
idleMaster.m_axi :=> m_axi
```

## Routing Components

### `axi4f.components.Demux`

Fans one slave-side AXI port out to `numMasters` master-side ports. `decodeFn(addr)` selects the
target port for AR and AW. The module tracks outstanding IDs so read data and write responses can
return from the correct selected master.

```scala
val cfg = axi4f.components.DemuxConfig(
  axiSlaveCfg = axiCfg,
  numMasters = 4,
  decodeFn = addr => addr(13, 12),
  numIdsTrackedRead = 8,
  numIdsTrackedWrite = 8,
  numOutstandingRead = 16,
  numOutstandingWrite = 16,
  slaveBuffers = axi4.BufferConfig.all(2),
  masterBuffers = axi4.BufferConfig.all(0)
)

val demux = Module(new axi4f.components.Demux(cfg))
s_axi :=> demux.s_axi
demux.m_axi(0) :=> m0_axi
```

### `axi4f.components.DemuxMm`

`DemuxMm` replaces the Chisel `decodeFn` with elastic address decoders generated from hierarchical
`MemoryMap` properties. Assign one map to each master interface and call `genDecoder()` to aggregate
them and generate the private read/write decoders.

```scala
import chext.amba.axi4.tracking.{properties => p, values => v}

val demux = Module(
  new axi4f.components.DemuxMm(
    axi4f.components.DemuxMmConfig(axiCfg, numMasters = childMaps.size)
  )
)

demux.m_axi :=> m_axi

// Properties attached downstream propagate through the AXI Connect components.
m_axi.zip(childMaps).foreach { case (master, childMap) =>
  require(childMap.path.nonEmpty)
  master.properties(p.SlaveMemoryMap) = childMap
}

// Place m_axi(2), then m_axi(0); reserve m_axi(1) for decode errors:
val combinedMemoryMap = demux.genDecoder(
  permutation = Some(Seq(2, 0)),
  errorSlave = Some(1),
  allocationScheme = v.MemoryMap.AllocationScheme.AlignedPacked
)
```

Every `MemoryMap` declares its `size`. `MemoryMap.aggregate` processes maps in the supplied order
using one of three allocation schemes: `AlignedPacked` reserves each child's power-of-two-aligned
extent, `AlignedLargest` gives every child the largest such extent, and `Tight` places declared
extents consecutively. The map itself contains no AXI routing indices; `DemuxMm` privately
associates each address-ordered child with its original `m_axi` index. The resulting combined map is
validated and installed as `demux.s_axi`'s `SlaveMemoryMap`. Validation rejects children or
segments outside their containing map and rejects overlapping direct entries. A child map must
have a non-empty component `path`, and the complete layout must fit in `axiCfg.wAddr`. AXI boundary
buffering is left to the caller.

`errorSlave = Some(index)` excludes that interface from aggregation and routes addresses outside
all mapped segments to it; the error interface therefore does not need a `SlaveMemoryMap`.
The permutation must contain every remaining interface exactly once. Without an error slave,
unmapped addresses select the first interface in address-map order.

DemuxMm instances can be composed hierarchically. Give a non-root instance's generated map a
component path, for example `childDemux.genDecoder(memoryMapPath = Seq("child"))`, before a parent
DemuxMm resolves it. Each AXI full or lite channel buffer is a tracked component that owns its
property resolver and its channel-level elastic children. Buffers transparently forward AXI
tracking properties, so buffered links between the parent and child do not interrupt memory-map
resolution. Each decoder rebases the forwarded `ARADDR` or `AWADDR` by the selected child's map
offset, so every level receives an address relative to its own memory map.
Set `captureResolutionTrace = true` to attach typed `resolutionTrace` arguments to resolved child
maps. Each step records absolute `interfaceFrom` and `interfaceTo` paths, a stable `kind` such as
`connect` or `buffer`, a stable resolver name, and the resolver component or module's absolute
`resolverPath`. Maps and segments also carry an absolute `origin`; `/` is the top module and an
empty origin means that tracking provenance was unavailable. Trace collection is disabled by
default, while origins are still recorded.

Child offsets are relative to their immediate parent. `MemoryMap.flattenOnce` promotes direct-child
segments and grandchildren by adding the child offset and prefixing names with the child's path;
`flatten` repeats this until no children remain. For example, a segment `control` below paths
`Seq("peripheral")` and `Seq("registers")` becomes `/peripheral/registers/control`.

### `axi4f.components.Mux`

Merges `numSlaves` slave-side AXI ports into one master-side port. It appends a port-select field
above the incoming ID bits, so `axiMasterCfg.wId = axiSlaveCfg.wId + log2Ceil(numSlaves)`.

```scala
val cfg = axi4f.components.MuxConfig(
  axiSlaveCfg = axiCfg,
  numSlaves = 4,
  arbiterPolicy = chext.elastic.Chooser.rr
)

val mux = Module(new axi4f.components.Mux(cfg))
client_axi :=> mux.s_axi(0)
mux.m_axi :=> memory_axi
```

### `axi4f.components.IdDemux`

Splits a bus by low ID bits instead of by address. `wIdSel` low bits choose one of
`1 << wIdSel` master ports; those bits are removed from the outgoing ID.

```scala
val cfg = axi4f.components.IdDemuxConfig(
  axiSlaveCfg = axiCfg.copy(wId = 6),
  wIdSel = 2
)

val idDemux = Module(new axi4f.components.IdDemux(cfg))
```

### `axi4f.components.IdMux`

Inverse of `IdDemux`. It accepts `1 << wIdSel` slave-side ports, arbitrates requests, and appends
the selected port index to outgoing IDs so responses can be routed back.

```scala
val cfg = axi4f.components.IdMuxConfig(
  axiSlaveCfg = axiCfg.copy(wId = 4),
  wIdSel = 2
)

val idMux = Module(new axi4f.components.IdMux(cfg))
```

## ID Conversion Components

### `axi4f.components.IdSerialize`

Serializes all transactions to master ID `0`. Read responses and write responses are joined with
saved original IDs before returning to the slave side.

```scala
val cfg = axi4f.components.IdSerializeConfig(
  axiSlaveCfg = axiCfg.copy(wId = 4),
  numOutstandingRead = 8,
  numOutstandingWrite = 8
)

val idSerialize = Module(new axi4f.components.IdSerialize(cfg))
```

### `axi4f.components.IdParallelize`

Accepts an ID-less slave interface (`axiSlaveCfg.wId == 0`) and issues requests on a master
interface with `wIdMaster` generated IDs. Responses are buffered and drained in original request
order.

```scala
val cfg = axi4f.components.IdParallelizeConfig(
  axiSlaveCfg = axi4.Config(wId = 0, wAddr = 32, wData = 64),
  wIdMaster = 3,
  wBufferIndex = 10
)

val idParallelize = Module(new axi4f.components.IdParallelize(cfg))
```

## Width and Burst Conversion

### `axi4f.components.Upscale`

Adapts a narrower slave-side data bus to a wider master-side data bus. It requires `wId == 0`,
AXI4-Full, and `wDataMaster > axiSlaveCfg.wData`.

```scala
val cfg = axi4f.components.UpscaleConfig(
  axiSlaveCfg = axi4.Config(wId = 0, wAddr = 32, wData = 32),
  wDataMaster = 128
)

val upscale = Module(new axi4f.components.Upscale(cfg))
```

### `axi4f.components.Downscale`

Adapts a wider slave-side data bus to a narrower master-side data bus. Its input has two important
traffic preconditions:

- There are no transaction IDs: the configured `wId` must be zero.
- There are no input bursts: every accepted request must have `ARLEN == 0` or `AWLEN == 0`.

It also requires AXI4-Full, `wDataMaster < axiSlaveCfg.wData`, and no R-channel user data.
`Downscale` can turn one wide input beat into a burst of narrower output beats. `simCheckBurst`
controls optional `ARLEN`/`AWLEN` checks for the single-beat precondition. Use `Unburst` before it
when single-beat input is not guaranteed, and after it when the downstream interface accepts only
single-beat transactions.

```scala
val cfg = axi4f.components.DownscaleConfig(
  axiSlaveCfg = axi4.Config(wId = 0, wAddr = 32, wData = 128, wUserR = 0),
  wDataMaster = 32,
  simCheckBurst = chext.util.SimulationCheck.Default
)

val downscale = Module(new axi4f.components.Downscale(cfg))
```

### `axi4f.components.Unburst`

Converts burst transactions into single-beat AXI transactions. It requires `wId == 0`, AXI4-Full,
AR/AW outstanding queue capacities of at least 2, and no B-channel user data.

```scala
val cfg = axi4f.components.UnburstConfig(
  axiCfg = axi4.Config(wId = 0, wAddr = 32, wData = 64),
  numOutstandingRead = 32,
  numOutstandingWrite = 32
)

val unburst = Module(new axi4f.components.Unburst(cfg))
```

### `axi4f.components.Widen`

Converts narrow transfers on an AXI4-Full bus into full-sized transfers on the same bus width.
It preserves the external `axiCfg`; it does not support FIXED bursts.

```scala
val cfg = axi4f.components.WidenConfig(
  axiCfg = axi4.Config(wId = 0, wAddr = 32, wData = 128)
)

val widen = Module(new axi4f.components.Widen(cfg))
```

## Address Stream Generators

These are elastic helper modules, not full AXI bus modules. They are used by the conversion
components and can be used directly when a design needs per-beat address metadata.

### `AddressGenerator`

Consumes `AddrLenSizeBurstBundle` and emits one `AddrSizeLastBundle` per beat. It handles FIXED,
INCR, and WRAP address progression.

```scala
val gen = Module(new axi4f.components.AddressGenerator(wAddr = 32))
source :=> gen.source
gen.sink :=> sink
```

### `StrobeGenerator`

Consumes per-beat address/size/last metadata and emits byte strobe bounds and strobe bits for the
configured data width.

```scala
val gen = Module(new axi4f.components.StrobeGenerator(wAddr = 32, wData = 64))
```

### `AddressStrobeGenerator`

Composes `AddressGenerator` and `StrobeGenerator`.

```scala
val gen = Module(new axi4f.components.AddressStrobeGenerator(wAddr = 32, wData = 64))
```

## Credit and Response Buffers

### `axi4f.components.CreditBuffer`

Delays AR, AW, or W progress until local response/payload buffers can accept the traffic implied
by the request. Set a depth to zero to bypass that channel group.

```scala
val cfg = axi4f.components.CreditBufferConfig(
  axiCfg = axiCfg,
  rBuffer = 32,
  wBuffer = 8,
  bBuffer = 32
)

val credit = Module(new axi4f.components.CreditBuffer(cfg))
```

When enabled, `rBuffer` limits the accepted read burst to the number of locally
buffered R beats. Likewise, `wBuffer` limits the accepted write burst so the
complete W payload can be accepted before AW is released, even if the
downstream interface does not accept W before AW.

### `ReadResponseBuffer`

Channel-level read buffer. AR is forwarded only if the R buffer has room for the whole burst
(`ar.len + 1` beats).

```scala
val rb = Module(new axi4f.components.ReadResponseBuffer(
  axi4f.components.ReadResponseBufferConfig(axiCfg, bufLengthR = 32)
))
```

### `WriteResponseBuffer`

Channel-level write response buffer. AW is forwarded only if one B-buffer slot can be reserved;
W is not part of this component.

```scala
val wb = Module(new axi4f.components.WriteResponseBuffer(
  axi4f.components.WriteResponseBufferConfig(axiCfg, bufLengthB = 8)
))
```

### `WritePayloadBuffer`

Channel-level write payload buffer. W beats are buffered and AW is released after the matching W
burst has been accepted locally; B is not part of this component.

```scala
val wp = Module(new axi4f.components.WritePayloadBuffer(
  axi4f.components.WritePayloadBufferConfig(axiCfg, bufLengthW = 8)
))
```

## Protocol Converter

`axi4f.components.ProtocolConverter` composes the lower-level modules needed to bridge two AXI4-Full
configs. Depending on the slave/master configs it may instantiate ID demuxing, ID serialization,
upscaling, unbursting, downscaling, AXI3-compatible unbursting, and ID muxing. If the configs are
already compatible except for allowed ID widening, it becomes a pass-through.

```scala
val cfg = axi4f.components.ProtocolConverterConfig(
  axiSlaveCfg = axi4.Config(wId = 8, wAddr = 32, wData = 32),
  axiMasterCfg = axi4.Config(wId = 2, wAddr = 32, wData = 128),
  slaveNeverBursts = false
)

val converter = Module(new axi4f.components.ProtocolConverter(cfg))
s_axi :=> converter.s_axi
converter.m_axi :=> m_axi
```

## Lite Converter

`axi4f.components.LiteConverter` terminates a Full AXI interface as an AXI4-Lite master. It can
increase or reduce the data width and decompose bursts. It requires an ID-free Full interface
(`wId == 0`) and does not instantiate `IdSerialize`. Input unbursting always occurs before width
conversion. `Upscale` selects the addressed read-data lane and shifts write data and `WSTRB` into
the corresponding wider lanes. Because `Downscale` can create a narrow burst, only its output needs
a second unburst stage before AXI4-Lite.

```scala
val cfg = axi4f.components.LiteConverterConfig(
  axiSlaveCfg = axi4.Config(wId = 0, wAddr = 32, wData = 64),
  wDataMaster = 32,
  numOutstandingRead = 2,
  numOutstandingWrite = 2,
  simCheckNarrow = chext.util.SimulationCheck.Default,
  simCheckAligned = chext.util.SimulationCheck.Default
)

val converter = Module(new axi4f.components.LiteConverter(cfg))
s_axi :=> converter.s_axi
converter.m_axil :=> registerBlock.s_axil
```

The Full interface must have `wId == 0`, and all AXI user widths must be zero. Input transfers must
use the full Full-side data width and must be naturally aligned to that width. `simCheckNarrow`
checks `ARSIZE`/`AWSIZE`; `simCheckAligned` checks the low address bits of `ARADDR`/`AWADDR`. Both
support `SimulationCheck.Default`, `None`, `Printf`, or `Assert`.

Upscaling is instantiated when the Full data width is narrower than the Lite width; downscaling is
instantiated when it is wider. Equal widths need neither stage. After downscaling, a second internal
unburst stage is always instantiated. Upscaling does not create additional beats and therefore
needs no output unburst stage.

The converter publishes generated-traffic properties on `m_axil` and accepted-traffic properties
on `s_axi`. The downstream `SlaveMemoryMap` property is resolved through the converter to `s_axi`.
