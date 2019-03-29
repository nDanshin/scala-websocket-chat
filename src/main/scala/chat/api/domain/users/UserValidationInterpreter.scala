package chat.api.domain.users

import cats.Monad
import cats.data.EitherT
import cats.implicits.toFunctorOps
import chat.api.domain.{UserAlreadyExistsError, UserNotFoundError}

class UserValidationInterpreter[F[_]: Monad](userRepo: UserRepositoryAlgebra[F]) extends UserValidationAlgebra[F] {
  override def doesNotExist(userName: String): EitherT[F, UserAlreadyExistsError, Unit] = EitherT {
    userRepo.findByUserName(userName).map {
      case None => Right(())
      case Some(user) => Left(UserAlreadyExistsError(user))
    }
  }

  override def exists(userId: Long): EitherT[F, UserNotFoundError.type, Unit] = EitherT {
    userRepo.get(userId).map {
      case Some(_) => Right(())
      case _ => Left(UserNotFoundError)
    }
  }
}

object UserValidationInterpreter {
  def apply[F[_]: Monad](repo: UserRepositoryAlgebra[F]): UserValidationInterpreter[F] =
    new UserValidationInterpreter[F](repo)
}
