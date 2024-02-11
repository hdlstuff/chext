package chext.axi4

import chext.axi4
import axi4.Casts._

import chisel3._
import chisel3.util._

object connect {
  private[axi4] def apply[T <: Data](
      source: ReadyValidIO[T],
      sink: ReadyValidIO[T]
  ): Unit = {
    source.ready := sink.ready
    sink.valid := source.valid
    sink.bits := source.bits
  }

  def apply(master: axi4.RawInterface, slave: axi4.full.Interface): Unit = {
    assert(master.cfg == slave.cfg)

    apply(master.asFull, slave)
  }

  def apply(master: axi4.RawInterface, slave: axi4.lite.Interface): Unit = {
    assert(master.cfg == slave.cfg)

    apply(master.asLite, slave)
  }

  def apply(master: axi4.RawInterface, slave: axi4.RawInterface): Unit = {
    assert(master.cfg == slave.cfg)

    if (master.cfg.lite)
      apply(master.asLite, slave.asLite)
    else
      apply(master.asFull, slave.asFull)
  }

  def apply(master: axi4.full.Interface, slave: axi4.full.Interface): Unit = {
    assert(master.cfg == slave.cfg)

    if (master.cfg.read) {
      apply(master.ar, slave.ar)
      apply(slave.r, master.r)
    }

    if (master.cfg.write) {
      apply(master.aw, slave.aw)
      apply(master.w, slave.w)
      apply(slave.b, master.b)
    }
  }

  def apply(master: axi4.lite.Interface, slave: axi4.lite.Interface): Unit = {
    assert(master.cfg == slave.cfg)

    if (master.cfg.read) {
      apply(master.ar, slave.ar)
      apply(slave.r, master.r)
    }

    if (master.cfg.write) {
      apply(master.aw, slave.aw)
      apply(master.w, slave.w)
      apply(slave.b, master.b)
    }
  }
}

object ConnectOps {
  implicit class axi4_interface_master(master: axi4.RawInterface) {
    def :=>(slave: axi4.RawInterface) = {
      connect(master, slave)
    }

    def :=>(slave: axi4.full.Interface) = {
      connect(master, slave)
    }

    def :=>(slave: axi4.lite.Interface) = {
      connect(master, slave)
    }
  }

  implicit class axi4_full_interface_master(master: axi4.full.Interface) {
    def :=>(slave: axi4.full.Interface) = {
      connect(master, slave)
    }
  }

  implicit class axi4_lite_interface_master(master: axi4.lite.Interface) {
    def :=>(slave: axi4.lite.Interface) = {
      connect(master, slave)
    }
  }
}
