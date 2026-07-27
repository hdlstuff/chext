package chext.amba.axi4.lite.components

import chext.amba.axi4
import chext.elastic

import chisel3._
import chisel3.util._
import chisel3.experimental.SourceInfo
import chisel3.experimental.BundleLiterals._


class SyncReadMemController(
    val log2numElements: Int,
    val axiCfg: axi4.Config,
    val debugEnabled: Boolean = false
) extends Module
    with chext.AnnotatedModule {
  private val require_ = chext.util.Require.inferred()

  val genData = Bits(axiCfg.wData.W)

  val addrBitLow = log2Ceil(axiCfg.wData) - 3
  val addrBitHigh = addrBitLow + log2numElements - 1

  require_(axiCfg.lite)
  require_(log2numElements > 0)
  require_(addrBitHigh < axiCfg.wAddr)

  /** AXI4-Lite slave interface for reading from and writing to the Synchronous
    * Read Memory.
    *
    * @note
    *   byte-addressed.
    */
  val s_axil = IO(axi4.lite.Slave(axiCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axil)

  private val resolver = new SyncReadMemController_Resolver(this)

  /** Debug port.
    *
    * @note
    *   Addresses the BRAM elements directly, this is different from the
    *   AXI4-Lite convention.
    */
  val debug: MemDebugPort =
    if (debugEnabled) IO(new MemDebugPort(axiCfg.wAddr, axiCfg.wData)) else null

  private val counter = RegInit(0.U(32.W))
  counter := counter + 1.U

  private val ar = elastic.SourceBuffered(s_axil.ar, 4)
  private val r = elastic.SinkBuffered(s_axil.r)

  private val aw = elastic.SourceBuffered(s_axil.aw, 4)
  private val w = elastic.SourceBuffered(s_axil.w, 4)
  private val b = elastic.SinkBuffered(s_axil.b)

  val mem = SyncReadMem(1 << log2numElements, genData)

  private val addrW = aw.$bits.addr(addrBitHigh, addrBitLow)
  private val dataW = w.$bits.data
  private val addrR = ar.$bits.addr(addrBitHigh, addrBitLow)

  // Write logic
  w.nodeq()
  aw.nodeq()
  b.noenq()

  b.$valid := w.$valid && aw.$valid

  when(w.$valid && aw.$valid && b.$ready) {
    w.deq()
    aw.deq()
    b.enq(
      (new axi4.lite.WriteResponseChannel()(axiCfg))
        .Lit(_.resp -> axi4.ResponseFlag.OKAY)
    )

    // no write strobe support
    mem(addrW) := dataW

    if (debugEnabled)
      printf(
        "Write: address = 0x%x, data = 0x%x, counter = %d\n",
        aw.$bits.addr,
        dataW,
        counter
      )
  }

  // Read logic
  r.$bits.resp := axi4.ResponseFlag.OKAY
  r.$bits.data := mem.read(addrR)
  r.$valid := RegNext(ar.$valid)
  ar.$ready := r.$ready

  when(r.$ready && r.$valid) {
    if (debugEnabled)
      printf(
        "Read: address = 0x%x, data = 0x%x, counter = %d\n",
        ar.$bits.addr,
        r.$bits.data,
        counter
      )
  }

  if (debugEnabled) {
    debug.rdata := mem(debug.raddr)

    when(debug.wen) {
      mem(debug.waddr) := debug.wdata
    }
  }
}

private final class SyncReadMemController_Resolver(owner: SyncReadMemController)(implicit
    sourceInfo: SourceInfo
) extends axi4.tracking.Resolver(owner) {
  import axi4.tracking._

  bindSlave(owner.s_axil)

  override def kind: String = "sync-read-memory-controller"

  if (owner.axiCfg.read) {
    owner.s_axil.slaveProps(properties.Slave.ReadThreadMode) =
      values.ThreadMode.SingleThread
  }
  if (owner.axiCfg.write) {
    owner.s_axil.slaveProps(properties.Slave.WriteThreadMode) =
      values.ThreadMode.SingleThread
  }
  owner.s_axil.slaveProps(properties.Slave.MemoryMap) =
    values.MemoryMap(
      size = BigInt(1) << (owner.log2numElements + owner.addrBitLow)
    )

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case Request(TrafficProfile()) => request.incomplete()
      case _ =>
        missingCase(request)
    }
}
