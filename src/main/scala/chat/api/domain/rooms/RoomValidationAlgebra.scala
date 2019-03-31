package chat.api.domain.rooms

import cats.data.EitherT
import chat.api.domain.{RoomAlreadyExistsError, RoomNotFoundError}

trait RoomValidationAlgebra[F[_]] {
  def doesNotExist(roomName: String): EitherT[F, RoomAlreadyExistsError, Unit]
  def exists(roomId: Room.Id): EitherT[F, RoomNotFoundError.type, Unit]
}
