package appname.web

import appname.web.auth.LoginFormComponent
import appname.web.auth.RegisterFormComponent
import com.raquo.laminar.api.L._
import org.scalajs.dom
import com.raquo.airstream.ownership.ManualOwner
import io.laminext.fetch.FetchResponse

object App {

  private def setAuthToken(apiTokenWriter: Observer[Option[String]]): Unit = {
    Option(dom.window.localStorage.getItem("apitoken")).foreach { token =>
      apiTokenWriter.onNext(Some(token))
    }
  }

  private def setAutoLogin(
      authTokenSignal: Signal[Option[String]]
  )(implicit owner: Owner): Unit = {
    authTokenSignal
      .changes
      .collect { case Some(apiToken) =>
        apiToken
      }
      .flatMap { token =>
        HttpClient.requestUserInfo(token)
      }
      .addObserver(
        Observer[FetchResponse[String]] { resp =>
          dom.console.log(resp.data)
        }
      )
  }

  def create(): Element = {
    val userAuthToken: Var[Option[String]] = Var(None)
    val globalOwner: ManualOwner = new ManualOwner

    setAuthToken(userAuthToken.writer)
    setAutoLogin(userAuthToken.signal)(globalOwner)

    val loginForm: HtmlElement = LoginFormComponent.create(Observer.empty)
    val registerForm: HtmlElement = RegisterFormComponent.create(Observer.empty)

    div(h1("Login"), loginForm, h1("Register"), registerForm)
  }

}
