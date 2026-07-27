package chext.amba.axi4.full.components

import chisel3._
import chisel3.util._
import chisel3.experimental.{prefix, SourceInfo}

import chext.amba.axi4
import chext.elastic
import chext.util.SimulationCheck

import chext.util.BitOps._
import elastic.ConnectOp._

import helpers.{SteerLeft, SteerRight}

/** Configuration for [[Downscale]].
  *
  * The slave interface must have no transaction IDs (`wId == 0`), and input traffic must contain
  * only single-beat transactions (`ARLEN == 0` and `AWLEN == 0`). `Downscale` may turn one wide
  * input beat into a multi-beat transaction on its narrower master interface.
  */
case class DownscaleConfig(
    val axiSlaveCfg: axi4.Config,
    val wDataMaster: Int,
    val numOutstandingRead: Int = 32,
    val numOutstandingWrite: Int = 32,
    val simCheckBurst: SimulationCheck = SimulationCheck.Default
) {
  private val require_ = chext.util.Require.inferred()

  require_(axiSlaveCfg.wId == 0, "axiSlaveCfg.wId must be zero!")
  require_(!axiSlaveCfg.lite, "axiSlaveCfg.lite must be false!")
  require_(wDataMaster < axiSlaveCfg.wData, "wDataMaster must be < axiSlaveCfg.wData")

  require_(wDataMaster >= 8)
  require_(isPow2(wDataMaster))

  require_(axiSlaveCfg.wUserR == 0, "User data is not supported on channel R.")

  require_(
    axiSlaveCfg.wData / wDataMaster <=
      (if (axiSlaveCfg.axi3Compat) 16 else 256),
    "axiSlaveCfg.wData / wDataMaster must be less than 16 or 256 depending on the AXI version!"
  )

  val wDataSlave = axiSlaveCfg.wData
  val wStrobeMaster = wDataMaster / 8
  val wStrobeSlave = wDataSlave / 8
  val wOffset = log2Ceil(wDataSlave) - log2Ceil(wDataMaster)
  val wAddr = axiSlaveCfg.wAddr
  val axsizeMaxMaster = log2Ceil(wDataMaster >> 3)
  val axiMasterCfg = axiSlaveCfg.copy(wData = wDataMaster)
}

/** Reduces the data width of single-beat, ID-free AXI4-Full traffic.
  *
  * This module does not decompose input bursts. Place `Unburst` before it unless single-beat input
  * is guaranteed, and place `Unburst` after it when the downstream interface requires single-beat
  * transactions.
  */
class Downscale(val cfg: DownscaleConfig) extends Module with chext.AnnotatedModule {
  import cfg._

