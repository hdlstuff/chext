package chext.elastic.tracking

import chisel3.Data
import chext.elastic.Interface

import hdlinfo.TypedObject

/** Defines a components that forms a part of the elastic dataflow graph of the circuit.
  *
  * @param name
  * @param tpe
  * @param sources
  * @param sinks
  * @param args
  */
case class Component(
    name: String,
    tpe: String,
    sources: Seq[(String, Interface[Data])],
    sinks: Seq[(String, Interface[Data])],
    args: Map[String, TypedObject] = Map.empty
) {
  def register(): Unit = {
    Tracked.registerComponent(this)
  }
}
