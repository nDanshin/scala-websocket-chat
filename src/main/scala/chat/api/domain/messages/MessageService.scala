package chat.api.domain.messages

import cats.Monad
import cats.data.EitherT
import chat.api.domain.ValidationError
import chat.api.domain.rooms.{Room, RoomValidationAlgebra}
import chat.api.domain.users.UserValidationAlgebra

class MessageService[F[_]: Monad](repo: MessageRepositoryAlgebra[F],
                                  userValidation: UserValidationAlgebra[F],
                                  roomValidation: RoomValidationAlgebra[F]) {

  def createMessage(createMessage: CreateMessage): EitherT[F, ValidationError, Message] = for {
    _ <- userValidation.exists(createMessage.user)
    _ <- roomValidation.exists(createMessage.room)
    message <- EitherT.liftF(repo.create(createMessage))
  } yield message

  def getRoomsMessages(ids: List[Room.Id]): F[List[Message]] = repo.getByRoomIds(ids)
}

object MessageService {
  def apply[F[_]: Monad](repo: MessageRepositoryAlgebra[F],
                         userValidation: UserValidationAlgebra[F],
                         roomValidation: RoomValidationAlgebra[F]): MessageService[F] =
    new MessageService[F](repo, userValidation, roomValidation)
}
