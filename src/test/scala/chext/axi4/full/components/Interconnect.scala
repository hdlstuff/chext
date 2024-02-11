package chext.axi4.full.components

import chisel3._
import chisel3.util._

import chiseltest._

import chext.axi4
import chext.utils.Expect

trait InterconnectHelper[M <: Module] {
  def slaveInterfaces(module: M): Seq[axi4.full.Interface]
  def masterInterfaces(module: M): Seq[axi4.full.Interface]
}

abstract class InterconnectTester[T <: Module](
    val dut: T,
    val timeout: Int = 10000,
    val logEnabled: Boolean = true
)(implicit val helper: InterconnectHelper[T])
    extends chext.test.TesterBase {

  private val slaveInterfaces = helper.slaveInterfaces(dut)
  private val masterInterfaces = helper.masterInterfaces(dut)

  private var counter = 0

  val axiSlaveConfig = slaveInterfaces(0).cfg
  require(slaveInterfaces.forall { _.cfg == axiSlaveConfig })

  val axiMasterConfig = masterInterfaces(0).cfg
  require(masterInterfaces.forall { _.cfg == axiMasterConfig })

  val threadShift = axiSlaveConfig.wId
  val threadMask = (1 << threadShift) - 1
  val numThreadsPerSlave = 1 << threadShift
  val numThreads = slaveInterfaces.length * numThreadsPerSlave

  import scala.collection.mutable.Queue

  import axi4.full._
  import axi4.full.test._
  import axi4.full.test.PacketUtils._

  class Thread {
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

  val threads = Array.fill(numThreads) { new Thread }

  private def handleMaster(master: Interface, masterIdx: Int) = {
    fork {
      while (true) {
        logMaster(masterIdx, "waiting for read address")
        val arPacket = master.receiveReadAddress()
        val thread = threads(arPacket.id)

        logMaster(masterIdx, "received read address", arPacket)

        assert(
          thread.arExpectedQueue.nonEmpty,
          "thread.arExpectedQueue.nonEmpty"
        )
        Expect.equals(thread.arExpectedQueue.head, arPacket)
        thread.arExpectedQueue.removeHead()

        assert(thread.rTaskQueue.nonEmpty, "thread.rTaskQueue.nonEmpty")
        val rNext = thread.rTaskQueue.head
        thread.rTaskQueue.removeHead()
        assert(
          rNext.forall { _.id == arPacket.id },
          "rNext.forall { _.id == arPacket.id }"
        )
        val id = arPacket.id & threadMask
        thread.rExpectedQueue.addOne(rNext.map { _.copy(id = id) })

        stepRandom(16)
        logMaster(masterIdx, "send read data", rNext)
        master.sendReadDataBurst(rNext)
      }
    }
      .fork {
        while (true) {
          var awPacket_ = Option.empty[AddressPacket]
          var wPacket_ = Option.empty[Seq[WriteDataPacket]]

          fork {
            logMaster(masterIdx, "waiting for write address")
            awPacket_ = Some(master.receiveWriteAddress())
            logMaster(masterIdx, "received write address", awPacket_.get)
          }.fork {
            logMaster(masterIdx, "waiting for write data")
            wPacket_ = Some(master.receiveWriteDataBurst())
            logMaster(masterIdx, "received write data", wPacket_.get)
          }.join()

          val awPacket = awPacket_.get
          val wPacket = wPacket_.get

          assert(
            wPacket.forall { _.id == awPacket.id },
            "wPacket.forall { _.id == awPacket.id }"
          )

          val thread = threads(awPacket.id)

          assert(
            thread.awExpectedQueue.nonEmpty,
            "thread.awExpectedQueue.nonEmpty"
          )
          Expect.equals(thread.awExpectedQueue.head, awPacket)
          thread.awExpectedQueue.removeHead()

          assert(
            thread.wExpectedQueue.nonEmpty,
            "thread.wExpectedQueue.nonEmpty"
          )
          Expect.equals(thread.wExpectedQueue.head, wPacket)
          thread.wExpectedQueue.removeHead()

          assert(thread.bTaskQueue.nonEmpty, "thread.bTaskQueue.nonEmpty")
          val bNext = thread.bTaskQueue.head
          thread.bTaskQueue.removeHead()
          assert(bNext.id == awPacket.id, "bNext.id == awPacket.id")
          val id = awPacket.id & threadMask
          thread.bExpectedQueue.addOne(bNext.copy(id = id))

          stepRandom(16)
          logMaster(masterIdx, "send write response", bNext)
          master.sendWriteResponse(bNext)
        }
      }
      .join()
  }

  private def handleSlave(slave: Interface, slaveIdx: Int) = {
    fork {
      while (true) {
        for (threadId <- (0 until numThreadsPerSlave)) {
          val threadIdx = threadId + (slaveIdx << threadShift)
          val thread = threads(threadIdx)

          val popNum = rand.nextInt(4 /* TODO make reconfigurable */ )

          for (i <- (0 until popNum)) {
            if (thread.arTaskQueue.nonEmpty) {
              val arNext = thread.arTaskQueue.head
              thread.arTaskQueue.removeHead()
              thread.arExpectedQueue.addOne(arNext.copy(id = threadIdx))
              logSlave(slaveIdx, "send read address", arNext)
              slave.sendReadAddress(arNext)
              stepRandom(4)
            }
          }
        }

        stepRandom(4)
      }
    }
      .fork {
        while (true) {
          logSlave(slaveIdx, "waiting for read data")
          val rPacket = slave.receiveReadDataBurst()
          logSlave(slaveIdx, "received read data", rPacket)

          val thread = threads(rPacket(0).id + (slaveIdx << threadShift))

          assert(
            thread.rExpectedQueue.nonEmpty,
            "thread.rExpectedQueue.nonEmpty"
          )
          Expect.equals(thread.rExpectedQueue.head, rPacket)
          thread.rExpectedQueue.removeHead()

          stepRandom(4)
        }
      }
      .fork {
        while (true) {
          for (threadId <- (0 until numThreadsPerSlave)) {
            val threadIdx = threadId + (slaveIdx << threadShift)
            val thread = threads(threadIdx)

            val popNum = rand.nextInt(4 /* TODO make reconfigurable */ )

            for (i <- (0 until popNum)) {
              if (thread.awTaskQueue.nonEmpty) {
                val awNext = thread.awTaskQueue.head
                thread.awTaskQueue.removeHead()
                thread.awExpectedQueue.addOne(awNext.copy(id = threadIdx))

                assert(thread.wTaskQueue.nonEmpty, "thread.wTaskQueue.nonEmpty")
                val wNext = thread.wTaskQueue.head
                thread.wTaskQueue.removeHead()
                thread.wExpectedQueue.addOne(wNext.map {
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
              }
            }
          }

          stepRandom(4)
        }
      }
      .fork {
        while (true) {
          logSlave(slaveIdx, "waiting for write response")
          val bPacket = slave.receiveWriteResponse()
          logSlave(slaveIdx, "received write response", bPacket)
          val thread = threads(bPacket.id + (slaveIdx << threadShift))

          assert(
            thread.bExpectedQueue.nonEmpty,
            "thread.bExpectedQueue.nonEmpty"
          )
          Expect.equals(thread.bExpectedQueue.head, bPacket)
          thread.bExpectedQueue.removeHead()

          stepRandom(4)
        }
      }
      .join()
  }

  protected def readTask(
      slaveIdx: Int,
      addr: Int,
      len: Int,
      id: Int = 0
  ): Unit = {
    require(len >= 0)
    val threadIdx = id + (slaveIdx << threadShift)
    val thread = threads(threadIdx)
    thread.arTaskQueue.addOne(AddressPacket(id, addr, len))
    thread.rTaskQueue.addOne(Seq.tabulate(len + 1) { (pktIdx) =>
      ReadDataPacket(
        threadIdx,
        rand.nextInt(0x7fff_ffff),
        pktIdx == len
      )
    })
  }

  protected def writeTask(
      slaveIdx: Int,
      addr: Int,
      len: Int = 0,
      id: Int = 0
  ): Unit = {
    require(len >= 0)
    val threadIdx = id + (slaveIdx << threadShift)
    val thread = threads(threadIdx)
    thread.awTaskQueue.addOne(AddressPacket(id, addr, len))
    thread.wTaskQueue.addOne(Seq.tabulate(len + 1) { (pktIdx) =>
      WriteDataPacket(
        id,
        rand.nextInt(0x7fff_ffff),
        pktIdx == len
      )
    })
    thread.bTaskQueue.addOne(WriteResponsePacket(threadIdx))
  }

  protected def createTasks(): Unit

  override def onTimeout(e: TimeoutException): Unit = {
    println("Simulation timed out.")

    threads.zipWithIndex.foreach {
      case (thread, id) => {
        assert(
          thread.arTaskQueue.isEmpty,
          f"threads(${id}).arTaskQueue.isEmpty"
        )
        assert(thread.rTaskQueue.isEmpty, f"threads(${id}).rTaskQueue.isEmpty")
        assert(
          thread.awTaskQueue.isEmpty,
          f"threads(${id}).awTaskQueue.isEmpty"
        )
        assert(thread.wTaskQueue.isEmpty, f"threads(${id}).wTaskQueue.isEmpty")
        assert(thread.bTaskQueue.isEmpty, f"threads(${id}).bTaskQueue.isEmpty")
      }
    }
  }

  override def onTest() = {
    import chiseltest.internal.Context

    createTasks()

    fork {
      val masterThreads = masterInterfaces.zipWithIndex.map {
        case (master, idx) => {
          Context().backend.doFork(
            () => handleMaster(master, idx),
            Some(f"master_$idx"),
            None
          )
        }
      }
      Context().backend.doJoin(masterThreads, false)
    }.fork {
      val slaveThreads = slaveInterfaces.zipWithIndex.map {
        case (slave, idx) => {
          Context().backend.doFork(
            () => handleSlave(slave, idx),
            Some(f"slave_$idx"),
            None
          )
        }
      }
      Context().backend.doJoin(slaveThreads, false)
    }.fork {
      while (true) {
        counter += 1
        step(1)
      }
    }.fork {
      while (true) {
        log("periodic counter")
        step(100)
      }
    }.join()
  }
}
