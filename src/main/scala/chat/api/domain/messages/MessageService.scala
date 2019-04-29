package chat.api.domain.messages

import cats.Monad
import cats.data.EitherT
import cats.effect.ConcurrentEffect
import chat.api.domain.ValidationError
import chat.api.domain.rooms.{Room, RoomValidationAlgebra}
import chat.api.domain.users.{User, UserValidationAlgebra}
import fs2.concurrent.Topic

class MessageService[F[_]: Monad: ConcurrentEffect](repo: MessageRepositoryAlgebra[F],
                                                    userValidation: UserValidationAlgebra[F],
                                                    roomValidation: RoomValidationAlgebra[F],
                                                   // todo: make topic with type ExtendenMessages, get it in createMessage, and sent it to topic there
                                                    messageTopic: Topic[F, Message]) {

  def createMessage(createMessage: CreateMessage): EitherT[F, ValidationError, Message] = for {
    _ <- userValidation.exists(createMessage.user)
    _ <- roomValidation.exists(createMessage.room)
    message <- EitherT.liftF(repo.create(createMessage))
    _ <- EitherT.liftF(messageTopic.publish1(message))
  } yield message

  def getRoomsMessages(ids: List[Room.Id]): F[List[Message]] = repo.getByRoomIds(ids)

  def subscribe(userId: User.Id) =
    messageTopic.subscribe(10).filter(_.userId == userId)
}

object MessageService {
  def apply[F[_]: Monad: ConcurrentEffect](repo: MessageRepositoryAlgebra[F],
                                           userValidation: UserValidationAlgebra[F],
                                           roomValidation: RoomValidationAlgebra[F],
                                           messageTopic: Topic[F, Message]): MessageService[F] =
    new MessageService[F](repo, userValidation, roomValidation, messageTopic)
}
