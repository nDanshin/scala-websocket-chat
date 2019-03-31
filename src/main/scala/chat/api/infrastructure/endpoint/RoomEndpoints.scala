package chat.api.infrastructure.endpoint

import cats.data.{EitherT, Nested}
import cats.effect.Effect
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.applicative._
import cats.syntax.semigroupk._
import chat.api.domain.{RoomAlreadyExistsError, ValidationError}
import chat.api.domain.rooms.{CreateRoom, Room, RoomService}
import chat.api.domain.users.{User, UserService}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.{EntityDecoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl

class RoomEndpoints[F[_]: Effect] extends Http4sDsl[F] {

  implicit val roomDecoder: EntityDecoder[F, Room] = jsonOf
  implicit val createRoomDecoder: EntityDecoder[F, CreateRoom] = jsonOf


  private def createRoomEndpoint(userService: UserService[F],
                                 roomService: RoomService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "rooms"  =>
        val action = for {
          create <- req.as[CreateRoom]
          _ <- userService.getUser(create.creator).value
          result <- roomService.createRoom(create).value
        } yield result

        action.flatMap {
          case Right(saved) => Ok(saved.asJson)
          case Left(RoomAlreadyExistsError(existing)) =>
            Conflict(s"The room with name ${existing.name} already exists")
        }
    }

  private def joinRoomEndpoint(roomService: RoomService[F],
                               userService: UserService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case PUT -> Root / "rooms" / "users" / LongVar(roomId) / LongVar(userId) =>
        val action = for {
          room <- roomService.getRoom(Room.Id @@ roomId)
          user <- userService.getUser(User.Id @@ userId)
          update = room.copy(members = user.id +: room.members)
          result <- roomService.update(room).value
        } yield result

        action.flatMap {
          case Right() =>
          case Left() =>
        }


    }

  private def leaveRoomEndpoint(): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case DELETE -> Root / "rooms" / "users" / LongVar(roomId) / LongVar(userId)
    }

  private def userRoomsEndpoint(): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "users" / "rooms" / LongVar(userId)
    }

  private def deleteRoomEndpoint(): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case DELETE -> Root / "rooms" / LongVar(roomId)
    }

  def endpoints(roomService: RoomService[F], userService: UserService[F]): HttpRoutes[F] =
    createRoomEndpoint(???, ???) <+>
    joinRoomEndpoint() <+>
    leaveRoomEndpoint() <+>
    userRoomsEndpoint() <+>
    deleteRoomEndpoint()
}

object RoomEndpoints {
  def endpoints[F[_]: Effect](roomService: RoomService[F],
                              userService: UserService[F]): HttpRoutes[F] =
    new RoomEndpoints[F].endpoints(roomService, userService)
}
