package organization.templates

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {
  val run = TemplatesServer.run[IO]
}
