package chat.api.domain.messages

import chat.api.domain.rooms.Room
import chat.api.domain.users.User
import chat.api.utils.Entity
import doobie.util.{Get, Put}
import io.circe.{Decoder, Encoder}

case class Message(id: Message.Id,
                   userId: User.Id,
                   roomId: Room.Id,
                   content: String)

object Message extends Entity[Long] {
  implicit val idEncoder: Encoder[Message.Id] = Encoder.encodeLong.contramap[Message.Id](identity)
  implicit val idDecoder: Decoder[Message.Id] = Decoder.decodeLong.map(messageId => Message.Id @@ messageId)
  implicit val idPut: Put[Message.Id] = Put[Long].contramap(identity)
  implicit val idGet: Get[Message.Id] = Get[Long].map(messageId => Message.Id @@ messageId)

  val empty = Message(Message.Id @@ 0l, User.Id @@ 0l, Room.Id @@ 0l, "")
}
