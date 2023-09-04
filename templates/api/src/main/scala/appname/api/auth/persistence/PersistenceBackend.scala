package appname.api.auth.persistence

import appname.api.auth.ApiToken
import appname.auth.Model
import cats.data.OptionT

trait PersistenceBackend[F[_]] {
  def findByLoginAndPassword(
      login: String,
      password: String
  ): F[Option[Model.User]]
  def saveApiToken(apiToken: ApiToken, userId: Long): F[Unit]
  def findUserByLogin(login: String): F[Option[Model.User]]
  def createUser(login: String, hashedPassword: String): F[Long]
  def deleteApiToken(userId: Long): F[Unit]
  def findUserById(id: Long): OptionT[F, Model.User]
  def findUserIdByApiToken(value: String): OptionT[F, Long]
}
