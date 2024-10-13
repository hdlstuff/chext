package chext

import chisel3._

private object emitHdlinfo {
  def apply(module: hdlinfo.Module, targetDir: String): Unit = {
    import io.circe.syntax._
    import io.circe.generic.auto._
    import java.io.PrintWriter

    val pw = new PrintWriter(f"${targetDir}/${module.name}.hdlinfo.json")
    pw.write(module.asJson.toString())
    pw.close()
  }
}

trait TestBench extends App {
  private val _pkgName = Option(this.getClass.getPackage).map(_.getName).getOrElse(".")

  def ns(x: String): String = f"${_pkgName}.${x}".stripPrefix(".")

  def emit[T <: chext.Module](genModule: => T): Unit = {
    val pkgPath = _pkgName.replace('.', '/')
    val hdlPath = f"./sysc_tb/${pkgPath}/hdl/"
    var cfg: chext.ModuleConfig = null
    emitVerilog(
      {
        val module = genModule
        cfg = module.cfg
        module
      },
      Array("--target-dir", hdlPath)
    )
    emitHdlinfo(cfg.hdlinfoModule, hdlPath)
  }
}
