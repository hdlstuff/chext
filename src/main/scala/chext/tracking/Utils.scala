package chext.tracking

import chext.{elastic => e}

object Utils {
  implicit class component_fns(component: Component) {
    def getSourceInterfaces() =
      component.sourcePorts.asInstanceOf[Seq[(String, e.Interface[_])]]

    def getSinkInterfaces() =
      component.sinkPorts.asInstanceOf[Seq[(String, e.Interface[_])]]

  }
}
