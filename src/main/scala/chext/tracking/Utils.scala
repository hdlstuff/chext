package chext.tracking

import chext.{elastic => e}

object Utils {
  implicit class component_fns(val component: Component) extends AnyVal {
    def getSourceInterfaces() =
      component.sourcePorts.asInstanceOf[Seq[(String, e.Interface[_])]]

    def getSinkInterfaces() =
      component.sinkPorts.asInstanceOf[Seq[(String, e.Interface[_])]]

  }
}
