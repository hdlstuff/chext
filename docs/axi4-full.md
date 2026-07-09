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
    tieOffMaster = true,
    tieOffSlave = true
  )
)
```

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
  capacityPortQueueW = 8,
  slaveBuffers = axi4.BufferConfig.all(2),
  masterBuffers = axi4.BufferConfig.all(0)
)

val demux = Module(new axi4f.components.Demux(cfg))
s_axi :=> demux.s_axi
demux.m_axi(0) :=> m0_axi
```

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

Adapts a wider slave-side data bus to a narrower master-side data bus. It requires `wId == 0`,
AXI4-Full, `wDataMaster < axiSlaveCfg.wData`, and no R-channel user data.

```scala
val cfg = axi4f.components.DownscaleConfig(
  axiSlaveCfg = axi4.Config(wId = 0, wAddr = 32, wData = 128, wUserR = 0),
  wDataMaster = 32
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
