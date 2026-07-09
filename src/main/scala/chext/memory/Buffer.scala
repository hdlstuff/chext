package chext.memory

import chisel3._
import chisel3.experimental.SourceInfo
import chisel3.experimental.prefix

import chext.elastic
import elastic.{SinkBuffer, SourceBuffer}
import elastic.ConnectOp._

import chext.tracking.uniquePrefix

case class BufferConfig(
    req: Int = 0,
    resp: Int = 0
) {
  private val require_ = chext.util.Require.inferred()

  require_(req >= 0)
  require_(resp >= 0)
}

object BufferConfig {
  def all(n: Int): BufferConfig = BufferConfig(n, n)
}

trait BufferOps[I <: Data] {
  def slaveLike(interface: I)(implicit si: SourceInfo): I
  def masterLike(interface: I)(implicit si: SourceInfo): I
  def buffer(master: I, slave: I, cfg: BufferConfig)(implicit si: SourceInfo): Unit
}

object BufferOps {
  implicit object Read extends BufferOps[ReadInterface] {
    def slaveLike(interface: ReadInterface)(implicit si: SourceInfo): ReadInterface =
      new ReadInterface(interface.wAddr, interface.wData)

    def masterLike(interface: ReadInterface)(implicit si: SourceInfo): ReadInterface =
      Flipped(new ReadInterface(interface.wAddr, interface.wData))

    def buffer(
        master: ReadInterface,
        slave: ReadInterface,
        cfg: BufferConfig
    )(implicit si: SourceInfo): Unit =
      BufferImpl.read(master, slave, cfg)
  }

  implicit object Write extends BufferOps[WriteInterface] {
    def slaveLike(interface: WriteInterface)(implicit si: SourceInfo): WriteInterface =
      new WriteInterface(interface.wAddr, interface.wData)

    def masterLike(interface: WriteInterface)(implicit si: SourceInfo): WriteInterface =
      Flipped(new WriteInterface(interface.wAddr, interface.wData))

    def buffer(
        master: WriteInterface,
        slave: WriteInterface,
        cfg: BufferConfig
    )(implicit si: SourceInfo): Unit =
      BufferImpl.write(master, slave, cfg)
  }
}

private object BufferImpl {
  private val require_ = chext.util.Require.inferred()

  def apply[I <: Data](
      master: I,
      slave: I,
      cfg: BufferConfig
  )(implicit si: SourceInfo, ops: BufferOps[I]): Unit =
    ops.buffer(master, slave, cfg)

  def read(
      master: ReadInterface,
      slave: ReadInterface,
      cfg: BufferConfig
  )(implicit si: SourceInfo): Unit = {
    require_.here(master.wData == slave.wData, "wData of interfaces must match!")
    require_.here(master.wAddr == slave.wAddr, "wAddr of interfaces must match!")

    SourceBuffer(master.req, cfg.req, name = "reqBuffer") :=> slave.req
    slave.resp :=> SinkBuffer(master.resp, cfg.resp, name = "respBuffer")
  }

  def write(
      master: WriteInterface,
      slave: WriteInterface,
      cfg: BufferConfig
  )(implicit si: SourceInfo): Unit = {
    require_.here(master.wData == slave.wData, "wData of interfaces must match!")
    require_.here(master.wAddr == slave.wAddr, "wAddr of interfaces must match!")

    SourceBuffer(master.req, cfg.req, name = "reqBuffer") :=> slave.req
    slave.resp :=> SinkBuffer(master.resp, cfg.resp, name = "respBuffer")
  }

  def left[I <: Data](
      interface: I,
      cfg: BufferConfig
  )(implicit si: SourceInfo, ops: BufferOps[I]): I = {
    val result = Wire(ops.slaveLike(interface))
    ops.buffer(interface, result, cfg)
    result
  }

  def right[I <: Data](
      interface: I,
      cfg: BufferConfig
  )(implicit si: SourceInfo, ops: BufferOps[I]): I = {
    val result = Wire(ops.masterLike(interface))
    ops.buffer(result, interface, cfg)
    result
  }
}

trait LeftBufferOps {
  protected val defaultName: String

  def apply[I <: Data](
      interface: I,
      cfg: BufferConfig = BufferConfig.all(2),
      name: String = defaultName
  )(implicit si: SourceInfo, ops: BufferOps[I]): I =
    uniquePrefix(name) {
      BufferImpl.left(interface, cfg)
    }

