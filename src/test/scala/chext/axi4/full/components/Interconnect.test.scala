package chext.axi4.full.components

import chisel3._
import chisel3.util._

import chext.axi4

trait InterconnectBridge[M <: Module] {
  def slaveInterfaces(module: M): Seq[axi4.full.Interface]
  def masterInterfaces(module: M): Seq[axi4.full.Interface]
}



