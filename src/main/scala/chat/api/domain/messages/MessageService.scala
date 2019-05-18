package chat.api.domain.messages

import cats.Monad
import cats.data.EitherT
import cats.effect.ConcurrentEffect
import cats.syntax.functor._
import chat.api.domain.ValidationError
import chat.api.domain.rooms.{Room, RoomRepositoryAlgebra, RoomValidationAlgebra}
import chat.api.domain.users.{User, UserValidationAlgebra}
import fs2.Stream
import fs2.concurrent.Topic

class MessageService[F[_]: Monad: ConcurrentEffect](repo: MessageRepositoryAlgebra[F],
                                                    userValidation: UserValidationAlgebra[F],
                                                    roomValidation: RoomValidationAlgebra[F],
                                                    roomRepository: RoomRepositoryAlgebra[F],
                                                    messageTopic: Topic[F, Message]) {

  def createMessage(createMessage: CreateMessage): EitherT[F, ValidationError, Message] = for {
    _ <- userValidation.exists(createMessage.user)
    _ <- roomValidation.exists(createMessage.room)
    message <- EitherT.liftF(repo.create(createMessage))
    _ <- EitherT.liftF(messageTopic.publish1(message))
  } yield message

  def getRoomsMessages(ids: List[Room.Id]): F[List[Message]] = repo.getByRoomIds(ids)

  def subscribe(userId: User.Id): Stream[F, Message] =
    messageTopic.subscribe(10)
      .evalMap { message => roomRepository.get(message.roomId).map(roomOpt => (message, roomOpt)) }
      .filter { case (_, roomOpt) => roomOpt.exists(_.members.contains(userId)) }
      .map(_._1)
}

object MessageService {
  def apply[F[_]: Monad: ConcurrentEffect](repo: MessageRepositoryAlgebra[F],
                                           userValidation: UserValidationAlgebra[F],
                                           roomValidation: RoomValidationAlgebra[F],
                                           roomRepository: RoomRepositoryAlgebra[F],
                                           messageTopic: Topic[F, Message]): MessageService[F] =
    new MessageService[F](repo, userValidation, roomValidation, roomRepository, messageTopic)
}
