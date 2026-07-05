package chext.amba.axi4.full.components.helpers

import chisel3._
import chisel3.util._

import chext.amba.axi4
import chext.elastic

import elastic.ConnectOp._

object IdExtend {
  private val require_ = chext.util.Require.inferred()

  def read(
      slaveInterfaces: Seq[axi4.full.Interface],
      masterInterfaces: Seq[axi4.full.Interface]
  ): Unit = {
    require_(slaveInterfaces.length == masterInterfaces.length)

    slaveInterfaces.zipWithIndex.zip(masterInterfaces).map {
      case ((slave, port), master) => {
        slave.ar :=> master.ar
        master.r :=> slave.r

        master.ar.$bits.id := port.U ## slave.ar.$bits.id

        require_(slave.cfg.read && master.cfg.read)
        require_(master.cfg.wId >= slave.cfg.wId + log2Ceil(slaveInterfaces.length))
      }
    }
  }

  def write(
      slaveInterfaces: Seq[axi4.full.Interface],
      masterInterfaces: Seq[axi4.full.Interface]
  ): Unit = {
    require_(slaveInterfaces.length == masterInterfaces.length)

    slaveInterfaces.zipWithIndex.zip(masterInterfaces).map {
      case ((slave, port), master) => {
        slave.aw :=> master.aw
        slave.w :=> master.w
        master.b :=> slave.b

        master.aw.$bits.id := port.U ## slave.aw.$bits.id

        require_(slave.cfg.write && master.cfg.write)
        require_(master.cfg.wId >= slave.cfg.wId + log2Ceil(slaveInterfaces.length))
      }
    }
  }
}
