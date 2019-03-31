package chat.api.domain.rooms

import enumeratum._

sealed trait RoomType extends EnumEntry

case object RoomType extends Enum[RoomType] with CirceEnum[RoomType] {
  case object Direct extends RoomType
  case object Group extends RoomType

  val values = findValues
}
