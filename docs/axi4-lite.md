# AXI4-Lite Components

This page documents `chext.amba.axi4.lite` interfaces and
`chext.amba.axi4.lite.components`. AXI4-Lite ports are represented as elastic channel interfaces:
`ar`, `r`, `aw`, `w`, and `b`, but the payloads are the Lite subsets of the full AXI channels.

## Imports

```scala
import chisel3._
import chext.amba.axi4
import axi4.{lite => axi4l}
import axi4.Ops._
```

## Configuration and Interfaces

Use `axi4.Config(..., lite = true)` for AXI4-Lite. Lite data width must be 32 or 64 bits. `read`
and `write` can independently remove channel groups.

```scala
val cfg = axi4.Config(
  wAddr = 32,
  wData = 32,
  read = true,
  write = true,
  lite = true
)

val s_axil = IO(axi4l.Slave(cfg))
val m_axil = IO(axi4l.Master(cfg))
```

Lite channel payloads are:

| Channel | Payload |
|---|---|
| `ar` / `aw` | `AddressChannel`: `addr`, `prot`. |
| `r` | `ReadDataChannel`: `data`, `resp`. |
| `w` | `WriteDataChannel`: `data`, `strb`. |
| `b` | `WriteResponseChannel`: `resp`. |

## Connections and Buffers

`master :=> slave` creates an `axi4l.Connect` container and connects matching channel groups.
Use `.connect(slave, ConnectConfig(...))` to allow address-width differences with optional
warnings and simulation checks.

```scala
s_axil :=> m_axil

s_axil.connect(
  m_axil,
  axi4l.ConnectConfig(
    tieOffMaster = true,
    tieOffSlave = true,
    warnAddrWidth = true,
    // Disabled by default; select Default, Printf, or Assert to enable it.
    simCheckAddrWidth = chext.util.SimulationCheck.None
  )
)
```

`simCheckAddrWidth` defaults to `SimulationCheck.None`, so address-width differences do not emit
simulation-checking hardware unless the option is explicitly set to `Default`, `Printf`, or
`Assert`. When enabled for a narrowing connection, it checks that the upper `ARADDR` and `AWADDR`
bits are zero while the corresponding channel is valid.

`axi4.BufferConfig` has per-channel queue depths: `aw`, `w`, `b`, `ar`, and `r`.

```scala
val buf = axi4.BufferConfig.all(2)

axi4l.SlaveBuffer(s_axil, buf) :=> m_axil
s_axil :=> axi4l.MasterBuffer(m_axil, buf)

val sBuffered = axi4l.SlaveBuffered(s_axil, buf)
val mBuffered = axi4l.MasterBuffered(m_axil, buf)
sBuffered :=> mBuffered
```

`SlaveBuffer` and `MasterBuffer` add a `uniquePrefix` for inline use. `SlaveBuffered` and
`MasterBuffered` rely on the assigned Scala `val` name instead; their sequence overloads add only
per-element index prefixes.

At the channel level, use `SinkBuffer(..., name = ...)` for inline insertion and
`SinkBuffered(...)` when assigning the result to a named `val`. `SinkBuffered` uses that value for
naming and defaults to a two-entry queue:

```scala
val rBuffered = elastic.SinkBuffered(s_axil.r)
```

## Constant-response slaves

`ConstantSlave` accepts AXI4-Lite transactions, discards writes, and returns a configured constant
for reads. AW and W may arrive in either order. It applies `SlaveBuffered` to the complete interface,
so its two-entry channel buffers, read transform, and write join are visible in the Chext component
graph.

```scala
val constantSlave = Module(
  new axi4l.components.ConstantSlave(
    axiCfg = cfg,
    readData = "h1234".U,
    response = axi4.ResponseFlag.OKAY
  )
)

s_axil :=> constantSlave.s_axil
```

`ZeroSlave` returns zero data with `OKAY`. `ErrorSlave` returns zero data and defaults to `DECERR`;
pass `ResponseFlag.SLVERR` when the mapped slave exists but cannot complete the transaction.

```scala
val zeroSlave = Module(new axi4l.components.ZeroSlave(cfg))
val errorSlave0 = Module(new axi4l.components.ErrorSlave(cfg))
val errorSlave1 = Module(
  new axi4l.components.ErrorSlave(cfg, axi4.ResponseFlag.SLVERR)
)
```

`StallSlave` accepts no requests and produces no responses. `IdleMaster` issues no requests and
keeps its response channels ready. Both use tracked elastic termination components internally.

```scala
val stallSlave = Module(new axi4l.components.StallSlave(cfg))
val idleMaster = Module(new axi4l.components.IdleMaster(cfg))

s_axil :=> stallSlave.s_axil
idleMaster.m_axil :=> m_axil
```

## Routing Components

### `axi4l.components.Demux`

Fans one AXI4-Lite slave-side port out to multiple master-side ports. `decodeFn(addr)` selects the
target for AR and AW; small queues remember the selection so R, W, and B channels follow the
matching address transaction.

