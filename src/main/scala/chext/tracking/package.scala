package chext

import chisel3.Data
import chisel3.experimental.{BaseModule, SourceInfo}
import chisel3.experimental.prefix

package object tracking {
  /** Registers the current module for tracking. Tracking allows for checking interfaces and
    * creating a circuit graph.
    */
  def register(): Unit =
    Manager.registerCurrentModule()

  private[chext] def registerView(
      view: Data,
      source: Data,
      suffix: Option[String] = None,
      sourceInfo: Option[SourceInfo] = None
  ): Unit =
    Manager.registerCurrentModule().registerView(view, source, suffix, sourceInfo)

  /** Returns the module graph. Must be called **after** tracking is complete. Do consider using
    * this function within `tracking.onComplete(module) { ... }`.
    *
    * @param module
    * @return
    */
  def moduleGraphOption(module: BaseModule) =
    Manager.registerModule(module).moduleGraphOption

  /** Called when tracking is complete.
    *
    * @param module
    * @param f
    * @return
    */
  def onComplete(module: BaseModule)(f: => Unit) =
    Manager.registerModule(module).onComplete { f }


  def suggestInstanceName(module: BaseModule, name: String): Unit =
    Manager.registerModule(module).suggestInstanceName(name)

  def suggestInstanceName(component: Component, name: String): Unit = ()

  def uniquePrefix[T](name: String)(f: => T): T =
    prefix(Manager.registerCurrentModule().uniquePrefix(name)) { f }

  def uniquePrefix[T](f: => T): T =
    uniquePrefix("elastic")(f)
}
