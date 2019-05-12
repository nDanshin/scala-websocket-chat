package chat.api.config

import cats.effect.{Async, ContextShift, Resource, Sync}
import cats.syntax.functor._
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext

case class DatabaseConnectionsConfig(poolSize: Int)

case class DatabaseConfig(url: String,
                          driver: String,
                          user: String,
                          password: String,
                          connections: DatabaseConnectionsConfig)

object DatabaseConfig {
  def dbTransactor[F[_]: Async: ContextShift](cfg: DatabaseConfig,
                                              connectEc: ExecutionContext,
                                              transactEc: ExecutionContext): Resource[F, HikariTransactor[F]] =
    HikariTransactor.newHikariTransactor[F](
      driverClassName = cfg.driver,
      url = cfg.url,
      user = cfg.user,
      pass = cfg.password,
      connectEC = connectEc,
      transactEC = transactEc)

  /**
    * Runs the flyway migrations against the target database
    */
  def initializeDb[F[_]](cfg: DatabaseConfig)(implicit S: Sync[F]): F[Unit] = S.delay {
    Flyway
      .configure()
      .dataSource(cfg.url, cfg.user, cfg.password)
      .load()
      .migrate()
  }.as(())
}
