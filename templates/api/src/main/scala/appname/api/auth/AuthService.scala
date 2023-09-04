package appname.api.auth

import appname.api.auth.persistence.PersistenceBackend
import appname.auth.HttpModel
import appname.auth.Model
import cats.data._
import cats.effect._
import cats.syntax.all._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.server.AuthMiddleware
import org.http4s.headers.Authorization
import org.http4s.syntax.header._

case class ApiToken(value: String)

trait RandomBytes {
  def nextBytes(n: Int): IO[Array[Byte]]
}

object AuthService {
  import appname.api.Routes.dsl._

  private val ApiTokenSizeInBytes = 32
  private val LoginFailedError = "Invalid username or password."

  def authUser(
      request: Request[IO],
      backend: PersistenceBackend[IO]
  ): OptionT[IO, Model.User] = {
    val authCookieOpt = request.headers.get[Authorization]
    authCookieOpt.map { a =>
      a.value
    }

    println(authCookieOpt)

    for {
      userId <- authCookieOpt match {
        case None => OptionT.none[IO, Long]
        case Some(authCookie) =>
          backend.findUserIdByApiToken(authCookie.content)
      }
      user <- backend.findUserById(userId)
    } yield (user)
  }

  def routes(
      basePath: Uri.Path,
      backend: PersistenceBackend[IO],
      random: RandomBytes,
      handleRegister: Request[IO] => IO[Response[IO]]
  ): HttpRoutes[IO] = {
    val authMiddleware: AuthMiddleware[IO, Model.User] = AuthMiddleware
      .withFallThrough(Kleisli(req => authUser(req, backend)))

    val authedRoutes: AuthedRoutes[Model.User, IO] = AuthedRoutes.of {
      case GET -> basePath / "logout" as user => handleLogout(user.id, backend)
      case GET -> basePath / "info" as user   => handleGetInfo(user.id, backend)
    }

    val nonAuthedRoutes: HttpRoutes[IO] = HttpRoutes.of {
      case req @ POST -> basePath / "login" => handleLogin(req, backend, random)
      case req @ POST -> basePath / "register" => handleRegister(req)
    }

    authMiddleware(authedRoutes) <+> nonAuthedRoutes
  }

  def handleGetInfo(
      userId: Long,
      backend: PersistenceBackend[IO]
  ): IO[Response[IO]] = backend
    .findUserById(userId)
    .map(_.login)
    .value
    .flatMap {
      case Some(userLogin) => Ok(userLogin)
      case None            => Forbidden()
    }

  def handleLogout(
      userId: Long,
      backend: PersistenceBackend[IO]
  ): IO[Response[IO]] = backend.deleteApiToken(userId) >> Ok()

  def handleLogin(
      request: Request[IO],
      backend: PersistenceBackend[IO],
      random: RandomBytes
  ): IO[Response[IO]] = for {
    data <- request.as[HttpModel.Login_IN]
    userOpt <- backend.findByLoginAndPassword(data.login, data.password)
    resp <- userOpt
      .map { user =>
        for {
          apiToken <- createApiToken(user.id, random, backend)
          resp <- apiTokenResponse(apiToken)
        } yield (resp)
      }
      .getOrElse(Forbidden(LoginFailedError))
  } yield (resp)

  def apiTokenResponse(apiToken: ApiToken): IO[Response[IO]] =
    Ok(apiToken.value)

  def createApiToken(
      userId: Long,
      random: RandomBytes,
      backend: PersistenceBackend[IO]
  ): IO[ApiToken] = {
    for {
      apiToken <- getApiToken(random)
      _ <- backend.saveApiToken(apiToken, userId)
    } yield (apiToken)
  }

  private def getApiToken(random: RandomBytes): IO[ApiToken] = for {
    tokenBytes <- random.nextBytes(ApiTokenSizeInBytes)
    tokenValue = tokenBytes.map(b => String.format("%02x", b)).mkString
    token = ApiToken(value = tokenValue)
  } yield (token)

}
