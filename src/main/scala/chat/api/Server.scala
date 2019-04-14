package chat.api

import config._
import cats.effect._
import cats.implicits._
import chat.api.config.ChatConfig
import chat.api.domain.messages.MessageService
import chat.api.domain.rooms.{RoomService, RoomValidationInterpreter}
import chat.api.domain.users.{UserService, UserValidationInterpreter}
import chat.api.infrastructure.endpoint.{MessageEndpoints, RoomEndpoints, UserEndpoints}
import chat.api.infrastructure.repository.inmemory.{MessageRepositoryInMemoryInterpreter, RoomRepositoryInMemoryInterpreter, UserRepositoryInMemoryInterpreter}
import io.circe.config.parser
import org.http4s.server.{Router, Server => H4Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import tsec.passwordhashers.jca.BCrypt

object Server extends IOApp {
  def createServer[F[_] : ContextShift : ConcurrentEffect : Timer]: Resource[F, H4Server[F]] = for {
    conf           <- Resource.liftF(parser.decodePathF[F, ChatConfig]("chat-api"))

    roomRepo       = RoomRepositoryInMemoryInterpreter[F]()
    userRepo       = UserRepositoryInMemoryInterpreter[F]()
    messageRepo    = MessageRepositoryInMemoryInterpreter[F]()

    userValidation = UserValidationInterpreter[F](userRepo)
    roomValidation = RoomValidationInterpreter[F](roomRepo)

    roomService    = RoomService[F](roomRepo, roomValidation)
    userService    = UserService[F](userRepo, userValidation)
    messageService = MessageService[F](messageRepo, userValidation, roomValidation)

    services       = RoomEndpoints.endpoints[F](roomService, userService) <+>
      MessageEndpoints.endpoints[F](messageService, userService, roomService) <+>
      UserEndpoints.endpoints[F, BCrypt](userService, BCrypt.syncPasswordHasher[F])

    httpApp  = Router("/" -> services).orNotFound
    server <- BlazeServerBuilder[F]
      .bindHttp(conf.server.port, conf.server.host)
      .withHttpApp(httpApp)
      .resource

    } yield server

  def run(args: List[String]): IO[ExitCode] = createServer.use(_ => IO.never).as(ExitCode.Success)
}