package appname.api

import appname.api.auth.AuthService
import cats.effect._
import cats.syntax.all._
import com.comcast.ip4s._
import org.http4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware._
import appname.api.auth.persistence.RamBackend
import appname.api.auth.RandomBytes
import java.security.SecureRandom
import appname.api.auth.persistence.PersistenceBackend

object Server {

  private def routes(
      random: RandomBytes,
      backend: PersistenceBackend
  ): HttpRoutes[IO] = {
    import Routes.dsl._

    val mainRoutes = Routes.main
    val authRoutes = AuthService
      .routes(basePath = Root / "user", backend = backend, random = random)

    mainRoutes <+> authRoutes
  }

  def create(): IO[Resource[IO, Unit]] = {

    val random = for {
      secureRandomInstanceRef <- Ref
        .of[IO, SecureRandom](SecureRandom.getInstance("NativePRNGNonBlocking"))
      random = new RandomBytes {
        override def nextBytes(n: Int): IO[Array[Byte]] = {
          for {
            random <- secureRandomInstanceRef.get
            array <- IO(new Array[Byte](32))
            _ <- secureRandomInstanceRef.update(r => {
              r.nextBytes(array)
              r
            })
          } yield (array)
        }
      }
    } yield (random)

    for {
      _random <- random
      persistanceBackend <- RamBackend.create()
      httpApp = {
        // FIXME: unsafe CORS rule for production
        val corsService = CORS
          .policy
          .withAllowOriginAll(routes(_random, persistanceBackend))
          .orNotFound
        Logger.httpApp(true, true)(corsService)
      }
      server = EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(httpApp)
        .build
        .map(_ => ())
    } yield (server)
  }

}
