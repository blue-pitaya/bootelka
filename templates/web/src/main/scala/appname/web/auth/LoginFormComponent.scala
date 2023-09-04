package appname.web.auth

import appname.auth.HttpModel
import appname.web.HttpClient
import com.raquo.laminar.api.L._
import io.laminext.fetch.circe._

object LoginFormComponent {
  sealed trait Event
  final case class LoggedIn(authToken: String) extends Event

  def create(handler: Observer[Event]): HtmlElement = {
    val login: Var[String] = Var("")
    val password: Var[String] = Var("")
    val status: Var[String] = Var("No status")
    val eventBus: EventBus[Event] = new EventBus

    val loginResponseObserver = Observer[FetchResponse[String]] { resp =>
      if (resp.ok) {
        status.set("logged in")
        eventBus.emit(LoggedIn("???"))
      } else status.set(resp.data)
    }

    div(
      form(
        action := "javascript:void(0);",
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
          "Log in",
          onClick.compose(
            _.mapToUnit
              .withCurrentValueOf(login)
              .withCurrentValueOf(password)
              .flatMap { case (login, password) =>
                val data = HttpModel.Login_IN(login, password)
                HttpClient.requestLogin(data)
              }
          ) --> loginResponseObserver
        ),
        div(child.text <-- status)
      )
    )

  }
}
