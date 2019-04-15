package chat.api.domain

import chat.api.domain.rooms.Room
import chat.api.domain.users.User

sealed trait ValidationError extends Product with Serializable
case object RoomNotFoundError extends ValidationError
case class RoomAlreadyExistsError(room: Room) extends ValidationError

case object UserNotFoundError extends ValidationError
case class UserAlreadyExistsError(user: User) extends ValidationError
case class UserAuthenticationFailedError(userName: String) extends ValidationError