  val s_axi = IO(axi4.full.Slave(axiSlaveCfg))
  val m_axi = IO(axi4.full.Master(axiMasterCfg))

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)
  declareAxi4Interface(m_axi)

  private val resolver = new Downscale_Resolver(this)

  if (axiSlaveCfg.read)
    simCheckBurst(
      !s_axi.ar.$valid || s_axi.ar.$bits.len === 0.U,
      "axi4.full.components.Downscale: ARLEN must be zero"
    )
  if (axiSlaveCfg.write)
    simCheckBurst(
      !s_axi.aw.$valid || s_axi.aw.$bits.len === 0.U,
      "axi4.full.components.Downscale: AWLEN must be zero"
    )

  private val genOffsetLast = chext.bundles.BundleN(UInt(wOffset.W), Bool())

  private def implRead(): Unit = prefix("read") {
    val addressGenerator = Module(new AddressGenerator(log2Ceil(wDataSlave >> 3)))
    val ewireOffsetLastQueue = elastic.EWire(genOffsetLast)

    def implAR(): Unit = prefix("ar") {
      val arTransformed = elastic.EWire.like(m_axi.ar)

      val transform0 = new elastic.Transform(s_axi.ar, arTransformed) {
        out := in

        out.burst := axi4.BurstType.INCR

        when(in.size <= axsizeMaxMaster.U) {
          out.size := in.size
          out.len := 0.U
        }.otherwise {
          out.size := axsizeMaxMaster.U
          out.len := (1.U << (in.size - axsizeMaxMaster.U)) - 1.U
        }
      }

      val fork0 = new elastic.Fork(arTransformed) {
        val transform0 = new elastic.Transform(
          fork(),
          elastic.SinkBuffer(addressGenerator.source, numOutstandingRead)
        ) {
          out.addr := in.addr
          out.len := in.len
          out.size := in.size
          out.burst := axi4.BurstType.INCR
          out.user := 0.U
        }

        fork() :=> m_axi.ar
      }

      val transform1 = new elastic.Transform(addressGenerator.sink, ewireOffsetLastQueue) {
        out._1 := in.addr.dropLsbN(log2Ceil(wDataMaster >> 3))
        out._2 := in.last
      }
    }

    def implR(): Unit = prefix("r") {
      val zipped = elastic.Zip(m_axi.r, ewireOffsetLastQueue)

      val transducerReduceResp = new elastic.Transducer(zipped, s_axi.r) {
        val dataReg = RegInit(0.U(axiSlaveCfg.wData.W))
        val respReg = RegInit(0.U(2.W))

        val steerLeft = Module(new SteerLeft(wDataMaster, wDataSlave))

        steerLeft.dataIn := in._1.data
        steerLeft.offsetIn := in._2._1 /* offset */

        out.id := in._1.id
        out.data := dataReg | steerLeft.dataOut
        out.resp := Mux(in._1.resp > respReg, in._1.resp, respReg)
        out.last := true.B
        out.user := in._1.user

        packet {
          when(in._1.last) {
            accept {
              dataReg := 0.U
              respReg := 0.U
            }
          }.otherwise {
            consume {
              dataReg := out.data
              respReg := out.resp
            }
          }
        }
      }
    }

    implAR()
    implR()
  }

  private def implWrite(): Unit = prefix("write") {
    val addressGenerator = Module(new AddressGenerator(log2Ceil(wDataSlave >> 3)))
    val ewireOffsetLast = elastic.EWire(genOffsetLast)

    def implAW(): Unit = prefix("aw") {
      val awTransformed = elastic.EWire.like(m_axi.aw)

      val transform0 = new elastic.Transform(s_axi.aw, awTransformed) {
        out := in

        out.burst := axi4.BurstType.INCR

        when(in.size <= axsizeMaxMaster.U) {
          out.size := in.size
          out.len := 0.U
        }.otherwise {
          out.size := axsizeMaxMaster.U
          out.len := (1.U << (in.size - axsizeMaxMaster.U)) - 1.U
        }
      }

      val fork0 = new elastic.Fork(awTransformed) {
        val transform0 = new elastic.Transform(
          fork(),
          elastic.SinkBuffer( addressGenerator.source, numOutstandingWrite)
        ) {
          out.addr := in.addr
          out.len := in.len
          out.size := in.size
          out.burst := axi4.BurstType.INCR
          out.user := 0.U
        }

        fork() :=> m_axi.aw
      }

      val transform1 = new elastic.Transform(addressGenerator.sink, ewireOffsetLast) {
        out._1 := in.addr.dropLsbN(log2Ceil(wDataMaster >> 3))
        out._2 := in.last
      }
    }

    def implW(): Unit = prefix("w") {
      ewireOffsetLast.nodeq()
      ewireOffsetLast.markSource()

      val transducerRepeatData = new elastic.Transducer(s_axi.w, m_axi.w) {
        val bits = ewireOffsetLast.$bits
        val valid = ewireOffsetLast.$valid

        val steerRight = Module(new SteerRight(wDataSlave, wDataMaster))
        val steerRightStrobe = Module(new SteerRight(wStrobeSlave, wStrobeMaster))

        steerRight.dataIn := in.data
        steerRight.offsetIn := bits._1 /* offset */

        steerRightStrobe.dataIn := in.strb
        steerRightStrobe.offsetIn := bits._1 /* offset */

        out.data := steerRight.dataOut
        out.strb := steerRightStrobe.dataOut
        out.last := bits._2 /* last */
        out.user := in.user

        packet {
          when(valid) {
            when(bits._2 /* last */ ) {
              accept { ewireOffsetLast.deq() }
            }.otherwise {
              produce { ewireOffsetLast.deq() }
            }
          }.otherwise {
            stall {}
          }
        }
      }
    }

    def implB(): Unit = {
      m_axi.b :=> s_axi.b
    }

    implAW()
    implW()
    implB()
  }

  if (axiSlaveCfg.read) implRead()
  if (axiSlaveCfg.write) implWrite()
}

