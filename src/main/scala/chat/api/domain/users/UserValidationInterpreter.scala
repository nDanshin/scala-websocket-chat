package chat.api.domain.users

import cats.Monad
import cats.data.EitherT
import cats.implicits.toFunctorOps
import cats.implicits.catsSyntaxApplicativeId
import cats.syntax.either._
import chat.api.domain.{UserAlreadyExistsError, UserNotFoundError}

class UserValidationInterpreter[F[_]: Monad](userRepo: UserRepositoryAlgebra[F]) extends UserValidationAlgebra[F] {
  override def doesNotExist(user: User): EitherT[F, UserAlreadyExistsError, Unit] = EitherT {
    userRepo.findByUserName(user.userName).map {
      case None => Right(())
      case Some(_) => Left(UserAlreadyExistsError(user))
    }
  }

  override def exists(userId: Option[Long]): EitherT[F, UserNotFoundError.type, Unit] = EitherT {
    userId.map { id =>
      userRepo.get(id).map {
        case Some(_) => Right(())
        case _ => Left(UserNotFoundError)
      }
    }.getOrElse(Either.left[UserNotFoundError.type, Unit](UserNotFoundError).pure[F])
  }
}

object UserValidationInterpreter {
  def apply[F[_]: Monad](repo: UserRepositoryAlgebra[F]): UserValidationInterpreter[F] =
    new UserValidationInterpreter[F](repo)
}
