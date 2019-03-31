package chat.api.domain.users

trait UserRepositoryAlgebra[F[_]] {
  def create(user: CreateUser): F[User]
  def update(user: User): F[Option[User]]
  def get(userId: User.Id): F[Option[User]]
  def delete(userId: User.Id): F[Option[User]]
  def findByUserName(userName: String): F[Option[User]]
  def deleteByUserName(userName: String): F[Option[User]]
}
