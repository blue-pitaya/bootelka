package appname.api.auth

import appname.api.auth.persistence.PersistenceBackend
import appname.auth.HttpModel
import cats.effect._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._

case class ApiToken(value: String)

trait RandomBytes {
  def nextBytes(n: Int): IO[Array[Byte]]
}

object AuthService {
  import appname.api.Routes.dsl._

  private val ApiTokenSizeInBytes = 32
  private val LoginFailedError = appname
    .HttpModel
    .Error("Invalid username or password.")

  def routes(
      basePath: Uri.Path,
      backend: PersistenceBackend,
      random: RandomBytes
  ): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> basePath / "login" => handleLogin(req, backend, random)
    case req @ POST -> basePath / "register" => Ok("register")
    case req @ GET -> basePath / "logout"    => Ok("logout")
  }

  def handleLogin(
      request: Request[IO],
      backend: PersistenceBackend,
      random: RandomBytes
  ): IO[Response[IO]] = for {
    data <- request.as[HttpModel.Login_IN]
    userOpt <- backend.findByLoginAndPassword(data.login, data.password)
    resp <- userOpt
      .map { user =>
        for {
          apiToken <- createApiToken(random)
          _ <- backend.saveApiToken(apiToken, user.id)
          data = HttpModel.Login_OUT(apiToken.value)
          resp <- Ok(data)
        } yield (resp)
      }
      .getOrElse(Forbidden(LoginFailedError))
  } yield (resp)

  private def createApiToken(random: RandomBytes): IO[ApiToken] = for {
    tokenBytes <- random.nextBytes(ApiTokenSizeInBytes)
    tokenValue = tokenBytes.map(b => String.format("%02x", b)).mkString
    token = ApiToken(value = tokenValue)
  } yield (token)

}
