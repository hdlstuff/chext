package chext.tracking

import hdlinfo.TypedObject

import scala.collection.mutable.ArrayBuffer

import chext.util.sourceInfoToString

/** A tracked construction.
  *
  * Components form a hierarchy independently of Chisel's module hierarchy. A component may own
  * layer-specific state, such as Elastic source/sink ports, or it may own child components. The
  * Elastic layer requires components with children to have no operational Elastic ports, though
  * they may expose hierarchy-only boundary ports.
  */
trait Component extends HasPath {
  private val require_ = chext.util.Require.inferred()

  private var parentOption_ = Option.empty[Component]
  final def parentOption: Option[Component] = parentOption_
  final def parent: Component = parentOption_.get
  private[tracking] final def setParent(component: Component): Unit = {
    parentOption_ = Some(component)
  }

  private val children_ = ArrayBuffer.empty[Component]

  /** Adds a child to this component. */
  final def addChild(component: Component): Unit = {
    require_(
      component.parentOption.isEmpty,
      "Component.addChild: the component must not have a parent already!"
    )

    children_.addOne(component)
    component.setParent(this)
  }

  final def children: Seq[Component] = children_.toSeq

  private val args_ = ArrayBuffer.empty[(String, TypedObject)]
  protected final def addArgument(name: String, arg: TypedObject): Unit = {
    args_.addOne(name -> arg)
  }
  private[chext] final def args = args_.toSeq

  // addChild(...) reads parentOption_, so hierarchy fields must be initialized before registration.
  private[chext] final val moduleInfo = Manager.registerCurrentModule()
  moduleInfo.addComponent(this)
  moduleInfo.lastComponentOption.foreach { _.addChild(this) }

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

  override def toString(): String =
    f"Component[$tpe]: $pathStr @[${sourceInfoToString(sourceInfo)}]"
}

/** Evaluates `fn` with `component` as the parent of newly created tracked components. */
object withComponent {
  def apply[T](component: Component)(fn: => T): T = {
    val moduleInfo = Manager.registerCurrentModule()

    moduleInfo.pushComponent(component)
    try fn
    finally moduleInfo.popComponent()
  }
}
