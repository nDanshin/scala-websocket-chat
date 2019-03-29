package chat.api.domain.users

import cats.data.EitherT
import chat.api.domain.{UserAlreadyExistsError, UserNotFoundError}

trait UserValidationAlgebra[F[_]] {
  def doesNotExist(userName: String): EitherT[F, UserAlreadyExistsError, Unit]
  def exists(userId: Long): EitherT[F, UserNotFoundError.type, Unit]
}
