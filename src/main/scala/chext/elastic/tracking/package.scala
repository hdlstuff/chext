package chext.elastic

import chisel3.Data
import chisel3.experimental.BaseModule
import chisel3.experimental.SourceInfo
import chext.{elastic => e}

package object tracking {
  implicit class ComponentOps(val component: chext.tracking.Component) extends AnyVal {
    def getSourceInterfaces(): Seq[(String, e.Interface[_])] = {
      val elasticState = component.trackingState(Tag)
      elasticState.sourcePorts.asInstanceOf[Seq[(String, e.Interface[_])]]
    }

    def getSinkInterfaces(): Seq[(String, e.Interface[_])] = {
      val elasticState = component.trackingState(Tag)
      elasticState.sinkPorts.asInstanceOf[Seq[(String, e.Interface[_])]]
    }
  }

  def registerView(
      view: Data,
      source: Data,
      suffix: Option[String] = None,
      sourceInfo: Option[SourceInfo] = None
  ): Unit = {
    val moduleInfo = chext.tracking.Manager.registerCurrentModule()
    val elasticState = moduleInfo.trackingState(Tag)
    elasticState.registerView(view, source, suffix, sourceInfo)
  }

  def moduleGraphOption(module: BaseModule): Option[Graph.Module] = {
    val moduleInfo = chext.tracking.Manager.registerModule(module)
    val elasticState = moduleInfo.trackingState(Tag)
    elasticState.moduleGraphOption
  }
}
