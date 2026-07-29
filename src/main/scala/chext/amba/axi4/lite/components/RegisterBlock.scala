package chext.amba.axi4.lite.components

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.hacks.{deferred, PrefixManager}
import chisel3.util._

import chext.amba.axi4
import chext.amba.axi4.tracking.{properties => p}
import chext.amba.axi4.util.MemoryMap
import chext.elastic
import chext.tracking.{Component, withComponent}

/** Defines an AXI4-Lite register block.
  *
  * Register and reserved-region declarations are collected until [[complete]] is called. Completion
  * creates the Elastic request/response logic and makes the block's memory map available to its
  * resolver. `complete()` must be called exactly once.
  *
  * The block assumes data-width-aligned accesses. A mapped value narrower than the data width still
  * occupies one full data-width word. Read-only wires should be registered with `write = false`.
  *
  * @param wAddr
  *   address width of the AXI4-Lite interface
  * @param wData
  *   data width of the AXI4-Lite interface
  * @param wMask
  *   address-space width; the block occupies `2^wMask` bytes
  * @param memoryMapPath
  *   logical path used when this block is aggregated into a memory map; when empty, the latest
  *   component prefix is used
  */
class RegisterBlock(
    val wAddr: Int = 32,
    val wData: Int = 32,
    val wMask: Int = 4,
    val memoryMapPath: Seq[String] = Seq.empty
)(implicit si_ : SourceInfo)
    extends Component {
  val sourceInfo: SourceInfo = si_
  private val require_ = chext.util.Require.inferred(sourceInfo)

  def tpe: String = "Axi4l_RegisterBlock"
  def namePrefix: String = "registerBlock"

  require_(isPow2(wData))
  require_(wData == 32 || wData == 64, "AXI4-Lite data width must be 32 or 64")
  require_(wMask <= 31, "the implementation supports only 31-bit address-space widths")
  require_(wMask <= wAddr, "register-block address space exceeds the AXI address width")
  require_(wMask >= log2Ceil(wData) - 3)
  require_(memoryMapPath.forall(_.nonEmpty), "memory-map path elements must not be empty")
  require_(
    memoryMapPath.forall(element => !element.contains('/')),
    "memory-map path elements must not contain '/'"
  )

  /** Address increment of one mapped register, in bytes. */
  val addrIncr: Int = wData / 8

  /** Declared byte extent of the register block. */
  val sizeAddressSpace: BigInt = BigInt(1) << wMask

  /** Corresponding AXI4-Lite configuration. */
  val cfgAxi: axi4.Config = axi4.Config(wAddr = wAddr, wData = wData, lite = true)

  /** Slave AXI4-Lite interface. */
  val s_axil = Wire(axi4.lite.Slave(cfgAxi))

  private val elasticState = trackingState(elastic.tracking.Tag)
  elasticState.addSink("s_axil_ar", s_axil.ar, boundary = true)
  elasticState.addSource("s_axil_r", s_axil.r, boundary = true)
  elasticState.addSink("s_axil_aw", s_axil.aw, boundary = true)
  elasticState.addSink("s_axil_w", s_axil.w, boundary = true)
  elasticState.addSource("s_axil_b", s_axil.b, boundary = true)

  private val resolver = new RegisterBlock_Resolver(this)

  private final class Entry(
      val startAddr: Int,
      val endAddr: Int,
      val readFn: () => Bits,
      val writeFn: (Bits, Bits) => Unit,
      val desc: String
  )

  private var lastAddr_ = 0
  private val addrMap_ = scala.collection.mutable.ListBuffer.empty[Entry]
  private var completed_ = false
  private var memoryMap_ = Option.empty[MemoryMap]

  private def requireOpen(operation: String): Unit =
    require_.here(!completed_, s"RegisterBlock.$operation cannot be called after complete()")

  /** Rebases the next allocation address. */
  def base(addr: Int): Unit = {
    requireOpen("base")
    require_.here(addr >= 0, "base address must not be negative")
    require_.here(BigInt(addr) <= sizeAddressSpace, "base address exceeds the address space")
    require_.here(addr % addrIncr == 0, "base address must be data-width aligned")
    lastAddr_ = addr
  }

  /** Returns the next byte address to be allocated. */
  def nextAddr: Int = lastAddr_

  /** Assigns a value to the current address and advances by one data-width word. */
  def reg[T <: Data](
      t: T,
      read: Boolean = true,
      write: Boolean = true,
      desc: String = "<no description>"
  ): Int = {
    requireOpen("reg")
    require_.here(t.getWidth <= wData, "mapped value is wider than the AXI data width")

    val ret = lastAddr_
    lastAddr_ += addrIncr
    require_.here(BigInt(lastAddr_) <= sizeAddressSpace, "address space is too small")

    val readFn =
      if (read) () => t.asUInt
      else () => (-1).S(wData.W).asUInt
    val writeFn =
      if (write)
        (wdata: Bits, wstrb: Bits) => {
          t := axi4.util
            .writeStrobeLogic(t.asTypeOf(wdata), wdata, wstrb)
            .asTypeOf(t)
        }
      else (_: Bits, _: Bits) => ()

    addrMap_.addOne(new Entry(ret, lastAddr_ - 1, readFn, writeFn, desc))
    ret
  }

  /** Reserves a byte region, rounded up to a whole data-width word. */
  def reserve(size: Int, desc: String = "<no description>"): Int = {
    requireOpen("reserve")
    require_.here(size >= 0, "reserved size must not be negative")

    val ret = lastAddr_
    val roundedSize = ((size + addrIncr - 1) / addrIncr) * addrIncr
    lastAddr_ += roundedSize
    require_.here(BigInt(lastAddr_) <= sizeAddressSpace, "address space is too small")
    if (roundedSize > 0)
      addrMap_.addOne(new Entry(ret, lastAddr_ - 1, () => 0.U, (_, _) => (), desc))
    ret
  }

  /** Writes the declared register map as CSV. */
  def saveRegisterMap(directory: String, name: String): Unit = {
    val write = new java.io.PrintWriter(f"$directory/$name.csv")
    try {
      write.println("sep=,")
      write.println("startAddr, endAddr, desc")
      addrMap_.foreach { entry =>
        write.println(
          f"0x${entry.startAddr}%08x, 0x${entry.endAddr}%08x, ${entry.desc}"
        )
      }
    } finally write.close()
  }

  /** The generated memory map, or `None` until [[complete]] has been called. */
  def memoryMapOption: Option[MemoryMap] = memoryMap_

  /** The generated memory map, available after [[complete]]. */
  def memoryMap: MemoryMap = memoryMapOption.getOrElse {
    throw new IllegalStateException("RegisterBlock.memoryMap is available only after complete()")
  }

  private def implementRead(): Unit = {
    val addressMask = (sizeAddressSpace - 1) & ~(BigInt(addrIncr) - 1)
    val request = elastic.SourceBuffered(s_axil.ar, 1)
    val response = elastic.SinkBuffered(s_axil.r, 1)

    val transformRead = new elastic.Transform(request, response) {
      val address = in.addr & addressMask.U(wAddr.W)

      out.data := (-1).S(wData.W).asUInt
      out.resp := axi4.ResponseFlag.OKAY

      addrMap_.foreach { entry =>
        when(address === entry.startAddr.U) {
          out.data := entry.readFn().asUInt
        }
      }
    }
  }

  private def implementWrite(): Unit = {
    val addressMask = (sizeAddressSpace - 1) & ~(BigInt(addrIncr) - 1)
    val requestAddress = elastic.SourceBuffered(s_axil.aw, 1)
    val requestData = elastic.SourceBuffered(s_axil.w, 1)
    val response = elastic.SinkBuffered(s_axil.b, 1)

    val joinWrite = new elastic.Join(response) {
      val aw = join(requestAddress)
      val w = join(requestData)
      val address = aw.addr & addressMask.U(wAddr.W)

      out.resp := axi4.ResponseFlag.OKAY

      fire {
        addrMap_.foreach { entry =>
          when(address === entry.startAddr.U) {
            entry.writeFn(w.data, w.strb)
          }
        }
      }
    }
  }

  /** Finalizes the block, creates its hardware, and makes its memory map resolvable. */
  def complete(): MemoryMap = {
    require_.here(!completed_, "RegisterBlock.complete() must be called exactly once")

    val resolvedPath = memoryMapPath match {
      case path if path.nonEmpty => path
      case _                     => path.headOption.toSeq
    }
    require_.here(
      resolvedPath.nonEmpty,
      "RegisterBlock needs a component prefix or an explicit memoryMapPath"
    )

    val origin = s_axil.trackingPath
    val result = MemoryMap(
      path = resolvedPath,
      size = sizeAddressSpace,
      segments = Seq(
        MemoryMap.Segment(
          path = Seq("registers"),
          baseAddress = 0,
          size = sizeAddressSpace,
          origin = origin
        )
      ),
      origin = origin
    )

    PrefixManager.withAbsolute(path) {
      withComponent(this) {
        PrefixManager.withRelative("read") {
          implementRead()
        }
        PrefixManager.withRelative("write") {
          implementWrite()
        }
      }
    }

    memoryMap_ = Some(result)
    s_axil.properties(p.SlaveMemoryMap) = result
    completed_ = true
    result
  }

  deferred {
    require_(completed_, "RegisterBlock.complete() must be called exactly once")
  }
}

/** Resolves and initializes interface properties for one [[RegisterBlock]]. */
private final class RegisterBlock_Resolver(owner: RegisterBlock)(implicit sourceInfo: SourceInfo)
    extends axi4.tracking.Resolver(owner) {
  import axi4.tracking._
  import axi4.tracking.{properties => p, values => v}

  bindSlave(owner.s_axil)

  owner.s_axil.properties(p.SlaveReadThreadMode) = v.ThreadMode.SingleTransaction
  owner.s_axil.properties(p.SlaveWriteThreadMode) = v.ThreadMode.SingleTransaction

  def setMemoryMap(memoryMap: v.MemoryMap): Unit =
    owner.s_axil.properties(p.SlaveMemoryMap) = memoryMap

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case ResolveRequest(_, p.Key(p.Slave, _, p.MemoryMap)) =>
        request.failure(
          "RegisterBlock.complete() was not called before resolving slave.memoryMap"
        )
      case ResolveRequest(_, p.Key(_, _, p.TrafficProfile)) => request.incomplete()
      case _ =>
        request.missingCase()
    }
}
