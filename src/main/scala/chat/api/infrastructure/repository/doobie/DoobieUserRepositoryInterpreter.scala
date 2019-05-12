package chat.api.infrastructure.repository.doobie

import cats.Monad
import cats.data.OptionT
import cats.syntax.functor._
import chat.api.domain.users.{CreateUser, User, UserRepositoryAlgebra}
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.Update0

private object UserSQL {

  def insert(user: CreateUser): Update0 = sql"""
      insert into users (user_name, first_name, last_name, email, hash)
      values (${user.userName}, ${user.firstName}, ${user.lastName}, ${user.email}, ${user.hash})
    """.update

  def update(user: User): Update0 = sql"""
      update users
      set first_name = ${user.firstName}, last_name = ${user.lastName}, email = ${user.email}
      where id = ${user.id}
    """.update

  def select(userId: User.Id): Query0[User] = sql"""
    select user_name, first_name, last_name, email, hash, id
    from users
    where id = $userId
  """.query

  def byUserName(userName: String): Query0[User] = sql"""
    select user_name, first_name, last_name, email, hash, id
    from users
    where user_name = $userName
  """.query

  def delete(userId: User.Id): Update0 = sql"""
    delete from users where id = $userId
  """.update
}

class DoobieUserRepositoryInterpreter[F[_]: Monad](xa: Transactor[F]) extends UserRepositoryAlgebra[F] {
  import UserSQL._

  override def create(user: CreateUser): F[User] =
    insert(user)
      .withUniqueGeneratedKeys[Long]("id")
      .map(id => User(user.userName, user.firstName, user.lastName, user.email, user.hash, User.Id @@ id))
      .transact(xa)

  override def update(user: User): F[Option[User]] =
    OptionT.liftF(UserSQL.update(user).run.transact(xa).as(user)).value

  override def get(userId: User.Id): F[Option[User]] = select(userId).option.transact(xa)

  override def delete(userId: User.Id): F[Option[User]] =
    OptionT(get(userId))
      .semiflatMap(user => UserSQL.delete(user.id).run.transact(xa).as(user))
      .value

  override def findByUserName(userName: String): F[Option[User]] = byUserName(userName).option.transact(xa)

  override def deleteByUserName(userName: String): F[Option[User]] =
    OptionT(findByUserName(userName))
      .semiflatMap(user => UserSQL.delete(user.id).run.transact(xa).as(user))
      .value
}

object DoobieUserRepositoryInterpreter {
  def apply[F[_]: Monad](xa: Transactor[F]): DoobieUserRepositoryInterpreter[F] =
    new DoobieUserRepositoryInterpreter[F](xa)
}