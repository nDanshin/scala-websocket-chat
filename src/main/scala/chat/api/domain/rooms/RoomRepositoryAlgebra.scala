package chat.api.domain.rooms

import chat.api.domain.users.User

trait RoomRepositoryAlgebra[F[_]] {
  def create(createRoom: CreateRoom): F[Room]
  def update(room: Room): F[Option[Room]]
  def get(roomId: Room.Id): F[Option[Room]]
  def delete(roomId: Room.Id): F[Option[Room]]
  def findByRoomName(name: String): F[Option[Room]]
  def getUserRooms(userId: User.Id): F[List[Room]]
}
