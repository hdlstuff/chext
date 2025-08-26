package chext.tracking

import chisel3.ChiselException
import chisel3.Module
import chisel3.RawModule
import chisel3.experimental.BaseModule

import chisel3.hacks.ModuleInternals

import scala.collection.mutable.HashMap

private[tracking] object Manager {

  private val moduleInfos_ = HashMap.empty[BaseModule, ModuleInfo]

  private[tracking] def registerModule(module: BaseModule): ModuleInfo = {
    val parentModule = ModuleInternals.getParent(module)
    parentModule.foreach { registerModule(_) }

    val parentModuleInfo = parentModule.map { moduleInfos_(_) }

    moduleInfos_.getOrElseUpdate(
      module, {
        import chisel3.hacks.deferred

        val moduleInfo = new ModuleInfo(module, parentModuleInfo)

        // println("Created a module info for: ", module)

        deferred.add(-1, Some(module.asInstanceOf[RawModule])) {
          moduleInfo.atModuleBodyEnd()
          moduleInfos_.remove(module)
        }

        moduleInfo
      }
    )
  }

  private def registerCurrentModule_(): ModuleInfo = {
    val currentModule = Module.currentModule.getOrElse(
      throw new ChiselException(
        "chext.naming: register must be called from a module!"
      )
    )

    registerModule(currentModule)
  }

  private[tracking] final def registerCurrentModule() =
    registerCurrentModule_()

}
