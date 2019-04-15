package chat.api.infrastructure.repository.inmemory

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import chat.api.domain.users.{CreateUser, User, UserRepositoryAlgebra}
import scala.collection.concurrent.TrieMap
import scala.util.Random

class UserRepositoryInMemoryInterpreter[F[_]: Applicative] extends UserRepositoryAlgebra[F] {
  private val cache = new TrieMap[User.Id, User]
  private val random = new Random

  override def create(user: CreateUser): F[User] = {
    val id = User.Id @@ random.nextLong
    val toSave = User(user.userName, user.firstName, user.lastName, user.email, user.hash, id)
    cache += (id -> toSave)
    toSave.pure[F]
  }

  override def update(user: User): F[Option[User]] = cache.replace(user.id, user).map(_ => user).pure[F]

  override def get(userId: User.Id): F[Option[User]] = cache.get(userId).pure[F]

  override def delete(userId: User.Id): F[Option[User]] = cache.remove(userId).pure[F]

  override def findByUserName(userName: String): F[Option[User]] =
    cache.values.find(_.userName == userName).pure[F]

  override def deleteByUserName(userName: String): F[Option[User]] = cache.values
    .find(_.userName == userName)
    .flatMap(user => cache.remove(user.id))
    .pure[F]
}

object UserRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() = new UserRepositoryInMemoryInterpreter[F]()
}
