package chat.api.domain.rooms

import chat.api.domain.users.User
import chat.api.utils.Entity
import doobie.util.{Get, Put}
import io.circe.{Decoder, Encoder}

case class Room(id: Room.Id,
                name: String,
                members: List[User.Id])

object Room extends Entity[Long] {
  implicit val idEncoder: Encoder[Room.Id] = Encoder.encodeLong.contramap[Room.Id](identity)
  implicit val idDecoder: Decoder[Room.Id] = Decoder.decodeLong.map(roomId => Room.Id @@ roomId)
  implicit val idPut: Put[Room.Id] = Put[Long].contramap(identity)
  implicit val idGet: Get[Room.Id] = Get[Long].map(roomId => Room.Id @@ roomId)

}
