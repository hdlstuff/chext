package chext.test

import chisel3._
import chiseltest._

import org.scalatest.freespec.AnyFreeSpec
import scala.collection.mutable.ArrayBuffer

abstract class TesterSpec extends AnyFreeSpec with ChiselScalatestTester {
  private val annotations = ArrayBuffer.empty[firrtl2.annotations.Annotation]
  protected val rand = new scala.util.Random

  protected final def clearAnnotations() = annotations.clear()
  protected final def addAnnotation(anno: firrtl2.annotations.Annotation) =
    annotations.addOne(anno)
  protected final def enableVcd() =
    addAnnotation(WriteVcdAnnotation)
  protected final def useVerilator() =
    addAnnotation(VerilatorBackendAnnotation)

  override def test[T <: Module](dutGen: => T): TestBuilder[T] =
    super.test(dutGen).withAnnotations(annotations.toSeq)

  def stepRandom(maxExclusive: Int): Unit = stepRandom(1, maxExclusive)

  def stepRandom(minInclusive: Int, maxExclusive: Int): Unit = {
    assert(minInclusive > 0, "The number of steps should be at least 1.")
    step(rand.between(minInclusive, maxExclusive))
  }
}
