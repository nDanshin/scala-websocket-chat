package chat.api.domain.authentification

import chat.api.domain.users.User
import tsec.passwordhashers.PasswordHash

final case class LoginRequest(userName: String,
                              password: String)

final case class SignupRequest(userName: String,
                               firstName: String,
                               lastName: String,
                               email: String,
                               password: String) {

  def asUser[A](hashedPassword: PasswordHash[A]): User = User(
    userName = userName,
    firstName = firstName,
    lastName = lastName,
    email = email,
    hash = hashedPassword,
    id = None
  )
}
