package chat.api.domain.authentification

import chat.api.domain.users.CreateUser
import tsec.passwordhashers.PasswordHash

final case class LoginRequest(userName: String,
                              password: String)

final case class SignupRequest(userName: String,
                               firstName: String,
                               lastName: String,
                               email: String,
                               password: String) {

  def asCreateUser[A](hashedPassword: PasswordHash[A]): CreateUser = CreateUser(
    userName = userName,
    firstName = firstName,
    lastName = lastName,
    email = email,
    hash = hashedPassword
  )
}
