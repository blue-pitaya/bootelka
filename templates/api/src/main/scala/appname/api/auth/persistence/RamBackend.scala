package appname.api.auth.persistence

import appname.api.auth.ApiToken
import appname.auth.Model
import cats.effect.IO
import cats.effect.kernel.Ref
import monocle.syntax.all._

import java.time.Instant

// Store information about users in RAM (good for development)
object RamBackend {
  case class UserRecord(login: String, password: String)
  case class ApiTokenRecord(
      token: String,
      userId: Long,
      expires: Option[Instant]
  )
  case class State(
      apiTokenNextId: Long,
      apiTokens: Map[Long, ApiTokenRecord],
      userNextId: Long,
      users: Map[Long, UserRecord]
  )
  object State {
    val empty = State(
      apiTokenNextId = 1,
      apiTokens = Map(),
      userNextId = 2,
      users = Map(1L -> UserRecord(login = "test", password = "test"))
    )
  }

  def create(): IO[PersistenceBackend] = for {
    stateRef <- Ref.of[IO, State](State.empty)
    backend <- IO(
      new PersistenceBackend {

        override def findByLoginAndPassword(
            login: String,
            password: String
        ): IO[Option[Model.User]] = for {
          state <- stateRef.get
          userOpt = state
            .users
            .find { case (_, user) =>
              user.login == login && user.password == password
            }
            .map { case (id, user) =>
              Model.User(id = id, login = user.login)
            }
        } yield (userOpt)

        override def saveApiToken(apiToken: ApiToken, userId: Long): IO[Unit] =
          for {
            _ <- stateRef.update { state =>
              val record = ApiTokenRecord(apiToken.value, userId, None)
              state
                .focus(_.apiTokens)
                .modify(_.updated(state.apiTokenNextId, record))
                .focus(_.apiTokenNextId)
                .modify(_ + 1)
            }
            // TODO: log current state after operation (for dev) or just test it :)
          } yield ()

      }
    )
  } yield (backend)
}
