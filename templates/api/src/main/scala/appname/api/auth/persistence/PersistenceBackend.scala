package appname.api.auth.persistence

import cats.effect._
import appname.auth.Model
import appname.api.auth.ApiToken

trait PersistenceBackend {
  def findByLoginAndPassword(
      login: String,
      password: String
  ): IO[Option[Model.User]]
  def saveApiToken(apiToken: ApiToken, userId: Long): IO[Unit]
}
