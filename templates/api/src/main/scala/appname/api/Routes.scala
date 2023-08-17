package appname.api

import cats.effect._
import org.http4s._
import org.http4s.dsl._

object Routes {
  implicit val dsl = new Http4sDsl[IO] {}
  import dsl._

  val main: HttpRoutes[IO] = HttpRoutes.of[IO] { case req @ GET -> Root =>
    Ok("Hello!")
  }

}
