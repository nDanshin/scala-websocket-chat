package chat.api.infrastructure.endpoint

import cats.data.EitherT
import cats.effect.Effect
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.semigroupk._
import chat.api.domain.{RoomNotFoundError, UserNotFoundError}
import chat.api.domain.messages.{CreateMessage, Message, MessageService}
import chat.api.domain.rooms.RoomService
import chat.api.domain.users.{User, UserService}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds

class MessageEndpoints[F[_]: Effect] extends Http4sDsl[F] {

  implicit val messageDecoder: EntityDecoder[F, Message] = jsonOf
  implicit val messageEncoder: EntityEncoder[F, Message] = jsonEncoderOf
  implicit val messagesEncoder: EntityEncoder[F, List[Message]] = jsonEncoderOf
  implicit val createMessageDecoder: EntityDecoder[F, CreateMessage] = jsonOf

  private def sendMessageEndpoint(messageService: MessageService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "messages" =>
        val action = for {
          create <- req.as[CreateMessage]
          result <- messageService.createMessage(create).value
        } yield result

        action.flatMap {
          case Right(message) => Ok(message.asJson)
          case Left(UserNotFoundError) => NotFound("User not found")
          case Left(RoomNotFoundError) => NotFound("Room not found")
          case _ => NotFound("User or room not found")
        }
    }

  private def userMessagesEndpoint(messageService: MessageService[F],
                                   userService: UserService[F],
                                   roomService: RoomService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "users" / "messages" / LongVar(userId) =>
        val action: EitherT[F, UserNotFoundError.type, List[Message]] = for {
          user <- userService.getUser(User.Id @@ userId)
          rooms <- EitherT.liftF(roomService.getUserRooms(user))
          messages <- EitherT.liftF(messageService.getRoomsMessages(rooms.map(_.id)))
        } yield messages

        action.value.flatMap {
          case Right(messages) => Ok(messages.asJson)
          case _ => NotFound("User not found")
        }
    }

  def endpoints(messageService: MessageService[F],
                userService: UserService[F],
                roomService: RoomService[F]): HttpRoutes[F] =
    sendMessageEndpoint(messageService) <+>
    userMessagesEndpoint(messageService, userService, roomService)
}

object MessageEndpoints {
  def endpoints[F[_]: Effect](messageService: MessageService[F],
                              userService: UserService[F],
                              roomService: RoomService[F]): HttpRoutes[F] =
    new MessageEndpoints[F].endpoints(messageService, userService, roomService)
}
