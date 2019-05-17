package chat.api

import config._
import cats.effect._
import cats.implicits._
import chat.api.config.ChatConfig
import chat.api.domain.messages.{Message, MessageService}
import chat.api.domain.rooms.{RoomService, RoomValidationInterpreter}
import chat.api.domain.users.{UserService, UserValidationInterpreter}
import chat.api.infrastructure.endpoint.{MessageEndpoints, RoomEndpoints, UserEndpoints, WebsocketEndpoints}
import chat.api.infrastructure.repository.doobie.{DoobieMessageRepositoryInterpreter, DoobieRoomRepositoryInterpreter, DoobieUserRepositoryInterpreter}
import doobie.util.ExecutionContexts
import fs2.concurrent.Topic
import io.circe.config.parser
import org.http4s.server.{Router, Server => H4Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import tsec.passwordhashers.jca.BCrypt

object Server extends IOApp {
  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer]: Resource[F, H4Server[F]] = for {
    conf           <- Resource.liftF(parser.decodePathF[F, ChatConfig]("chat-api"))
    messageTopic   <- Resource.liftF(Topic[F, Message](Message.empty))
    connectEc      <- ExecutionContexts.fixedThreadPool(conf.db.connections.poolSize)
    transactEc     <- ExecutionContexts.cachedThreadPool[F]
    xa             <- DatabaseConfig.dbTransactor(conf.db, connectEc, transactEc)

    userRepo       = DoobieUserRepositoryInterpreter[F](xa)
    roomRepo       = DoobieRoomRepositoryInterpreter[F](xa)
    messageRepo    = DoobieMessageRepositoryInterpreter[F](xa)

    userValidation = UserValidationInterpreter[F](userRepo)
    roomValidation = RoomValidationInterpreter[F](roomRepo)

    roomService    = RoomService[F](roomRepo, roomValidation)
    userService    = UserService[F](userRepo, userValidation)
    messageService = MessageService[F](messageRepo, userValidation, roomValidation, roomRepo, messageTopic)

    services       = RoomEndpoints.endpoints[F](roomService, userService) <+>
      MessageEndpoints.endpoints[F](messageService, userService, roomService) <+>
      UserEndpoints.endpoints[F, BCrypt](userService, BCrypt.syncPasswordHasher[F]) <+>
      WebsocketEndpoints.endpoints[F](messageService)

    httpApp  = Router("/" -> services).orNotFound
    _ <- Resource.liftF(DatabaseConfig.initializeDb(conf.db))
    server <- BlazeServerBuilder[F]
      .withWebSockets(true)
      .bindHttp(conf.server.port, conf.server.host)
      .withHttpApp(httpApp)
      .resource

  } yield server

  def run(args: List[String]): IO[ExitCode] = createServer.use(_ => IO.never).as(ExitCode.Success)
}