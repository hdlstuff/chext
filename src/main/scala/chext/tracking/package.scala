package chext

import chisel3.experimental.BaseModule
import chisel3.experimental.prefix

package object tracking {
  /** Registers the current module for tracking.
    */
  def register(): Unit =
    Manager.registerCurrentModule()

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
    uniquePrefix("tracking")(f)
}
