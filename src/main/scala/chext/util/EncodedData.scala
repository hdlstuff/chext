package chext.util

import chisel3._

case class EncodedDataEntry(
    val path: String,
    val width: Int,
    val tpe: String
)

case class EncodedData(
    val entries: Seq[EncodedDataEntry]
) {
  assert(entries.length >= 1)

  if (entries.length == 1)
    assert(entries.head.path == "")
}

private class Encoder(gen: Data) {
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

  val encodedData = EncodedData(buffer.toSeq)
}

object EncodedData {
  def encode(gen: Data): EncodedData = {
    (new Encoder(gen)).encodedData
  }
}

// TODO: Make the following a proper test
/*

import chisel3._

class MyTestBundle extends Bundle {
  val f0 = UInt(8.W)
  val f1 = UInt(18.W)

  val f2 = Vec(4, UInt(6.W))
  val f3 = new Bundle {
    val f0 = UInt(15.W)
    val f1 = Bool()
  }
}

object TestApp extends App {
  println(EncodedData.encode(new MyTestBundle))
}

*/
