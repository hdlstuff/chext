package chext

import chisel3.experimental.BaseModule

package object tracking {
  /** Registers the current module for tracking. Tracking allows for checking interfaces and
    * creating a circuit graph.
    */
  def register(): Unit =
    Manager.registerCurrentModule()

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

}
