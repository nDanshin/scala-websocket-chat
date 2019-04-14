package chat.api.infrastructure.repository.inmemory

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import chat.api.domain.messages.{CreateMessage, Message, MessageRepositoryAlgebra}
import chat.api.domain.rooms.Room

import scala.collection.concurrent.TrieMap
import scala.util.Random

class MessageRepositoryInMemoryInterpreter[F[_]: Applicative] extends MessageRepositoryAlgebra[F] {
  private val cache = new TrieMap[Message.Id, Message]
  private val random = new Random

  override def create(createMessage: CreateMessage): F[Message] = {
    val id = Message.Id @@ random.nextLong
    val toSave = Message(id, createMessage.user, createMessage.room, createMessage.content)
    cache += (id -> toSave)
    toSave.pure[F]
  }

  override def delete(messageId: Message.Id): F[Option[Message]] = cache.remove(messageId).pure[F]

  override def getByRoomIds(ids: List[Room.Id]): F[List[Message]] =
    cache.values.filter(message => ids.contains(message.roomId)).toList.pure[F]
}

object MessageRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() = new MessageRepositoryInMemoryInterpreter[F]()
}