package chat.api.domain.users

import cats.data.EitherT
import chat.api.domain.{UserAlreadyExistsError, UserNotFoundError}

trait UserValidationAlgebra[F[_]] {
  def doesNotExist(userName: String): EitherT[F, UserAlreadyExistsError, Unit]
  def exists(userId: User.Id): EitherT[F, UserNotFoundError.type, Unit]
}
