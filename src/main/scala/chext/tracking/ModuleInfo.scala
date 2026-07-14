package chext.tracking

import chisel3.experimental.{BaseModule, SourceInfo}

import chisel3.hacks.ModuleInternals
import chisel3.hacks.PrefixManager

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.{HashMap, LinkedHashMap}
import scala.collection.mutable.Stack

import chext.util.Logger
import chext.util.sourceInfoToString
import hdlinfo.TypedObject

private[chext] class ModuleInfo(
    val module: BaseModule,
    val parent: Option[ModuleInfo]
) {
  private val require_ = chext.util.Require.inferred()

  private var atModuleBodyEndCalled_ = false
  private val logger = new Logger("tracking")

  private val children_ = LinkedHashMap.empty[BaseModule, ModuleInfo]

  private val components_ = ArrayBuffer.empty[Component]

  private val componentStack_ = Stack.empty[Component]

  private val uniquePrefix_ = HashMap.empty[String, Int]

  private var suggestedInstanceName_ = Option.empty[String]

  private val args_ = ArrayBuffer.empty[(String, TypedObject)]

  private var onComplete_ = ArrayBuffer.empty[() => Unit]

  /** @return
    *   Children `ModuleInfo`s.
    */
  def children = children_.toSeq

  /** @return
    *   Components used by the module.
    */
  def components = components_.toSeq

  /** Suggests a name for this instance.
    *
    * @param name
    */
  def suggestInstanceName(name: String): Unit = {
    val fullName = (name :: PrefixManager.current).reverse.mkString("_")
    suggestedInstanceName_ = Some(fullName)
  }

  /** @return
    *   Instance name, if used as an instance.
    */
  lazy val instanceName = suggestedInstanceName_ match {
    case Some(value) => value
    case None =>
      try {
        module.instanceName
      } catch {
        case _: NoSuchElementException => {
          logger.error(
            "instanceName",
            f"Instance name of $this could not be determined.",
            f"You instantiated a module in a deferred block, check 'suggestInstanceName'."
          )

          "???"
        }
      }
  }

  private[chext] def childInstanceName(childModule: BaseModule) =
    children_(childModule).instanceName

  /** Registers a new component.
    *
    * @param component
    */
  def addComponent(component: Component): Unit = {
    components_.addOne(component)
  }

  def pushComponent(component: Component): Unit = {
    componentStack_.push(component)
  }

  def popComponent(): Unit = {
    componentStack_.pop()
  }

  def lastComponentOption: Option[Component] =
    if (componentStack_.length > 0)
      Some(componentStack_.top)
    else
      None

  def uniquePrefix(x: String): String = {
    val fullPrefixStr = (x :: PrefixManager.current).reverse.mkString("_")
    val n = uniquePrefix_.getOrElseUpdate(fullPrefixStr, 0)
    uniquePrefix_.update(fullPrefixStr, n + 1)
    f"$x$n"
  }

  def addArgument(name: String, arg: TypedObject): Unit = {
    args_.addOne(name -> arg)
  }

  def args = args_.toSeq

  private val trackingStates_ = ArrayBuffer.empty[ModuleState]

  TagRegistry.tags.foreach { tag =>
    trackingState(tag)
  }

  final def trackingState[T <: Tag](tag: T): tag.MS =
    trackingStateFor(tag).asInstanceOf[tag.MS]

  private def trackingStateFor(tag: Tag): ModuleState = {
    while (trackingStates_.length <= tag.index)
      trackingStates_.addOne(null)

    val existing = trackingStates_(tag.index)
    if (existing ne null)
      existing
    else {
      val created = tag.newModuleState(this)
      trackingStates_(tag.index) = created
      created
    }
  }

  /** Adds an onComplete handler.
    *
    * @param f
    * @return
    */
  def onComplete(f: => Unit) = onComplete_.addOne(() => f)

  parent.foreach { _.children_.addOne(module -> this) }

  /** Called when the module body completes.
    */
  def atModuleBodyEnd(): Unit = {
    require_(!atModuleBodyEndCalled_, "atModuleBodyEnd must be called once!")
    atModuleBodyEndCalled_ = true

    /** Path checks.
      */
    def pathChecks() = {
      val usedPaths = components_.groupBy(_.pathStr)

      usedPaths.foreach {
        case (pathStr, users) => {
          def warn(msg: String): Unit =
            logger.warn(
              "pathChecks",
              // format: off
              Seq(
                msg,
                f"Module: ${module.toString()} @[${sourceInfoToString(ModuleInternals.getSourceInfo(module))}]",
                f"Prefix: $pathStr"
              ) ++ users.map { _.toString() }: _*
              // format: on
            )

          if (pathStr.isEmpty) {
            warn("Multiple components use an empty path, which should be avoided")
          } else {
            if (users.length == 1) {
              // this is OK, the prefix has a single user
            } else {
              warn(
                "Multiple components use the same path, which should be avoided"
              )
            }
          }
        }
      }
    }

    /** Name-prefix convention checks.
      */
    def namePrefixChecks() = {
      components_.foreach { component =>
        component.path.headOption.foreach { latestPrefix =>
          val expectedPrefix = component.namePrefix
          val suffix = latestPrefix.stripPrefix(expectedPrefix)
          val startsWithExpectedPrefix =
            expectedPrefix.nonEmpty &&
              latestPrefix.startsWith(expectedPrefix) &&
              suffix.headOption.forall { c =>
                c.isDigit || c.isUpper || c == '_'
              }

          if (!startsWithExpectedPrefix) {
            logger.warn(
              "namePrefixChecks",
              "Component path does not start with its expected namePrefix",
              f"Module: ${module.toString()} @[${sourceInfoToString(ModuleInternals.getSourceInfo(module))}]",
              f"Expected namePrefix: $expectedPrefix",
              f"Latest prefix: $latestPrefix",
              f"Path: ${component.pathStr}",
              component.toString()
            )
          }
        }
      }
    }

    pathChecks()
    namePrefixChecks()
    TagRegistry.tags.foreach { tag =>
      trackingState(tag).onComplete()
    }

    onComplete_.foreach { _() }

    /** Unregisters the children, and if there is no parent, this module.
      */
    def unregister() = {
      children.foreach { //
        case (module, _) => Manager.unregisterModule(module)
      }

      if (parent.isEmpty) {
        Manager.unregisterModule(module)
      }
    }

    unregister()

  }
}