  def apply[I <: Data](
      interfaces: Seq[I],
      cfg: BufferConfig,
      name: String
  )(implicit si: SourceInfo, ops: BufferOps[I]): Seq[I] =
    uniquePrefix(s"${name}Many") {
      interfaces.zipWithIndex.map { case (interface, index) =>
        prefix(index.toString) {
          BufferImpl.left(interface, cfg)
        }
      }
    }

  def apply[I <: Data](
      interfaces: Seq[I],
      cfg: BufferConfig
  )(implicit si: SourceInfo, ops: BufferOps[I]): Seq[I] =
    apply(interfaces, cfg, defaultName)

  def apply[I <: Data](
      interfaces: Seq[I]
  )(implicit si: SourceInfo, ops: BufferOps[I]): Seq[I] =
    apply(interfaces, BufferConfig.all(2), defaultName)
}

trait LeftBufferedOps {
  def apply[I <: Data](
      interface: I,
      cfg: BufferConfig = BufferConfig.all(2)
  )(implicit si: SourceInfo, ops: BufferOps[I]): I =
    BufferImpl.left(interface, cfg)

  def apply[I <: Data](
      interfaces: Seq[I],
      cfg: BufferConfig
  )(implicit si: SourceInfo, ops: BufferOps[I]): Seq[I] =
    interfaces.zipWithIndex.map { case (interface, index) =>
      prefix(index.toString) {
        apply(interface, cfg)
      }
    }

  def apply[I <: Data](
      interfaces: Seq[I]
  )(implicit si: SourceInfo, ops: BufferOps[I]): Seq[I] =
    apply(interfaces, BufferConfig.all(2))
}

trait RightBufferOps {
  protected val defaultName: String

  def apply[I <: Data](
      interface: I,
      cfg: BufferConfig = BufferConfig.all(2),
      name: String = defaultName
  )(implicit si: SourceInfo, ops: BufferOps[I]): I =
    uniquePrefix(name) {
      BufferImpl.right(interface, cfg)
    }

  def apply[I <: Data](
      interfaces: Seq[I],
      cfg: BufferConfig,
      name: String
  )(implicit si: SourceInfo, ops: BufferOps[I]): Seq[I] =
    uniquePrefix(s"${name}Many") {
      interfaces.zipWithIndex.map { case (interface, index) =>
        prefix(index.toString) {
          BufferImpl.right(interface, cfg)
        }
      }
    }

  def apply[I <: Data](
      interfaces: Seq[I],
      cfg: BufferConfig
  )(implicit si: SourceInfo, ops: BufferOps[I]): Seq[I] =
    apply(interfaces, cfg, defaultName)

  def apply[I <: Data](
      interfaces: Seq[I]
  )(implicit si: SourceInfo, ops: BufferOps[I]): Seq[I] =
    apply(interfaces, BufferConfig.all(2), defaultName)
}

trait RightBufferedOps {
  def apply[I <: Data](
      interface: I,
      cfg: BufferConfig = BufferConfig.all(2)
  )(implicit si: SourceInfo, ops: BufferOps[I]): I =
    BufferImpl.right(interface, cfg)

  def apply[I <: Data](
      interfaces: Seq[I],
      cfg: BufferConfig
  )(implicit si: SourceInfo, ops: BufferOps[I]): Seq[I] =
    interfaces.zipWithIndex.map { case (interface, index) =>
      prefix(index.toString) {
        apply(interface, cfg)
      }
    }

  def apply[I <: Data](
      interfaces: Seq[I]
  )(implicit si: SourceInfo, ops: BufferOps[I]): Seq[I] =
    apply(interfaces, BufferConfig.all(2))
}

object SlaveBuffer extends LeftBufferOps {
  protected val defaultName: String = "slaveBuffer"
}

object SlaveBuffered extends LeftBufferedOps

object LeftBuffer extends LeftBufferOps {
  protected val defaultName: String = "leftBuffer"
}

object MasterBuffer extends RightBufferOps {
  protected val defaultName: String = "masterBuffer"
}

object MasterBuffered extends RightBufferedOps

object RightBuffer extends RightBufferOps {
  protected val defaultName: String = "rightBuffer"
}
