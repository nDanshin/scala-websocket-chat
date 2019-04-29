package chat.api.infrastructure.endpoint

import cats.effect.{ConcurrentEffect, Timer}
import cats.implicits.catsSyntaxApplicativeId
import chat.api.domain.messages.MessageService
import chat.api.domain.users.User
import fs2.{Pipe, Stream}
import org.http4s._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket._
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame._

import scala.language.higherKinds
import chat.api.domain.messages

class WebsocketEndpoints[F[_]: ConcurrentEffect: Timer] extends Http4sDsl[F] {

  implicit val messageDecoder: EntityDecoder[F, messages.Message] = jsonOf
  implicit val messageEncoder: EntityEncoder[F, messages.Message] = jsonEncoderOf

  private def openWebsocketEndpoint(messageService: MessageService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "ws" =>
        val send: Stream[F, WebSocketFrame] =
          messageService.subscribe(User.Id @@ 1l).map(msg => Text(msg.asJson.toString))

        val receive: Pipe[F, WebSocketFrame, Unit] = _.evalMap {
          case Text(t, _) => println(t).pure[F]
          case f => println(s"Unknown type: $f").pure[F]
        }

        WebSocketBuilder[F].build(send, receive)
    }

  def endpoints(messageService: MessageService[F]): HttpRoutes[F] =
    openWebsocketEndpoint(messageService)
}

object WebsocketEndpoints {
  def endpoints[F[_]: ConcurrentEffect: Timer](messageService: MessageService[F]): HttpRoutes[F] =
    new WebsocketEndpoints[F].endpoints(messageService)
}

//Stream.awakeEvery[F](1.seconds).map(d => Text(s"Ping! $d"))


