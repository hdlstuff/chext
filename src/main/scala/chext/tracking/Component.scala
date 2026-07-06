package chext.tracking

import chisel3.experimental.SourceInfo

import hdlinfo.TypedObject

import scala.collection.mutable.ArrayBuffer

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

  private val args_ = ArrayBuffer.empty[(String, TypedObject)]
  protected final def addArgument(name: String, arg: TypedObject): Unit = {
    args_.addOne(name -> arg)
  }
  private[tracking] final def args = args_.toSeq

  // addChild(...) calls parentOption_, which must initialized
  // for this reason, we have init code here.
  private val moduleInfo = Manager.registerCurrentModule()
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
  private val sourcePorts_ = ArrayBuffer.empty[(String, Tracked)]
  private val sinkPorts_ = ArrayBuffer.empty[(String, Tracked)]

  protected final def addSourcePort(name: String, source: Tracked)(implicit
      sourceInfo: SourceInfo
  ): Unit = {
    sourcePorts_.addOne(name -> source)
    source.markSource()
  }

  protected final def addSinkPort(name: String, sink: Tracked)(implicit
      sourceInfo: SourceInfo
  ): Unit = {
    sinkPorts_.addOne(name -> sink)
    sink.markSink()
  }

  private[tracking] final def sourcePorts = sourcePorts_.toSeq
  private[tracking] final def sinkPorts = sinkPorts_.toSeq

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

/** You should probably instantiate this one using `skipPrefix { ... }`.
  *
  * @param tpe
  * @param namePrefix
  * @param sourceInfo
  */
final class RigidComponent(
    override val tpe: String,
    override val namePrefix: String = ""
)(implicit val sourceInfo: SourceInfo)
    extends Component {
  def source(name: String, source: Tracked) = addSourcePort(name, source)
  def sink(name: String, sink: Tracked) = addSinkPort(name, sink)

}
