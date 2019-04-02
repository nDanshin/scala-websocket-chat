package chat.api.infrastructure.repository.inmemory

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import chat.api.domain.rooms.{CreateRoom, Room, RoomRepositoryAlgebra}
import chat.api.domain.users.User

import scala.collection.concurrent.TrieMap
import scala.util.Random

class RoomRepositoryInMemoryInterpreter[F[_]: Applicative] extends RoomRepositoryAlgebra[F] {
  private val cache = new TrieMap[Room.Id, Room]
  private val random = new Random

  override def create(createRoom: CreateRoom): F[Room] = {
    val id = Room.Id @@ random.nextLong
    val toSave = Room(id, createRoom.name, List(createRoom.creator))
    cache += (id -> toSave)
    toSave.pure[F]
  }

  override def update(room: Room): F[Option[Room]] = cache.replace(room.id, room).pure[F]

  override def get(roomId: Room.Id): F[Option[Room]] = cache.get(roomId).pure[F]

  override def delete(roomId: Room.Id): F[Option[Room]] = cache.remove(roomId).pure[F]

  override def findByRoomName(roomName: String): F[Option[Room]] = cache.values.find(_.name == roomName).pure[F]

  override def getUserRooms(userId: User.Id): F[List[Room]] =
    cache.values.filter(_.members.contains(userId)).toList.pure[F]
}

object RoomRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() = new RoomRepositoryInMemoryInterpreter[F]()
}
