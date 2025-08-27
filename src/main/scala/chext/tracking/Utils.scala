package chext.tracking

import chisel3.experimental.BaseModule
import chisel3.hacks.PrefixManager

object uniquePrefix {
  private var counter_ = 0

  def apply[T](fn: => T): T = {
    val moduleInfo = Manager.registerCurrentModule()
    val result = PrefixManager.withRelative(moduleInfo.uniquePrefix("Unnamed")) {
      fn
    }
    counter_ += 1
    result
  }

  def apply[T](name: String)(fn: => T): T = {
    val moduleInfo = Manager.registerCurrentModule()
    val result = PrefixManager.withRelative(moduleInfo.uniquePrefix(name)) {
      fn
    }
    counter_ += 1
    result
  }
}

object suggestInstanceName {

  /** In case Chisel's naming algorithm fails, use this function to suggest a name.
    *
    * A notable use case is when instantiating a `Module` in a `deferred` block.
    *
    * @param module
    * @param name
    */
  def apply(module: BaseModule, name: String) = {
    Manager.registerModule(module).suggestInstanceName(name)
  }
}
