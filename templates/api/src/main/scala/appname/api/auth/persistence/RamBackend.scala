package appname.api.auth.persistence

import appname.api.auth.ApiToken
import appname.auth.Model
import cats.effect.IO
import cats.effect.kernel.Ref
import monocle.syntax.all._

import java.time.Instant
import cats.data.OptionT

// Store information about users in RAM (good for development)
object RamBackend {
  case class UserRecord(login: String, password: String) {
    def toUser(id: Long) = Model.User(id = id, login = login)
  }

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

  def create(): IO[PersistenceBackend[IO]] = for {
    stateRef <- Ref.of[IO, State](State.empty)
    backend <- IO(
      new PersistenceBackend[IO] {
        private def findUserBy(
            fn: UserRecord => Boolean,
            state: State
        ): Option[Model.User] = state
          .users
          .find { case (_, user) =>
            fn(user)
          }
          .map { case (id, user) =>
            Model.User(id = id, login = user.login)
          }

        override def findByLoginAndPassword(
            login: String,
            password: String
        ): IO[Option[Model.User]] = for {
          state <- stateRef.get
          userOpt = findUserBy(
            user => user.login == login && user.password == password,
            state
          )
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

        override def findUserByLogin(login: String): IO[Option[Model.User]] =
          for {
            state <- stateRef.get
            userOpt = findUserBy(user => user.login == login, state)
          } yield (userOpt)

        override def createUser(
            login: String,
            hashedPassword: String
        ): IO[Long] = for {
          nextState <- stateRef.updateAndGet { state =>
            val record = UserRecord(login = login, password = hashedPassword)
            state
              .focus(_.users)
              .modify(_.updated(state.userNextId, record))
              .focus(_.userNextId)
              .modify(_ + 1)
          }
          newUserId = nextState.userNextId - 1
        } yield (newUserId)

        override def deleteApiToken(userId: Long): IO[Unit] = for {
          _ <- stateRef.update { state =>
            state
              .focus(_.apiTokens)
              .modify(
                _.filter { case (id, record) =>
                  record.userId != userId
                }
              )
          }
        } yield ()

        override def findUserById(id: Long): OptionT[IO, Model.User] = OptionT(
          for {
            state <- stateRef.get
            userOpt = state.users.get(id).map(_.toUser(id))
          } yield (userOpt)
        )

        override def findUserIdByApiToken(value: String): OptionT[IO, Long] =
          OptionT(
            for {
              state <- stateRef.get
              userIdOpt = state
                .apiTokens
                .find { case (_, apiToken) =>
                  apiToken.token == value
                }
                .map { case (_, apiToken) =>
                  apiToken.userId
                }
            } yield (userIdOpt)
          )

      }
    )
  } yield (backend)
}
