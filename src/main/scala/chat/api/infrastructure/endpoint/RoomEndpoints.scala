package chat.api.infrastructure.endpoint

import cats.effect._
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.semigroupk._
import cats.syntax.bifunctor._
import chat.api.domain.{RoomAlreadyExistsError, UserNotFoundError, ValidationError}
import chat.api.domain.rooms.{CreateRoom, Room, RoomService}
import chat.api.domain.users.{User, UserService}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds

class RoomEndpoints[F[_]: Effect] extends Http4sDsl[F] {

  implicit val roomDecoder: EntityDecoder[F, Room] = jsonOf
  implicit val roomEncoder: EntityEncoder[F, Room] = jsonEncoderOf
  implicit val roomsEncoder: EntityEncoder[F, List[Room]] = jsonEncoderOf
  implicit val createRoomDecoder: EntityDecoder[F, CreateRoom] = jsonOf

  private def createRoomEndpoint(roomService: RoomService[F],
                                 userService: UserService[F]): HttpRoutes[F] =
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
          room <- roomService.getRoom(Room.Id @@ roomId).leftWiden[ValidationError]
          user <- userService.getUser(User.Id @@ userId).leftWiden[ValidationError]
          joined = room.copy(members = user.id +: room.members)
          result <- roomService.update(joined).leftWiden[ValidationError]
        } yield result

        action.value.flatMap {
          case Right(joined) => Ok(joined.asJson)
          case _ => NotFound("Room or user not found")
        }
    }

  private def leaveRoomEndpoint(roomService: RoomService[F],
                                userService: UserService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case DELETE -> Root / "rooms" / "users" / LongVar(roomId) / LongVar(userId) =>
        val action = for {
          room <- roomService.getRoom(Room.Id @@ roomId).leftWiden[ValidationError]
          user <- userService.getUser(User.Id @@ userId).leftWiden[ValidationError]
          lefted = room.copy(members = room.members.filterNot(_ == user.id))
          result <- roomService.update(lefted).leftWiden[ValidationError]
        } yield result

        action.value.flatMap {
          case Right(lefted) => Ok(lefted.asJson)
          case _ => NotFound("Room or user not found")
        }
    }

  private def userRoomsEndpoint(roomService: RoomService[F],
                                userService: UserService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "users" / "rooms" / LongVar(userId) =>
        userService.getUser(User.Id @@ userId).value.flatMap {
          case Right(user) => roomService.getUserRooms(user).flatMap(rooms => Ok(rooms))
          case Left(UserNotFoundError) => NotFound("User not found")
        }
    }

  private def deleteRoomEndpoint(roomService: RoomService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case DELETE -> Root / "rooms" / LongVar(roomId) =>
        roomService.deleteRoom(Room.Id @@ roomId).flatMap(_ => Ok())
    }

  def endpoints(roomService: RoomService[F], userService: UserService[F]): HttpRoutes[F] =
    createRoomEndpoint(roomService, userService) <+>
    joinRoomEndpoint(roomService, userService) <+>
    leaveRoomEndpoint(roomService, userService) <+>
    userRoomsEndpoint(roomService, userService) <+>
    deleteRoomEndpoint(roomService)
}

object RoomEndpoints {
  def endpoints[F[_]: Effect](roomService: RoomService[F],
                              userService: UserService[F]): HttpRoutes[F] =
    new RoomEndpoints[F].endpoints(roomService, userService)
}
