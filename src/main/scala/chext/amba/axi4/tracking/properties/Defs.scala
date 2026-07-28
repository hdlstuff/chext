package chext.amba.axi4.tracking.properties

import chext.amba.axi4.tracking.{values => v}

/** Matchable classification shared by property roles, accesses, and value types. */
sealed trait Selector {
  private[tracking] def accepts(key: Key[_]): Boolean
}

/** Origin role described by a property. */
sealed trait Role extends Selector {
  def name: String

  private[tracking] final def accepts(key: Key[_]): Boolean =
    key.role == this
}

object Role {
  case object Master extends Role {
    val name = "master"
  }

  case object Slave extends Role {
    val name = "slave"
  }

  case object None extends Role {
    val name = "none"
  }
}

/** Read/write classification described by a property. */
sealed trait Access extends Selector {
  def name: String

  private[tracking] final def accepts(key: Key[_]): Boolean =
    key.access == this
}

object Access {
  case object Read extends Access {
    val name = "read"
  }

  case object Write extends Access {
    val name = "write"
  }

  case object None extends Access {
    val name = "none"
  }
}

/** Runtime witness for one property value type. */
sealed abstract class ValueType[T](val name: String) extends Selector {
  private[tracking] final def accepts(key: Key[_]): Boolean =
    key.valueType == this
}

object ValueType {
  case object BurstShape extends ValueType[v.BurstShape]("burstShape")

  case object ThreadMode extends ValueType[v.ThreadMode]("threadMode")

  case object TrafficProfile extends ValueType[v.TrafficProfile]("trafficProfile")

  case object MemoryMap extends ValueType[v.MemoryMap]("memoryMap")
}

/** Typed identity and documentation for one AXI tracking property. */
final case class Key[T](
    role: Role,
    access: Access,
    valueType: ValueType[T],
    name: String,
    description: String
) {
  require(name.nonEmpty, "AXI4 tracking property names must not be empty")
  require(
    description.nonEmpty,
    s"AXI4 tracking property '${qualifiedName}' needs a description"
  )

  /** Stable role-qualified name used in diagnostics. */
  def qualifiedName: String =
    s"${role.name}.$name"

  override def toString: String =
    qualifiedName
}

object Key {

  /** Extracts a cell's key classifications for resolver pattern matching. */
  def unapply(
      cell: Cell[_]
  ): Some[(Role, Access, ValueType[_])] =
    Some((cell.key.role, cell.key.access, cell.key.valueType))
}
