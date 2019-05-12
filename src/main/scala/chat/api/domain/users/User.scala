package chat.api.domain.users

import chat.api.utils.Entity
import doobie.util.{Get, Put}
import io.circe.{Decoder, Encoder}

case class User(userName: String,
                firstName: String,
                lastName: String,
                email: String,
                hash: String,
                id: User.Id)

object User extends Entity[Long] {
  implicit val idEncoder: Encoder[User.Id] = Encoder.encodeLong.contramap[User.Id](identity)
  implicit val idDecoder: Decoder[User.Id] = Decoder.decodeLong.map(userId => User.Id @@ userId)
  implicit val idPut: Put[User.Id] = Put[Long].contramap(identity)
  implicit val idGet: Get[User.Id] = Get[Long].map(userId => User.Id @@ userId)
}
