package chext.amba.axi4.full

import chisel3._
import chisel3.util._
import chisel3.experimental.prefix
import chisel3.experimental.SourceInfo

import chext.amba.axi4
import chext.amba.axi4.BufferConfig

import chext.elastic
import elastic.{SinkBuffer, SourceBuffer}
import elastic.ConnectOp._

import chext.tracking.uniquePrefix

object buffer {
  val require_ = new chext.util.Require("axi4.full.buffer")

  private[full] def insertBufferR(
      master: Interface,
      slave: Interface,
      cfg: BufferConfig
  )(implicit si: SourceInfo): Unit = {
    SourceBuffer(master.ar, cfg.ar, name = "arBuffer") :=> slave.ar
    slave.r :=> SinkBuffer(master.r, cfg.r, name = "rBuffer")
  }

  private[full] def insertBufferW(
      master: Interface,
      slave: Interface,
      cfg: BufferConfig
  )(implicit si: SourceInfo): Unit = {
    SourceBuffer(master.aw, cfg.aw, name = "awBuffer") :=> slave.aw
    SourceBuffer(master.w, cfg.w, name = "wBuffer") :=> slave.w
    slave.b :=> SinkBuffer(master.b, cfg.b, name = "bBuffer")
  }

  /** Inserts a buffer between a master and a slave interface.
    *
    * @param master
    * @param slave
    * @param cfg
    */
  def apply(
      master: Interface,
      slave: Interface,
      cfg: BufferConfig
  )(implicit si: SourceInfo): Unit = {
    require_.here(
      master.cfg == slave.cfg,
      "master and slave configurations do not match!",
      Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
    )

    if (master.cfg.read)
      insertBufferR(master, slave, cfg)

    if (master.cfg.write)
      insertBufferW(master, slave, cfg)
  }
}

object SlaveBuffer {
  def apply(
      interface: Interface,
      cfg: BufferConfig = BufferConfig.all(2),
      name: String = "slaveBuffer"
  )(implicit si: SourceInfo): Interface = uniquePrefix(name) {
    val result = Wire(Slave(interface.cfg))
    buffer(interface, result, cfg)
    result
  }

  def apply(
      interfaces: Seq[Interface],
      cfg: BufferConfig,
      name: String
  )(implicit si: SourceInfo): Seq[Interface] =
    interfaces.zipWithIndex.map { case (interface, index) =>
      prefix(index.toString) {
        apply(interface, cfg, name)
      }
    }

  def apply(
      interfaces: Seq[Interface],
      cfg: BufferConfig
  )(implicit si: SourceInfo): Seq[Interface] =
    apply(interfaces, cfg, "slaveBuffer")

  def apply(
      interfaces: Seq[Interface]
  )(implicit si: SourceInfo): Seq[Interface] =
    apply(interfaces, BufferConfig.all(2), "slaveBuffer")
}

object SlaveBuffered {
  def apply(
      interface: Interface,
      cfg: BufferConfig = BufferConfig.all(2)
  )(implicit si: SourceInfo): Interface = {
    val result = Wire(Slave(interface.cfg))
    buffer(interface, result, cfg)
    result
  }

  def apply(
      interfaces: Seq[Interface],
      cfg: BufferConfig
  )(implicit si: SourceInfo): Seq[Interface] =
    interfaces.zipWithIndex.map { case (interface, index) =>
      prefix(index.toString) {
        apply(interface, cfg)
      }
    }

  def apply(
      interfaces: Seq[Interface]
  )(implicit si: SourceInfo): Seq[Interface] =
    apply(interfaces, BufferConfig.all(2))
}

object LeftBuffer {
  def apply(
      interface: Interface,
      cfg: BufferConfig = BufferConfig.all(2),
      name: String = "leftBuffer"
  )(implicit si: SourceInfo): Interface = uniquePrefix(name) {
    val result = Wire(Slave(interface.cfg))
    buffer(interface, result, cfg)
    result
  }

  def apply(
      interfaces: Seq[Interface],
      cfg: BufferConfig,
      name: String
  )(implicit si: SourceInfo): Seq[Interface] =
    interfaces.zipWithIndex.map { case (interface, index) =>
      prefix(index.toString) {
        apply(interface, cfg, name)
      }
    }

  def apply(
      interfaces: Seq[Interface],
      cfg: BufferConfig
  )(implicit si: SourceInfo): Seq[Interface] =
    apply(interfaces, cfg, "leftBuffer")

  def apply(
      interfaces: Seq[Interface]
  )(implicit si: SourceInfo): Seq[Interface] =
    apply(interfaces, BufferConfig.all(2), "leftBuffer")
}

object MasterBuffer {
  def apply(
      interface: Interface,
      cfg: BufferConfig = BufferConfig.all(2),
      name: String = "masterBuffer"
  )(implicit si: SourceInfo): Interface = uniquePrefix(name) {
    val result = Wire(Master(interface.cfg))
    buffer(result, interface, cfg)
    result
  }

  def apply(
      interfaces: Seq[Interface],
      cfg: BufferConfig,
      name: String
  )(implicit si: SourceInfo): Seq[Interface] =
    interfaces.zipWithIndex.map { case (interface, index) =>
      prefix(index.toString) {
        apply(interface, cfg, name)
      }
    }

  def apply(
      interfaces: Seq[Interface],
      cfg: BufferConfig
  )(implicit si: SourceInfo): Seq[Interface] =
    apply(interfaces, cfg, "masterBuffer")

  def apply(
      interfaces: Seq[Interface]
  )(implicit si: SourceInfo): Seq[Interface] =
    apply(interfaces, BufferConfig.all(2), "masterBuffer")
}

object MasterBuffered {
  def apply(
      interface: Interface,
      cfg: BufferConfig = BufferConfig.all(2)
  )(implicit si: SourceInfo): Interface = {
    val result = Wire(Master(interface.cfg))
    buffer(result, interface, cfg)
    result
  }

  def apply(
      interfaces: Seq[Interface],
      cfg: BufferConfig
  )(implicit si: SourceInfo): Seq[Interface] =
    interfaces.zipWithIndex.map { case (interface, index) =>
      prefix(index.toString) {
        apply(interface, cfg)
      }
    }

  def apply(
      interfaces: Seq[Interface]
  )(implicit si: SourceInfo): Seq[Interface] =
    apply(interfaces, BufferConfig.all(2))
}

object RightBuffer {
  def apply(
      interface: Interface,
      cfg: BufferConfig = BufferConfig.all(2),
      name: String = "rightBuffer"
  )(implicit si: SourceInfo): Interface = uniquePrefix(name) {
    val result = Wire(Master(interface.cfg))
    buffer(result, interface, cfg)
    result
  }

  def apply(
      interfaces: Seq[Interface],
      cfg: BufferConfig,
      name: String
  )(implicit si: SourceInfo): Seq[Interface] =
    interfaces.zipWithIndex.map { case (interface, index) =>
      prefix(index.toString) {
        apply(interface, cfg, name)
      }
    }

  def apply(
      interfaces: Seq[Interface],
      cfg: BufferConfig
  )(implicit si: SourceInfo): Seq[Interface] =
    apply(interfaces, cfg, "rightBuffer")

  def apply(
      interfaces: Seq[Interface]
  )(implicit si: SourceInfo): Seq[Interface] =
    apply(interfaces, BufferConfig.all(2), "rightBuffer")
}
