package chext.amba.axi4

import chisel3.experimental.SourceInfo

private[axi4] case class Diagnostic(
    isError: Boolean,
    message: String,
    lines: Seq[String] = Seq()
) {
  def kind: String = if (isError) "error" else "warning"
}

private[axi4] object Diagnostic {
  def error(message: String, lines: Seq[String] = Seq()): Diagnostic =
    Diagnostic(isError = true, message, lines)

  def warning(message: String, lines: Seq[String] = Seq()): Diagnostic =
    Diagnostic(isError = false, message, lines)
}

private[axi4] object DiagnosticReporter {
  def emit(
      prefix: String,
      sourceInfo: SourceInfo,
      diagnostics: Seq[Diagnostic],
      contextLines: Seq[String],
      failureMessage: String
  ): Unit = {
    if (diagnostics.nonEmpty) {
      val source = sourceInfo.makeMessage((x) => x)
      println(f"$prefix : diagnostics $source")

      diagnostics.foreach { diagnostic =>
        println(f"$prefix : ${diagnostic.kind}: ${diagnostic.message}")
        diagnostic.lines.foreach { line => println(f"$prefix :   $line") }
      }

      contextLines.foreach { line => println(f"$prefix : $line") }

      if (diagnostics.exists(_.isError))
        throw new IllegalArgumentException(failureMessage)
    }
  }
}
