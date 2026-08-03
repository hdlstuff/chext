package chext.amba.axi4.tracking

package object properties extends chext.amba.axi4.tracking.properties.Keys {
  final val Master: Role.Master.type = Role.Master
  final val Slave: Role.Slave.type = Role.Slave

  final val Read: Access.Read.type = Access.Read
  final val Write: Access.Write.type = Access.Write
  final val NoAccess: Access.None.type = Access.None

  final val BurstShape: ValueType.BurstShape.type =
    ValueType.BurstShape
  final val ThreadMode: ValueType.ThreadMode.type =
    ValueType.ThreadMode
  final val TrafficProfile: ValueType.TrafficProfile.type =
    ValueType.TrafficProfile
  final val MemoryMap: ValueType.MemoryMap.type =
    ValueType.MemoryMap
}
