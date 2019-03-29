package chat.api.infrastructure.endpoint

import cats.data.EitherT
import cats.effect.Effect
import cats.implicits.toFunctorOps
import cats.implicits.toFlatMapOps
import cats.implicits.catsSyntaxApplicativeId
//import cats.implicits.toSemigroupKOps for route binding
import cats.implicits.toSemigroupKOps
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}

import scala.language.higherKinds
import chat.api.domain._
import chat.api.domain.authentification._
import chat.api.domain.users._
import tsec.common.Verified
import tsec.passwordhashers.{PasswordHash, PasswordHasher}

class UserEndpoints[F[_]: Effect, A] extends Http4sDsl[F] {

  implicit val userDecoder: EntityDecoder[F, User] = jsonOf
  implicit val loginRequestDecoder: EntityDecoder[F, LoginRequest] = jsonOf
  implicit val signUpRequestDecoder: EntityDecoder[F, SignupRequest] = jsonOf

  private def loginEndpoint(userService: UserService[F], cryptService: PasswordHasher[F, A]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "login" =>
        val action: EitherT[F, UserAuthenticationFailedError, User] = for {
          login <- EitherT.liftF(req.as[LoginRequest])
          name = login.userName
          user <- userService.getUserByName(name).leftMap(_ => UserAuthenticationFailedError(name))
          checkResult <- EitherT.liftF(cryptService.checkpw(login.password, PasswordHash[A](user.hash)))
          resp <-
            if (checkResult == Verified) EitherT.rightT[F, UserAuthenticationFailedError](user)
            else EitherT.leftT[F, User](UserAuthenticationFailedError(name))
        } yield resp

        action.value.flatMap {
          case Right(user) => Ok(user.asJson)
          case Left(UserAuthenticationFailedError(name)) => BadRequest(s"Authentication failed for user $name")
        }
    }

  private def signupEndpoint(userService: UserService[F], cryptService: PasswordHasher[F, A]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "users" =>
        val action = for {
          signup <- req.as[SignupRequest]
          hash <- cryptService.hashpw(signup.password)
          createUser <- signup.asCreateUser(hash).pure[F]
          result <- userService.createUser(createUser).value
        } yield result

        action.flatMap {
          case Right(saved) => Ok(saved.asJson)
          case Left(UserAlreadyExistsError(existing)) =>
            Conflict(s"The user with user name ${existing.userName} already exists")
        }
    }

  private def updateEndpoint(userService: UserService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ PUT -> Root / "users" / name =>
        val action = for {
          user <- req.as[User]
          updated = user.copy(userName = name)
          result <- userService.update(updated).value
        } yield result

        action.flatMap {
          case Right(saved) => Ok(saved.asJson)
          case Left(UserNotFoundError) => NotFound("User not found")
        }
    }

  private def searchByNameEndpoint(userService: UserService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "users" / userName =>
        userService.getUserByName(userName).value.flatMap {
          case Right(found) => Ok(found.asJson)
          case Left(UserNotFoundError) => NotFound("The user was not found")
        }
    }

  private def deleteUserEndpoint(userService: UserService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case DELETE -> Root / "users" / userName =>
        userService.deleteUserByName(userName).flatMap(_ => Ok())
    }

  def endpoints(userService: UserService[F], cryptService: PasswordHasher[F, A]): HttpRoutes[F] =
    loginEndpoint(userService, cryptService) <+>
    signupEndpoint(userService, cryptService) <+>
    updateEndpoint(userService) <+>
    searchByNameEndpoint(userService) <+>
    deleteUserEndpoint(userService)
}

object UserEndpoints {
  def endpoints[F[_]: Effect, A](userService: UserService[F],
                                 cryptService: PasswordHasher[F, A]): HttpRoutes[F] =
    new UserEndpoints[F, A].endpoints(userService, cryptService)
}
