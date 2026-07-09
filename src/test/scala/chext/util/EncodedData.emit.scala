package chext.util

import chisel3._

// TODO: Make the following a proper test.
private object TestApp extends App {
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

  val encodedData = EncodedData.encode("MyTestBundle", new MyTestBundle1)
  import io.circe.generic.auto._
  import io.circe.syntax._

  println(hdlinfo.TypedObject(encodedData.asJson).toString())

  val encodedDataBuilder = new EncodedDataBuilder
  encodedDataBuilder.add("MyTestBundle1", new MyTestBundle1)
  encodedDataBuilder.add("MyTestBundle2", new MyTestBundle2)
  println(encodedDataBuilder.build().asJson.toString())
}
