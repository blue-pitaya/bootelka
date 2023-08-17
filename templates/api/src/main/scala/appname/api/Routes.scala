package appname.api

import cats.effect._
import org.http4s._

object Routes {
  // for global use
  implicit val dsl = new org.http4s.dsl.Http4sDsl[IO] {}

  val main: HttpRoutes[IO] = {
    import dsl._

    HttpRoutes.of[IO] { case req @ GET -> Root =>
      Ok("Hello!")
    }
  }

}
