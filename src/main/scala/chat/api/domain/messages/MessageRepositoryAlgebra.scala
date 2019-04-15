package chat.api.domain.messages

import chat.api.domain.rooms.Room

trait MessageRepositoryAlgebra[F[_]] {
  def create(createMessage: CreateMessage): F[Message]
  def delete(messageId: Message.Id): F[Option[Message]]
  def getByRoomIds(ids: List[Room.Id]): F[List[Message]]
}
