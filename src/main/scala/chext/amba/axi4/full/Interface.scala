package chext.amba.axi4.full

import chisel3._
import chisel3.util._
import chisel3.experimental.SourceInfo
import chisel3.experimental.dataview.{PartialDataView, DataView}
import chisel3.reflect.DataMirror

import chext.amba.axi4
import chext.amba.axi4.util._
import chext.util.NamedVec

import chext.elastic

/** Address channel. (AR and AW)
  *
  * @param cfg
  *   configuration
  */
abstract class AddressChannel(implicit cfg: axi4.Config) extends Bundle {

  /** identification tag */
  val id = UInt(cfg.wId.W)

  /** the address of the first transfer */
  val addr = UInt(cfg.wAddr.W)

  /** the exact number of data transfers */
  val len = UInt(cfg.wLen.W)

  /** the number of bytes in each data transfer */
  val size = UInt(3.W)

  /** burst type */
  val burst = UInt(2.W)

  /** atomic access */
  val lock = UInt(cfg.wLock.W)

  /** cache flag */
  val cache = UInt(cfg.wCache.W)

  /** protection flag */
  val prot = UInt(cfg.wProt.W)

  /** quality-of-service */
  val qos = UInt(cfg.wQos.W)

  /** region identifier */
  val region = UInt(cfg.wRegion.W)

  /** user-defined data */
  def user: Bits
}

class ReadAddressChannel(implicit cfg: axi4.Config) extends AddressChannel {
  val user = Bits(cfg.wUserAR.W)
}

object ReadAddressChannel {
  def apply(cfg: axi4.Config) = new ReadAddressChannel()(cfg)
}

class WriteAddressChannel(implicit cfg: axi4.Config) extends AddressChannel {
  val user = Bits(cfg.wUserAW.W)
}

object WriteAddressChannel {
  def apply(cfg: axi4.Config) = new WriteAddressChannel()(cfg)
}

/** Read data channel. (R)
  *
  * @param cfg
  *   configuration
  */
class ReadDataChannel(implicit cfg: axi4.Config) extends Bundle {

  /** identification tag */
  val id = UInt(cfg.wId.W)

  /** data */
  val data = Bits(cfg.wData.W)

  /** response flag */
  val resp = UInt(2.W)

  /** last burst */
  val last = Bool()

  /** user-defined data */
  val user = Bits(cfg.wUserR.W)
}

object ReadDataChannel {
  def apply(cfg: axi4.Config) = new ReadDataChannel()(cfg)
}

/** Write data channel. (W)
  *
  * @param cfg
  *   configuration
  */
class WriteDataChannel(implicit cfg: axi4.Config) extends Bundle {

  /** data */
  val data = Bits(cfg.wData.W)

  /** strobe */
  val strb = UInt(cfg.wStrobe.W)

  /** last burst */
  val last = Bool()

  /** user-defined data */
  val user = Bits(cfg.wUserW.W)
}

object WriteDataChannel {
  def apply(cfg: axi4.Config) = new WriteDataChannel()(cfg)
}

/** Write response channel. (B)
  *
  * @param cfg
  *   configuration
  */
class WriteResponseChannel(implicit cfg: axi4.Config) extends Bundle {

  /** identification tag */
  val id = UInt(cfg.wId.W)

  /** response flag */
  val resp = UInt(2.W)

  /** user-defined data */
  val user = Bits(cfg.wUserB.W)
}

object WriteResponseChannel {
  def apply(cfg: axi4.Config) = new WriteResponseChannel()(cfg)
}

abstract class Interface extends Bundle {

  /** Interface configuration. */
  def cfg: axi4.Config

  /** Interface declaration location. */
  def sourceInfo: SourceInfo

  /** read address channel */
  def ar: elastic.Interface[AddressChannel] = throw NotSupported("read")

  /** read data channel */
  def r: elastic.Interface[ReadDataChannel] = throw NotSupported("read")

  /** write address channel */
  def aw: elastic.Interface[AddressChannel] = throw NotSupported("write")

  /** write data channel */
  def w: elastic.Interface[WriteDataChannel] = throw NotSupported("write")

  /** write response channel */
  def b: elastic.Interface[WriteResponseChannel] = throw NotSupported("write")
}

object Interface {
  def apply(cfg: axi4.Config)(implicit si: SourceInfo): Interface = {
    assert(!cfg.lite)
    implicit val _cfg: axi4.Config = cfg

    (cfg.read, cfg.write) match {
      case (true, true)   => new ReadWriteInterface
      case (true, false)  => new ReadInterface
      case (false, true)  => new WriteInterface
      case (false, false) => throw BadConfig("supports neither read nor write")
    }
  }

  def many(
      n: Int,
      cfg: axi4.Config,
      naming: NamedVec.Naming = NamedVec.Plain
  )(implicit si: SourceInfo): NamedVec[Interface] =
    NamedVec.many(n, apply(cfg), naming)

