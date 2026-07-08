package chext.util

import chisel3.experimental.SourceInfo

final class Require private (
    val identifier: String,
    val sourceInfo: Option[SourceInfo] = None
) {
  private val indent = " " * identifier.length

  private def sourceLine(label: String, si: SourceInfo): Option[String] =
    Option(si).map { si => s"$label: ${si.makeMessage(identity).trim}" }

  private def format(
      message: String,
      lines: Seq[String],
      hereInfo: Option[SourceInfo]
  ): String = {
    val first = f"Requirement failed\n${identifier} : $message"
    val sourceLines =
      hereInfo.flatMap(sourceLine("local SourceInfo  ", _)).toSeq ++
        sourceInfo.flatMap(sourceLine("global SourceInfo ", _)).toSeq
    val rest = (sourceLines ++ lines).map { line => f"${indent}   $line" }
    (first +: rest).mkString(System.lineSeparator())
  }

  private def raise(
      message: String,
      lines: Seq[String],
      hereInfo: Option[SourceInfo] = None
  ): Nothing =
    throw new IllegalArgumentException(format(message, lines, hereInfo))

  def apply(cond: sourcecode.Text[Boolean]): Unit = {
    if (!cond.value)
      raise(s"requirement failed: ${cond.source}", Seq.empty)
  }

  def apply(cond: sourcecode.Text[Boolean], message: String)(implicit
      si: SourceInfo = null
  ): Unit = {
    if (!cond.value)
      raise(message, Seq(s"requirement: ${cond.source}"))
  }

  def apply(
      cond: sourcecode.Text[Boolean],
      message: String,
      lines: Seq[String]
  )(implicit si: SourceInfo): Unit = {
    if (!cond.value)
      raise(message, s"requirement: ${cond.source}" +: lines)
  }

  def here(cond: sourcecode.Text[Boolean], message: String)(implicit
      si: SourceInfo
  ): Unit =
    if (!cond.value)
      failHere(message, Seq(s"requirement: ${cond.source}"))

  def here(
      cond: sourcecode.Text[Boolean],
      message: String,
      lines: Seq[String]
  )(implicit si: SourceInfo): Unit =
    if (!cond.value)
      failHere(message, s"requirement: ${cond.source}" +: lines)

  def fail(message: String): Nothing =
    fail(message, Seq.empty)

  def fail(message: String, lines: Seq[String]): Nothing =
    raise(message, lines)

  def failHere(message: String)(implicit si: SourceInfo): Nothing =
    failHere(message, Seq.empty)

  def failHere(message: String, lines: Seq[String])(implicit
      si: SourceInfo
  ): Nothing =
    raise(message, lines, Option(si))
}

object Require {
  def apply(identifier: String): Require =
    new Require(identifier)

  def apply(identifier: String, sourceInfo: Option[SourceInfo]): Require =
    new Require(identifier, sourceInfo)

  def inferred(): Require =
    new Require(inferIdentifier())

  def inferred(sourceInfo: SourceInfo): Require =
    new Require(inferIdentifier(), Option(sourceInfo))

  private def inferIdentifier(): String = {
    val stack = Thread.currentThread().getStackTrace()
    stack
      .map(_.getClassName)
      .find(isUserFrame)
      .map(normalizeClassName)
      .getOrElse("unknown")
  }

  private def isUserFrame(className: String): Boolean =
    !className.startsWith("java.lang.Thread") &&
      !className.startsWith("chext.util.Require") &&
      !className.startsWith("scala.") &&
      !className.startsWith("java.lang.reflect.") &&
      !className.startsWith("jdk.internal.reflect.")

  private def normalizeClassName(className: String): String =
    className
      .stripSuffix("$")
      .replace("$package", "")
      .replace('$', '.')
      .replaceAll("\\.anon\\$[0-9]+", ".anon")
}
