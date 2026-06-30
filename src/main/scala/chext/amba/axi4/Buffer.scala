package chext.amba.axi4

import chisel3._
import chisel3.experimental.prefix
import chisel3.experimental.SourceInfo

import chext.amba.axi4
import axi4.Casts._

import chext.tracking.uniquePrefix

case class BufferConfig(
    aw: Int = 0,
    w: Int = 0,
    b: Int = 0,
    ar: Int = 0,
    r: Int = 0
) {
  require(aw >= 0)
  require(w >= 0)
  require(b >= 0)
  require(ar >= 0)
  require(r >= 0)
}

object BufferConfig {
  def all(n: Int) = BufferConfig(n, n, n, n, n)
}

object buffer {
  private val require_ = new chext.util.Require("axi4.buffer")

  def apply(
      master: RawInterface,
      slave: RawInterface,
      cfg: BufferConfig = BufferConfig.all(2)
  )(implicit si: SourceInfo): Unit = {
    require_(
      master.cfg.lite == slave.cfg.lite,
      "master and slave must both be either full or lite axi4 interfaces",
      Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
    )

    if (master.cfg.lite)
      lite.buffer(master.asLite, slave.asLite, cfg)
    else
      full.buffer(master.asFull, slave.asFull, cfg)
  }
}

object SlaveBuffer {
  def apply(
      interface: RawInterface,
      cfg: BufferConfig = BufferConfig.all(2),
      name: String = "slaveBuffer"
  )(implicit si: SourceInfo): RawInterface = uniquePrefix(name) {
    val result = Wire(Slave(interface.cfg))
    buffer(interface, result, cfg)
    result
  }

  def apply(
      interfaces: Seq[RawInterface],
      cfg: BufferConfig,
      name: String
  )(implicit si: SourceInfo): Seq[RawInterface] =
    interfaces.zipWithIndex.map { case (interface, index) =>
      prefix(index.toString) {
        apply(interface, cfg, name)
      }
    }

  def apply(
      interfaces: Seq[RawInterface],
      cfg: BufferConfig
  )(implicit si: SourceInfo): Seq[RawInterface] =
    apply(interfaces, cfg, "slaveBuffer")

  def apply(
      interfaces: Seq[RawInterface]
  )(implicit si: SourceInfo): Seq[RawInterface] =
    apply(interfaces, BufferConfig.all(2), "slaveBuffer")
}

object SlaveBuffered {
  def apply(
      interface: RawInterface,
      cfg: BufferConfig = BufferConfig.all(2)
  )(implicit si: SourceInfo): RawInterface = {
    val result = Wire(Slave(interface.cfg))
    buffer(interface, result, cfg)
    result
  }

  def apply(
      interfaces: Seq[RawInterface],
      cfg: BufferConfig
  )(implicit si: SourceInfo): Seq[RawInterface] =
    interfaces.zipWithIndex.map { case (interface, index) =>
      prefix(index.toString) {
        apply(interface, cfg)
      }
    }

  def apply(
      interfaces: Seq[RawInterface]
  )(implicit si: SourceInfo): Seq[RawInterface] =
    apply(interfaces, BufferConfig.all(2))
}

object LeftBuffer {
  def apply(
      interface: RawInterface,
      cfg: BufferConfig = BufferConfig.all(2),
      name: String = "leftBuffer"
  )(implicit si: SourceInfo): RawInterface = uniquePrefix(name) {
    val result = Wire(Slave(interface.cfg))
    buffer(interface, result, cfg)
    result
  }

  def apply(
      interfaces: Seq[RawInterface],
      cfg: BufferConfig,
      name: String
  )(implicit si: SourceInfo): Seq[RawInterface] =
    interfaces.zipWithIndex.map { case (interface, index) =>
      prefix(index.toString) {
        apply(interface, cfg, name)
      }
    }

  def apply(
      interfaces: Seq[RawInterface],
      cfg: BufferConfig
  )(implicit si: SourceInfo): Seq[RawInterface] =
    apply(interfaces, cfg, "leftBuffer")

  def apply(
      interfaces: Seq[RawInterface]
  )(implicit si: SourceInfo): Seq[RawInterface] =
    apply(interfaces, BufferConfig.all(2), "leftBuffer")
}

object MasterBuffer {
  def apply(
      interface: RawInterface,
      cfg: BufferConfig = BufferConfig.all(2),
      name: String = "masterBuffer"
  )(implicit si: SourceInfo): RawInterface = uniquePrefix(name) {
    val result = Wire(Master(interface.cfg))
    buffer(result, interface, cfg)
    result
  }

  def apply(
      interfaces: Seq[RawInterface],
      cfg: BufferConfig,
      name: String
  )(implicit si: SourceInfo): Seq[RawInterface] =
    interfaces.zipWithIndex.map { case (interface, index) =>
      prefix(index.toString) {
        apply(interface, cfg, name)
      }
    }

  def apply(
      interfaces: Seq[RawInterface],
      cfg: BufferConfig
  )(implicit si: SourceInfo): Seq[RawInterface] =
    apply(interfaces, cfg, "masterBuffer")

  def apply(
      interfaces: Seq[RawInterface]
  )(implicit si: SourceInfo): Seq[RawInterface] =
    apply(interfaces, BufferConfig.all(2), "masterBuffer")
}

object MasterBuffered {
  def apply(
      interface: RawInterface,
      cfg: BufferConfig = BufferConfig.all(2)
  )(implicit si: SourceInfo): RawInterface = {
    val result = Wire(Master(interface.cfg))
    buffer(result, interface, cfg)
    result
  }

  def apply(
      interfaces: Seq[RawInterface],
      cfg: BufferConfig
  )(implicit si: SourceInfo): Seq[RawInterface] =
    interfaces.zipWithIndex.map { case (interface, index) =>
      prefix(index.toString) {
        apply(interface, cfg)
      }
    }

  def apply(
      interfaces: Seq[RawInterface]
  )(implicit si: SourceInfo): Seq[RawInterface] =
    apply(interfaces, BufferConfig.all(2))
}

object RightBuffer {
  def apply(
      interface: RawInterface,
      cfg: BufferConfig = BufferConfig.all(2),
      name: String = "rightBuffer"
  )(implicit si: SourceInfo): RawInterface = uniquePrefix(name) {
    val result = Wire(Master(interface.cfg))
    buffer(result, interface, cfg)
    result
  }

  def apply(
      interfaces: Seq[RawInterface],
      cfg: BufferConfig,
      name: String
  )(implicit si: SourceInfo): Seq[RawInterface] =
    interfaces.zipWithIndex.map { case (interface, index) =>
      prefix(index.toString) {
        apply(interface, cfg, name)
      }
    }

  def apply(
      interfaces: Seq[RawInterface],
      cfg: BufferConfig
  )(implicit si: SourceInfo): Seq[RawInterface] =
    apply(interfaces, cfg, "rightBuffer")

  def apply(
      interfaces: Seq[RawInterface]
  )(implicit si: SourceInfo): Seq[RawInterface] =
    apply(interfaces, BufferConfig.all(2), "rightBuffer")
}
