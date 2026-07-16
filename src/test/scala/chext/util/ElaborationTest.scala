package chext.util

import chisel3.RawModule
import circt.stage.ChiselStage

import java.io.{ByteArrayOutputStream, PrintStream, PrintWriter, StringWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal

object ElaborationTest {
  sealed abstract class Artifact[A](val reportName: String, val missingText: String) {
    final def check(
        description: String
    )(validate: A => Option[String]): CustomCheck[A] =
      CustomCheck(None, this, description, validate)
  }

  sealed abstract class StringArtifact(reportName: String, missingText: String)
      extends Artifact[String](reportName, missingText) {
    final def contains(text: String): Check =
      Check(None, this, text, (contents, needle) => contents.contains(needle), "did not contain")

    final def excludes(text: String): Check =
      Check(None, this, text, (contents, needle) => !contents.contains(needle), "unexpectedly contained")

    final def occurs(text: String, expected: Int): OccurrenceCheck =
      OccurrenceCheck(None, this, text, expected)
  }

  case object SystemVerilog extends StringArtifact("SYSTEMVERILOG", "(not emitted)")
  case object Log extends StringArtifact("LOG", "(empty)")
  case object FailureMessage extends StringArtifact("FAILURE MESSAGE", "(none)")
  case object Errors extends StringArtifact("ERRORS", "(none)")
  case object ModuleGraph
      extends Artifact[chext.elastic.tracking.Graph.Module]("MODULE GRAPH", "(not available)")
  case object ModuleGraphJson extends StringArtifact("MODULE GRAPH", "(not available)")
  case object HdlInfo extends Artifact[hdlinfo.Module]("HDLINFO", "(not available)")
  case object HdlInfoJson extends StringArtifact("HDLINFO", "(not available)")

  def textArtifact(reportName: String, missingText: String = "(empty)"): StringArtifact =
    new StringArtifact(reportName, missingText) {}

  sealed trait ExpectedOutcome
  case object Success extends ExpectedOutcome
  case object Failure extends ExpectedOutcome

  sealed trait Assertion {
    def id: Option[String]
    def named(value: String): Assertion
    private[util] def validate(result: Result): Option[String]
  }

  final case class Check(
      id: Option[String],
      artifact: StringArtifact,
      needle: String,
      predicate: (String, String) => Boolean,
      failurePhrase: String
  ) extends Assertion {
    override def named(value: String): Check = copy(id = Some(value))

    override private[util] def validate(result: Result): Option[String] = {
      val contents = result.contents(artifact)
      Option.unless(predicate(contents, needle))(
        s"${artifact.reportName} $failurePhrase: $needle"
      )
    }
  }

  final case class OccurrenceCheck(
      id: Option[String],
      artifact: StringArtifact,
      needle: String,
      expected: Int
  ) extends Assertion {
    override def named(value: String): OccurrenceCheck = copy(id = Some(value))

    override private[util] def validate(result: Result): Option[String] = {
      val actual = java.util.regex.Pattern
        .quote(needle)
        .r
        .findAllMatchIn(result.contents(artifact))
        .length
      Option.unless(actual == expected)(
        s"${artifact.reportName} contained '$needle' $actual times, expected $expected"
      )
    }
  }

  final case class CustomCheck[A](
      id: Option[String],
      artifact: Artifact[A],
      description: String,
      validateValue: A => Option[String]
  ) extends Assertion {
    override def named(value: String): CustomCheck[A] = copy(id = Some(value))

    override private[util] def validate(result: Result): Option[String] =
      result.get(artifact) match {
        case Some(value) =>
          validateValue(value).map(details =>
            s"${artifact.reportName} failed '$description': $details"
          )
        case None => Some(s"${artifact.reportName} was not available for '$description'")
      }
  }

  private def stackTrace(error: Throwable): String = {
    val writer = new StringWriter()
    error.printStackTrace(new PrintWriter(writer))
    writer.toString
  }

  final case class SuiteConfig(
      name: String,
      outputDir: Path,
      commonChecks: Seq[Assertion] = Seq.empty,
      reportArtifacts: Seq[StringArtifact] = Seq(SystemVerilog, Log, Errors)
  )

  final case class TestCase(
      name: String,
      description: String,
      expected: ExpectedOutcome,
      gen: () => RawModule,
      checks: Seq[Assertion],
      disabledCommonChecks: Set[String],
      captureArtifacts: (RawModule, ArtifactOutput) => Unit
  )

  final class ArtifactOutput private[util] (
      saveValue: (Artifact[_], Any) => Unit
  ) {
    def write(artifact: StringArtifact, contents: String): Unit =
      saveValue(artifact, contents)
  }

  final class Result private[util] (
      private[util] val artifacts: Map[Artifact[_], Any],
      val failure: Option[Throwable]
  ) {
    def get[A](artifact: Artifact[A]): Option[A] =
      artifacts.get(artifact).map(_.asInstanceOf[A])

    def contents(artifact: StringArtifact): String = get(artifact).getOrElse("")
  }

  final case class CaseResult(testCase: TestCase, result: Result, issues: Seq[String]) {
    def passed: Boolean = issues.isEmpty
  }
}

trait ElaborationTest {
  import ElaborationTest._

  protected final val SystemVerilog: StringArtifact = ElaborationTest.SystemVerilog
  protected final val Log: StringArtifact = ElaborationTest.Log
  protected final val FailureMessage: StringArtifact = ElaborationTest.FailureMessage
  protected final val Errors: StringArtifact = ElaborationTest.Errors
  protected final val ModuleGraph: Artifact[chext.elastic.tracking.Graph.Module] =
    ElaborationTest.ModuleGraph
  protected final val ModuleGraphJson: StringArtifact = ElaborationTest.ModuleGraphJson
  protected final val HdlInfo: Artifact[hdlinfo.Module] = ElaborationTest.HdlInfo
  protected final val HdlInfoJson: StringArtifact = ElaborationTest.HdlInfoJson
  protected final val Success: ExpectedOutcome = ElaborationTest.Success
  protected final val Failure: ExpectedOutcome = ElaborationTest.Failure

  protected final def textArtifact(
      reportName: String,
      missingText: String = "(empty)"
  ): StringArtifact = ElaborationTest.textArtifact(reportName, missingText)

  private var suiteConfig = Option.empty[SuiteConfig]
  private val testCases = ArrayBuffer.empty[TestCase]

  protected final def suite(
      name: String,
      outputDir: Path,
      commonChecks: Seq[Assertion] = Seq.empty,
      reportArtifacts: Seq[StringArtifact] = Seq(SystemVerilog, Log, Errors)
  ): Unit = {
    require(suiteConfig.isEmpty, "elaboration test suite must be configured exactly once")
    suiteConfig = Some(SuiteConfig(name, outputDir, commonChecks, reportArtifacts))
  }

  protected final def test(
      name: String,
      description: String = "",
      expected: ExpectedOutcome = Success,
      gen: () => RawModule,
      checks: Seq[Assertion] = Seq.empty,
      disabledCommonChecks: Set[String] = Set.empty,
      captureArtifacts: (RawModule, ArtifactOutput) => Unit = (_, _) => ()
  ): Unit =
    testCases += TestCase(
      name,
      description,
      expected,
      gen,
      checks,
      disabledCommonChecks,
      captureArtifacts
    )

  protected final def runTests(): Unit = {
    val config = suiteConfig.getOrElse(
      throw new IllegalStateException("call suite(...) before runTests()")
    )
    Files.createDirectories(config.outputDir)

    val results = testCases.toSeq.map { testCase =>
      println(s"[${config.name}] running: ${testCase.name}")
      val result = elaborate(testCase.gen(), testCase.captureArtifacts)
      val checks = config.commonChecks.filterNot(check =>
        check.id.exists(testCase.disabledCommonChecks.contains)
      ) ++ testCase.checks
      val issues = validateOutcome(testCase, result) ++ checks.flatMap(_.validate(result))
      val caseResult = CaseResult(testCase, result, issues)

      val status =
        if (!caseResult.passed) "fail"
        else if (testCase.expected == Failure) "expected failure"
        else "pass"
      println(s"[${config.name}] $status: ${testCase.name}")
      caseResult
    }

    results.foreach { result =>
      Files.writeString(
        pathFor(config.outputDir, result.testCase.name),
        reportFor(result, config.reportArtifacts),
        StandardCharsets.UTF_8
      )
    }

    val failures = results.filterNot(_.passed)
    if (failures.nonEmpty) {
      val message = failures
        .map(failure => s"${failure.testCase.name}:\n${failure.issues.mkString("\n")}")
        .mkString("\n\n")
      throw new RuntimeException(
        s"${failures.length} elaboration test(s) failed; see ${config.outputDir}\n$message"
      )
    }

    println(s"[${config.name}] wrote reports under: ${config.outputDir}")
  }

  private def elaborate(
      gen: => RawModule,
      captureArtifacts: (RawModule, ArtifactOutput) => Unit
  ): Result = {
    val logBytes = new ByteArrayOutputStream()
    val logStream = new PrintStream(logBytes)
    val artifacts = scala.collection.mutable.Map.empty[Artifact[_], Any]
    def save[A](artifact: Artifact[A], value: A): Unit = artifacts(artifact) = value
    val artifactOutput = new ArtifactOutput((artifact, value) => artifacts(artifact) = value)

    val failure =
      try {
        val sv = Console.withOut(logStream) {
          Console.withErr(logStream) {
            ChiselStage.emitSystemVerilog(
              {
                val module = gen
                captureArtifacts(module, artifactOutput)

                chext.tracking.onComplete(module) {
                  import io.circe.generic.auto._
                  import io.circe.syntax._

                  chext.elastic.tracking
                    .moduleGraphOption(module)
                    .foreach { graph =>
                      save(ModuleGraph, graph)
                      save(ModuleGraphJson, graph.flatten.asJson.toString())
                    }

                  module match {
                    case annotated: chext.HasHdlinfoModule =>
                      val hdlInfo = annotated.hdlinfoModule
                      save(HdlInfo, hdlInfo)
                      save(HdlInfoJson, hdlInfo.asJson.toString())
                    case _ => ()
                  }

                }
                module
              },
              args = Array("--no-source-info"),
              firtoolOpts = Array(
                "-disable-all-randomization",
                "-strip-debug-info",
                "-default-layer-specialization=enable"
              )
            )
          }
        }
        save(SystemVerilog, sv)
        None
      } catch {
        case NonFatal(error) => Some(error)
      }

    logStream.flush()
    save(Log, logBytes.toString(StandardCharsets.UTF_8))
    failure.foreach { error =>
      save(FailureMessage, Option(error.getMessage).getOrElse(""))
      save(Errors, stackTrace(error))
    }
    new Result(artifacts.toMap, failure)
  }

  private def validateOutcome(testCase: TestCase, result: Result): Seq[String] =
    (testCase.expected, result.failure) match {
      case (Success, Some(error)) =>
        Seq(s"expected emit success, but failed with: ${Option(error.getMessage).getOrElse("")}")
      case (Failure, None) => Seq("expected emit failure, but emit succeeded")
      case _               => Seq.empty
    }

  private def reportFor(caseResult: CaseResult, reportArtifacts: Seq[StringArtifact]): String = {
    val testCase = caseResult.testCase
    val status =
      if (!caseResult.passed) "FAIL"
      else if (testCase.expected == Failure) "EXPECTED FAILURE"
      else "PASS"
    (Seq(
      section("TEST", testCase.name),
      section("DESCRIPTION", testCase.description),
      section("STATUS", status),
      section(
        "ASSERTION ERRORS",
        if (caseResult.issues.isEmpty) "(none)" else caseResult.issues.mkString("\n")
      )
    ) ++ reportArtifacts.map { artifact =>
      val contents = caseResult.result.get(artifact) match {
        case Some(value) if value.nonEmpty => value
        case _                             => artifact.missingText
      }
      section(artifact.reportName, contents)
    }).mkString
  }

  private def section(name: String, body: String): String =
    s"> $name\n$body\n${"=" * 80}\n"

  private def pathFor(outputDir: Path, name: String): Path = {
    val normalized = name.toLowerCase.replaceAll("[^a-z0-9]+", "_").stripPrefix("_").stripSuffix("_")
    outputDir.resolve(s"$normalized.txt")
  }
}
