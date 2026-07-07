package chext.tracking

import chisel3.experimental.SourceInfo
import chisel3.hacks.PrefixManager

trait HasPath extends chisel3.experimental.AffectsChiselPrefix {
  def tpe: String
  def namePrefix: String

  private[chext] final val path = PrefixManager.current
  private[chext] final val pathStr = PrefixManager.currentStr

  /** This variable shall be provided by derived classes. It is advisable for a class to have a
    * private implicit `SourceInfo` because if that class contains other modules needing source
    * infos, source locator is broken.
    *
    * @return
    */
  val sourceInfo: SourceInfo
}
