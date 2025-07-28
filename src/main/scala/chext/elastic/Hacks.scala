package chisel3.hacks

import chisel3.internal.Builder
import chisel3.internal.HasId

import chisel3.RawModule
import chisel3.ChiselException

import scala.collection.mutable.HashMap
import scala.collection.mutable.Queue

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
  private val hooks =
    HashMap.empty[RawModule, Queue[() => Unit]]

  private def addHook(fn: => Unit): Unit = {
    // This is a little bit too hacky
    val currentModule = Builder.currentModule
      .getOrElse(throw new ChiselException("There is no active module!"))
      .asInstanceOf[RawModule]

    hooks
      .getOrElseUpdate(
        currentModule, {
          val queue = Queue.empty[() => Unit]

          val method = {
            val methods = classOf[RawModule].getDeclaredMethods()
            methods.filter(_.getName() == "atModuleBodyEnd").head
          }

          method.invoke(
            currentModule,
            () => {
              while (queue.nonEmpty) {
                val item = queue.dequeue()
                item()
              }

              hooks.remove(currentModule)
            }
          )

          queue
        }
      )
      .addOne(() => fn)
  }

  /** Defers the execution of the code block after the end of the current module body. Unlike
    * `atModuleBodyEnd`, can be called from outside the module class. It respects the prefix.
    *
    * @param gen
    */
  def apply(fn: => Unit): Unit = {
    val currentPrefix = PrefixManager.current

    addHook {
      PrefixManager.withAbsolute(currentPrefix) { fn }
    }
  }

  def prefix(prefix: String)(fn: => Unit): Unit = {
    PrefixManager.withRelative(prefix) {
      apply { fn }
    }
  }
}
