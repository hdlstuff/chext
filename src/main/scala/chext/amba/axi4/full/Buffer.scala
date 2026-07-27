package chext.amba.axi4.full

import chisel3._
import chisel3.experimental.prefix
import chisel3.experimental.SourceInfo

import chext.amba.axi4
import chext.amba.axi4.BufferConfig

import chext.elastic
import elastic.{SinkBuffer, SourceBuffer}
import elastic.ConnectOp._

import chext.tracking.{uniquePrefix, withComponent}

final class Buffer(
    val master: Interface,
    val slave: Interface,
    val cfg: BufferConfig
)(implicit si_ : SourceInfo)
    extends chext.tracking.Component {
  private val require_ = chext.util.Require.inferred()

  val sourceInfo: SourceInfo = si_
  def tpe: String = "Axi4f_Buffer"
  def namePrefix: String = "axi4fBuffer"

  require_.here(
    master.cfg == slave.cfg,
    "master and slave configurations do not match!",
    Seq(f"master.cfg = ${master.cfg}", f"slave.cfg = ${slave.cfg}")
  )

  private val elasticState = trackingState(elastic.tracking.Tag)
  private val resolver = new Buffer_Resolver(this)

  if (master.cfg.read) {
    elasticState.addSource("master_ar", master.ar, boundary = true)
    elasticState.addSink("master_r", master.r, boundary = true)
    elasticState.addSink("slave_ar", slave.ar, boundary = true)
    elasticState.addSource("slave_r", slave.r, boundary = true)
  }
  if (master.cfg.write) {
    elasticState.addSource("master_aw", master.aw, boundary = true)
    elasticState.addSource("master_w", master.w, boundary = true)
    elasticState.addSink("master_b", master.b, boundary = true)
    elasticState.addSink("slave_aw", slave.aw, boundary = true)
    elasticState.addSink("slave_w", slave.w, boundary = true)
    elasticState.addSource("slave_b", slave.b, boundary = true)
  }

  private def insertBufferR(): Unit = {
    SourceBuffer(master.ar, cfg.ar, name = "arBuffer") :=> slave.ar
    slave.r :=> SinkBuffer(master.r, cfg.r, name = "rBuffer")
  }

  private def insertBufferW(): Unit = {
    SourceBuffer(master.aw, cfg.aw, name = "awBuffer") :=> slave.aw
    SourceBuffer(master.w, cfg.w, name = "wBuffer") :=> slave.w
    slave.b :=> SinkBuffer(master.b, cfg.b, name = "bBuffer")
  }

  withComponent(this) {
    if (master.cfg.read)
      insertBufferR()

    if (master.cfg.write)
      insertBufferW()
  }
}

private[axi4] object BufferImpl {
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
    uniquePrefix("axi4fBuffer") {
      new Buffer(master, slave, cfg)
    }
  }
}

object SlaveBuffer {
  def apply(
      interface: Interface,
      cfg: BufferConfig = BufferConfig.all(2),
      name: String = "slaveBuffer"
  )(implicit si: SourceInfo): Interface =
    uniquePrefix(name) {
      impl(interface, cfg)
    }

  private def impl(
      interface: Interface,
      cfg: BufferConfig
  )(implicit si: SourceInfo): Interface = {
    val result = Wire(Slave(interface.cfg))
    BufferImpl(interface, result, cfg)
    result
  }

  def apply(
      interfaces: Seq[Interface],
      cfg: BufferConfig,
      name: String
  )(implicit si: SourceInfo): Seq[Interface] =
    uniquePrefix(s"${name}Many") {
      interfaces.zipWithIndex.map { case (interface, index) =>
        prefix(index.toString) {
          impl(interface, cfg)
        }
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
    BufferImpl(interface, result, cfg)
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
  )(implicit si: SourceInfo): Interface =
    uniquePrefix(name) {
      impl(interface, cfg)
    }

  private def impl(
      interface: Interface,
      cfg: BufferConfig
  )(implicit si: SourceInfo): Interface = {
    val result = Wire(Slave(interface.cfg))
    BufferImpl(interface, result, cfg)
    result
  }

  def apply(
      interfaces: Seq[Interface],
      cfg: BufferConfig,
      name: String
  )(implicit si: SourceInfo): Seq[Interface] =
    uniquePrefix(s"${name}Many") {
      interfaces.zipWithIndex.map { case (interface, index) =>
        prefix(index.toString) {
          impl(interface, cfg)
        }
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
  )(implicit si: SourceInfo): Interface =
    uniquePrefix(name) {
      impl(interface, cfg)
    }

  private def impl(
      interface: Interface,
      cfg: BufferConfig
  )(implicit si: SourceInfo): Interface = {
    val result = Wire(Master(interface.cfg))
    BufferImpl(result, interface, cfg)
    result
  }

  def apply(
      interfaces: Seq[Interface],
      cfg: BufferConfig,
      name: String
  )(implicit si: SourceInfo): Seq[Interface] =
    uniquePrefix(s"${name}Many") {
      interfaces.zipWithIndex.map { case (interface, index) =>
        prefix(index.toString) {
          impl(interface, cfg)
        }
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
    BufferImpl(result, interface, cfg)
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
  )(implicit si: SourceInfo): Interface =
    uniquePrefix(name) {
      impl(interface, cfg)
    }

  private def impl(
      interface: Interface,
      cfg: BufferConfig
  )(implicit si: SourceInfo): Interface = {
    val result = Wire(Master(interface.cfg))
    BufferImpl(result, interface, cfg)
    result
  }

  def apply(
      interfaces: Seq[Interface],
      cfg: BufferConfig,
      name: String
  )(implicit si: SourceInfo): Seq[Interface] =
    uniquePrefix(s"${name}Many") {
      interfaces.zipWithIndex.map { case (interface, index) =>
        prefix(index.toString) {
          impl(interface, cfg)
        }
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

private final class Buffer_Resolver(owner: Buffer)(implicit sourceInfo: SourceInfo)
    extends axi4.tracking.Resolver(owner) {
  import axi4.tracking._

  bindSlave(owner.master)
  bindMaster(owner.slave)

  def resolve[T](request: ResolveRequest[T]): ResolveResult =
    request match {
      case Request(TrafficProfile()) =>
        request.incomplete()
      case SlaveRequests(_) =>
        forwardTo(request, owner.slave)
      case MasterRequests(_) =>
        forwardTo(request, owner.master)
      case _ =>
        missingCase(request)
    }
}
