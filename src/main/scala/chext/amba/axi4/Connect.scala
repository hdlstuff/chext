package chext.amba.axi4

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix
import chisel3.experimental.SourceInfo

import chext.amba.axi4

object connect {
  import axi4.Casts._
  import axi4.full.ConnectOp._
  import axi4.lite.ConnectOp._

  private val require_ = chext.util.Require.inferred()

  def apply(
      master: axi4.RawInterface,
      slave: axi4.full.Interface
  )(implicit si: SourceInfo): Unit = {
    require_.here(
      !master.cfg.lite,
      "master must be a full axi4 interface",
      Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
    )
    master.asFull :=> slave
  }

  def apply(
      master: axi4.RawInterface,
      slave: axi4.lite.Interface
  )(implicit si: SourceInfo): Unit = {
    require_.here(
      master.cfg.lite,
      "master must be a lite axi4 interface",
      Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
    )
    master.asLite :=> slave
  }

  def apply(
      master: axi4.RawInterface,
      slave: axi4.RawInterface
  )(implicit si: SourceInfo): Unit = {
    require_.here(
      master.cfg.lite == slave.cfg.lite,
      "master and slave must both be either full or lite axi4 interfaces",
      Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
    )

    if (master.cfg.lite)
      master.asLite :=> slave.asLite
    else
      master.asFull :=> slave.asFull
  }
}

trait ConnectOp {
  private val require_ = chext.util.Require.inferred()

  /* implicit class names should be different, otherwise shadowed */
  implicit class axi4_connect_op(master: axi4.RawInterface) {
    def :=>(slave: axi4.RawInterface)(implicit si: SourceInfo) = {
      connect(master, slave)
    }

    def :=>(slave: axi4.full.Interface)(implicit si: SourceInfo) = {
      connect(master, slave)
    }

    def :=>(slave: axi4.lite.Interface)(implicit si: SourceInfo) = {
      connect(master, slave)
    }
  }

  implicit class axi4_connect_seq_op(masters: Seq[axi4.RawInterface]) {
    def :=>(slaves: Seq[axi4.RawInterface])(implicit si: SourceInfo): Unit = {
      require_(
        masters.length == slaves.length,
        f"master/slave sequence length mismatch: ${masters.length} != ${slaves.length}"
      )

      masters.zip(slaves).zipWithIndex.foreach { case ((master, slave), index) =>
        prefix(index.toString) {
          connect(master, slave)
        }
      }
    }

  }
}

object ConnectOp extends ConnectOp
