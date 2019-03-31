package chat.api.infrastructure.endpoint

import cats.data.EitherT
import cats.effect.IO
import chat.api.domain.{UserAlreadyExistsError, UserNotFoundError}
import chat.api.domain.authentification.SignupRequest
import chat.api.domain.users.{CreateUser, User, UserService}
import chat.api.utils.TestProtocol
import io.circe.generic.auto._
import org.http4s.{EntityEncoder, Request, Uri}
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.http4sKleisliResponseSyntax
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.{PasswordHash, PasswordHasher}

class UserEndpointsTest
  extends FlatSpec
    with Matchers
    with MockitoSugar
    with ScalaCheckPropertyChecks
    with TestProtocol
    with Http4sDsl[IO] {

  trait mocks {
    implicit val signupRequestEnc: EntityEncoder[IO, SignupRequest] = jsonEncoderOf
    implicit val userEnc: EntityEncoder[IO, User] = jsonEncoderOf

    val password = "pass"
    val passwordHash = PasswordHash[BCrypt]("hash")
    val user = User("userName", "firstName", "lastName", "test@test.com", passwordHash, 123)

    val userService = mock[UserService[IO]]
    val cryptService = mock[PasswordHasher[IO, BCrypt]]
    val userHttpService = UserEndpoints.endpoints(userService, cryptService).orNotFound
  }

  behavior of "UserEndpoints"

  "signupEndpoint" should "create user" in new mocks {
    val signup = SignupRequest(user.userName, user.firstName, user.lastName, user.email, password)
    val createUser = CreateUser(user.userName, user.firstName, user.lastName, user.email, passwordHash)
    val request = Request(POST, Uri.uri("/users")).withEntity(signup)

    when(cryptService.hashpw(password)).thenReturn(IO(passwordHash))
    when(userService.createUser(createUser)).thenReturn(EitherT.rightT[IO, UserAlreadyExistsError](user))

    val response = userHttpService.run(request).unsafeRunSync

    response.status shouldBe Ok
    testJsonProtocol(response,
      """
        |{
        |  "userName":"userName",
        |  "firstName":"firstName",
        |  "lastName":"lastName",
        |  "email":"test@test.com",
        |  "hash":"hash",
        |  "id":123
        |}
      """.stripMargin)
  }

  "login endpoint" should "login user" in new mocks {}

    it should "update user" in new mocks {
      val request = Request(PUT, Uri.uri("/users/123")).withEntity(user.copy(id = 555))

      when(userService.update(user)).thenReturn(EitherT.rightT[IO, UserNotFoundError.type ](user))

      val response = userHttpService.run(request).unsafeRunSync

      response.status shouldBe Ok
      testJsonProtocol(response,
        """
          |{
          |  "userName":"userName",
          |  "firstName":"firstName",
          |  "lastName":"lastName",
          |  "email":"test@test.com",
          |  "hash":"hash",
          |  "id":123
          |}
        """.stripMargin)
    }

    it should "get user by userName" in new mocks {}

    it should "delete user by userName" in new mocks {}
}
