package appname.api.auth

import appname.api.auth.persistence.PersistenceBackend
import appname.auth.HttpModel
import cats.data.EitherT
import cats.effect._
import com.password4j.Argon2Function
import com.password4j.Password
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._

object UserRegister {
  type RegisterIO[A] = EitherT[IO, RegisterError, A]
  type UserId = Long

  sealed trait RegisterError
  final case class ValidationError(msgs: List[String]) extends RegisterError
  final case object UserAlreadyExistError extends RegisterError
}

class UserRegister(backend: PersistenceBackend[IO], random: RandomBytes) {
  import UserRegister._

  def _validate(data: HttpModel.Register_IN): Either[ValidationError, Unit] = {
    // TODO:
    def validateLogin: Either[ValidationError, Unit] = Right()
    def validatePassword: Either[ValidationError, Unit] = Right()

    for {
      _ <- validateLogin
      _ <- validatePassword
    } yield ()
  }

  def handle(request: Request[IO]): IO[Response[IO]] = {
    def createUserInDb(
        login: String,
        hashedPassword: String
    ): RegisterIO[UserId] = EitherT
      .liftF(backend.createUser(login, hashedPassword))

    def validate(data: HttpModel.Register_IN): RegisterIO[Unit] =
      EitherT(IO(_validate(data)))

    def checkIfUserAlreadyExists(login: String): RegisterIO[Unit] = {
      EitherT(
        for {
          userOpt <- backend.findUserByLogin(login)
          res = userOpt match {
            case Some(user) => Left(UserAlreadyExistError)
            case None       => Right()
          }
        } yield (res)
      )
    }

    def hashPassword(password: String): RegisterIO[String] = {
      val MemoryInKib = 12
      val NumberOfIterations = 20
      val LevelOfParallelism = 2
      val LengthOfTheFinalHash = 32
      val Type = com.password4j.types.Argon2.ID
      val Version = 19
      val Argon2: Argon2Function = Argon2Function.getInstance(
        MemoryInKib,
        NumberOfIterations,
        LevelOfParallelism,
        LengthOfTheFinalHash,
        Type,
        Version
      )

      EitherT.right(IO(Password.hash(password).`with`(Argon2).getResult))
    }

    def doRegister(): RegisterIO[ApiToken] = for {
      data <- EitherT.liftF(request.as[HttpModel.Register_IN])
      _ <- validate(data)
      _ <- checkIfUserAlreadyExists(data.login)
      hashedPassword <- hashPassword(data.password)
      userId <- createUserInDb(data.login, hashedPassword)
      apiToken <- EitherT
        .liftF(AuthService.createApiToken(userId, random, backend))
    } yield (apiToken)

    doRegister()
      .value
      .flatMap {
        case Right(apiToken) => AuthService.apiTokenResponse(apiToken)
        case Left(error)     => toResponse(error)
      }
  }

  import appname.api.Routes.dsl._
  private def toResponse(error: RegisterError): IO[Response[IO]] = error match {
    case ValidationError(msgs) => BadRequest(msgs.mkString("\n"))
    case UserAlreadyExistError => BadRequest("User already exists.")
  }
}
