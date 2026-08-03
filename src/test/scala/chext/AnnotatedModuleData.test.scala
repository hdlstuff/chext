package chext

import chisel3._

import chext.util.ElaborationTest

import java.nio.file.Path

private class AnnotatedModuleDataTestTop extends Module with AnnotatedModule {
  val enable = IO(Input(Bool()))
  val oneBit = IO(Input(UInt(1.W)))
  val request = IO(Input(UInt(12.W)))
  val result = IO(Output(UInt(40.W)))
  val signedResult = IO(Output(SInt(9.W)))

  result := request
  signedResult := request.asSInt

  declareClock(clock)
  declareReset(reset)
  declareData(enable)
  declareData(oneBit)
  declareData(request)
  declareData(result)
  declareData(signedResult, portName = Some("signed_result"))
}

private class AnnotatedModuleUnsupportedDataTestTop extends Module with AnnotatedModule {
  class Payload extends Bundle {
    val value = UInt(8.W)
  }

  val payload = IO(Input(new Payload))

  declareClock(clock)
  declareReset(reset)
  declareData(payload)
}

private class AnnotatedModuleUnknownWidthDataTestTop extends Module with AnnotatedModule {
  val data = IO(Input(UInt()))

  declareClock(clock)
  declareReset(reset)
  declareData(data)
}

object AnnotatedModuleData_Test extends App with ElaborationTest {
  suite(
    name = "annotated-module-data",
    outputDir = Path.of("output", "annotated_module_data")
  )

  test(
    name = "supported_data_ports",
    gen = () => new AnnotatedModuleDataTestTop,
    checks = Seq(
      HdlInfo.check("data port metadata") { module =>
        val ports = module.ports.map(port => port.name -> port).toMap

        def check(
            name: String,
            direction: hdlinfo.PortDirection,
            isBus: Boolean,
            busRange: (Int, Int)
        ): Option[String] =
          ports.get(name) match {
            case None => Some(s"missing port: $name")
            case Some(port) =>
              Option.unless(
                port.direction == direction &&
                  port.kind == hdlinfo.PortKind.data &&
                  port.sensitivity == hdlinfo.PortSensitivity.none &&
                  port.isBus == isBus &&
                  port.busRange == busRange &&
                  port.associatedClock == "clock" &&
                  port.associatedReset == "reset"
              )(s"unexpected metadata for $name: $port")
          }

        Seq(
          check("enable", hdlinfo.PortDirection.input, false, (0, 0)),
          check("oneBit", hdlinfo.PortDirection.input, false, (0, 0)),
          check("request", hdlinfo.PortDirection.input, true, (11, 0)),
          check("result", hdlinfo.PortDirection.output, true, (39, 0)),
          check("signed_result", hdlinfo.PortDirection.output, true, (8, 0))
        ).flatten.headOption
      }
    )
  )

  test(
    name = "unsupported_aggregate",
    expected = Failure,
    gen = () => new AnnotatedModuleUnsupportedDataTestTop,
    checks = Seq(
      Errors contains "unsupported data port type",
      Errors contains "Expected Bool, UInt, or SInt."
    )
  )

  test(
    name = "unknown_width",
    expected = Failure,
    gen = () => new AnnotatedModuleUnknownWidthDataTestTop,
    checks = Seq(Errors contains "data port width must be known")
  )

  runTests()
}
