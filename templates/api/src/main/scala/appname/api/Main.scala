package appname.api

import cats.effect._

object Main extends IOApp.Simple {

  override def run: IO[Unit] = for {
    serverResource <- Server.create()
    _ <- serverResource.useForever
  } yield ()

}
