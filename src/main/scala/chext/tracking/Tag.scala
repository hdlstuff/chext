package chext.tracking

import scala.collection.mutable.ArrayBuffer

trait ComponentState

trait ModuleState {
  def newComponentState(component: Component): ComponentState
  def onComplete(): Unit
}

trait Tag {
  type CS <: ComponentState
  type MS <: ModuleState

  def name: String

  final private[tracking] val index: Int =
    TagRegistry.register(this)

  def newModuleState(moduleInfo: ModuleInfo): MS
}

private[tracking] object TagRegistry {
  private var nextIndex_ = 0
  private val tags_ = ArrayBuffer.empty[Tag]

  def register(tag: Tag): Int = {
    val result = nextIndex_
    nextIndex_ += 1
    tags_.addOne(tag)
    result
  }

  def tags: Seq[Tag] =
    tags_.toSeq
}
