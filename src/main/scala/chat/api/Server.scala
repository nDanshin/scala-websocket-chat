package chat.api

import config._
import cats.effect._
import cats.implicits._
import chat.api.config.ChatConfig
import chat.api.domain.users.{UserRepositoryAlgebra, UserService, UserValidationInterpreter}
import chat.api.infrastructure.endpoint.UserEndpoints
import io.circe.config.parser
import org.http4s.server.{Router, Server => H4Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import tsec.passwordhashers.jca.BCrypt

object Server extends IOApp {
  def createServer[F[_] : ContextShift : ConcurrentEffect : Timer]: Resource[F, H4Server[F]] =
    for {
      conf           <- Resource.liftF(parser.decodePathF[F, ChatConfig]("chat-api"))
      /*connEc   <- ???
      txnEc    <- ???
      roomRepo =  ???
      messagesRepo =  ???
      */
      userRepo       = ??? : UserRepositoryAlgebra[F]
      userValidation = UserValidationInterpreter[F](userRepo)
      userService    = UserService[F](userRepo, userValidation)
      services       = UserEndpoints.endpoints[F, BCrypt](userService, BCrypt.syncPasswordHasher[F])
      httpApp  = Router("/" -> services).orNotFound

      server <- BlazeServerBuilder[F]
        .bindHttp(conf.server.port, conf.server.host)
        .withHttpApp(httpApp)
        .resource
    } yield server

  def run(args: List[String]): IO[ExitCode] = createServer.use(_ => IO.never).as(ExitCode.Success)
}