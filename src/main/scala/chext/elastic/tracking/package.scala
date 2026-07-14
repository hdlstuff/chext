package chext.elastic

import chisel3.Data
import chisel3.experimental.BaseModule
import chisel3.experimental.SourceInfo

package object tracking {
  /** Manually initializes Elastic tracking for the current module.
    *
    * This is needed when a module exposes Elastic interfaces but contains no
    * Elastic components that would otherwise initialize tracking.
    */
  def register(): Unit = {
    Tag.initialize()
    chext.tracking.register()
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
