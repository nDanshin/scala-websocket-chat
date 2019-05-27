package chat.api.infrastructure.repository.doobie

import cats.Monad
import cats.data.OptionT
import cats.syntax.functor._
import chat.api.domain.messages
import chat.api.domain.messages.{CreateMessage, Message, MessageRepositoryAlgebra}
import chat.api.domain.rooms.Room
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.Update0

object MessageSQL {

  def insert(message: CreateMessage): Update0 = sql"""
    insert into messages (user_id, room_id, content)
    values (${message.user}, ${message.room}, ${message.content})
  """.update

  def select(messageId: Message.Id): Query0[Message] = sql"""
    select id, user_id, room_id, content
    from messages
    where id = $messageId
  """.query

  def delete(messageId: Message.Id): Update0 = sql"""
    delete from messages where id = $messageId
  """.update

  def messagesByRoomIds: Query0[Message] = sql"""
    select id, user_id, room_id, content from messages
  """.query
}

class DoobieMessageRepositoryInterpreter[F[_]: Monad](xa: Transactor[F])
  extends MessageRepositoryAlgebra[F] {
  import MessageSQL._


  override def create(createMessage: CreateMessage): F[Message] =
    insert(createMessage)
      .withUniqueGeneratedKeys[Long]("id")
      .map(id => Message(Message.Id @@ id, createMessage.user, createMessage.room, createMessage.content))
      .transact(xa)

  override def delete(messageId: messages.Message.Id): F[Option[Message]] =
    OptionT(select(messageId).option.transact(xa))
      .semiflatMap(message => MessageSQL.delete(messageId).run.transact(xa).as(message))
      .value

  override def getByRoomIds(ids: List[Room.Id]): F[List[Message]] =
    messagesByRoomIds
      .to[List]
      .map(_.filter(message => ids.contains(message.roomId)))
      .transact(xa)
}

object DoobieMessageRepositoryInterpreter {
  def apply[F[_]: Monad](xa: Transactor[F]): DoobieMessageRepositoryInterpreter[F] =
    new DoobieMessageRepositoryInterpreter[F](xa)
}
