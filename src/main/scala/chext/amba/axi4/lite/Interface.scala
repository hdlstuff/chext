package chext.amba.axi4.lite

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
class AddressChannel(implicit cfg: axi4.Config) extends Bundle {

  /** the address of the first transfer */
  val addr = UInt(cfg.wAddr.W)

  /** protection flag */
  val prot = UInt(cfg.wProt.W)
}

object AddressChannel {
  def apply(cfg: axi4.Config) = new AddressChannel()(cfg)
}

/** Read data channel. (R)
  *
  * @param cfg
  *   configuration
  */
class ReadDataChannel(implicit cfg: axi4.Config) extends Bundle {

  /** data */
  val data = UInt(cfg.wData.W)

  /** response flag */
  val resp = UInt(2.W)
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
  val data = UInt(cfg.wData.W)

  /** strobe */
  val strb = UInt(cfg.wStrobe.W)
}

object WriteDataChannel {
  def apply(cfg: axi4.Config) = new WriteDataChannel()(cfg)
}

/** Write response channel. (B)
  *
  * @param cfg
  *   configuration (not used)
  */
class WriteResponseChannel(implicit val cfg: axi4.Config) extends Bundle {

  /** response flag */
  val resp = UInt(2.W)
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
    assert(cfg.lite)
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
      x.ARADDR -> y.ar.$bits.addr,
      x.ARPROT -> y.ar.$bits.prot,

      // R
      x.RREADY -> y.r.$ready,
      x.RVALID -> y.r.$valid,
      x.RDATA -> y.r.$bits.data,
      x.RRESP -> y.r.$bits.resp
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
      x.AWADDR -> y.aw.$bits.addr,
      x.AWPROT -> y.aw.$bits.prot,

      // W
      x.WREADY -> y.w.$ready,
      x.WVALID -> y.w.$valid,
      x.WDATA -> y.w.$bits.data,
      x.WSTRB -> y.w.$bits.strb,

      // B
      x.BREADY -> y.b.$ready,
      x.BVALID -> y.b.$valid,
      x.BRESP -> y.b.$bits.resp
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
  override val ar = elastic.Sink(new AddressChannel)
  override val r = elastic.Source(new ReadDataChannel)

  def sourceInfo: SourceInfo = si
}

private class WriteInterface(implicit
    val cfg: axi4.Config,
    val si: SourceInfo
) extends Interface {
  override val aw = elastic.Sink(new AddressChannel)
  override val w = elastic.Sink(new WriteDataChannel)
  override val b = elastic.Source(new WriteResponseChannel)

  def sourceInfo: SourceInfo = si
}

private class ReadWriteInterface(implicit
    val cfg: axi4.Config,
    val si: SourceInfo
) extends Interface {
  override val ar = elastic.Sink(new AddressChannel)
  override val r = elastic.Source(new ReadDataChannel)
  override val aw = elastic.Sink(new AddressChannel)
  override val w = elastic.Sink(new WriteDataChannel)
  override val b = elastic.Source(new WriteResponseChannel)

  def sourceInfo: SourceInfo = si
}