```scala
val cfg = axi4l.components.DemuxConfig(
  axiSlaveCfg = axiCfg,
  numMasters = 4,
  decodeFn = addr => addr(13, 12),
  capacityPortQueueR = 8,
  capacityPortQueueW = 8,
  capacityPortQueueB = 8,
  slaveBuffers = axi4.BufferConfig.all(2),
  masterBuffers = axi4.BufferConfig.all(0)
)

val demux = Module(new axi4l.components.Demux(cfg))
s_axil :=> demux.s_axil
demux.m_axil(0) :=> peripheral0
```

### `axi4l.components.Mux`

Merges several AXI4-Lite slave-side ports into one master-side port. Address channels are
arbitrated; selection queues route read and write responses back to the initiating slave.

```scala
val cfg = axi4l.components.MuxConfig(
  axiSlaveCfg = axiCfg,
  numSlaves = 4,
  arbiterPolicy = chext.elastic.Chooser.rr
)

val mux = Module(new axi4l.components.Mux(cfg))
client0 :=> mux.s_axil(0)
mux.m_axil :=> interconnect
```

## Credit and Channel Buffers

### `axi4l.components.CreditBuffer`

Delays addresses until local R, W, or B buffering can accept the traffic implied by the request.
A zero depth bypasses that buffer group.

```scala
val cfg = axi4l.components.CreditBufferConfig(
  axiCfg = axiCfg,
  rBuffer = 8,
  wBuffer = 8,
  bBuffer = 8
)

val credit = Module(new axi4l.components.CreditBuffer(cfg))
s_axil :=> credit.s_axi
credit.m_axi :=> m_axil
```

### `ReadResponseBuffer`

Channel-level read buffer. AR is forwarded only when one R-buffer entry can be reserved.

```scala
val rb = Module(new axi4l.components.ReadResponseBuffer(
  axi4l.components.ReadResponseBufferConfig(axiCfg, bufLengthR = 8)
))
```

### `WriteResponseBuffer`

Channel-level write response buffer. AW is forwarded only when one B-buffer entry can be reserved;
W is intentionally separate.

```scala
val wb = Module(new axi4l.components.WriteResponseBuffer(
  axi4l.components.WriteResponseBufferConfig(axiCfg, bufLengthB = 8)
))
```

### `WritePayloadBuffer`

Channel-level write payload buffer. W is buffered and AW is released after the matching W payload
has been accepted locally; B is intentionally separate.

```scala
val wp = Module(new axi4l.components.WritePayloadBuffer(
  axi4l.components.WritePayloadBufferConfig(axiCfg, bufLengthW = 8)
))
```

## Memory Controllers

### `axi4l.components.MemController`

Implements an AXI4-Lite slave backed by a Chisel `Mem`. Addresses are byte-addressed on AXI and
converted to memory element indices using the data width. Writes ignore byte strobes and overwrite
the full word. When `debugEnabled = true`, the module exposes a direct memory debug port whose
addresses are element indices, not AXI byte addresses.

```scala
val mem = Module(new axi4l.components.MemController(
  log2numElements = 10,
  axiCfg = axi4.Config(wAddr = 32, wData = 32, lite = true),
  debugEnabled = false
))

s_axil :=> mem.s_axil
```

### `axi4l.components.SyncReadMemController`

Same external AXI4-Lite shape as `MemController`, but backed by `SyncReadMem`. Read responses are
registered with the synchronous memory latency.

```scala
val mem = Module(new axi4l.components.SyncReadMemController(
  log2numElements = 10,
  axiCfg = axi4.Config(wAddr = 32, wData = 32, lite = true)
))
```

### `MemDebugPort`

Optional debug interface used by both memory controllers when `debugEnabled` is true.

```scala
when(mem.debug.wen) {
  // debug.waddr/debug.raddr are memory element indices.
}
```

## Register Blocks

### `axi4l.components.RegisterBlock`

`RegisterBlock` is a helper object, not a `Module`. It creates an AXI4-Lite slave wire
(`s_axil`) and a matching `cfgAxi`, then maps Chisel registers into an aligned address space.
Reads and writes are accepted by calling `rdOk`, `rdError`, `wrOk`, `wrDiscard`, or `wrError`.

```scala
class RegTop extends Module {
  val regs = new axi4l.components.RegisterBlock(
    wAddr = 16,
    wData = 32,
    wMask = 8
  )

  val s_axil = IO(axi4l.Slave(regs.cfgAxi))
  s_axil :=> regs.s_axil

  val control = RegInit(0.U(32.W))
  val status = Wire(UInt(32.W))
  status := 0x1234.U

  val controlAddr = regs.reg(control, read = true, write = true, desc = "control")
  val statusAddr = regs.reg(status, read = true, write = false, desc = "status")

  when(regs.rdReq) {
    regs.rdOk()
  }

  when(regs.wrReq) {
    regs.wrOk()
  }
}
```

`base(addr)` rebases the next allocation, `nextAddr` returns the next free byte address, `reserve`
skips a region, and `saveRegisterMap(directory, name)` writes a CSV map.

```scala
regs.base(0x40)
val scratch = RegInit(0.U(32.W))
val scratchAddr = regs.reg(scratch, desc = "scratch")
regs.reserve(16, desc = "reserved")
regs.saveRegisterMap("build", "registers")
```
