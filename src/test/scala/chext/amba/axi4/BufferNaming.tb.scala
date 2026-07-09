package chext.amba.axi4

import chisel3._

class RawBufferedManyNaming_Tbtop extends Module with chext.AnnotatedModule {
  import chext.amba.axi4.ConnectOp._

  private val cfg = Config(wId = 2, wAddr = 16, wData = 32)

  val s_axi = IO(Slave.many(2, cfg))
  val m_axi = IO(Master.many(2, cfg))

  private val slaveBuffered = SlaveBuffered(s_axi, BufferConfig.all(1))
  private val masterBuffered = MasterBuffered(m_axi, BufferConfig.all(1))

  slaveBuffered :=> masterBuffered

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)
  declareAxi4Interface(m_axi)
}

object RawBufferedManyNaming_Tb extends chext.TestBench {
  emit(new RawBufferedManyNaming_Tbtop)
}

class FullBufferedManyNaming_Tbtop extends Module with chext.AnnotatedModule {
  import chext.amba.axi4.full.ConnectOp._

  private val cfg = Config(wId = 2, wAddr = 16, wData = 32)

  val s_axi = IO(full.Slave.many(2, cfg))
  val m_axi = IO(full.Master.many(2, cfg))

  private val slaveBuffered = full.SlaveBuffered(s_axi, BufferConfig.all(1))
  private val masterBuffered = full.MasterBuffered(m_axi, BufferConfig.all(1))

  slaveBuffered :=> masterBuffered

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axi)
  declareAxi4Interface(m_axi)
}

object FullBufferedManyNaming_Tb extends chext.TestBench {
  emit(new FullBufferedManyNaming_Tbtop)
}

class LiteBufferedManyNaming_Tbtop extends Module with chext.AnnotatedModule {
  import chext.amba.axi4.lite.ConnectOp._

  private val cfg = Config(wAddr = 16, wData = 32, lite = true)

  val s_axil = IO(lite.Slave.many(2, cfg))
  val m_axil = IO(lite.Master.many(2, cfg))

  private val slaveBuffered = lite.SlaveBuffered(s_axil, BufferConfig.all(1))
  private val masterBuffered = lite.MasterBuffered(m_axil, BufferConfig.all(1))

  slaveBuffered :=> masterBuffered

  declareClock(clock)
  declareReset(reset)
  declareAxi4Interface(s_axil)
  declareAxi4Interface(m_axil)
}

object LiteBufferedManyNaming_Tb extends chext.TestBench {
  emit(new LiteBufferedManyNaming_Tbtop)
}
