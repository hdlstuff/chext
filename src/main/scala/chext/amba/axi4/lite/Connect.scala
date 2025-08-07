package chext.amba.axi4.lite

import chext.elastic
import elastic.ConnectOp._
import chisel3.experimental.SourceInfo

private object connect {
  private val require_ = new chext.util.Require("axi4.lite.connect")

  def apply(
      master: Interface,
      slave: Interface
  )(implicit si: SourceInfo): Unit = {
    val masterCfg = master.cfg.copy(wAddr = 0)
    val slaveCfg = slave.cfg.copy(wAddr = 0)
    require_(
      masterCfg == slaveCfg,
      "configurations do not match after normalization",
      Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
    )

    if (master.cfg.read) {
      master.ar :=> slave.ar
      slave.r :=> master.r
    }

    if (master.cfg.write) {
      master.aw :=> slave.aw
      master.w :=> slave.w
      slave.b :=> master.b
    }
  }
}

trait ConnectOp {
  /* implicit class names should be different, otherwise shadowed */
  implicit class axi4_lite_connect_op(master: Interface)(implicit si: SourceInfo) {
    def :=>(slave: Interface) = {
      connect(master, slave)
    }
  }
}

object ConnectOp extends ConnectOp