  /** pairs for DataView */
  def readPairs(
      x: axi4.RawInterface,
      y: Interface
  ): Iterable[(Data, Data)] =
    Seq(
      // AR
      x.ARREADY -> y.ar.$ready,
      x.ARVALID -> y.ar.$valid,
      x.ARID -> y.ar.$bits.id,
      x.ARADDR -> y.ar.$bits.addr,
      x.ARLEN -> y.ar.$bits.len,
      x.ARSIZE -> y.ar.$bits.size,
      x.ARBURST -> y.ar.$bits.burst,
      x.ARLOCK -> y.ar.$bits.lock,
      x.ARCACHE -> y.ar.$bits.cache,
      x.ARPROT -> y.ar.$bits.prot,
      x.ARQOS -> y.ar.$bits.qos,
      x.ARREGION -> y.ar.$bits.region,
      x.ARUSER -> y.ar.$bits.user,

      // R
      x.RREADY -> y.r.$ready,
      x.RVALID -> y.r.$valid,
      x.RID -> y.r.$bits.id,
      x.RDATA -> y.r.$bits.data,
      x.RRESP -> y.r.$bits.resp,
      x.RLAST -> y.r.$bits.last,
      x.RUSER -> y.r.$bits.user
    ).map { case (a, b) => a.get -> b }

  /** pairs for DataView */
  def writePairs(
      x: axi4.RawInterface,
      y: Interface
  ): Iterable[(Data, Data)] =
    Seq(
      // AW
      x.AWREADY -> y.aw.$ready,
      x.AWVALID -> y.aw.$valid,
      x.AWID -> y.aw.$bits.id,
      x.AWADDR -> y.aw.$bits.addr,
      x.AWLEN -> y.aw.$bits.len,
      x.AWSIZE -> y.aw.$bits.size,
      x.AWBURST -> y.aw.$bits.burst,
      x.AWLOCK -> y.aw.$bits.lock,
      x.AWCACHE -> y.aw.$bits.cache,
      x.AWPROT -> y.aw.$bits.prot,
      x.AWQOS -> y.aw.$bits.qos,
      x.AWREGION -> y.aw.$bits.region,
      x.AWUSER -> y.aw.$bits.user,

      // W
      x.WREADY -> y.w.$ready,
      x.WVALID -> y.w.$valid,
      x.WDATA -> y.w.$bits.data,
      x.WSTRB -> y.w.$bits.strb,
      x.WLAST -> y.w.$bits.last,
      x.WUSER -> y.w.$bits.user,

      // B
      x.BREADY -> y.b.$ready,
      x.BVALID -> y.b.$valid,
      x.BID -> y.b.$bits.id,
      x.BRESP -> y.b.$bits.resp,
      x.BUSER -> y.b.$bits.user
    ).map { case (a, b) => a.get -> b }

  implicit val view: DataView[axi4.RawInterface, Interface] =
    PartialDataView.mapping[axi4.RawInterface, Interface](
      (x) => {
        val interface = Interface(x.cfg)(x.sourceInfo)

        DataMirror.specifiedDirectionOf(x) match {
          case SpecifiedDirection.Flip => Flipped(interface)
          case _                       => interface
        }
      },
      (x, y) => {
        (x.cfg.read, x.cfg.write) match {
          case (true, true)   => readPairs(x, y) ++ writePairs(x, y)
          case (true, false)  => readPairs(x, y)
          case (false, true)  => writePairs(x, y)
          case (false, false) => Seq()
        }
      }
    )
}

object Slave {
  def apply(cfg: axi4.Config)(implicit si: SourceInfo) = Flipped(Interface(cfg))

  def many(
      n: Int,
      cfg: axi4.Config,
      naming: NamedVec.Naming = NamedVec.Plain
  )(implicit si: SourceInfo): NamedVec[Interface] =
    NamedVec.many(n, apply(cfg), naming)
}

object Master {
  def apply(cfg: axi4.Config)(implicit si: SourceInfo) = Interface(cfg)

  def many(
      n: Int,
      cfg: axi4.Config,
      naming: NamedVec.Naming = NamedVec.Plain
  )(implicit si: SourceInfo): NamedVec[Interface] =
    NamedVec.many(n, apply(cfg), naming)
}

private class ReadInterface(implicit
    val cfg: axi4.Config,
    val si: SourceInfo
) extends Interface {
  override val ar = elastic.Sink(new ReadAddressChannel)
  override val r = elastic.Source(new ReadDataChannel)

  def sourceInfo: SourceInfo = si
}

private class WriteInterface(implicit
    val cfg: axi4.Config,
    val si: SourceInfo
) extends Interface {
  override val aw = elastic.Sink(new WriteAddressChannel)
  override val w = elastic.Sink(new WriteDataChannel)
  override val b = elastic.Source(new WriteResponseChannel)

  def sourceInfo: SourceInfo = si
}

private class ReadWriteInterface(implicit
    val cfg: axi4.Config,
    val si: SourceInfo
) extends Interface {
  override val ar = elastic.Sink(new ReadAddressChannel)
  override val r = elastic.Source(new ReadDataChannel)
  override val aw = elastic.Sink(new WriteAddressChannel)
  override val w = elastic.Sink(new WriteDataChannel)
  override val b = elastic.Source(new WriteResponseChannel)

  def sourceInfo: SourceInfo = si
}
