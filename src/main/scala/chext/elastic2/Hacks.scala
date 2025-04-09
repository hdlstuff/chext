package chisel3.hacks


import chisel3.internal.Builder
import chisel3.internal.HasId

import chisel3.RawModule
import chisel3.ChiselException

// Based on the experimental prefix
object PrefixManager {
  def current: Builder.Prefix = Builder.getPrefix
  def currentStr = current.reverse.mkString("_")

  def set(prefix: Builder.Prefix): Unit = Builder.setPrefix(prefix)
  def clear(): Unit = Builder.clearPrefix()

  def push(p: String) = Builder.pushPrefix(p)
  def push(p: HasId) = Builder.pushPrefix(p)
  def pop(): Unit = Builder.popPrefix()

  def withRelative[T](p: String)(f: => T): T = {
    push(p)
    val ret = f
    if (current.nonEmpty) pop()
    ret
  }

  def withRelative[T](p: HasId)(f: => T): T = {
    val pushed = push(p)
    val ret = f
    if (pushed) pop()
    ret
  }

  def withAbsolute[T](prefix: Builder.Prefix)(f: => T): T = {
    val oldPrefix = current
    set(prefix)
    val ret = f
    set(oldPrefix)
    ret
  }

  def withNone[T](f: => T): T = {
    val oldPrefix = current
    clear()
    val ret = f
    set(oldPrefix)
    ret
  }
}

object deferred {

  /** Defers the execution of the code block after the end of the current module body. Unlike
    * `atModuleBodyEnd`, can be called from outside the module class. It respects the prefix.
    *
    * @param gen
    */
  def apply(gen: => Unit): Unit = {
    val currentPrefix = PrefixManager.current

    // This is a little bit too hacky
    val currentModule = Builder.currentModule
      .getOrElse(throw new ChiselException("There is no active module!"))
      .asInstanceOf[RawModule]

    val methods = classOf[RawModule].getDeclaredMethods()
    val method = methods.filter(_.getName() == "atModuleBodyEnd").head

    method.invoke(
      currentModule,
      () => {
        PrefixManager.withAbsolute(currentPrefix) {
          gen
        }
      }
    )
  }
}
