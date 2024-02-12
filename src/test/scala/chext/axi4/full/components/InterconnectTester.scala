package chext.axi4.full.components

import chisel3._
import chisel3.util._

import chiseltest._

import chext.axi4
import chext.util.Expect

trait InterconnectHelper[M <: Module] {
  def slaveInterfaces(module: M): Seq[axi4.full.Interface]
  def masterInterfaces(module: M): Seq[axi4.full.Interface]
}

abstract class InterconnectTester[T <: Module](
    val dut: T,
    val logEnabled: Boolean = true
)(implicit val helper: InterconnectHelper[T])
    extends chext.test.TestMixin {

  private val slaveInterfaces = helper.slaveInterfaces(dut)
  private val masterInterfaces = helper.masterInterfaces(dut)

  private var counter = 0

  val axiSlaveConfig = slaveInterfaces(0).cfg
  require(slaveInterfaces.forall { _.cfg == axiSlaveConfig })

  val axiMasterConfig = masterInterfaces(0).cfg
  require(masterInterfaces.forall { _.cfg == axiMasterConfig })

  val threadInfoShift = axiSlaveConfig.wId
  val threadInfoMask = (1 << threadInfoShift) - 1
  val numThreadsPerSlave = 1 << threadInfoShift
  val numThreads = slaveInterfaces.length * numThreadsPerSlave

  import scala.collection.mutable.Queue

  import axi4.full._
  import axi4.full.test._
  import axi4.full.test.PacketUtils._

  class ThreadInfo {
    val arTaskQueue = Queue.empty[AddressPacket]
    val rTaskQueue = Queue.empty[Seq[ReadDataPacket]]
    val awTaskQueue = Queue.empty[AddressPacket]
    val wTaskQueue = Queue.empty[Seq[WriteDataPacket]]
    val bTaskQueue = Queue.empty[WriteResponsePacket]

    val arExpectedQueue = Queue.empty[AddressPacket]
    val rExpectedQueue = Queue.empty[Seq[ReadDataPacket]]
    val awExpectedQueue = Queue.empty[AddressPacket]
    val wExpectedQueue = Queue.empty[Seq[WriteDataPacket]]
    val bExpectedQueue = Queue.empty[WriteResponsePacket]
  }

  class MasterInfo {
    var arReceiveCount = 0
    var awReceiveCount = 0
    var wReceiveCount = 0
  }

  class SlaveInfo {
    var rReceiveCount = 0
    var bReceiveCount = 0
  }

  protected def log(x: String) =
    if (logEnabled) println(f"[t = ${counter}%6d] $x")

  protected def logSlave(
      slaveIdx: Int,
      event: String,
      o: Object = null
  ): Unit = {
    val objString = if (o != null) o.toString() else ""
    log(f"[Slave ${slaveIdx}] ${event} ${objString}")
  }

  protected def logMaster(
      masterIdx: Int,
      event: String,
      o: Object = null
  ): Unit = {
    val objString = if (o != null) o.toString() else ""
    log(f"[Master ${masterIdx}] ${event} ${objString}")
  }

  val threadInfos = Array.fill(numThreads) { new ThreadInfo }
  val masterInfos = Array.fill(masterInterfaces.length) { new MasterInfo }
  val slaveInfos = Array.fill(slaveInterfaces.length) { new SlaveInfo }

  private def handleMaster(masterIdx: Int) = {
    // behavior of a master
    val master = masterInterfaces(masterIdx)
    val masterInfo = masterInfos(masterIdx)

    fork {
      while (masterInfo.arReceiveCount > 0) {
        logMaster(
          masterIdx,
          f"Remaining AR packets = ${masterInfo.arReceiveCount}"
        )
        logMaster(masterIdx, "waiting for read address")
        val arPacket = master.receiveReadAddress()
        val threadInfo = threadInfos(arPacket.id)

        logMaster(masterIdx, "received read address", arPacket)

        assert(
          threadInfo.arExpectedQueue.nonEmpty,
          "threadInfo.arExpectedQueue.nonEmpty"
        )
        Expect.equals(threadInfo.arExpectedQueue.head, arPacket)
        threadInfo.arExpectedQueue.removeHead()

        assert(threadInfo.rTaskQueue.nonEmpty, "threadInfo.rTaskQueue.nonEmpty")
        val rNext = threadInfo.rTaskQueue.head
        threadInfo.rTaskQueue.removeHead()
        assert(
          rNext.forall { _.id == arPacket.id },
          "rNext.forall { _.id == arPacket.id }"
        )
        val id = arPacket.id & threadInfoMask
        threadInfo.rExpectedQueue.addOne(rNext.map { _.copy(id = id) })

        stepRandom(16)
        logMaster(masterIdx, "send read data", rNext)
        master.sendReadDataBurst(rNext)

        masterInfo.arReceiveCount -= 1
      }
    }.fork {
      while (masterInfo.awReceiveCount > 0 || masterInfo.wReceiveCount > 0) {
        logMaster(
          masterIdx,
          f"Remaining AW packets = ${masterInfo.awReceiveCount}, W packets = ${masterInfo.wReceiveCount}"
        )

        var awPacket_ = Option.empty[AddressPacket]
        var wPacket_ = Option.empty[Seq[WriteDataPacket]]

        fork {
          if (masterInfo.awReceiveCount > 0) {
            logMaster(masterIdx, "waiting for write address")
            awPacket_ = Some(master.receiveWriteAddress())
            logMaster(masterIdx, "received write address", awPacket_.get)

            masterInfo.awReceiveCount -= 1
          }
        }.fork {
          if (masterInfo.wReceiveCount > 0) {
            logMaster(masterIdx, "waiting for write data")
            wPacket_ = Some(master.receiveWriteDataBurst())
            logMaster(masterIdx, "received write data", wPacket_.get)

            masterInfo.wReceiveCount -= 1
          }
        }.join()

        val awPacket = awPacket_.get
        val wPacket = wPacket_.get

        assert(
          wPacket.forall { _.id == awPacket.id },
          "wPacket.forall { _.id == awPacket.id }"
        )

        val threadInfo = threadInfos(awPacket.id)

        assert(
          threadInfo.awExpectedQueue.nonEmpty,
          "threadInfo.awExpectedQueue.nonEmpty"
        )
        Expect.equals(threadInfo.awExpectedQueue.head, awPacket)
        threadInfo.awExpectedQueue.removeHead()

        assert(
          threadInfo.wExpectedQueue.nonEmpty,
          "threadInfo.wExpectedQueue.nonEmpty"
        )
        Expect.equals(threadInfo.wExpectedQueue.head, wPacket)
        threadInfo.wExpectedQueue.removeHead()

        assert(threadInfo.bTaskQueue.nonEmpty, "threadInfo.bTaskQueue.nonEmpty")
        val bNext = threadInfo.bTaskQueue.head
        threadInfo.bTaskQueue.removeHead()
        assert(bNext.id == awPacket.id, "bNext.id == awPacket.id")
        val id = awPacket.id & threadInfoMask
        threadInfo.bExpectedQueue.addOne(bNext.copy(id = id))

        stepRandom(16)
        logMaster(masterIdx, "send write response", bNext)
        master.sendWriteResponse(bNext)
      }
    }.join()

    logMaster(masterIdx, "Complete.")
  }

  private def handleSlave(slaveIdx: Int) = {
    // behavior of a master
    val slave = slaveInterfaces(slaveIdx)
    val slaveInfo = slaveInfos(slaveIdx)

    fork {
      var done = false

      while (!done) {
        var sentAny = false

        for (threadInfoId <- (0 until numThreadsPerSlave)) {
          val threadIdx = threadInfoId + (slaveIdx << threadInfoShift)
          val threadInfo = threadInfos(threadIdx)

          // for sentAny trick to work, we should pop at least once
          // otherwise, if popNum == 0 for all threads, sentAny = false,
          // and we exit early (resulting in a deadlock)
          val popNum = 1 + rand.nextInt(4 /* TODO make reconfigurable */ )

          for (i <- (0 until popNum)) {
            if (threadInfo.arTaskQueue.nonEmpty) {
              val arNext = threadInfo.arTaskQueue.head
              threadInfo.arTaskQueue.removeHead()
              threadInfo.arExpectedQueue.addOne(arNext.copy(id = threadIdx))
              logSlave(slaveIdx, "send read address", arNext)
              slave.sendReadAddress(arNext)
              stepRandom(4)

              sentAny = true
            }
          }

        }

        done = !sentAny
        stepRandom(4)
      }
    }.fork {
      while (slaveInfo.rReceiveCount > 0) {
        logSlave(slaveIdx, f"Remaining R packets: ${slaveInfo.rReceiveCount}")
        logSlave(slaveIdx, "waiting for read data")
        val rPacket = slave.receiveReadDataBurst()
        logSlave(slaveIdx, "received read data", rPacket)

        val threadInfo =
          threadInfos(rPacket(0).id + (slaveIdx << threadInfoShift))

        assert(
          threadInfo.rExpectedQueue.nonEmpty,
          "threadInfo.rExpectedQueue.nonEmpty"
        )
        Expect.equals(threadInfo.rExpectedQueue.head, rPacket)
        threadInfo.rExpectedQueue.removeHead()

        stepRandom(4)

        slaveInfo.rReceiveCount -= 1
      }
    }.fork {
      var done = false

      while (!done) {
        var sentAny = false

        for (threadInfoId <- (0 until numThreadsPerSlave)) {
          val threadIdx = threadInfoId + (slaveIdx << threadInfoShift)
          val threadInfo = threadInfos(threadIdx)

          // for sentAny trick to work, we should pop at least once
          val popNum = 1 + rand.nextInt(4 /* TODO make reconfigurable */ )

          for (i <- (0 until popNum)) {
            if (threadInfo.awTaskQueue.nonEmpty) {
              val awNext = threadInfo.awTaskQueue.head
              threadInfo.awTaskQueue.removeHead()
              threadInfo.awExpectedQueue.addOne(awNext.copy(id = threadIdx))

              assert(
                threadInfo.wTaskQueue.nonEmpty,
                "threadInfo.wTaskQueue.nonEmpty"
              )
              val wNext = threadInfo.wTaskQueue.head
              threadInfo.wTaskQueue.removeHead()
              threadInfo.wExpectedQueue.addOne(wNext.map {
                _.copy(id = threadIdx)
              })
              fork {
                logSlave(slaveIdx, "send write address", awNext)
                slave.sendWriteAddress(awNext)
              }.fork {
                logSlave(slaveIdx, "send write data", wNext)
                slave.sendWriteDataBurst(wNext)
              }.join()
              stepRandom(4)

              sentAny = true
            }
          }
        }

        done = !sentAny
        stepRandom(4)
      }
    }.fork {
      while (slaveInfo.bReceiveCount > 0) {
        logSlave(slaveIdx, f"Remaining B packets: ${slaveInfo.bReceiveCount}")
        logSlave(slaveIdx, "waiting for write response")
        val bPacket = slave.receiveWriteResponse()
        logSlave(slaveIdx, "received write response", bPacket)
        val threadInfo = threadInfos(bPacket.id + (slaveIdx << threadInfoShift))

        assert(
          threadInfo.bExpectedQueue.nonEmpty,
          "threadInfo.bExpectedQueue.nonEmpty"
        )
        Expect.equals(threadInfo.bExpectedQueue.head, bPacket)
        threadInfo.bExpectedQueue.removeHead()

        stepRandom(4)

        slaveInfo.bReceiveCount -= 1
      }
    }.join()

    logSlave(slaveIdx, "Complete.")
  }

  protected def readTask(
      slaveIdx: Int,
      masterIdx: Int,
      addr: Int,
      len: Int,
      id: Int = 0
  ): Unit = {
    require(len >= 0)
    val slaveInfo = slaveInfos(slaveIdx)
    val masterInfo = masterInfos(masterIdx)

    val threadIdx = id + (slaveIdx << threadInfoShift)
    val threadInfo = threadInfos(threadIdx)
    threadInfo.arTaskQueue.addOne(AddressPacket(id, addr, len))
    threadInfo.rTaskQueue.addOne(Seq.tabulate(len + 1) { (pktIdx) =>
      ReadDataPacket(
        threadIdx,
        rand.nextInt(0x7fff_ffff),
        pktIdx == len
      )
    })

    slaveInfo.rReceiveCount += 1
    masterInfo.arReceiveCount += 1
  }

  protected def writeTask(
      slaveIdx: Int,
      masterIdx: Int,
      addr: Int,
      len: Int = 0,
      id: Int = 0
  ): Unit = {
    require(len >= 0)
    val slaveInfo = slaveInfos(slaveIdx)
    val masterInfo = masterInfos(masterIdx)

    val threadIdx = id + (slaveIdx << threadInfoShift)
    val threadInfo = threadInfos(threadIdx)
    threadInfo.awTaskQueue.addOne(AddressPacket(id, addr, len))
    threadInfo.wTaskQueue.addOne(Seq.tabulate(len + 1) { (pktIdx) =>
      WriteDataPacket(
        id,
        rand.nextInt(0x7fff_ffff),
        pktIdx == len
      )
    })
    threadInfo.bTaskQueue.addOne(WriteResponsePacket(threadIdx))

    slaveInfo.bReceiveCount += 1
    masterInfo.awReceiveCount += 1
    masterInfo.wReceiveCount += 1
  }

  protected def createTasks(): Unit

  def run() = {
    import chiseltest.internal.Context

    createTasks()

    var mastersComplete = false
    var slavesComplete = false

    fork {
      val masterThreads = masterInterfaces.zipWithIndex.map {
        case (master, idx) => {
          Context().backend.doFork(
            () => handleMaster(idx),
            Some(f"master_$idx"),
            None
          )
        }
      }
      Context().backend.doJoin(masterThreads, false)
      mastersComplete = true
    }.fork {
      val slaveThreads = slaveInterfaces.zipWithIndex.map {
        case (slave, idx) => {
          Context().backend.doFork(
            () => handleSlave(idx),
            Some(f"slave_$idx"),
            None
          )
        }
      }
      Context().backend.doJoin(slaveThreads, false)
      slavesComplete = true
    }.fork {
      while (!mastersComplete || !slavesComplete) {
        counter += 1
        step(1)
      }
    }.join()
  }
}
