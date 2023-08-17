package appname.web

import com.raquo.laminar.api.L._
import appname.web.auth.LoginFormComponent

object App {

  def create(): Element = {
    val loginForm = LoginFormComponent
      .create(Observer.empty[LoginFormComponent.Event])

    div(h1("Login"), loginForm)
  }

}
