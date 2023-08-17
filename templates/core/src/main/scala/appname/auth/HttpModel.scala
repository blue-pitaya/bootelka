package appname.auth

object HttpModel {
  final case class Login_IN(login: String, password: String)
  final case class Login_OUT(apiKey: String)
}
