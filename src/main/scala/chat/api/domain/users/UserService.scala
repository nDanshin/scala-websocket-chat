package chat.api.domain.users

import cats.Monad
import cats.data.EitherT
import cats.syntax.functor._
import chat.api.domain.{UserAlreadyExistsError, UserNotFoundError}

class UserService[F[_]: Monad](repo: UserRepositoryAlgebra[F], validation: UserValidationAlgebra[F]) {

  def createUser(user: CreateUser): EitherT[F, UserAlreadyExistsError, User] = for {
    _ <- validation.doesNotExist(user.userName)
    saved <- EitherT.liftF(repo.create(user))
  } yield saved

  def getUser(userId: User.Id): EitherT[F, UserNotFoundError.type , User] =
    EitherT.fromOptionF(repo.get(userId), UserNotFoundError)

  def getUserByName(userName: String): EitherT[F, UserNotFoundError.type, User] =
    EitherT.fromOptionF(repo.findByUserName(userName), UserNotFoundError)

  def deleteUser(userId: User.Id): F[Unit] = repo.delete(userId).as(())

  def deleteUserByName(userName: String): F[Unit] = repo.deleteByUserName(userName).as(())

  def update(user: User): EitherT[F, UserNotFoundError.type, User] = for {
    _ <- validation.exists(user.id)
    saved <- EitherT.fromOptionF(repo.update(user), UserNotFoundError)
  } yield saved
}

object UserService {
  def apply[F[_]: Monad](repo: UserRepositoryAlgebra[F],
                         validation: UserValidationAlgebra[F]): UserService[F] = new UserService[F](repo, validation)
}
