package chext.amba.axi4.full

import chext.elastic
import elastic.ConnectOp._
import chisel3.experimental.SourceInfo

private object connect {
  private val require_ = new chext.util.Require("axi4.full.connect")

  def apply(
      master: Interface,
      slave: Interface
  )(implicit si: SourceInfo): Unit = {
    require_(
      master.cfg.wId <= slave.cfg.wId,
      "master interface should have a narrower ID field than the slave interface",
      Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
    )

    require_(
      !master.cfg.axi3Compat && !slave.cfg.axi3Compat || master.cfg.axi3Compat,
      "master interface that is not AXI3-compatible cannot drive an AXI3-compatible interface",
      Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
    )

    val masterCfg = master.cfg.copy(wId = 0, wAddr = 0, axi3Compat = false)
    val slaveCfg = slave.cfg.copy(wId = 0, wAddr = 0, axi3Compat = false)

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
  implicit class axi4_full_connect_op(master: Interface)(implicit si: SourceInfo) {
    def :=>(slave: Interface) = {
      connect(master, slave)
    }
  }
}

object ConnectOp extends ConnectOp
