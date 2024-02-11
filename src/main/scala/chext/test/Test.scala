package chext.test

import chisel3._
import chiseltest._

import org.scalatest.freespec.AnyFreeSpec
import scala.collection.mutable.ArrayBuffer

/** Provides some useful functionality for tests.
  */
trait TestMixin {
  protected val rand = new scala.util.Random

  protected def stepRandom(maxExclusive: Int): Unit =
    stepRandom(1, maxExclusive)

  protected def stepRandom(minInclusive: Int, maxExclusive: Int): Unit = {
    assert(minInclusive > 0, "The number of steps should be at least 1.")
    step(rand.between(minInclusive, maxExclusive))
  }
}

abstract class TesterBase extends TestMixin {
  def onTest(): Unit
  def onTimeout(e: TimeoutException): Unit = throw e
  def run() = onTest()
}

abstract class TesterSpec extends AnyFreeSpec with ChiselScalatestTester {
  private val annotations = ArrayBuffer.empty[firrtl2.annotations.Annotation]

  protected final def clearAnnotations() = annotations.clear()
  protected final def addAnnotation(anno: firrtl2.annotations.Annotation) =
    annotations.addOne(anno)
  protected final def enableVcd() =
    addAnnotation(WriteVcdAnnotation)
  protected final def useVerilator() =
    addAnnotation(VerilatorBackendAnnotation)

  override def test[T <: Module](dutGen: => T): TestBuilder[T] =
    super.test(dutGen).withAnnotations(annotations.toSeq)

  /** @note
    *   Should probably return a TestResult or something. Figure out later.
    */
  def testWithTester[T <: Module](
      dutGen: => T
  )(testerGen: (T) => TesterBase): Unit = {
    var tester = Option.empty[TesterBase]

    try {
      test(dutGen).withAnnotations(annotations.toSeq) { (dut) =>
        {
          tester = Some(testerGen(dut))
          tester.get.run()
          chiseltest.step()
        }
      }
    } catch {
      case e: TimeoutException => tester.get.onTimeout(e)
      case e: Exception        => throw e
    }
  }
}
