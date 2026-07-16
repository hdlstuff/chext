package chext.amba.axi4.util

import chisel3._

import chext.elastic

object Decoder {
  /** A decoded address paired with its destination port. */
  final class Response(val wAddr: Int, val wPort: Int) extends Bundle {
    val address = UInt(wAddr.W)
    val port = UInt(wPort.W)
  }

  private final case class ResolvedSegment(
      name: String,
      baseAddress: BigInt,
      size: BigInt,
      index: Int
  ) {
    def endAddress: BigInt = baseAddress + size
  }

}

/** Combinational elastic address decoder synthesized from routed memory maps. */
final class Decoder(
    val wAddr: Int,
    val wPort: Int,
    val routes: Seq[(MemoryMap, Int)],
    val defaultPort: Int = 0
)
    extends Module
    with chext.AnnotatedModule {
  require(wAddr > 0, "Decoder address width must be positive")
  require(wPort >= 0, "Decoder port width must not be negative")

  private val addressLimit = BigInt(1) << wAddr
  private val portLimit = BigInt(1) << wPort
  private val segments = routes.flatMap { case (memoryMap, index) =>
    val flattened = memoryMap.flatten
    flattened.segments.map { segment =>
      Decoder.ResolvedSegment(
        (flattened.path ++ segment.path).mkString("/", "/", ""),
        flattened.offset + segment.baseAddress,
        segment.size,
        index
      )
    }
  }

  require(
    BigInt(defaultPort) < portLimit,
    s"Decoder default port $defaultPort does not fit in wPort=$wPort"
  )

  segments.foreach { segment =>
    require(
      segment.endAddress <= addressLimit,
      s"Memory map segment '${segment.name}' exceeds wAddr=$wAddr"
    )
    require(
      BigInt(segment.index) < portLimit,
      s"Memory map segment '${segment.name}' index ${segment.index} does not fit in wPort=$wPort"
    )
  }

  segments.combinations(2).foreach {
    case Seq(a, b) =>
      require(
        a.endAddress <= b.baseAddress || b.endAddress <= a.baseAddress,
        s"Memory map segments '${a.name}' and '${b.name}' overlap"
      )
    case _ => ()
  }

  val dec_req = IO(elastic.Source(UInt(wAddr.W)))
  val dec_resp = IO(elastic.Sink(new Decoder.Response(wAddr, wPort)))

  declareClock(clock)
  declareReset(reset)
  declareElasticInterface(dec_req, "Address")
  declareElasticInterface(dec_resp, "Port")

  val transform0 = new elastic.Transform(dec_req, dec_resp) {
    out.address := in

    val decoded = WireDefault(defaultPort.U(wPort.W))
    segments.foreach { segment =>
      val atOrAboveBase = in >= segment.baseAddress.U(wAddr.W)
      val belowEnd =
        if (segment.endAddress == addressLimit) true.B
        else in < segment.endAddress.U(wAddr.W)

      when(atOrAboveBase && belowEnd) {
        decoded := segment.index.U(wPort.W)
      }
    }
    out.port := decoded
  }
}
