package chext.util

import chisel3._

case class EncodedDataEntry(
    val path: String,
    val width: Int,
    val tpe: String
)

case class EncodedData(
    val name: String,
    val entries: Seq[EncodedDataEntry]
)

case class EncodedDataList(val seq: Seq[EncodedData])

private class Encoder(name: String, gen: Data) {
  private val buffer = scala.collection.mutable.ArrayBuffer.empty[EncodedDataEntry]
  private var pathStack = scala.collection.mutable.Stack.empty[String]

  private def currentPath = pathStack.reverseIterator.mkString("_")
  private def pushPath(x: String): Unit = pathStack.push(x)
  private def popPath(): Unit = pathStack.pop()

  private def processVec(gen: Vec[_]): Unit = {
    for (i <- (0 until gen.length)) { //
      pushPath(f"$i")
      processData(gen(i).asInstanceOf[Data])
      popPath()
    }
  }

  private def processRecord(gen: Record): Unit = {
    gen.elements.toSeq.reverse.foreach { //
      case (name, data) => { //
        pushPath(name)
        processData(data)
        popPath()
      }
    }
  }

  private def processElement(gen: Element): Unit = {
    buffer.addOne(EncodedDataEntry(currentPath, gen.getWidth, gen.toString()))
  }

  private def processData(gen: Data): Unit = {
    if (gen.isInstanceOf[Element])
      processElement(gen.asInstanceOf[Element])
    else if (gen.isInstanceOf[Vec[_]])
      processVec(gen.asInstanceOf[Vec[_]])
    else if (gen.isInstanceOf[Record])
      processRecord(gen.asInstanceOf[Record])
    else
      throw new RuntimeException(f"unknown type: ${gen.toString()}")
  }

  processData(gen)

  val encodedData = EncodedData(name, buffer.toSeq)
}

object EncodedData {
  def encode(name: String, gen: Data): EncodedData = {
    (new Encoder(name, gen)).encodedData
  }
}

class EncodedDataBuilder {
  private val encodedDataBuffer = scala.collection.mutable.ArrayBuffer.empty[EncodedData]
  private val addedNames = scala.collection.mutable.Set.empty[String]

  def add[T <: Data](name: String, gen: T) = {
    if (addedNames.contains(name))
      throw new RuntimeException(f"a data with the given name '$name' is already added!")

    encodedDataBuffer.addOne(EncodedData.encode(name, gen))
    addedNames.addOne(name)
  }
  def build() = {
    import io.circe.generic.auto._
    "encodedDataList" -> hdlinfo.TypedObject(EncodedDataList(encodedDataBuffer.toSeq))
  }
}

// TODO: Make the following a proper test
import chisel3._

class MyTestBundle1 extends Bundle {
  val f0 = UInt(8.W)
  val f1 = UInt(18.W)

  val f2 = Vec(4, UInt(6.W))
  val f3 = new Bundle {
    val f0 = UInt(15.W)
    val f1 = Bool()
  }
}

class MyTestBundle2 extends Bundle {
  val f0 = Vec(4, UInt(6.W))
  val f1 = new Bundle {
    val f0 = UInt(15.W)
    val f1 = Bool()
  }
}

object TestApp extends App {
  val encodedData = EncodedData.encode("MyTestBundle", new MyTestBundle1)
  import io.circe.generic.auto._
  import io.circe.syntax._

  println(hdlinfo.TypedObject(encodedData.asJson).toString())

  val encodedDataBuilder = new EncodedDataBuilder
  encodedDataBuilder.add("MyTestBundle1", new MyTestBundle1)
  encodedDataBuilder.add("MyTestBundle2", new MyTestBundle2)
  println(encodedDataBuilder.build().asJson.toString())
}
