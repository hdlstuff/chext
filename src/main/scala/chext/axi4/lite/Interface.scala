package chext.axi4.lite

import chisel3._
import chisel3.util._
import chisel3.experimental.dataview.{PartialDataView, DataView}

import chext.axi4
import os.write

/** Address channel. (AR and AW)
  *
  * @param cfg
  *   configuration
  */
class AddressChannel(implicit cfg: axi4.Config) extends Bundle {

  /** the address of the first transfer */
  val addr = UInt(cfg.wAddr.W)

  /** protection flag */
  val prot = UInt(3.W)
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

/** Write response channel. (B)
  *
  * @param cfg
  *   configuration (not used)
  */
class WriteResponseChannel(implicit val cfg: axi4.Config) extends Bundle {

  /** response flag */
  val resp = UInt(2.W)
}

private object NotSupported {
  def apply(str: String) = new RuntimeException(f"Not supported: $str")
}

private object BadConfig {
  def apply(str: String) = new RuntimeException(f"Bad config: $str")
}

abstract class Interface extends Bundle {

  /** Interface configuration. */
  def cfg: axi4.Config

  /** read address channel */
  def ar: IrrevocableIO[AddressChannel] = throw NotSupported("read")

  /** read data channel */
  def r: IrrevocableIO[ReadDataChannel] = throw NotSupported("read")

  /** write address channel */
  def aw: IrrevocableIO[AddressChannel] = throw NotSupported("write")

  /** write data channel */
  def w: IrrevocableIO[WriteDataChannel] = throw NotSupported("write")

  /** write response channel */
  def b: IrrevocableIO[WriteResponseChannel] = throw NotSupported("write")
}

object Interface {
  def apply(cfg: axi4.Config): Interface = {
    assert(cfg.lite)
    implicit val _cfg: axi4.Config = cfg

    (cfg.read, cfg.write) match {
      case (true, true)   => new ReadWriteInterface
      case (true, false)  => new ReadInterface
      case (false, true)  => new WriteInterface
      case (false, false) => throw BadConfig("supports neither read nor write")
    }
  }

  def master(cfg: axi4.Config): Interface = apply(cfg)
  def slave(cfg: axi4.Config): Interface = Flipped(apply(cfg))

  /** pairs for DataView */
  private[axi4] def readPairs(
      x: axi4.Interface,
      y: axi4.lite.Interface
  ): Iterable[(Data, Data)] =
    Seq(
      // AR
      x.ARREADY -> y.ar.ready,
      x.ARVALID -> y.ar.valid,
      x.ARADDR -> y.ar.bits.addr,
      x.ARPROT -> y.ar.bits.prot,

      // R
      x.RREADY -> y.r.ready,
      x.RVALID -> y.r.valid,
      x.RDATA -> y.r.bits.data,
      x.RRESP -> y.r.bits.resp
    ).map { case (a, b) => a.get -> b }

  /** pairs for DataView */
  private[axi4] def writePairs(
      x: axi4.Interface,
      y: axi4.lite.Interface
  ): Iterable[(Data, Data)] =
    Seq(
      // AW
      x.AWREADY -> y.aw.ready,
      x.AWVALID -> y.aw.valid,
      x.AWADDR -> y.aw.bits.addr,
      x.AWPROT -> y.aw.bits.prot,

      // W
      x.WREADY -> y.w.ready,
      x.WVALID -> y.w.valid,
      x.WDATA -> y.w.bits.data,
      x.WSTRB -> y.w.bits.strb,

      // B
      x.BREADY -> y.b.ready,
      x.BVALID -> y.b.valid,
      x.BRESP -> y.b.bits.resp
    ).map { case (a, b) => a.get -> b }

  implicit val view: DataView[axi4.Interface, Interface] =
    PartialDataView.mapping[axi4.Interface, Interface](
      (x) => x.propagateFlip(Interface(x.cfg)),
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

private class ReadInterface(implicit val cfg: axi4.Config) extends Interface {
  override val ar = Irrevocable(new AddressChannel)
  override val r = Flipped(Irrevocable(new ReadDataChannel))
}

private class WriteInterface(implicit val cfg: axi4.Config) extends Interface {
  override val aw = Irrevocable(new AddressChannel)
  override val w = Irrevocable(new WriteDataChannel)
  override val b = Flipped(Irrevocable(new WriteResponseChannel))
}
private class ReadWriteInterface(implicit val cfg: axi4.Config)
    extends Interface {
  override val ar = Irrevocable(new AddressChannel)
  override val r = Flipped(Irrevocable(new ReadDataChannel))
  override val aw = Irrevocable(new AddressChannel)
  override val w = Irrevocable(new WriteDataChannel)
  override val b = Flipped(Irrevocable(new WriteResponseChannel))
}

import axi4.Casts._

object main extends App {
  class LiteInterfaceTestDevice extends Module {
    private val cfg1 = axi4.Config(read = true, write = false, lite = true)
    private val cfg2 = axi4.Config(lite = true)

    val slave1 = IO(axi4.Interface.slave(cfg1))
    val master1 = IO(axi4.Interface.master(cfg1))

    val slave2 = IO(axi4.Interface.slave(cfg2))
    val master2 = IO(axi4.Interface.master(cfg2))

    val slave3 = IO(axi4.Interface.slave(cfg2))
    val master3 = IO(axi4.Interface.master(cfg2))

    master1.asLite <> slave1.asLite
    master2.asLite <> slave2.asLite
    master2.asLite <> slave2.asLite

    slave3.asLite <> master3.asLite
  }

  emitVerilog(new LiteInterfaceTestDevice, Array("--target-dir", "output/"))
}
