package chisel3.hacks

import chisel3.{Data, RawModule}
import chisel3.experimental.{BaseModule, SourceInfo}
import chisel3.internal.HasId
import chisel3.internal.binding.WireBinding
import chisel3.internal.firrtl.ir.{Command, DefInstance}

object ModuleInternals {
  def getIds(module: BaseModule): Seq[HasId] = {
    val field = classOf[BaseModule].getDeclaredField("_ids")
    field.setAccessible(true)
    field.get(module).asInstanceOf[scala.collection.mutable.ArrayBuffer[HasId]].toSeq
  }

  /** Returns the ports even before the module is closed.
    *
    * @param module
    * @return
    */
  def getPorts(module: BaseModule): Seq[(Data, SourceInfo)] = {
    val field = classOf[BaseModule].getDeclaredField("_ports")
    field.setAccessible(true)
    field.get(module).asInstanceOf[scala.collection.mutable.ArrayBuffer[(Data, SourceInfo)]].toSeq
  }

  def getWires(module: BaseModule): Seq[Data] = {
    val field = classOf[BaseModule].getDeclaredField("_ids")
    field.setAccessible(true)
    val ids = field.get(module).asInstanceOf[scala.collection.mutable.ArrayBuffer[HasId]].toSeq

    ids
      .filter(_.isInstanceOf[Data])
      .map(_.asInstanceOf[Data])
      .filter(_.topBinding.isInstanceOf[WireBinding])
  }

  def getParent(module: BaseModule): Option[BaseModule] = {
    classOf[HasId].getDeclaredFields().foreach { println(_) }
    val method = classOf[HasId].getDeclaredMethod("_parent")
    method.setAccessible(true)
    method.invoke(module).asInstanceOf[Option[BaseModule]]
  }

  def getChildren(module: BaseModule): Seq[BaseModule] = {
    val field = classOf[BaseModule].getDeclaredField("_ids")
    field.setAccessible(true)
    val ids = field.get(module).asInstanceOf[scala.collection.mutable.ArrayBuffer[HasId]].toSeq

    ids.filter(_.isInstanceOf[BaseModule]).map(_.asInstanceOf[BaseModule])
  }

  def getSourceInfo(module: BaseModule): SourceInfo = {
    val method = classOf[BaseModule].getDeclaredMethod("_sourceInfo")
    method.setAccessible(true)
    method.invoke(module).asInstanceOf[SourceInfo]
  }

  def getCommands(module: RawModule): Seq[Command] = {
    import chisel3.internal.firrtl.ir.Block
    import scala.collection.mutable.Builder
    import scala.collection.immutable.ArraySeq

    val fieldBody = classOf[BaseModule].getDeclaredField("_body")
    fieldBody.setAccessible(true)
    val body = fieldBody.get(module).asInstanceOf[Block]

    val fieldCommands = classOf[Block].getDeclaredField("_commands")
    fieldCommands.setAccessible(true)
    val arrayBuilder = fieldCommands.get(body).asInstanceOf[Builder[Command, ArraySeq[Command]]]

    try {
      arrayBuilder.result()
    } catch {
      case _: NullPointerException => Seq.empty
      case e: Exception            => throw e
    }
  }

  def getChildrenSourceInfo(parent: RawModule): Map[BaseModule, SourceInfo] = {
    getCommands(parent)
      .filter { _.isInstanceOf[DefInstance] }
      .map { _.asInstanceOf[DefInstance] }
      .map { case DefInstance(sourceInfo, id, ports) => (id, sourceInfo) }
      .toMap
  }

}
