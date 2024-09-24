package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._

import chext.amba.axi4
import chext.elastic
import elastic.ConnectOp._
import axi4.Ops._

private[full] trait DataWidthConverterLike {
  def s_axi: axi4.full.Interface
  def m_axi: axi4.full.Interface
}

case class DataWidthConverterConfig(
    val axiCfgSlave: axi4.Config,
    val wDataMaster: Int,
    val wIdTracked: Int = 0
) {
  require(!axiCfgSlave.lite, "axiCfgSlave.lite must be false!")
  require(wIdTracked <= axiCfgSlave.wId)

  require(wDataMaster >= 8)
  require(isPow2(wDataMaster))

  val wDataSlave = axiCfgSlave.wData
  val wAddr = axiCfgSlave.wAddr
  val axiCfgMaster = axiCfgSlave.copy(wData = wDataMaster)
}

class DataWidthConverter(cfg: DataWidthConverterConfig) extends DataWidthConverterLike {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiCfgSlave))
  val m_axi = IO(axi4.full.Master(axiCfgMaster))

  if (wDataMaster > wDataSlave) {
    val upscale = Module(
      new Upscale(
        UpscaleConfig(axiCfgSlave, wDataMaster /* TODO: let's pass args */)
      )
    )

    s_axi :=> upscale.s_axi
    upscale.m_axi :=> m_axi
  }
  else if (wDataMaster < wDataSlave) {
    val downscale = Module(
      new Downscale(
        DownscaleConfig(axiCfgSlave, wDataMaster /* TODO: let's pass args */)
      )
    )

    s_axi :=> downscale.s_axi
    downscale.m_axi :=> m_axi
  }
  else {
    // pass-through
    s_axi :=> m_axi
  }
}
