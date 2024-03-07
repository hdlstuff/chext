package chext.axi4

import chisel3._
import chisel3.util._
import chisel3.reflect.DataMirror

import chext.{axi4 => axi4}

// TODO maybe bring in the `cache` flags
// TODO may support ACE as well?

object BurstType {
  val FIXED = 0x0.U(2.W)
  val INCR = 0x1.U(2.W)
  val WRAP = 0x2.U(2.W)
}

object ResponseFlag {
  val OKAY = 0x0.U(2.W)
  val EXOKAY = 0x1.U(2.W)
  val SLVERR = 0x2.U(2.W)
  val DECERR = 0x3.U(2.W)
}

/** AXI4 interface configuration.
  *
  * @param wId
  *   identification tag width
  * @param wAddr
  *   address width
  * @param wData
  *   data bus width
  * @param read
  *   enable read channels
  * @param write
  *   enable write channels
  * @param lite
  *   use AXI4-Lite variant
  * @param wUserReq
  *   data width for ARUSER and AWUSER (see p. A8-104 in ARM IHI 0022H.c)
  * @param wUserData
  *   user data width for RUSER and WUSER (see p. A8-104 in ARM IHI 0022H.c)
  * @param wUserResp
  *   user data width for RUSER and BUSER (see p. A8-104 in ARM IHI 0022H.c)
  */
case class Config(
    val wId: Int = 0,
    val wAddr: Int = 32,
    val wData: Int = 32,
    val read: Boolean = true,
    val write: Boolean = true,
    val lite: Boolean = false,
    val wUserAR: Int = 0,
    val wUserR: Int = 0,
    val wUserAW: Int = 0,
    val wUserW: Int = 0,
    val wUserB: Int = 0
) {
  require(wData >= 8)
  require(isPow2(wData))
  require(!lite || (wData == 32 || wData == 64))

  /** width of the strobe signal for the write data channel */
  val wStrobe = wData / 8

  /** Creates a full strobe.
    * @note
    *   `(-1).S(wStrobe.W).asUInt` does not work in tests.
    */
  def fullStrobe = ("b" + ("1" * wStrobe)).U(wStrobe.W)
}

/** AXI4 interface that complies with the standard naming convention.
  *
  * This interface is not supposed to be used directly. Please use
  * `.asFull` and `.asLite` functions defined in corresponding
  * implicit classes.
  *
  * @param cfg
  *   configuration
  */
class RawInterface(val cfg: axi4.Config) extends Bundle {

  val ARREADY = if (cfg.read) Some(Input(Bool())) else None
  val ARVALID = if (cfg.read) Some(Output(Bool())) else None
  val ARID =
    if (cfg.read && !cfg.lite) Some(Output(UInt(cfg.wId.W))) else None
  val ARADDR = if (cfg.read) Some(Output(UInt(cfg.wAddr.W))) else None
  val ARLEN = if (cfg.read && !cfg.lite) Some(Output(UInt(8.W))) else None
  val ARSIZE = if (cfg.read && !cfg.lite) Some(Output(UInt(3.W))) else None
  val ARBURST = if (cfg.read && !cfg.lite) Some(Output(UInt(2.W))) else None
  val ARLOCK = if (cfg.read && !cfg.lite) Some(Output(Bool())) else None
  val ARCACHE = if (cfg.read && !cfg.lite) Some(Output(UInt(4.W))) else None
  val ARPROT = if (cfg.read) Some(Output(UInt(3.W))) else None
  val ARQOS = if (cfg.read && !cfg.lite) Some(Output(UInt(4.W))) else None
  val ARREGION = if (cfg.read && !cfg.lite) Some(Output(UInt(4.W))) else None
  val ARUSER =
    if (cfg.read && !cfg.lite)
      Some(Output(Bits(cfg.wUserAR.W)))
    else None

  val RREADY = if (cfg.read) Some(Output(Bool())) else None
  val RVALID = if (cfg.read) Some(Input(Bool())) else None
  val RID =
    if (cfg.read && !cfg.lite) Some(Input(UInt(cfg.wId.W))) else None
  val RDATA = if (cfg.read) Some(Input(Bits(cfg.wData.W))) else None
  val RRESP = if (cfg.read) Some(Input(UInt(2.W))) else None
  val RLAST = if (cfg.read && !cfg.lite) Some(Input(Bool())) else None
  val RUSER =
    if (cfg.read && !cfg.lite)
      Some(Input(Bits(cfg.wUserR.W)))
    else None

  val AWREADY = if (cfg.write) Some(Input(Bool())) else None
  val AWVALID = if (cfg.write) Some(Output(Bool())) else None
  val AWID =
    if (cfg.write && !cfg.lite) Some(Output(UInt(cfg.wId.W))) else None
  val AWADDR = if (cfg.write) Some(Output(UInt(cfg.wAddr.W))) else None
  val AWLEN = if (cfg.write && !cfg.lite) Some(Output(UInt(8.W))) else None
  val AWSIZE = if (cfg.write && !cfg.lite) Some(Output(UInt(3.W))) else None
  val AWBURST = if (cfg.write && !cfg.lite) Some(Output(UInt(2.W))) else None
  val AWLOCK = if (cfg.write && !cfg.lite) Some(Output(Bool())) else None
  val AWCACHE = if (cfg.write && !cfg.lite) Some(Output(UInt(4.W))) else None
  val AWPROT = if (cfg.write) Some(Output(UInt(3.W))) else None
  val AWQOS = if (cfg.write && !cfg.lite) Some(Output(UInt(4.W))) else None
  val AWREGION = if (cfg.write && !cfg.lite) Some(Output(UInt(4.W))) else None
  val AWUSER =
    if (cfg.write && !cfg.lite)
      Some(Output(Bits(cfg.wUserAW.W)))
    else None

  val WREADY = if (cfg.write) Some(Input(Bool())) else None
  val WVALID = if (cfg.write) Some(Output(Bool())) else None
  val WDATA = if (cfg.write) Some(Output(Bits(cfg.wData.W))) else None
  val WSTRB = if (cfg.write) Some(Output(UInt(cfg.wStrobe.W))) else None
  val WLAST = if (cfg.write && !cfg.lite) Some(Output(Bool())) else None
  val WUSER =
    if (cfg.write && !cfg.lite)
      Some(Output(Bits(cfg.wUserW.W)))
    else None

  val BREADY = if (cfg.write) Some(Output(Bool())) else None
  val BVALID = if (cfg.write) Some(Input(Bool())) else None
  val BID =
    if (cfg.write && !cfg.lite) Some(Input(UInt(cfg.wId.W))) else None
  val BRESP = if (cfg.write) Some(Input(UInt(2.W))) else None
  val BUSER =
    if (cfg.write && !cfg.lite)
      Some(Input(Bits(cfg.wUserB.W)))
    else None
}

object Interface {
  def apply(cfg: axi4.Config) = new RawInterface(cfg)
}

object Slave {
  def apply(cfg: axi4.Config) = Flipped(Interface(cfg))
}

object Master {
  def apply(cfg: axi4.Config) = Interface(cfg)
}
