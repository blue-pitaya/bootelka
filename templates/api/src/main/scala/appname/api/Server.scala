package appname.api

import cats.effect.IO
import cats.effect.kernel.Resource
import com.comcast.ip4s._
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import org.http4s.server.middleware._

object Server {

  def create(): IO[Resource[IO, Unit]] = {
    val routes: HttpRoutes[IO] = ???
    // FIXME: unsafe CORS rule
    val corsService = CORS.policy.withAllowOriginAll(routes).orNotFound
    val finalHttpApp = Logger.httpApp(true, true)(corsService)

    IO(
      EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(finalHttpApp)
        .build
        .map(_ => ())
    )
  }

}
