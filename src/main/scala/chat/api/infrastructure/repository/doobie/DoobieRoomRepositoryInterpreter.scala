package chat.api.infrastructure.repository.doobie

import cats.Monad
import cats.data.OptionT
import cats.syntax.functor._
import chat.api.domain.rooms.{CreateRoom, Room, RoomRepositoryAlgebra}
import chat.api.domain.users.User
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.Update0

private object RoomSQL {

  def insert(room: CreateRoom): Update0 = {
    sql"""
    insert into rooms (name, members)
    values (${room.name}, ${List(room.creator)})
  """.update
  }

  def update(room: Room): Update0 = sql"""
    update rooms
    set name = ${room.name}, members = ${room.members}
    where id = ${room.id}
  """.update

  def select(roomId: Room.Id): Query0[Room] = sql"""
    select id, name, members
    from rooms
    where id = $roomId
  """.query

  def byRoomName(roomName: String): Query0[Room] = sql"""
    select id, name, members
    from rooms
    where name = $roomName
  """.query

  def delete(roomId: Room.Id): Update0 = sql"""
    delete from rooms where id = $roomId
  """.update

  def selectUserRooms(userId: User.Id): Query0[Room] = sql"""
    select id, name, members
    from rooms
    where $userId = ANY (members)
  """.query

}

class DoobieRoomRepositoryInterpreter[F[_]: Monad](xa: Transactor[F]) extends RoomRepositoryAlgebra[F] {
  import RoomSQL._

  override def create(createRoom: CreateRoom): F[Room] =
    insert(createRoom)
      .withUniqueGeneratedKeys[Long]("id")
      .map(id => Room(Room.Id @@ id, createRoom.name, List(createRoom.creator)))
      .transact(xa)

  override def update(room: Room): F[Option[Room]] =
    OptionT.liftF(RoomSQL.update(room).run.transact(xa).as(room)).value

  override def get(roomId: Room.Id): F[Option[Room]] = select(roomId).option.transact(xa)

  override def delete(roomId: Room.Id): F[Option[Room]] =
    OptionT(get(roomId))
      .semiflatMap(room => RoomSQL.delete(roomId).run.transact(xa).as(room))
      .value

  override def findByRoomName(name: String): F[Option[Room]] = byRoomName(name).option.transact(xa)

  override def getUserRooms(userId: User.Id): F[List[Room]] = selectUserRooms(userId).to[List].transact(xa)
}

object DoobieRoomRepositoryInterpreter {
  def apply[F[_]: Monad](xa: Transactor[F]): DoobieRoomRepositoryInterpreter[F] =
    new DoobieRoomRepositoryInterpreter[F](xa)
}