private final class Downscale_Resolver(owner: Downscale)(implicit sourceInfo: SourceInfo)
    extends axi4.tracking.Resolver(owner) {
  import axi4.tracking._

  bindSlave(owner.s_axi)
  bindMaster(owner.m_axi)

  private val slaveFullSize = values.BurstShape.fullSize(owner.cfg.axiSlaveCfg.wData)
  private val masterFullSize = values.BurstShape.fullSize(owner.cfg.axiMasterCfg.wData)
  private val incr = axi4.BurstType.INCR.litValue.toInt
  private val inputTypes =
    Set(axi4.BurstType.FIXED.litValue.toInt, incr)

  if (owner.cfg.axiSlaveCfg.read) {
    owner.s_axi.slaveProps(properties.Slave.ReadThreadMode) =
      values.ThreadMode.SingleThread
    owner.m_axi.masterProps(properties.Master.ReadThreadMode) =
      values.ThreadMode.SingleThread
  }
  if (owner.cfg.axiSlaveCfg.write) {
    owner.s_axi.slaveProps(properties.Slave.WriteThreadMode) =
      values.ThreadMode.SingleThread
    owner.m_axi.masterProps(properties.Master.WriteThreadMode) =
      values.ThreadMode.SingleThread
  }

  private def outputSize(inputSize: Int): Int =
    inputSize min masterFullSize

  private def outputBeats(inputSize: Int): Int =
    1 << ((inputSize - masterFullSize) max 0)

  private def masterShape(input: values.BurstShape): values.BurstShape = {
    val sizes = input.burstSizes.map(outputSize)
    if (sizes.isEmpty)
      values.BurstShape()
    else
      values.BurstShape(
        burstBeats = input.burstSizes.map(outputBeats).max,
        burstNarrow = sizes.exists(_ < masterFullSize),
        burstTypes = Set(incr),
        burstSizes = sizes
      )
  }

  private def slaveShape(downstream: values.BurstShape): values.BurstShape = {
    val acceptsIncr = downstream.burstTypes.contains(incr)
    val sizes =
      values.BurstShape
        .validSizes(owner.cfg.axiSlaveCfg.wData)
        .filter { inputSize =>
          acceptsIncr &&
          downstream.burstSizes.contains(outputSize(inputSize)) &&
          downstream.burstBeats >= outputBeats(inputSize)
        }

    if (sizes.isEmpty)
      values.BurstShape()
    else
      values.BurstShape(
        burstBeats = 1,
        burstNarrow = sizes.exists(_ < slaveFullSize),
        burstTypes = inputTypes,
        burstSizes = sizes
      )
  }

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case SlaveRequests(MemoryMap()) =>
        forwardTo(request, owner.m_axi)
      case Request(TrafficProfile()) =>
        request.incomplete()
      case MasterRequests(BurstShape()) =>
        mapFrom(request, owner.s_axi)(masterShape)
      case SlaveRequests(BurstShape()) =>
        mapFrom(request, owner.m_axi)(slaveShape)
      case _ =>
        missingCase(request)
    }
}
