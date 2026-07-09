package chext.tracking

import chisel3.experimental.SourceInfo

import hdlinfo.TypedObject

import scala.collection.mutable.ArrayBuffer

import chext.util.sourceInfoToString

sealed trait BaseComponent extends HasPath {
  final def isContainer: Boolean = this.isInstanceOf[Container]
  final def asContainer = this.asInstanceOf[Container]

  def isComponent: Boolean = this.isInstanceOf[Component]
  final def asComponent = this.asInstanceOf[Component]

  private var parentOption_ = Option.empty[Container]
  final def parentOption = parentOption_
  final def parent = parentOption_.get
  private[tracking] final def setParent(c: Container): Unit = {
    parentOption_ = Some(c)
  }

  def children: Seq[BaseComponent]

  override def toString(): String = {
    assert(isComponent || isContainer)

    val kind =
      if (isComponent) "Component"
      else "Container"

    f"$kind[$tpe]: $pathStr @[${sourceInfoToString(sourceInfo)}]"
  }

  private val args_ = ArrayBuffer.empty[(String, TypedObject)]
  protected final def addArgument(name: String, arg: TypedObject): Unit = {
    args_.addOne(name -> arg)
  }
  private[chext] final def args = args_.toSeq

  // addChild(...) calls parentOption_, which must initialized
  // for this reason, we have init code here.
  private[tracking] final val moduleInfo = Manager.registerCurrentModule()
  moduleInfo.addComponent(this)
  moduleInfo.lastContainerOption.foreach { _.addChild(this) }
}

trait Container extends BaseComponent {
  private val require_ = chext.util.Require.inferred()

  private val components_ = ArrayBuffer.empty[BaseComponent]

  /** Adds a child to this container.
    *
    * @param baseComponent
    */
  final def addChild(baseComponent: BaseComponent): Unit = {
    require_(
      baseComponent.parentOption.isEmpty,
      "Container.addChild: the baseComponent must not have a parent already!"
    )

    components_.addOne(baseComponent)
    baseComponent.setParent(this)
  }

  def children: Seq[BaseComponent] = components_.toSeq
}

// A component cannot have other components declared inside, though I am not going to enforce this
trait Component extends BaseComponent {
  private val trackingStates_ = ArrayBuffer.empty[ComponentState]

  TagRegistry.tags.foreach { tag =>
    trackingState(tag)
  }

  final def trackingState[T <: Tag](tag: T): tag.CS =
    trackingStateFor(tag).asInstanceOf[tag.CS]

  private def trackingStateFor(tag: Tag): ComponentState = {
    while (trackingStates_.length <= tag.index)
      trackingStates_.addOne(null)

    val existing = trackingStates_(tag.index)
    if (existing ne null)
      existing
    else {
      val created = moduleInfo.trackingState(tag).newComponentState(this)
      trackingStates_(tag.index) = created
      created
    }
  }

  def children: Seq[BaseComponent] = Seq.empty
}

object withContainer {
  def apply[T](container: Container)(fn: => T): T = {
    val moduleInfo = Manager.registerCurrentModule()

    moduleInfo.pushContainer(container)
    val result = fn
    moduleInfo.popContainer()

    result
  }
}
