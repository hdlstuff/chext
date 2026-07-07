package chext.memory

import chisel3.experimental.SourceInfo

import chext.elastic
import elastic.ConnectOp._

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
}
