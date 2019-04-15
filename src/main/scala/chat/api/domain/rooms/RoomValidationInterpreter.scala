package chat.api.domain.rooms

import cats.Monad
import cats.data.EitherT
import cats.syntax.functor._
import chat.api.domain.{RoomAlreadyExistsError, RoomNotFoundError, rooms}

class RoomValidationInterpreter[F[_]: Monad](roomRepo: RoomRepositoryAlgebra[F]) extends RoomValidationAlgebra[F] {
  override def doesNotExist(roomName: String): EitherT[F, RoomAlreadyExistsError, Unit] = EitherT {
    roomRepo.findByRoomName(roomName).map {
      case None => Right(())
      case Some(room) => Left(RoomAlreadyExistsError(room))
    }
  }

  override def exists(roomId: rooms.Room.Id): EitherT[F, RoomNotFoundError.type, Unit] = EitherT {
    roomRepo.get(roomId).map {
      case Some(_) => Right(())
      case _ => Left(RoomNotFoundError)
    }
  }
}

object RoomValidationInterpreter {
  def apply[F[_]: Monad](roomRepo: RoomRepositoryAlgebra[F]): RoomValidationInterpreter[F] =
    new RoomValidationInterpreter[F](roomRepo)
}
