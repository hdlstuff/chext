package chext

trait ModuleConfig {
  def moduleName: String
  def hdlinfoModule: hdlinfo.Module
}

trait Module extends chisel3.RawModule {
  def cfg: chext.ModuleConfig

  override def desiredName: String = cfg.moduleName
}
