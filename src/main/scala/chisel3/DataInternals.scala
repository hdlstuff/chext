package chisel3

import internal.{ChildBinding, PortBinding, SecretPortBinding, AggregateViewBinding}
import scala.annotation.nowarn

import scala.reflect.ClassTag

object DataInternals {
  @nowarn // deprecation warning
  def isIO(data: Data): Boolean = {
    data.binding match {
      case Some(value: PortBinding)       => true
      case Some(value: SecretPortBinding) => true
      case _                              => false
    }
  }

  @nowarn // deprecation warning
  def isParentIO(data: Data): Boolean = {
    data.binding match {
      case Some(value: ChildBinding)      => isParentIO(value.parent)
      case Some(value: PortBinding)       => true
      case Some(value: SecretPortBinding) => true
      case Some(value: AggregateViewBinding) => {
        // TODO figure out a way to make this function return the IO
        // binding of the original Data that is viewed.
        false
      }
      case _ => false
    }
  }

  def getChildrenOfType[T <: Data: ClassTag](data: Data): Seq[T] = {
    data match {
      case t: T => Seq(t)
      case agg: Aggregate =>
        agg.getElements.map { x => getChildrenOfType[T](x) }.flatten
      case _ => Seq.empty
    }
  }
}
