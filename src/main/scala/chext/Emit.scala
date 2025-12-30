package chext

import chisel3._

import circt.stage.ChiselStage
import java.io.PrintWriter

object emitSystemVerilog {
  def apply[M <: RawModule](
      gen: => M,
      hdlPath: String = "./",
      argsOverride: Option[Array[String]] = Option.empty,
      firtoolOptsOverride: Option[Array[String]] = Option.empty
  ): Unit = {

    var moduleName: String = null

    val svString =
      ChiselStage.emitSystemVerilog(
        {
          val module = gen
          moduleName = module.name
          module
        },
        args = argsOverride.getOrElse(Array("--no-source-info")),
        firtoolOpts = firtoolOptsOverride.getOrElse(
          Array(
            "-disable-all-randomization",
            "-strip-debug-info",
            "-default-layer-specialization=enable"
          )
        )
      )

    val pw = new PrintWriter(f"${hdlPath}/${moduleName}.sv")
    pw.write(svString.toString())
    pw.close()
  }
}
