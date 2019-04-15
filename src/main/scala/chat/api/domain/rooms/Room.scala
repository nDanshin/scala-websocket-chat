package chat.api.domain.rooms

import chat.api.domain.users.User
import chat.api.utils.Entity
import io.circe.{Decoder, Encoder}

case class Room(id: Room.Id,
                name: String,
                //roomType: RoomType,
                members: List[User.Id])

object Room extends Entity[Long] {
  implicit val idEncoder: Encoder[Room.Id] = Encoder.encodeLong.contramap[Room.Id](identity)
  implicit val idDecoder: Decoder[Room.Id] = Decoder.decodeLong.map(roomId => Room.Id @@ roomId)
}
