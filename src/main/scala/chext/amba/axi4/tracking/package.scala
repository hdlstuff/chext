package chext.amba.axi4

import chisel3.experimental.BaseModule

package object tracking {
  /** Initializes AXI property tracking for the current module. */
  def register(): Unit = {
    Tag.initialize()
    chext.tracking.register()
  }

  /** Registers a DataView-created Full or Lite interface by object identity. */
  private[chext] def registerInterface(interface: Tracked): Unit = {
    Tag.initialize()
    val moduleInfo = chext.tracking.Manager.registerCurrentModule()
    moduleInfo.trackingState(Tag).registerInterface(interface)
  }

  /** Registers a DataView-created Full or Lite interface by object identity. */
  private[axi4] def registerView(interface: Tracked): Unit =
    registerInterface(interface)

  /** Runs the exhaustive AXI property pass for a previously registered module tree. */
  def check(module: BaseModule): Unit = {
    Tag.initialize()
    val moduleInfo = chext.tracking.Manager.registerModule(module)
    moduleInfo.trackingState(Tag).checkRoot()
  }
}
