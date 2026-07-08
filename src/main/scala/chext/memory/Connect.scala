package chext.memory

import chisel3.experimental.SourceInfo
import chisel3.experimental.prefix

import chext.elastic
import elastic.ConnectOp._

import chext.tracking.uniquePrefix

object connect {
  private val require_ = chext.util.Require.inferred()

  def apply(
      master: ReadInterface,
      slave: ReadInterface
  )(implicit si: SourceInfo): Unit = {
    require_.here(master.wData == slave.wData, "wData of interfaces must match!")
    require_.here(master.wAddr == slave.wAddr, "wAddr of interfaces must match!")

    master.req :=> slave.req
    slave.resp :=> master.resp
  }

  def apply(
      master: WriteInterface,
      slave: WriteInterface
  )(implicit si: SourceInfo): Unit = {
    require_.here(master.wData == slave.wData, "wData of interfaces must match!")
    require_.here(master.wAddr == slave.wAddr, "wAddr of interfaces must match!")

    master.req :=> slave.req
    slave.resp :=> master.resp
  }
}

object ConnectOp {
  private val require_ = chext.util.Require.inferred()

  implicit class readInterface_connectOp(val master: ReadInterface) extends AnyVal {
    def :=>(slave: ReadInterface)(implicit si: SourceInfo): Unit = {
      connect(master, slave)
    }
  }

  implicit class writeInterface_connectOp(val master: WriteInterface) extends AnyVal {
    def :=>(slave: WriteInterface)(implicit si: SourceInfo): Unit = {
      connect(master, slave)
    }
  }

  implicit class readInterfaceSeq_connectOp(val masters: Seq[ReadInterface]) extends AnyVal {
    def :=>(slaves: Seq[ReadInterface])(implicit si: SourceInfo): Unit = {
      require_(
        masters.length == slaves.length,
        f"master/slave sequence length mismatch: ${masters.length} != ${slaves.length}"
      )

      uniquePrefix("connectMany") {
        masters.zip(slaves).zipWithIndex.foreach { case ((master, slave), index) =>
          prefix(index.toString) {
            connect(master, slave)
          }
        }
      }
    }
  }

  implicit class writeInterfaceSeq_connectOp(val masters: Seq[WriteInterface]) extends AnyVal {
    def :=>(slaves: Seq[WriteInterface])(implicit si: SourceInfo): Unit = {
      require_(
        masters.length == slaves.length,
        f"master/slave sequence length mismatch: ${masters.length} != ${slaves.length}"
      )

      uniquePrefix("connectMany") {
        masters.zip(slaves).zipWithIndex.foreach { case ((master, slave), index) =>
          prefix(index.toString) {
            connect(master, slave)
          }
        }
      }
    }
  }
}
