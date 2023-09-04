package appname.auth

import io.circe.generic.JsonCodec

object HttpModel {
  @JsonCodec
  final case class Login_IN(login: String, password: String)

  @JsonCodec
  final case class Register_IN(login: String, password: String)
}
