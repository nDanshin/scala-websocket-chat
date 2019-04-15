package chat.api.domain.rooms

import cats.Monad
import cats.data.EitherT
import cats.implicits.toFunctorOps
import chat.api.domain.users.User
import chat.api.domain.{RoomAlreadyExistsError, RoomNotFoundError}

class RoomService[F[_]: Monad](repo: RoomRepositoryAlgebra[F],
                               validation: RoomValidationAlgebra[F]) {

  def createRoom(createRoom: CreateRoom): EitherT[F, RoomAlreadyExistsError, Room] = for {
    _ <- validation.doesNotExist(createRoom.name)
    saved <- EitherT.liftF(repo.create(createRoom))
  } yield saved

  def getRoom(roomId: Room.Id): EitherT[F, RoomNotFoundError.type, Room] =
    EitherT.fromOptionF(repo.get(roomId), RoomNotFoundError)

  def getRoomByName(roomName: String): EitherT[F, RoomNotFoundError.type, Room] =
    EitherT.fromOptionF(repo.findByRoomName(roomName), RoomNotFoundError)

  def deleteRoom(roomId: Room.Id): F[Unit] = repo.delete(roomId).as(())

  def update(room: Room): EitherT[F, RoomNotFoundError.type, Room] = for {
    _ <- validation.exists(room.id)
    saved <- EitherT.fromOptionF(repo.update(room), RoomNotFoundError)
  } yield saved

  def getUserRooms(user: User): F[List[Room]] = repo.getUserRooms(user.id)
}

object RoomService {
  def apply[F[_]: Monad](repo: RoomRepositoryAlgebra[F],
                         validation: RoomValidationAlgebra[F]): RoomService[F] =
    new RoomService[F](repo, validation)
}
