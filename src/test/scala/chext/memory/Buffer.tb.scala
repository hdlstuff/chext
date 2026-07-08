package chext.memory

import chisel3._

import chext.memory.ConnectOp._

class Buffer_Tbtop extends Module with chext.AnnotatedModule {
  val readIn = IO(new ReadInterface(8, 32))
  val readOut = IO(Flipped(new ReadInterface(8, 32)))

  val writeIn = IO(new WriteInterface(8, 32))
  val writeOut = IO(Flipped(new WriteInterface(8, 32)))

  buffer(readIn, readOut, BufferConfig(req = 1, resp = 2))
  buffer(writeIn, writeOut, BufferConfig(req = 2, resp = 1))

  declareClock(clock)
  declareReset(reset)
}

object Buffer_Tb extends chext.TestBench {
  emit(new Buffer_Tbtop)
}

class BufferOp_Tbtop extends Module with chext.AnnotatedModule {
  val readIn = IO(new ReadInterface(8, 32))
  val readOut = IO(Flipped(new ReadInterface(8, 32)))

  val writeIn = IO(new WriteInterface(8, 32))
  val writeOut = IO(Flipped(new WriteInterface(8, 32)))

  SlaveBuffer(readIn, BufferConfig.all(1)) :=> MasterBuffer(readOut, BufferConfig.all(1))
  LeftBuffer(writeIn, BufferConfig.all(1)) :=> RightBuffer(writeOut, BufferConfig.all(1))

  declareClock(clock)
  declareReset(reset)
}

object BufferOp_Tb extends chext.TestBench {
  emit(new BufferOp_Tbtop)
}

class ConnectSeq_Tbtop extends Module with chext.AnnotatedModule {
  val readIn = Seq.fill(2)(IO(new ReadInterface(8, 32)))
  val readOut = Seq.fill(2)(IO(Flipped(new ReadInterface(8, 32))))

  val writeIn = Seq.fill(2)(IO(new WriteInterface(8, 32)))
  val writeOut = Seq.fill(2)(IO(Flipped(new WriteInterface(8, 32))))

  readIn :=> readOut
  writeIn :=> writeOut

  declareClock(clock)
  declareReset(reset)
}

object ConnectSeq_Tb extends chext.TestBench {
  emit(new ConnectSeq_Tbtop)
}
