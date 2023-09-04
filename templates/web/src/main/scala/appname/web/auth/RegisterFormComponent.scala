package appname.web.auth

import com.raquo.laminar.api.L._

object RegisterFormComponent {
  sealed trait Event
  final case class RegisterRequested(login: String, password: String)
      extends Event

  def create(handler: Observer[Event]): HtmlElement = {
    val login = Var[String]("")
    val password = Var[String]("")

    div(
      form(
        cls := "flex flex-col",
        label("Login"),
        input(
          typ := "text",
          autoComplete := "username",
          controlled(onInput.mapToValue --> login, value <-- login)
        ),
        label("Password"),
        input(
          typ := "password",
          autoComplete := "current-password",
          controlled(onInput.mapToValue --> password, value <-- password)
        ),
        button(
          "Register",
          onClick.compose(
            _.mapToUnit
              .withCurrentValueOf(login)
              .withCurrentValueOf(password)
              .map { case (login, password) =>
                RegisterRequested(login, password)
              }
          ) --> handler
        )
      )
    )
  }
}
